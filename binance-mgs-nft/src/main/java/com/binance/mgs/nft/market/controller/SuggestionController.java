package com.binance.mgs.nft.market.controller;


import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.mgs.nft.market.request.CommonSearchRequest;
import com.binance.mgs.nft.market.vo.MarketSuggestionListMgs;
import com.binance.nft.market.enums.SearchPageType;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.*;

@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class SuggestionController {

    private final MarketCacheProxy marketCacheProxy;

    private final MgsNftProperties mgsNftProperties;

    private final BaseHelper baseHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @GetMapping("/public/nft/suggestion-search")
    public CommonRet<MarketSuggestionListMgs> suggestionSearch(@RequestParam("keyword") String keyword) {
        return new CommonRet(doSuggestionSearch(keyword));
    }

    @PostMapping("/public/nft/suggestion-search")
    public CommonRet<MarketSuggestionListMgs> suggestionSearchPost(@RequestBody CommonSearchRequest request) {
        return new CommonRet(doSuggestionSearch(request.getKeyword()));
    }

    @PostMapping("/friendly/nft/suggestion-search")
    public CommonRet<MarketSuggestionListMgs> suggestionSearchV2(@RequestBody CommonSearchRequest request) {
        MarketSuggestionListMgs marketSuggestionListMgs;
        Long userId = baseHelper.getUserId();
        if (isUseNewApi(userId)) {
            //new api
            String scenesType = SearchPageType.SUGGESTION.getCode();
            marketSuggestionListMgs = marketCacheProxy.suggestionListV2(request, userId, AES_PASSWORD, scenesType);
        } else {
            //old search api
            marketSuggestionListMgs = doSuggestionSearch(request.getKeyword());
        }
        return new CommonRet(marketSuggestionListMgs);
    }

    @PostMapping("/friendly/nft/landing-search")
    public CommonRet<MarketSuggestionListMgs> landingSearch(@RequestBody CommonSearchRequest request) {
        MarketSuggestionListMgs marketSuggestionListMgs;
        Long userId = baseHelper.getUserId();
        String scenesType = SearchPageType.LANDING.getCode();
        marketSuggestionListMgs = marketCacheProxy.suggestionListV2(request, userId, AES_PASSWORD, scenesType);
        return new CommonRet(marketSuggestionListMgs);
    }

    @SneakyThrows
    private MarketSuggestionListMgs doSuggestionSearch(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return MarketSuggestionListMgs.builder().build();
        }
        keyword = URLDecoder.decode(keyword, "UTF-8");
        return marketCacheProxy.suggestionList(keyword, AES_PASSWORD);
    }

    private boolean isUseNewApi(Long userId) {
        userId = Optional.ofNullable(userId).orElse(99L);
        List<Long> searchWhitelist = mgsNftProperties.getSearchWhitelist();
        Integer searchAbPercent = mgsNftProperties.getSearchAbPercent();

        if(CollectionUtils.isNotEmpty(searchWhitelist) && searchWhitelist.contains(userId)) {
             return true;
        }

        if(Objects.nonNull(searchAbPercent)) {
            return (userId % 100) < searchAbPercent;
        }

        return false;
    }
}
