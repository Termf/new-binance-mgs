package com.binance.mgs.account.account.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ApiModel("当前委托订单信息")
public class OpenOrderRet implements Serializable {


    private static final long serialVersionUID = -1925669478835225420L;
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "订单号")
    private Long orderId;

    @ApiModelProperty(value = "订单号字符串")
    @Long2String
    private Long orderIdStr;

    @ApiModelProperty(value = "交易账号")
    private Long accountId;

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "产品代码")
    private String symbol;

    @ApiModelProperty(value = "客户单号")
    private String clientOrderId;

    @ApiModelProperty(value = "原始客户单号")
    private String origClientOrderId;

    @ApiModelProperty(value = "价格")
    private String price;

    @ApiModelProperty(value = "平均价格")
    private String avgPrice;

    @ApiModelProperty(value = "下单数量")
    private String origQty;

    @ApiModelProperty(value = "qty")
    private BigDecimal qty;

    @ApiModelProperty(value = "money")
    private BigDecimal money;

    @ApiModelProperty(value = "成交数量")
    private String executedQty;

    @ApiModelProperty(value = "已成交金额")
    private String executedQuoteQty;

    @ApiModelProperty(value = "tradedVolume")
    private BigDecimal tradedVolume;

    @ApiModelProperty(value = "状态(NEW: 未成交, PARTIALLY_FILLED: 部分成交, FILLED: 全部成交, CANCELLED: 已撤销)")
    private String status;

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

    @ApiModelProperty(value = "用户邮箱，当查询子账号信息时才返回")
    private String email;

    @ApiModelProperty(name = "order list id")
    private Long orderListId;

    @ApiModelProperty(name = "msg auth type")
    private String msgAuth;
}
