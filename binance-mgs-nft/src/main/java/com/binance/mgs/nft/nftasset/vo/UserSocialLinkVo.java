package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocialLinkVo implements Serializable {

    private String twitterUrl;
    private String discordUrl;
    private String instagramUrl;
    private String tiktokUrl;
    private String telegramUrl;
    private String facebookUrl;

}
