package com.binance.mgs.nft.mysterybox.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.mgs.nft.mysterybox.helper.MysteryBoxCacheHelper;
import com.binance.mgs.nft.mysterybox.helper.MysteryBoxI18nHelper;
import com.binance.mgs.nft.mysterybox.vo.MysteryBoxProductDetailIncludeAssetVo;
import com.binance.mgs.nft.mysterybox.vo.MysteryBoxProductDto;
import com.binance.mgs.nft.mysterybox.vo.RarityVo;
import com.binance.mgs.nft.trade.proxy.TradeCacheProxy;
import com.binance.nft.assetservice.api.ICashBalanceApi;
import com.binance.nft.assetservice.api.data.request.UserCashBalanceRequest;
import com.binance.nft.assetservice.api.data.vo.UserCashBalanceVo;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.common.data.MappingProductType;
import com.binance.nft.common.data.ProductStatusMappingHelper;
import com.binance.nft.common.data.ProductTimeHelper;
import com.binance.nft.market.vo.MysteryBoxCollectionForMiniProgram;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.mystery.api.vo.*;
import com.binance.nft.mystery.api.vo.query.MysteryBoxOrderQueryRequest;
import com.binance.nft.tradeservice.dto.LaunchpadConfigDto;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.EnvUtil;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Api(value = "mystery-box", tags = "nft-mystery-box")
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class MysteryBoxController {

    private final NFTMysteryBoxApi nftMysteryBoxApi;

    private final BaseHelper baseHelper;

    private final ICashBalanceApi cashBalanceApi;

    private final MysteryBoxI18nHelper mysteryBoxI18nHelper;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final ConcurrentMap<Long, List<Long>> userIdBlackMap = Maps.newConcurrentMap();

    private final MysteryBoxCacheHelper mysteryBoxCacheHelper;

    private final TradeCacheProxy tradeCacheProxy;

    public static final String GRAY_ENV_HEADER = "X-GRAY-ENV";

    @PostConstruct
    public void initBlackUser() {
        reloadBlackUser();
        scheduledExecutorService.scheduleWithFixedDelay(
                this::reloadBlackUser, 100, 100, TimeUnit.MILLISECONDS
        );
    }

    @SneakyThrows
    public void reloadBlackUser() {
//        try {
//            // fixme 替换成只看可售的商品
//            List<MysteryBoxProductVo> mysteryBoxProductVos = mysteryBoxCacheHelper.getOnSellMysteryBoxProductVo();
//            for (MysteryBoxProductVo mysteryBoxProductVo : mysteryBoxProductVos) {
//                APIResponse<List<Long>> blackListResponse = nftMysteryBoxApi.blackList(APIRequest.instance(mysteryBoxProductVo.getProductId()));
//                baseHelper.checkResponse(blackListResponse);
//                List<Long> longList = blackListResponse.getData();
//                if (org.apache.commons.collections4.CollectionUtils.isEmpty(longList)) {
//                    userIdBlackMap.put(mysteryBoxProductVo.getProductId(), Lists.newArrayList());
//                } else {
//                    userIdBlackMap.put(mysteryBoxProductVo.getProductId(), longList);
//                }
//            }
//        } catch (Exception e) {
//            log.error("reloadBlackUser error:", e);
//        }
    }

    @SneakyThrows
    @GoogleRecaptha("/private/nft/mystery-box/purchase")
    @PostMapping("/private/nft/mystery-box/purchase")
    @UserOperation(eventName = "NFT_Box_Purchase", name = "NFT_Box_Purchase", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<PurchaseMysteryBoxResponse> purchaseMysteryBox(@RequestBody PurchaseMysteryBoxRequest purchaseMysteryBoxRequest) {
        Long userId = baseHelper.getUserId();
        Long productId = purchaseMysteryBoxRequest.getProductId();
        if (Optional.of(userIdBlackMap)
                .map(map -> map.get(productId)).orElse(Collections.emptyList())
                .contains(userId)) {
            throw new BusinessException(NftAssetErrorCode.USER_CLICK_SO_FAST);
        }
        MysteryBoxProductDetailVo productDetailVo = mysteryBoxCacheHelper.getProductDetailVo(productId);
        if (Objects.isNull(productDetailVo)) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        if (Optional.ofNullable(productDetailVo.getIsGray()).orElse(Boolean.FALSE)) {
            if (!isGrayRequest()) {
                throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
            }
            if (!isCurrentGrayEnv()) {
                throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
            }
        }
        Integer limitPerTime = Optional.ofNullable(productDetailVo.getLimitPerTime()).orElse(100);
        Integer number = purchaseMysteryBoxRequest.getNumber();
        if (number > limitPerTime) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }

        purchaseMysteryBoxRequest.setUserId(userId);
        APIResponse<PurchaseMysteryBoxResponse> mysteryBoxResponseAPIResponse = nftMysteryBoxApi.purchaseMysteryBox(
                APIRequest.instance(purchaseMysteryBoxRequest)
        );
        baseHelper.checkResponse(mysteryBoxResponseAPIResponse);
        return new CommonRet<>(mysteryBoxResponseAPIResponse.getData());
    }

    private boolean isCurrentGrayEnv() {
        String envFlag = EnvUtil.getEnvFlag();
        return StringUtils.isNotBlank(envFlag) && "gray".equalsIgnoreCase(envFlag);
    }

    private boolean isGrayRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest httpServletRequest = requestAttributes.getRequest();
            String envKey = httpServletRequest.getHeader(GRAY_ENV_HEADER);
            if (envKey == null) {
                envKey = RpcContext.getContext().get(GRAY_ENV_HEADER);
            }
            return org.apache.commons.lang3.StringUtils.isNoneBlank(envKey) && "gray".equalsIgnoreCase(envKey);
        }
        return false;
    }

    @UserOperation(eventName = "NFT_Mystery_Status", name = "NFT_Mystery_Status",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/mystery-box/purchase-status")
    public CommonRet<PurchaseMysteryBoxStatusResponse> purchaseStatus(@RequestBody PurchaseMysteryBoxStatusRequest purchaseMysteryBoxStatusRequest) {
        purchaseMysteryBoxStatusRequest.setUserId(baseHelper.getUserId());
        APIResponse<PurchaseMysteryBoxStatusResponse> statusResponseAPIResponse = nftMysteryBoxApi.purchaseStatus(
                APIRequest.instance(purchaseMysteryBoxStatusRequest)
        );
        baseHelper.checkResponse(statusResponseAPIResponse);
        return new CommonRet<>(statusResponseAPIResponse.getData());
    }

    @GetMapping("/public/nft/mystery-box/list")
    public CommonPageRet<MysteryBoxProductDto> listMysteryBox(Integer page, Integer size,@RequestParam(name = "type",defaultValue = "0") Integer type) {
        try {
            List<MysteryBoxProductVo> mysteryBoxProductVos = mysteryBoxCacheHelper.getMysteryBoxProductVo(isGrayRequest());
            if(size >= 100) {
                List<MysteryBoxProductDto> list = mergeMysteryAndLpd(mysteryBoxProductVos);
                return new CommonPageRet<>(list,list.size());
            }
            if (!CollectionUtils.isEmpty(mysteryBoxProductVos)) {
                List<MysteryBoxProductDto> results = Lists.newArrayList();
                page = Objects.isNull(page) || page <= 0 ? 1 : page;
                size = Objects.isNull(size) || size <= 0 ? 10 : size;
                if ((page - 1) * size <= mysteryBoxProductVos.size()) {
                    List<MysteryBoxProductVo> boxProductVos = mysteryBoxProductVos.subList((page - 1) * size, Math.min(page * size, mysteryBoxProductVos.size()));
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(boxProductVos)) {
                        refreshStatusAndDuration(boxProductVos);
                        if(type == 1) {
                            results = refreshMysteryBoxProductVos(boxProductVos,page,size);
                        }
                        if(CollectionUtils.isEmpty(results)) {
                            results = boxProductVos.stream().map(item -> {
                                MysteryBoxProductDto mysteryBoxProductDto = CopyBeanUtils.fastCopy(item, MysteryBoxProductDto.class);
                                mysteryBoxProductDto.setTimestamp(DateUtils.getNewUTCDate());
                                return mysteryBoxProductDto;

                            }).collect(Collectors.toList());
                        }
                    }
                    return new CommonPageRet<>(
                            results,
                            results.size()
                    );
                }
            }
            return new CommonPageRet<>(
                    Collections.emptyList(),
                    Objects.isNull(mysteryBoxProductVos) ? 0L : mysteryBoxProductVos.size()
            );
        } catch (Exception e) {
            log.error("/public/nft/mystery-box/list due error : ", e);
        }
        return new CommonPageRet<>(
                Collections.emptyList(),
                0L
        );
    }

    private List<MysteryBoxProductDto> refreshMysteryBoxProductVos(List<MysteryBoxProductVo> boxProductVos, Integer page, Integer size) {
        List<LaunchpadConfigDto> launchpadConfigDtos = tradeCacheProxy.queryAllMysteryLpdConfig();
        List<MysteryBoxProductDto> collect = boxProductVos.stream().map(item ->
                {
                    MysteryBoxProductDto mysteryBoxProductDto = CopyBeanUtils.fastCopy(item, MysteryBoxProductDto.class);
                    mysteryBoxProductDto.setConfigType(1);
                    mysteryBoxProductDto.setCompareStartTime(item.getStartTime());
                    mysteryBoxProductDto.setTimestamp(DateUtils.getNewUTCDate());
                    return mysteryBoxProductDto;
                }
        ).collect(Collectors.toList());


        List<MysteryBoxProductDto> items = launchpadConfigDtos.stream()
                .map(item -> {
                    MysteryBoxProductDto mysteryBoxProductDto = new MysteryBoxProductDto();
                    mysteryBoxProductDto.setDto(item);
                    mysteryBoxProductDto.setCompareStartTime(item.getPreparationTime());
                    mysteryBoxProductDto.setConfigType(2);
                    mysteryBoxProductDto.setTimestamp(DateUtils.getNewUTCDate());
                    return mysteryBoxProductDto;
                }).collect(Collectors.toList());
        items.addAll(collect);
        items.sort(Comparator.comparing(MysteryBoxProductDto::getCompareStartTime).reversed());

        return items.subList((page - 1) * size, Math.min(page * size, items.size()));
    }


    @GetMapping("/public/nft/mystery-box/list-collections-for-mini-program")
    public CommonPageRet<MysteryBoxCollectionForMiniProgram> listCollectionsForMiniProgram(Integer page, Integer size) {
        try {
            List<MysteryBoxProductVo> mysteryBoxProductVos = mysteryBoxCacheHelper.getMysteryBoxProductVo(isGrayRequest());
            List<MysteryBoxProductDto> combinedMysteryBoxProductVos = mergeMysteryAndLpd(mysteryBoxProductVos);
            if (CollectionUtils.isEmpty(combinedMysteryBoxProductVos)) {
                return new CommonPageRet<>(Collections.emptyList(), 0L);
            }
            List<Long> serials_no = combinedMysteryBoxProductVos.stream().map(MysteryBoxProductDto::getSerialsNo).collect(Collectors.toList());
            page = Objects.isNull(page) || page <= 0 ? 1 : page;
            size = Objects.isNull(size) || size <= 0 ? 10 : size;
            List<MysteryBoxCollectionForMiniProgram> res = mysteryBoxCacheHelper.getCollectionsDetail(page, size, serials_no);
            return new CommonPageRet<>(res, serials_no.size());
        } catch (Exception e) {
            log.error("listCollectionsForMiniProgram: ", e);
        }
        return new CommonPageRet<>(
                Collections.emptyList(),
                0L
        );
    }

    private List<MysteryBoxProductDto> mergeMysteryAndLpd(List<MysteryBoxProductVo> mysteryBoxProductVos) {
        List<LaunchpadConfigDto> lpdList = tradeCacheProxy.queryAllMysteryLpdConfig();
        if(CollectionUtils.isEmpty(lpdList)) return Lists.newArrayList();

        List<MysteryBoxProductVo> lpdMysteryList = lpdList.stream().map(l -> MysteryBoxProductVo.builder()
                .name(l.getStakingName())
                .serialsNo(l.getCollectionId())
                .startTime(l.getDistributionTime())
                .build())
                .collect(Collectors.toList());
        lpdMysteryList.addAll(mysteryBoxProductVos);

        lpdMysteryList.sort(Comparator.comparing(MysteryBoxProductVo::getStartTime).reversed());
        return lpdMysteryList.stream().map(item -> {
            MysteryBoxProductDto mysteryBoxProductDto = CopyBeanUtils.fastCopy(item, MysteryBoxProductDto.class);
            mysteryBoxProductDto.setTimestamp(DateUtils.getNewUTCDate());
            return mysteryBoxProductDto;
        }).collect(Collectors.toList());
    }


    private void refreshStatusAndDuration(List<MysteryBoxProductVo> mysteryBoxProductVos) {
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(mysteryBoxProductVos)) {
            mysteryBoxProductVos.forEach(mysteryBoxProductVo -> {
                mysteryBoxProductVo.setDuration(ProductTimeHelper.getDuration(mysteryBoxProductVo.getStartTime(), mysteryBoxProductVo.getEndTime()).toString());
                mysteryBoxProductVo.setMappingStatus(ProductStatusMappingHelper.mappingStatus(MappingProductType.MYSTERY_BOX.name(), mysteryBoxProductVo.getStartTime(), mysteryBoxProductVo.getEndTime(), mysteryBoxProductVo.getStatus()));
            });
        }
    }

    private void refreshStatusAndDuration(MysteryBoxProductDetailIncludeAssetVo mysteryBoxProductDetailIncludeAssetVo) {
        mysteryBoxProductDetailIncludeAssetVo.setDuration(ProductTimeHelper.getDuration(mysteryBoxProductDetailIncludeAssetVo.getStartTime(), mysteryBoxProductDetailIncludeAssetVo.getEndTime()).toString());
        mysteryBoxProductDetailIncludeAssetVo.setMappingStatus(ProductStatusMappingHelper.mappingStatus(MappingProductType.MYSTERY_BOX.name(), mysteryBoxProductDetailIncludeAssetVo.getStartTime(), mysteryBoxProductDetailIncludeAssetVo.getEndTime(), mysteryBoxProductDetailIncludeAssetVo.getStatus()));
    }
    private void checkAndSetSecondDelay(MysteryBoxProductDetailVo mysteryBoxProductDetailVo){
        if(mysteryBoxProductDetailVo == null || mysteryBoxProductDetailVo.getStartTime() == null || mysteryBoxProductDetailVo.getSecondMarketSellingDelay() == null) {
            return;
        }

//        final Long openTime = mysteryBoxProductDetailVo.getStartTime().getTime() + mysteryBoxProductDetailVo.getSecondMarketSellingDelay() * 60 * 60 * 1000;
//        final long c = System.currentTimeMillis();
//        if (openTime.compareTo(c) < 0){
//            mysteryBoxProductDetailVo.setSecondMarketSellingDelay(null);
//        }
    }

    @SneakyThrows
    @GetMapping("/friendly/nft/mystery-box/detail")
    public CommonRet<MysteryBoxProductDetailIncludeAssetVo> queryMysteryBoxDetail(Long productId, Boolean lazyCache) {
        APIResponse<MysteryBoxProductDetailVo> mysteryBoxProductDetailVoAPIResponse
                = nftMysteryBoxApi.queryMysteryBoxDetail(APIRequest.instance(
                QueryMysteryBoxDetailRequest.builder()
                        .productId(productId)
                        .lazyCache(lazyCache != null && lazyCache)
                        .userId(baseHelper.getUserId())
                        .mysteryBoxDetailQueryType(MysteryBoxDetailQueryType.PRODUCT_ID)
                        .build()));
        baseHelper.checkResponse(mysteryBoxProductDetailVoAPIResponse);
        MysteryBoxProductDetailVo productDetailVo = mysteryBoxProductDetailVoAPIResponse.getData();
        MysteryBoxProductDetailIncludeAssetVo mysteryBoxProductDetailIncludeAssetVo = new MysteryBoxProductDetailIncludeAssetVo();
        BeanUtils.copyProperties(productDetailVo
                , mysteryBoxProductDetailIncludeAssetVo);
        refreshStatusAndDuration(mysteryBoxProductDetailIncludeAssetVo);
        checkAndSetSecondDelay(mysteryBoxProductDetailIncludeAssetVo);
        mysteryBoxI18nHelper.doI18n(mysteryBoxProductDetailIncludeAssetVo);
        if (Objects.nonNull(baseHelper.getUserId())) {
            UserCashBalanceRequest userCashBalanceRequest = new UserCashBalanceRequest();
            userCashBalanceRequest.setUserId(baseHelper.getUserId());
            userCashBalanceRequest.setAssetList(Lists.newArrayList(mysteryBoxProductDetailIncludeAssetVo.getCurrency()));
            APIResponse<UserCashBalanceVo> userCashBalance = cashBalanceApi.getUserCashBalance(
                    APIRequest.instance(userCashBalanceRequest)
            );
            baseHelper.checkResponse(userCashBalance);
            UserCashBalanceVo userCashBalanceData = userCashBalance.getData();
            Optional.ofNullable(userCashBalanceData.getAssetBalanceList())
                    .ifPresent(
                            assetBalances -> assetBalances.forEach(
                                    assetBalance -> {
                                        if (assetBalance.getAsset().equalsIgnoreCase(mysteryBoxProductDetailIncludeAssetVo.getCurrency())) {
                                            mysteryBoxProductDetailIncludeAssetVo.setUserBalance(
                                                    Optional.ofNullable(assetBalance.getFree()).orElse(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                                            );
                                        }
                                    }
                            )
                    );
        }
        return new CommonRet<>(mysteryBoxProductDetailIncludeAssetVo);
    }
    @GetMapping("/private/nft/mystery-box-order/list")
    public CommonPageRet<UserMysteryOrderHistory> listMysteryBoxOrder(Integer page, Integer size, MysteryBoxOrderQueryRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<CommonPageResponse<UserMysteryOrderHistory>> commonPageResponseAPIResponse = nftMysteryBoxApi.listAvailableMysteryBoxOrderV2(
                APIRequest.instance(CommonPageRequest
                        .<MysteryBoxOrderQueryRequest>builder()
                        .page(Objects.isNull(page) ? 1 : page)
                        .size(Objects.isNull(size) ? 10 : size)
                        .params(request)
                        .build()
                ));
        baseHelper.checkResponse(commonPageResponseAPIResponse);
        CommonPageResponse<UserMysteryOrderHistory> commonPageResponseAPIResponseData = commonPageResponseAPIResponse.getData();
        return new CommonPageRet<>(
                commonPageResponseAPIResponseData.getData(),
                commonPageResponseAPIResponseData.getTotal()
        );
    }

    @GetMapping("/public/nft/mystery-box/series-rarity-list")
    public CommonRet<List<RarityVo>> listSerialsRarity(@RequestParam("collectionId") Long collectionId) {
        List<RarityVo> rarityVos = mysteryBoxCacheHelper.getSerialsRarityVo(collectionId);
        return new CommonRet<>(rarityVos);
    }



    @PostMapping("/private/nft/mystery-box-order/notify")
    public CommonRet<Void> notifyMysteryBox(@RequestBody SubscriptionMysteryBoxRequest request) {
        if(baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = nftMysteryBoxApi.subscriptionMysteryBox(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @GetMapping("/private/nft/mystery-box-order/notifyDetail")
    public CommonRet<SubscriptionMysteryBoxVo> notifyMysteryBoxDetail( Long productId) {
        if(baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        APIResponse<SubscriptionMysteryBoxVo> response = nftMysteryBoxApi.getSubscriptionMysteryBox(APIRequest.instance(GetSubscriptionMysteryBoxRequest.builder().productId(productId).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}


