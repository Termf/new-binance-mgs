package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.nft.common.utils.ObjectUtils;
import com.binance.nft.market.ifae.UserWatchListApi;
import com.binance.nft.market.request.WatchListRequest;
import com.binance.nft.market.request.WatchListSubscribeRequest;
import com.binance.nft.market.vo.watchlist.UserWatchItemVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class MarketWatchListController {

    private final BaseHelper baseHelper;

    private final UserWatchListApi userWatchListApi;

    private final MarketCacheProxy marketCacheProxy;

    private final MgsNftProperties mgsNftProperties;

    @GetMapping("/friendly/nft/watch-list")
    public CommonRet<List<UserWatchItemVo>> watchList(@RequestParam("period") String period) {
        Long userId = baseHelper.getUserId();
        if(ObjectUtils.isEmpty(userId)){
           return new CommonRet<>();
        }

        List<Long> whitelist = mgsNftProperties.getAssetListV2Whitelist();
        Integer abPercent = mgsNftProperties.getRankingV2AbPercent();
        boolean isUseNewApi = marketCacheProxy.isUseNewApi(userId, whitelist, abPercent);
        APIResponse<List<UserWatchItemVo>> response;
        if (isUseNewApi) {
            response = userWatchListApi.watchListV2(userId, period);
        } else {
            response = userWatchListApi.watchList(userId, period);
        }
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/friendly/nft/watch-list/move-to-top")
    public CommonRet<Boolean> moveToTop(@RequestBody WatchListRequest request) {
        Long userId = baseHelper.getUserId();
        if (ObjectUtils.isEmpty(userId)){
            return new CommonRet<>(Boolean.FALSE);
        }
        request.setUserId(userId);
        APIResponse<Boolean> response = userWatchListApi.moveToTop(request);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/friendly/nft/watch-list/subscribe")
    public CommonRet<Boolean> subscribe(@RequestBody WatchListSubscribeRequest request) {
        Long userId = baseHelper.getUserId();
        if(ObjectUtils.isEmpty(userId)){
            return new CommonRet<>(Boolean.FALSE);
        }
        request.setUserId(userId);
        APIResponse<Boolean> response = userWatchListApi.subscribe(request);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
    @GetMapping("/friendly/nft/watch-list/whetherSubscribed")
    public CommonRet<Boolean> whetherSubscribed(@RequestParam("collectionId") Long collectionId) {
        Long userId = baseHelper.getUserId();
        if(ObjectUtils.isEmpty(userId)){
            return new CommonRet<>(Boolean.FALSE);
        }
        WatchListRequest request = new WatchListRequest();
        request.setCollectionId(collectionId);
        request.setUserId(userId);
        APIResponse<Boolean> response = userWatchListApi.whetherSubscribed(request);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

}
