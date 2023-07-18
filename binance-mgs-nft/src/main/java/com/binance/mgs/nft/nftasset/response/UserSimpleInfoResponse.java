package com.binance.mgs.nft.nftasset.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleInfoResponse implements Serializable {

    private Long nftInfoId;
    private String nickName;

    private String userId;
    private String avatarUrl;
    private byte avatarType;
    private String bannerUrl;
    private String bio;

    private Integer whiteStatus = 0;
}
