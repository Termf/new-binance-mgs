package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIRequest;
import com.binance.nft.assetservice.api.alert.WeaBotApi;
import com.binance.nft.assetservice.api.data.request.mintmanager.WeaGroupMessageReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeaBotHelper {

    private final WeaBotApi weaBotApi;

    @Async
    public void sendMintErrorWea(Long userId, String message){
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("the user 【").append(userId).append("】can not mint, the reason is 【")
                    .append(message).append("】");
            WeaGroupMessageReq req = WeaGroupMessageReq.builder()
                    .groupName("asset-alert-4-xavier")
                    .message(sb.toString())
                    .build();
            weaBotApi.groupContentMessage(APIRequest.instance(req));
        }catch (Exception e){
        }
    }
}
