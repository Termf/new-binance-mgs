package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIResponse;
import com.binance.nft.market.ifae.MysteryRankingApi;
import com.binance.nft.market.vo.ranking.RankingResponse;
import com.binance.nft.market.vo.ranking.TopGainersItem;
import com.binance.nft.market.vo.ranking.TopPurchasedItem;
import com.binance.nft.market.vo.ranking.TopThemesItem;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1/")
@RestController
@RequiredArgsConstructor
public class MysteryRankingController {

    private final MysteryRankingApi mysteryRankingApi;
    private final BaseHelper baseHelper;

    @GetMapping("public/nft/ranking/top-themes")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<TopThemesItem>> topThemesRanking() throws Exception {
        APIResponse<RankingResponse<TopThemesItem>> ret = mysteryRankingApi.topThemesRanking();
        return new CommonRet<>(ret.getData());
    }

    @GetMapping("public/nft/ranking/top-themes/{period}/{limit}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<TopThemesItem>> topThemesRanking(@PathVariable("period") Integer period, @PathVariable("limit") Integer limit) throws Exception {
        APIResponse<RankingResponse<TopThemesItem>> ret = mysteryRankingApi.topThemesRanking(period, limit);
        return new CommonRet<>(ret.getData());
    }

    @GetMapping("public/nft/ranking/top-gainers")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<TopGainersItem>> topGainersRanking() throws Exception {
        APIResponse<RankingResponse<TopGainersItem>> ret = mysteryRankingApi.topGainersRanking();
        return new CommonRet<>(ret.getData());
    }


    @GetMapping("private/nft/ranking/top-purchased")
    public CommonRet<RankingResponse<TopPurchasedItem>> toppurchasedRanking() throws Exception {
        Long userId = baseHelper.getUserId();
        APIResponse<RankingResponse<TopPurchasedItem>> ret = mysteryRankingApi.topPurchaseRanking(userId);
        return new CommonRet<>(ret.getData());
    }
}
