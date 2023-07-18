package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.platform.openfeign.jackson.Long2String;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class TopicItemMgs implements Serializable {

    private String coverImage;

    @Long2String
    private Long productId;

    private String name;

    private String type;

    private Integer status;

    private String price;

    private String currency;

    private ArtistMgs artist;

    private Date startTime;

    private Date endTime;

    @JsonIgnore
    private Long topicId;

    private String duration;

    private Integer mappingStatus;

    private Integer sort;

    private String mediaType;

    @ApiModelProperty("status(1 normal nft 2 unopened box 3 opened box)")
    private Integer nftType;

    @ApiModelProperty("rarity 0-4")
    private Integer rarity;

    @ApiModelProperty("0 fixed 1 auction")
    private Integer tradeType;

    private String network;

    private UserApproveInfo approve;

    @Long2String
    private Long serialsNo;

    private String serialsName;

    private Integer verified;

    private Long timestamp;
}

