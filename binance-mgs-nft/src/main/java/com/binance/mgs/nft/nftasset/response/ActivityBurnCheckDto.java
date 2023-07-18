package com.binance.mgs.nft.nftasset.response;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ActivityBurnCheckDto implements Serializable {

    @Long2String
    private Long collectionId;
    private String collectionName;
    private String activityUrl;
    @Long2String
    private Long remainTime;

}
