package com.binance.mgs.nft.market.vo;

import lombok.Data;

import java.io.Serializable;


@Data
public class HomeArtistMgsVo implements Serializable {
    private Long creatorId;

    private String userId;

    private String nickName;

    private String avatarUrl;

    private String bannerUrl;

    private String description;

    //0 unfollowed 1 followed 2 userid = creatorId
    private Integer followRelation;
}
