package com.binance.mgs.nft.market.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.market.proxy.CollectionCacheProxy;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.mgs.nft.market.request.CommonSearchRequest;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.CollectionInfo;
import com.binance.mgs.nft.market.vo.MarketProductMgsItem;
import com.binance.mgs.nft.market.vo.UserInfoMgsVo;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.nft.assetservice.api.data.vo.ItemsApproveInfo;
import com.binance.nft.assetservice.api.data.vo.function.NftLayerConfigVo;
import com.binance.nft.assetservice.api.function.ILayerInfoApi;
import com.binance.nft.assetservice.enums.AssetTypeEnum;
import com.binance.nft.assetservice.enums.CategoryEnum;
import com.binance.nft.assetservice.enums.NetworkEnum;
import com.binance.nft.common.utils.ObjectUtils;
import com.binance.nft.market.ifae.ActivitiesApi;
import com.binance.nft.market.ifae.NftMarketCollectionApi;
import com.binance.nft.market.request.*;
import com.binance.nft.market.vo.CommonPageRequest;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nft.market.vo.MarketProductItem;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.market.vo.collection.*;
import com.binance.nft.market.vo.ranking.RankingResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class CollectionInfoController {

    private final CollectionCacheProxy collectionCacheProxy;

    private final ApproveHelper approveHelper;

    private final BaseHelper baseHelper;

    private final ILayerInfoApi layerInfoApi;

    private final NftMarketCollectionApi nftMarketCollectionApi;

    private final ActivitiesApi activitiesApi;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    private final MarketCacheProxy marketCacheProxy;

    private final MgsNftProperties mgsNftProperties;

    @GetMapping("/private/nft/user/has-any-collection")
    public CommonRet<Boolean> hasAnyCollection() {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>(false);
        }
        APIResponse<List<NftLayerConfigVo>> nftLayerConfigResponse = layerInfoApi.queryUserCollectionByUserId(userId);
        baseHelper.checkResponse(nftLayerConfigResponse);
        return new CommonRet<>(CollectionUtils.isEmpty(nftLayerConfigResponse.getData()) ? false : true);
    }

    @GetMapping("/private/nft/user/layer-list")
    public CommonRet<List<CollectionInfo>> layerList(@RequestParam(value = "networks", required = false) List<String> networks) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        APIResponse<List<NftLayerConfigVo>> nftLayerConfigResponse = layerInfoApi.queryUserCollectionByUserId(userId);
        baseHelper.checkResponse(nftLayerConfigResponse);

        if (CollectionUtils.isNotEmpty(nftLayerConfigResponse.getData())) {
            List<CollectionInfo> layerList =
                    nftLayerConfigResponse.getData().stream()
                            .filter(
                                    nftLayerConfigVo -> CollectionUtils.isEmpty(networks) ||
                                            networks.contains(NetworkEnum.getByCode(nftLayerConfigVo.getNetworkType()).getDesc()))
                            .map(nftLayerConfigVo ->
                                    CollectionInfo.builder()
                                            .layerId(nftLayerConfigVo.getCollectionId())
                                            .layerName(Optional.ofNullable(nftLayerConfigVo.getCollectionName()).orElse(nftLayerConfigVo.getSymbol()))
                                            .royaltyFee(nftLayerConfigVo.getRoyaltyFeeRate() != null ? nftLayerConfigVo.getRoyaltyFeeRate() : BigDecimal.ZERO)
                                            .avatarUrl(Optional.ofNullable(nftLayerConfigVo.getLogoUrl()).orElse(nftLayerConfigVo.getBannerUrl()))
                                            .category(CategoryEnum.getByCode(nftLayerConfigVo.getCategory()).getDesc())
                                            .nftProtocal(nftLayerConfigVo.getProtocol())
                                            .createTime(nftLayerConfigVo.getCreateTime())
                                            .build())
                            .sorted((collectionInfo1, collectionInfo2) -> collectionInfo2.getCreateTime().compareTo(collectionInfo1.getCreateTime()))
                            .collect(Collectors.toList());
            return new CommonRet<>(layerList);
        } else {
            return new CommonRet<>();
        }
    }

    @GetMapping("/public/nft/home-layer-detail")
    public CommonRet<HomeCollectionVo> layerDetail(@RequestParam("collectionId") Long collectionId,
                                                   HttpServletRequest httpServletRequest) {

        CollectionDetailRequest request = CollectionDetailRequest.builder()
                .layerId(collectionId).gray(isGray(httpServletRequest))
                .build();
        HomeCollectionVo collectionVo = collectionCacheProxy.collectionDetail(request);
        if (collectionVo == null) {
            return new CommonRet<>();
        }
        return new CommonRet(collectionVo);
    }

    @GetMapping("/public/nft/home-layer-price")
    public CommonRet<CollectionPriceVo> layerPrice(@RequestParam("collectionId") Long collectionId) {

        CollectionPriceVo collectionPriceVo = collectionCacheProxy.collectionPrice(collectionId);
        if (null == collectionPriceVo) {
            return new CommonRet<>();
        }
        return new CommonRet(collectionPriceVo);
    }

    @PostMapping({"/public/nft/layer-product-list", "/friendly/nft/layer-product-list"})
    public CommonRet<SearchResult<MarketProductMgsItem>> artistProductList(@RequestBody CollectionQuerySearchRequest request) {
        if (request.getRows() == null || request.getRows() != 16) {
            return new CommonRet<>();
        }
        SearchResult<MarketProductItem> resp = collectionCacheProxy.layerProductList(request);
        SearchResult<MarketProductMgsItem> result = new SearchResult<>();

        Long timestamp = DateUtils.getNewUTCTimeMillis();
        if (CollectionUtils.isNotEmpty(resp.getRows())) {
            List<MarketProductMgsItem> mgsItemList = new ArrayList<>(resp.getRows().size());
            resp.getRows().forEach(item -> {

                item.setTimestamp(timestamp);
                MarketProductMgsItem marketProductMgsItem = CopyBeanUtils.fastCopy(item, MarketProductMgsItem.class);
                if (!ObjectUtils.isEmpty(item.getOwner())) {
                    UserInfoMgsVo owner = CopyBeanUtils.fastCopy(item.getOwner(), UserInfoMgsVo.class);

                    if (!ObjectUtils.isEmpty(item.getOwner().getUserId())) {
                        owner.setUserId(AesUtil.encrypt(item.getOwner().getUserId().toString(), AES_PASSWORD));
                    }
                    marketProductMgsItem.setOwner(owner);
                }
                if (!ObjectUtils.isEmpty(item.getCreator())) {
                    UserInfoMgsVo creator = CopyBeanUtils.fastCopy(item.getCreator(), UserInfoMgsVo.class);
                    if (!ObjectUtils.isEmpty(item.getCreator().getUserId())) {
                        creator.setUserId(AesUtil.encrypt(item.getCreator().getUserId().toString(), AES_PASSWORD));
                    }
                    marketProductMgsItem.setCreator(creator);
                }
                mgsItemList.add(marketProductMgsItem);
            });
            result.setRows(mgsItemList);
            result.setTotal(resp.getTotal());
        }

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
        return new CommonRet<>(result);
    }

    @GetMapping({"/public/nft/layer-search"})
    public CommonRet<List<HomeCollectionVo>> collectionSearch(@RequestParam("keyword") String keyword) throws Exception {
        CommonPageRequest<String> request = CommonPageRequest.<String>builder().page(1).size(100).params(keyword).build();
        CommonPageResponse<HomeCollectionVo> resp = collectionCacheProxy.collectionSearch(request);
        return new CommonRet<>(Optional.ofNullable(resp.getData()).orElse(Lists.newArrayList()));
    }

    @GetMapping({"/public/nft/layer-search-v1"})
    public CommonRet<List<HomeCollectionVo>> collectionSearch(@RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "assetType",required = false) Integer assetType,
            @RequestParam(value = "count",required = false)Integer count) throws Exception {
        return new CommonRet<>(doCollectionSearch(keyword, assetType, count));
    }

    @PostMapping({"/public/nft/layer-search-v1"})
    public CommonRet<List<HomeCollectionVo>> collectionSearchPost(@RequestBody CommonSearchRequest request) throws Exception {
        return new CommonRet<>(doCollectionSearch(request.getKeyword(), request.getAssetType(), request.getCount()));
    }

    @SneakyThrows
    private List<HomeCollectionVo> doCollectionSearch(String keyword, Integer assetType, Integer count) {
        assetType = Objects.isNull(assetType) ? AssetTypeEnum.NORMAL.getCode() : assetType;
        count = Objects.isNull(count) ? 100 : count;
        LayerSearchRequest searchRequest = LayerSearchRequest.builder().keyword(keyword).assetType(assetType).build();
        CommonPageRequest<LayerSearchRequest> request = CommonPageRequest.<LayerSearchRequest>builder().page(1).size(count).params(searchRequest).build();
        CommonPageResponse<HomeCollectionVo> resp = collectionCacheProxy.collectionSearchV1(request);
        return resp.getData();
    }

    @GetMapping("/public/nft/mystery-box-collection-page/collection-detail")
    public CommonRet<MysteryBoxHomeCollectionVo> mysteryLayerDetail(@RequestParam("collectionId") Long collectionId, HttpServletRequest httpServletRequest) {
        if (collectionId == null) {
            return new CommonRet<>();
        }
        CollectionDetailRequest request = CollectionDetailRequest.builder()
                .layerId(collectionId).gray(isGray(httpServletRequest))
                .build();
        MysteryBoxHomeCollectionVo collectionVo = collectionCacheProxy.mysteryLayerDetail(request);
        if (collectionVo == null) {
            return new CommonRet<>();
        }
        return new CommonRet<>(collectionVo);
    }

    @GetMapping("/public/nft/mystery-box-collection-page/collection-price")
    public CommonRet<CollectionPriceVo> mysteryLayerPrice(@RequestParam("collectionId") Long collectionId, HttpServletRequest httpServletRequest) {
        CollectionDetailRequest request = CollectionDetailRequest.builder()
                .layerId(collectionId).gray(isGray(httpServletRequest))
                .build();
        MysteryBoxCollectionPriceVo collectionPriceVo = collectionCacheProxy.mysteryLayerPrice(request);
        if (null == collectionPriceVo) {
            return new CommonRet<>();
        }
        return new CommonRet(collectionPriceVo);
    }

    @GetMapping("/public/nft/mystery-box-collection-page/collection-series-list")
    public CommonRet<List<MysteryBoxSerialVo>> mysterySerialList(@RequestParam("collectionId") Long collectionId, HttpServletRequest httpServletRequest) {
        if (collectionId == null) {
            return new CommonRet<>();
        }
        CollectionDetailRequest request = CollectionDetailRequest.builder()
                .layerId(collectionId).gray(isGray(httpServletRequest))
                .build();
        List<MysteryBoxSerialVo> serialVos = collectionCacheProxy.mysterySerialList(request);
        if (null == serialVos) {
            return new CommonRet<>();
        }
        return new CommonRet(serialVos);
    }

    @GetMapping("/public/nft/collection/chart/avg-price-volume/{collectionId}/{timeInterval}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<PriceVolumeVo> collectionChartAvgPriceVolume(@PathVariable("collectionId") String collectionId, @PathVariable("timeInterval") Integer timeInterval) throws Exception {
        if (StringUtils.isEmpty(collectionId) || timeInterval < 0 || timeInterval > 30) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        APIResponse<PriceVolumeVo> response = nftMarketCollectionApi.getPriceVolume(APIRequest.instance(CollectionChartRequest.builder()
                .collectionId(collectionId)
                .timeInterval(timeInterval)
                .build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/public/nft/collection/chart/floor-price/{collectionId}/{timeInterval}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<CollectionChartFloorPriceVo> collectionChartFloorPrice(@PathVariable("collectionId") String collectionId, @PathVariable("timeInterval") Integer timeInterval) throws Exception {
        if (StringUtils.isEmpty(collectionId) || timeInterval < 0 || timeInterval > 30) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        APIResponse<CollectionChartFloorPriceVo> response = nftMarketCollectionApi.getFloorPrice(APIRequest.instance(CollectionChartRequest.builder()
                .collectionId(collectionId)
                .timeInterval(timeInterval)
                .build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/public/nft/collection/chart/list-price/{collectionId}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<ListingPriceVo> collectionChartListPrice(@PathVariable("collectionId") String collectionId,
                                                              @RequestParam(name = "step",required = false) String step,
                                                              @RequestParam(name="currency",required = false) String currency) throws Exception {
        if (StringUtils.isEmpty(collectionId)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        ListPriceRangeRequest request = ListPriceRangeRequest.builder()
                .collectionId(collectionId)
                .step(step)
                .currency(currency).build();
        APIResponse<ListingPriceVo> response = nftMarketCollectionApi.getListingPrice(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/friendly/nft/collection/single/volume-price-rate-24h/{collectionId}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<CollectionVolumePriceRateVo> collectionVolumeAndPriceRate24HV2(@PathVariable("collectionId") String collectionId) throws Exception {
        if (StringUtils.isEmpty(collectionId)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        Long userId = baseHelper.getUserId();
        List<Long> whitelist = mgsNftProperties.getAssetListV2Whitelist();
        Integer abPercent = mgsNftProperties.getRankingV2AbPercent();
        boolean isUseNewApi = marketCacheProxy.isUseNewApi(userId, whitelist, abPercent);

        APIResponse<CollectionVolumePriceRateVo> response;
        if (isUseNewApi) {
            response = nftMarketCollectionApi.getCollectionDexVolumeAndPriceRate24H(APIRequest.instance(collectionId));
        }else {
            response = nftMarketCollectionApi.getCollectionVolumeAndPriceRate24H(APIRequest.instance(collectionId));
        }

        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/public/nft/collection/single/volume-price-rate-24h/{collectionId}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<CollectionVolumePriceRateVo> collectionVolumeAndPriceRate24H(@PathVariable("collectionId") String collectionId) throws Exception {
        if (StringUtils.isEmpty(collectionId)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        APIResponse<CollectionVolumePriceRateVo> response = nftMarketCollectionApi.getCollectionVolumeAndPriceRate24H(APIRequest.instance(collectionId));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/public/nft/collection/top-activities")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<ActivitiesDetailVo>> getActivitiesList(@RequestBody ActivitiesRequest request) throws Exception {
        if (request == null) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        APIResponse<RankingResponse<ActivitiesDetailVo>> response = null;
        if (request.getCollectionId() == null || request.getCollectionId().intValue() == 0) {
            response = activitiesApi.getTopActivitiesList(APIRequest.instance(request));
        } else {
            response = activitiesApi.getActivitiesListByCollectionId(APIRequest.instance(request));
        }
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);
    }
}
