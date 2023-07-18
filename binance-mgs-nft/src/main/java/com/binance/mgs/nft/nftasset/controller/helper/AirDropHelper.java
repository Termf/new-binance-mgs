package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.market.ifae.INftAirdropApi;
import com.binance.nft.market.vo.airdrop.AirDropSimpleDto;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirDropHelper {

    private final INftAirdropApi nftAirdropApi;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_BIG)
    public List<AirDropSimpleDto> fetchAirdropDesc(List<Long> nftIds){

        final APIResponse<List<AirDropSimpleDto>> apiResponse = nftAirdropApi.queryAirDropTaskByNftId(APIRequest.instance(nftIds));
        BaseHelper.checkResponse(apiResponse);

        return apiResponse.getData();
    }

}
