package com.binance.mgs.nft.market.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArtistVo implements Serializable {

    private String nickName;
    private String avatarUrl;
    private String bannerUrl;
    private String description;
}
