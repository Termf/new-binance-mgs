package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.streamer.api.response.vo.OpenOrderVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author sean w
 * @date 2021/9/28
 **/
@Data
@ApiModel("当前委托订单信息返回 response")
public class OpenOrderRet implements Serializable {

    private static final long serialVersionUID = 948615833880478581L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "订单号")
    private Long orderId;

    @ApiModelProperty(value = "交易账号")
    private Long accountId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

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

    @ApiModelProperty(value = "原始状态(NEW、PARTIALLY_FILLED、FILLED、CANCELLED)")
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

    @ApiModelProperty(value = "order list id")
    private Long orderListId;

    @ApiModelProperty(value = "msg auth type")
    private String msgAuth;

    @ApiModelProperty(value = "冰山单")
    private String icebergQty;

    public static OpenOrderRet of (OpenOrderVo openOrderVo) {

        OpenOrderRet openOrderRet = new OpenOrderRet();
        BeanUtils.copyProperties(openOrderVo, openOrderRet);

        return openOrderRet;
    }
}
