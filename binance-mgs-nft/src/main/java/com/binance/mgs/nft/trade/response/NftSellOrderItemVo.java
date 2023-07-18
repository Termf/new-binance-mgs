package com.binance.mgs.nft.trade.response;

import com.binance.nft.tradeservice.vo.UserInfoVo;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class NftSellOrderItemVo {

    private Long id;

    @Long2String
    private Long orderNo;

    private Long productId;

    private Long sellerId;

    private String currency;

    @BigDecimal2String
    private BigDecimal amount;

    private Integer quantity;

    private Integer status;

    private String statusDesc;

    private Integer type;

    private Integer nftType;

    private Long feeId;

    private Date createTime;

    private Date tradeTime;

    // product
    private String title;

    private String coverUrl;

    private UserInfoVo buyer;

    private String network;

    // fee
    @BigDecimal2String
    private BigDecimal totalFee;

    private Integer extendAttr;

    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    private Boolean unreadFlag;

    private Long nftId;
}
