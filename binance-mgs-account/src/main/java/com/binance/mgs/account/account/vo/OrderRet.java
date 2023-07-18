package com.binance.mgs.account.account.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ApiModel("订单详细信息")
public class OrderRet implements Serializable {


    private static final long serialVersionUID = 7190372421890923966L;
    @ApiModelProperty(value = "订单号")
    private Long orderId;

    @ApiModelProperty(value = "订单号字符串")
    @Long2String
    private Long orderIdStr;

    @ApiModelProperty(value = "交易账号")
    private Long accountId;

    @ApiModelProperty(value = "交易账号字符串")
    @Long2String
    private Long accountIdStr;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户ID字符串")
    @Long2String
    private Long userIdStr;

    @ApiModelProperty(value = "产品代码")
    private String symbol;

    @ApiModelProperty(value = "客户单号")
    private String clientOrderId;

    @ApiModelProperty(value = "原始客户单号")
    private String origClientOrderId;

    @ApiModelProperty(value = "价格")
    private String price;

    @ApiModelProperty(value = "下单数量")
    private String origQty;

    @ApiModelProperty(value = "成交数量")
    private String executedQty;

    @ApiModelProperty(value = "已成交金额")
    private String executedQuoteQty;

    @ApiModelProperty(value = "状态(NEW: 未成交, PARTIALLY_FILLED: 部分成交, FILLED: 全部成交, CANCELLED: 已撤销)")
    private String status;

    @ApiModelProperty(name = "原始状态(NEW、PARTIALLY_FILLED、FILLED、CANCELLED)")
    private String origStatus;

    @ApiModelProperty(value = "订单生存周期")
    private String timeInForce;

    @ApiModelProperty(value = "订单类型")
    private String type;

    @ApiModelProperty(value = "买卖方向")
    private String side;

    @ApiModelProperty(value = "突破价格")
    private BigDecimal stopPrice;

    @ApiModelProperty(value = "下单时间")
    private Long time;

    @ApiModelProperty(value = "订单更新时间")
    private Date updateTime;

    @ApiModelProperty(value = "基础资产")
    private String baseAsset;

    @ApiModelProperty(value = "标价货币")
    private String quoteAsset;

    @ApiModelProperty(value = "delegateMoney")
    private String delegateMoney;

    @ApiModelProperty(value = "executedPrice")
    private String executedPrice;

    @ApiModelProperty(value = "productName")
    private String productName;

    @ApiModelProperty(value = "matchingUnitType")
    private String matchingUnitType;

    @ApiModelProperty(value = "订单类型")
    private String orderType;

    @ApiModelProperty(value = "language")
    private String language;

    @ApiModelProperty(value = "hasDetail")
    private Boolean hasDetail;

    @ApiModelProperty(value = "statusCode")
    private String statusCode;

    @ApiModelProperty(value = "用户邮箱，当查询子账号信息时才返回")
    private String email;

    @ApiModelProperty(name = "order list id")
    private Long orderListId;

    @ApiModelProperty(name = "msg auth type")
    private String msgAuth;

    @ApiModelProperty(value = "冰山单")
    private String icebergQty;
}
