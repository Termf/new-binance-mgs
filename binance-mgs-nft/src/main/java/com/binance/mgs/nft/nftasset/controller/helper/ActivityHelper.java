package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIResponse;
import com.binance.nft.activityservice.api.ActivityInfoApi;
import com.binance.nft.activityservice.api.SpinLotteryApi;
import com.binance.nft.activityservice.response.LotteryConfigResponse;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActivityHelper {

    private final SpinLotteryApi spinLotteryApi;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_BIG)
    public LotteryConfigResponse getActivityInfo(){

        final APIResponse<LotteryConfigResponse> apiResponse = spinLotteryApi.lotteryConfig();
        BaseHelper.checkResponse(apiResponse);
        return apiResponse.getData();
    }

}
