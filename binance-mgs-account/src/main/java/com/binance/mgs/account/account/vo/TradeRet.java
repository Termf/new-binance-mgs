package com.binance.mgs.account.account.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("交易详细信息")
public class TradeRet implements Serializable {

    private static final long serialVersionUID = -5607561979634729905L;

    @ApiModelProperty(value = "交易单号")
    private Long tradeId;

    @ApiModelProperty(value = "交易单号字符串")
    @Long2String
    private Long tradeIdStr;

    @ApiModelProperty(value = "交易价格")
    private String price;

    @ApiModelProperty(value = "交易时间")
    private Long time;

    @ApiModelProperty(value = "产品代码")
    private String symbol;

    @ApiModelProperty(value = "买卖方向")
    private String side;

    @ApiModelProperty(value = "是否主动买")
    private Boolean activeBuy;

    @ApiModelProperty(value = "realPnl")
    private BigDecimal realPnl;

    @ApiModelProperty(value = "交易数量")
    private String qty;

    @ApiModelProperty(value = "买家交易费")
    private String fee;

    @ApiModelProperty(value = "feeAsset")
    private String feeAsset;

    @ApiModelProperty(value = "totalQuota")
    private String totalQuota;

    @ApiModelProperty(value = "productName")
    private String productName;

    @ApiModelProperty(value = "基础资产")
    private String baseAsset;

    @ApiModelProperty(value = "标价货币")
    private String quoteAsset;

    @ApiModelProperty(value = "money")
    private String money;

    @ApiModelProperty(value = "userId")
    private Long userId;

    @ApiModelProperty(value = "userId字符串")
    @Long2String
    private Long userIdStr;

    @ApiModelProperty(value = "用户邮箱，当查询子账号信息时才返回")
    private String email;

}
