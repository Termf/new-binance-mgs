package com.binance.mgs.nft.trade.response;

import com.binance.nft.tradeservice.vo.UserInfoVo;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BuyOrderItemVo {
    private Long id;

    @Long2String
    private Long orderNo;

    private Long sellOrderId;

    private Long productId;

    private Long buyerId;

    private String currency;

    @BigDecimal2String
    private BigDecimal amount;

    private Integer quantity;

    private Integer status;

    private String statusDesc;

    private Integer type;

    private Integer nftType;

    private Date paymentTime;

    private Date tradeTime;

    private Date createTime;

    private Date updateTime;

    // product
    private String title;

    private String coverUrl;

    private String network;

    private UserInfoVo creator;

    private UserInfoVo owner;

    private String accountType;

    private String channel;

    /**
     * dex transaction hash
     * */
    private String txn;

    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    private Boolean unreadFlag;

    private String errorCode;

    private String errorMsg;

    private Long nftId;

    private Integer source;
}
