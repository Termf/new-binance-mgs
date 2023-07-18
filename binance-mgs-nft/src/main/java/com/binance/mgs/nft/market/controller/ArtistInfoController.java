package com.binance.mgs.nft.market.controller;


import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.proxy.ArtistCacheProxy;
import com.binance.mgs.nft.market.vo.ArtistQuerySearchMgsRequest;
import com.binance.mgs.nft.market.vo.ArtistVo;
import com.binance.mgs.nft.market.vo.MarketProductMgsItem;
import com.binance.mgs.nft.market.vo.UserInfoMgsVo;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.nft.assetservice.api.data.vo.ItemsApproveInfo;
import com.binance.nft.common.utils.ObjectUtils;
import com.binance.nft.market.ifae.NftMarketArtistApi;
import com.binance.nft.market.request.ArtistDetailRequest;
import com.binance.nft.market.request.ArtistQuerySearchRequest;
import com.binance.nft.market.vo.MarketProductItem;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.market.vo.artist.HomeArtistVo;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class ArtistInfoController {

    private final NftMarketArtistApi nftMarketArtistApi;

    private final ArtistCacheProxy artistCacheProxy;

    private final ApproveHelper approveHelper;

    private final BaseHelper baseHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @GetMapping("/public/nft/home-artist-detail")
    public CommonRet<ArtistVo> homeArtist(@RequestParam("artistId") String userIdStr, HttpServletRequest httpServletRequest) {
        Long userId;
        if (NumberUtils.isDigits(userIdStr)){
            userId = Long.parseLong(userIdStr);
        }else{
            try {
                userId = Long.parseLong(AesUtil.decrypt(userIdStr, AES_PASSWORD));
            }catch (Exception e){
                throw new BusinessException(TradeErrorCode.PARAM_ERROR);
            }
        }

        ArtistDetailRequest request = ArtistDetailRequest.builder()
                .userId(userId).gray(isGray(httpServletRequest))
                .build();
        APIResponse<HomeArtistVo> detail = nftMarketArtistApi.findArtistDetailByUserId(
                APIRequest.instance(request));
        baseHelper.checkResponse(detail);
        ArtistVo vo = new ArtistVo();
        BeanUtils.copyProperties(detail.getData(), vo);
        return new CommonRet(vo);
    }

    @PostMapping({"/public/nft/artist-product-list", "/friendly/nft/artist-product-list"})
    public CommonRet<SearchResult<MarketProductMgsItem>> artistProductList(@Valid @RequestBody ArtistQuerySearchMgsRequest requestMgs) {
        Long creatorId;
        if (NumberUtils.isDigits(requestMgs.getCreatorId())){
            creatorId = Long.parseLong(requestMgs.getCreatorId());
        }else{
            try {
                creatorId = Long.parseLong(AesUtil.decrypt(requestMgs.getCreatorId(), AES_PASSWORD));
            } catch (Exception e) {
                throw new BusinessException(TradeErrorCode.PARAM_ERROR);
            }
        }

        ArtistQuerySearchRequest request = new ArtistQuerySearchRequest();
        BeanUtils.copyProperties(requestMgs, request);
        request.setCreatorId(creatorId);

        SearchResult<MarketProductItem> resp = artistCacheProxy.productList(request);
        SearchResult<MarketProductMgsItem> result = new SearchResult<>();
        Long timestamp = DateUtils.getNewUTCTimeMillis();
        if (CollectionUtils.isNotEmpty(resp.getRows())) {
            List<MarketProductMgsItem> mgsItemList = new ArrayList<>(resp.getRows().size());
            resp.getRows().forEach(item -> {
                        item.setTimestamp(timestamp);
                        MarketProductMgsItem mgsItem = new MarketProductMgsItem();
                        BeanUtils.copyProperties(item, mgsItem);

                        if (!ObjectUtils.isEmpty(item.getOwner())) {
                            UserInfoMgsVo owner = CopyBeanUtils.fastCopy(item.getOwner(), UserInfoMgsVo.class);

                            if (!ObjectUtils.isEmpty(item.getOwner().getUserId())) {
                                owner.setUserId(AesUtil.encrypt(item.getOwner().getUserId().toString(), AES_PASSWORD));
                            }
                            mgsItem.setOwner(owner);
                        }
                        if (!ObjectUtils.isEmpty(item.getCreator())) {
                            UserInfoMgsVo creator = CopyBeanUtils.fastCopy(item.getCreator(), UserInfoMgsVo.class);
                            if (!ObjectUtils.isEmpty(item.getCreator().getUserId())) {
                                creator.setUserId(AesUtil.encrypt(item.getCreator().getUserId().toString(), AES_PASSWORD));
                            }
                            mgsItem.setCreator(creator);
                        }
                        mgsItemList.add(mgsItem);
                    }
            );
            result.setRows(mgsItemList);
            result.setTotal(resp.getTotal());

            if (CollectionUtils.isNotEmpty(result.getRows())) {
                List<Long> productIdList = result.getRows().stream()
                        .filter(p -> Objects.equals(p.getNftType(), 1))
                        .map(MarketProductMgsItem::getProductId)
                        .collect(Collectors.toList());
                Map<Long, ItemsApproveInfo> approveInfoMap = approveHelper.queryApproveInfoMap(productIdList, baseHelper.getUserId());
                result.getRows().forEach(item -> {
                    item.setTimestamp(timestamp);
                    ItemsApproveInfo approveInfo = approveInfoMap.get(item.getProductId());
                    if (approveInfo != null) {
                        item.setApprove(UserApproveInfo.builder()
                                .approve(approveInfo.isApprove())
                                .count(approveInfo.getCount())
                                .build());
                    }
                });
            }
        }
        return new CommonRet<>(result);
    }

    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);

    }
}
