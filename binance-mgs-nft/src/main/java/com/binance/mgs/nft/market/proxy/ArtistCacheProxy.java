package com.binance.mgs.nft.market.proxy;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.nft.market.ifae.NftMarketArtistApi;
import com.binance.nft.market.request.ArtistQuerySearchRequest;
import com.binance.nft.market.vo.MarketProductItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistCacheProxy {

    private final NftMarketArtistApi nftMarketArtistApi;

    private final BaseHelper baseHelper;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public SearchResult<MarketProductItem> productList(ArtistQuerySearchRequest request) {

        APIResponse<SearchResult<MarketProductItem>> response = nftMarketArtistApi.
                artistProductList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }
}
