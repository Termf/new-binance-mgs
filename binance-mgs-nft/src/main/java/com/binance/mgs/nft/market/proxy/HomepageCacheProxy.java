package com.binance.mgs.nft.market.proxy;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.market.converter.HomeArtistVoConverter;
import com.binance.mgs.nft.market.converter.TopCreatorsMgsItemConverter;
import com.binance.mgs.nft.market.vo.HomeArtistMgsVo;
import com.binance.mgs.nft.market.vo.TopCreatorsMgsItem;
import com.binance.nft.market.ifae.NftMarketArtistApi;
import com.binance.nft.market.ifae.NftRankingApi;
import com.binance.nft.market.request.ArtistQuerySearchRequest;
import com.binance.nft.market.request.HomeArtistRequest;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nft.market.vo.MarketProductItem;
import com.binance.nft.market.vo.artist.HomeArtistVo;
import com.binance.nft.market.vo.ranking.RankingResponse;
import com.binance.nft.market.vo.ranking.TopCreatorsItem;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomepageCacheProxy {

    private final NftMarketArtistApi nftMarketArtistApi;
    private final NftRankingApi nftRankingApi;

    private final BaseHelper baseHelper;
    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public SearchResult<MarketProductItem> productList(ArtistQuerySearchRequest request) {

        APIResponse<SearchResult<MarketProductItem>> response = nftMarketArtistApi.
                artistProductList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public CommonPageResponse<HomeArtistMgsVo> homeArtist(HomeArtistRequest request) {
        APIResponse<CommonPageResponse<HomeArtistVo>> response =
                nftMarketArtistApi.findArtistListByCondition(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return HomeArtistVoConverter.convert(response.getData(), AES_PASSWORD);
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public RankingResponse<TopCreatorsMgsItem> topCreatorsRanking(Integer period, Integer limit) {
        APIResponse<RankingResponse<TopCreatorsItem>> ret = nftRankingApi.topCreatorsRanking(period, limit);
        return TopCreatorsMgsItemConverter.convert(ret, AES_PASSWORD);
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public RankingResponse<TopCreatorsMgsItem> topCreatorsFollowsRanking(Integer period, Integer limit) {
        APIResponse<RankingResponse<TopCreatorsItem>> ret = nftRankingApi.topCreatorsFollowRanking(period, limit);
        return TopCreatorsMgsItemConverter.convert(ret, AES_PASSWORD);
    }
}
