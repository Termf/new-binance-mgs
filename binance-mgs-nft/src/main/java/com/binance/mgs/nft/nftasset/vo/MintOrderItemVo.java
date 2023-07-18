package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.OrderErrorSubType;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class MintOrderItemVo {
    private Long mintOrderId;
    private String title;
    private String image;
    private BigDecimal amount;
    private String asset;
    private Integer number;
    private Long nftInfoId;
    @Long2String
    private Long serialsNo;
    private String serialsName;
    private Integer status;
    private String network;
    private Date startTime;
    private String contractAddress;
    private String tokenId;
    private String walletAddress;
    private Integer errorType;
    private List<OrderErrorSubType> error;
    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    private Boolean unreadFlag;
}
