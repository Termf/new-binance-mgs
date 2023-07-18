package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.OrderErrorSubType;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class LayerOrderItemVo {
    private String layerName;
    @Long2String
    private Long layerId;
    private String logo;
    private String network;
    private Integer status;
    private String category;
    private BigDecimal royaltyFeeRate;
    private Date startTime;
    private String contractAddress;
    private String walletAddress;
    private Integer flowType;
    private Integer errorType;
    private List<OrderErrorSubType> error;
    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    private Boolean unreadFlag;
}
