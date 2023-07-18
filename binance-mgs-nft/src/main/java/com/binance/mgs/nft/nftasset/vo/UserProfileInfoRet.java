package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileInfoRet {
    //登录的用户id加密
    private String userId;
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

    private boolean isKyc;
    private boolean isOwner;

    private boolean artist;

    private Integer nftVipLevel;

    private boolean follow = false;
    private Integer following = 0;
    private Integer fans = 0;

    private Integer mintCount = 0;
    @BigDecimal2String
    private BigDecimal totalVolume;
    private boolean cr7Claimable;
    private String cr7ErrorMessage;
}
