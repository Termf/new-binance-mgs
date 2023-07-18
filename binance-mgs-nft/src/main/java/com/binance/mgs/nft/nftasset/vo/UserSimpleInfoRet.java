package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserSimpleInfoRet implements Serializable {

    @Long2String
    private Long userId;
    private String nickName;
    private String email;
    private String avatarUrl;
    private String bannerUrl;
    private String bio;

    private String twitterUrl;
    private String discordUrl;
    private String instagramUrl;
    private String facebookUrl;
    private String telegramUrl;

    private Integer mintCount;

    private boolean isKyc;
    private boolean isOwner;

    private boolean artist;

    private Integer nftVipLevel;
}
