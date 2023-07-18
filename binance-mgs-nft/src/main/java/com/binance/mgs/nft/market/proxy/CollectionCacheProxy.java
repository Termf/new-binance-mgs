package com.binance.mgs.nft.market.proxy;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.common.cache.CacheSeneEnum;
import com.binance.mgs.nft.common.cache.CacheUtils;
import com.binance.nft.market.ifae.NftMarketCollectionApi;
import com.binance.nft.market.request.CollectionDetailRequest;
import com.binance.nft.market.request.CollectionPriceRequest;
import com.binance.nft.market.request.CollectionQuerySearchRequest;
import com.binance.nft.market.request.LayerSearchRequest;
import com.binance.nft.market.vo.CommonPageRequest;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nft.market.vo.MarketProductItem;
import com.binance.nft.market.vo.collection.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionCacheProxy {

    private final NftMarketCollectionApi nftMarketCollectionApi;

    private final BaseHelper baseHelper;
    @Resource
    private RedissonClient redissonClient;

//    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public SearchResult<MarketProductItem> layerProductList(CollectionQuerySearchRequest request) {

        TypeReference typeReference = new TypeReference<SearchResult<MarketProductItem>>() {};
        Function<CollectionQuerySearchRequest, SearchResult<MarketProductItem>> function = req -> {
            APIResponse<SearchResult<MarketProductItem>> response =
                    nftMarketCollectionApi.collectionProductList(APIRequest.instance(request));
            baseHelper.checkResponse(response);
            return response.getData();
        };

        return CacheUtils.<CollectionQuerySearchRequest, SearchResult<MarketProductItem>>getData(redissonClient, request, CacheSeneEnum.SEARCH_REGULAR_LAYER,
                request.getPage() < 8, typeReference, function);
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CollectionPriceVo collectionPrice(Long collectionId) {

        CollectionPriceRequest request = CollectionPriceRequest.builder()
                .collectId(collectionId)
                .build();
        APIResponse<CollectionPriceVo> response =
                nftMarketCollectionApi.findCollectionPriceInfoById(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public HomeCollectionVo collectionDetail(CollectionDetailRequest request) {

        APIResponse<HomeCollectionVo> detail = nftMarketCollectionApi.findCollectionDetailByLayerId(
                APIRequest.instance(request));
        baseHelper.checkResponse(detail);
        return detail.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'collectionSearch-'+#keyword")
    public CommonPageResponse<HomeCollectionVo> collectionSearch(CommonPageRequest<String> request) throws Exception {
//        APIResponse<CommonPageResponse<HomeCollectionVo>> response = nftMarketCollectionApi.collectionSearch(APIRequest.instance(request));
//        baseHelper.checkResponse(response);
//        return response.getData();
        return null;
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'collectionSearch-'+#request")
    public CommonPageResponse<HomeCollectionVo> collectionSearchV1(CommonPageRequest<LayerSearchRequest> request) throws Exception {
        APIResponse<CommonPageResponse<HomeCollectionVo>> response = nftMarketCollectionApi.collectionSearchV1(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE, key = "'mb-detail-'+#request")
    public MysteryBoxHomeCollectionVo mysteryLayerDetail(CollectionDetailRequest request) {

        APIResponse<MysteryBoxHomeCollectionVo> detail = nftMarketCollectionApi.getMysteryBoxCollectionDetailByCollectionId(
                APIRequest.instance(request));
        baseHelper.checkResponse(detail);
        return detail.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE, key = "'mb-price-'+#request")
    public MysteryBoxCollectionPriceVo mysteryLayerPrice(CollectionDetailRequest request) {

        APIResponse<MysteryBoxCollectionPriceVo> detail = nftMarketCollectionApi.getMysteryBoxCollectionPriceByCollectionId(
                APIRequest.instance(request));
        baseHelper.checkResponse(detail);
        return detail.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE, key = "'mb-serial-'+#request")
    public List<MysteryBoxSerialVo> mysterySerialList(CollectionDetailRequest request) {

        APIResponse<List<MysteryBoxSerialVo>> detail = nftMarketCollectionApi.getMysteryBoxSerialsListByCollectionId(
                APIRequest.instance(request));
        baseHelper.checkResponse(detail);
        return detail.getData();
    }
}
