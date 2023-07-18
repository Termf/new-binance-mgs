package com.binance.mgs.nft.market.helper;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.market.ifae.NftMarketArtistApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistHelper {

    private final NftMarketArtistApi nftMarketArtistApi;

    private final BaseHelper baseHelper;

    public boolean checkUserArtist(Long userId){
        APIResponse<List<Long>> response = nftMarketArtistApi.queryArtistListByIds(
                APIRequest.instance(Arrays.asList(userId)));
        baseHelper.checkResponse(response);
        return response.getData().contains(userId);
    }

    public List<Long> getUserArtistListByUserIdList(List<Long> userIdList){
//
//        if(CollectionUtils.isEmpty(userIdList)){
//            return Collections.emptyList();
//        }
//        APIResponse<List<Long>> response = nftMarketArtistApi.queryArtistListByIds(
//                APIRequest.instance(userIdList));
//        baseHelper.checkResponse(response);
//        return response.getData();
        return userIdList;
    }
}
