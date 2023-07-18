package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIResponse;
import com.binance.nft.tradeservice.api.launchpad.ILaunchpadConfigApi;
import com.binance.nft.tradeservice.dto.LaunchpadSimpleDto;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LaunchpadHelper {

    private final ILaunchpadConfigApi launchpadConfigApi;

    public List<LaunchpadSimpleDto> queryByNftInfoIds(List<Long> nftInfoIdList){

        if (CollectionUtils.isEmpty(nftInfoIdList)){
            return new ArrayList<>();
        }

        final APIResponse<List<LaunchpadSimpleDto>> apiResponse = launchpadConfigApi.queryByNftIdList(nftInfoIdList);
        BaseHelper.checkResponse(apiResponse);

        return apiResponse.getData();
    }


}
