package com.binance.mgs.nft.market.proxy;

import com.binance.master.constant.Constant;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.market.utils.UrlUtills;
import com.binance.mgs.nft.market.vo.XrExhibitionExtItem;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.nft.market.ifae.XrSpaceApi;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.market.vo.xrspace.XrExhibitionListVo;
import com.binance.nft.market.vo.xrspace.XrRoomListVo;
import com.binance.nft.market.vo.xrspace.XrSpaceListVo;
import com.binance.nft.tradeservice.api.IProductApi;
import com.binance.nft.tradeservice.request.ProductDetailRequest;
import com.binance.nft.tradeservice.response.ProductDetailResponse;
import com.binance.nft.tradeservice.vo.NftInfoVo;
import com.binance.nft.tradeservice.vo.ProductDetailVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class XrSpaceCacheProxy {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final XrSpaceApi xrSpaceApi;

    private final IProductApi productApi;

    private final BaseHelper baseHelper;

    private final ApproveHelper approveHelper;

    @ApolloJsonValue("${nft.xrspace.detail.url:}")
    private Map<Integer, String> detailUrlMap;

    private final Cache<Integer, XrSpaceListVo> xrSpaceListCache =
            CacheBuilder.newBuilder()
                    .initialCapacity(2)
                    .maximumSize(2)
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .build();
    private final Cache<Long, XrRoomListVo> xrRoomListCache =
            CacheBuilder.newBuilder()
                    .initialCapacity(1)
                    .maximumSize(100)
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .build();

    private final Cache<Long, Map<Long, XrExhibitionListVo>> xrExhibitionListCache =
            CacheBuilder.newBuilder()
                    .initialCapacity(1)
                    .maximumSize(100)
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .build();

    public XrSpaceListVo xrspaceList() {
        return xrSpaceListCache.getIfPresent(0);
    }

    public XrRoomListVo roomList(Long spaceNo) {
        return xrRoomListCache.getIfPresent(spaceNo);
    }


    public XrExhibitionListVo exhibitionList(Long spaceNo, Long roomNo) {
        return Optional.of(xrExhibitionListCache.getIfPresent(spaceNo)).map(m -> m.get(roomNo)).orElse(null);
    }



    private XrSpaceListVo getXrspaceList() {
        APIResponse<XrSpaceListVo> response = xrSpaceApi.xrspaceList();
        if(!baseHelper.isOk(response)) {
            log.error("xrspaceList error {}", response);
            return null;
        }
        return response.getData();
    }


    private XrRoomListVo getRoomList(Long spaceNo) {
        APIResponse<XrRoomListVo> response = xrSpaceApi.roomList(spaceNo);
        if(!baseHelper.isOk(response)) {
            log.error("roomList error {}", response);
            return null;
        }
        return response.getData();
    }



    private XrExhibitionListVo getExhibitionList(Long spaceNo, Long roomNo) {
        APIResponse<XrExhibitionListVo> response = xrSpaceApi.exhibitionList(spaceNo, roomNo);
        if(!baseHelper.isOk(response)) {
            log.warn("exhibitionList {}", response);
            return null;
        }
        XrExhibitionListVo exhibitionListVo = response.getData();
        if(exhibitionListVo == null ||CollectionUtils.isEmpty(exhibitionListVo.getItemList())) {
            return null;
        }

        List<XrExhibitionListVo.XrExhibitionItem> itemList = new ArrayList<>(exhibitionListVo.getItemList().size());
        exhibitionListVo.getItemList().stream().map(item -> {
            ProductDetailRequest request = new ProductDetailRequest();
            request.setProductId(item.getRelateId());
            try {
                APIResponse<ProductDetailResponse> detailResp = productApi.detail(APIRequest.instance(request));
                if(!baseHelper.isOk(detailResp) ||Objects.isNull(detailResp.getData())) {
                    log.error("exhibitionList detail error {}", detailResp);
                    return null;
                }


                ProductDetailResponse detailResponse = detailResp.getData();
                ProductDetailVo detailVo = detailResponse.getProductDetail();
                NftInfoVo nftInfoVo = detailResponse.getNftInfo();

                UserApproveInfo approveInfo = approveHelper.queryApproveInfo(detailVo.getId(), null);

                XrExhibitionExtItem res = CopyBeanUtils.fastCopy(item, XrExhibitionExtItem.class);
                res.setProductId(detailVo.getId());
                res.setTitle(detailVo.getTitle());
                res.setDescription(detailVo.getDescription());
                res.setCreator(nftInfoVo.getCreator());
                res.setOwner(nftInfoVo.getOwner());
                res.setApproveCount(Optional.ofNullable(approveInfo).map(UserApproveInfo::getCount).orElse(0L));
                res.setRawUrl(UrlUtills.getZipUrl(nftInfoVo.getRawUrl()));
                res.setOriginRawUrl(nftInfoVo.getRawUrl());
                res.setOriginCoverUrl(res.getCoverUrl());
                res.setCoverUrl(UrlUtills.getZipUrl(res.getCoverUrl()));
                res.setRawSize(nftInfoVo.getRawSize());
                res.setMediaType(nftInfoVo.getMediaType());
                res.setSpecification(nftInfoVo.getSpecification());
                res.setDuration(nftInfoVo.getDuration());
                res.setUrl(String.format(detailUrlMap.get(detailVo.getNftType()), detailVo.getId()));
                return res;
            } catch (Exception e) {
                log.error("exhibitionList detail error", e);
            }
            return null;
        }).filter(Objects::nonNull)
        .forEach(itemList::add);

        exhibitionListVo.setItemList(itemList);

        return exhibitionListVo;
    }


    private Boolean isGray() {
        String env = WebUtils.getHeader(Constant.GRAY_ENV_HEADER);
        return StringUtils.isNotBlank(env) && !"normal".equals(env);

    }

    @Scheduled(initialDelay = 2000, fixedRate = 5 * 60 * 1000)
    public void autoLoad() {
        long delay = xrSpaceListCache.size() == 0 ? 0 : new SecureRandom().nextInt(30 * 1000);
        executor.schedule(() -> {
            log.info("xrspace autoLoad start");
            Arrays.asList(0,1).stream().forEach(env -> {
                XrSpaceListVo xrSpaceListVo = getXrspaceList();
                if(Objects.isNull(xrSpaceListVo)) return;
                xrSpaceListCache.put(env, xrSpaceListVo);

                ListUtils.emptyIfNull(xrSpaceListVo.getItemList()).stream().forEach(xs -> {
                    XrRoomListVo xrRoomListVo = getRoomList(xs.getSpaceNo());
                    if(Objects.isNull(xrRoomListVo)) return;
                    xrRoomListCache.put(xs.getSpaceNo(), xrRoomListVo);

                    Map<Long, XrExhibitionListVo> roomExhibitionMap = Maps.newHashMap();
                    ListUtils.emptyIfNull(xrRoomListVo.getItemList()).parallelStream().forEach(r -> {
                        XrExhibitionListVo xrExhibitionListVo = getExhibitionList(xs.getSpaceNo(), r.getRoomNo());
                        if(Objects.isNull(xrExhibitionListVo)) return;
                        roomExhibitionMap.put(r.getRoomNo(), xrExhibitionListVo);
                    });

                    xrExhibitionListCache.put(xs.getSpaceNo(), roomExhibitionMap);
                    log.info("xrspace autoLoad end");
                });
            });
        }, delay, TimeUnit.MILLISECONDS);
    }
}
