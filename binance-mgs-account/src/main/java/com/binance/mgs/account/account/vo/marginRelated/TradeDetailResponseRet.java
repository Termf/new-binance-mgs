package com.binance.mgs.account.account.vo.marginRelated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sean w
 * @date 2021/12/10
 **/
@Data
@ApiModel("交易详细信息")
public class TradeDetailResponseRet implements Serializable {

    private static final long serialVersionUID = 6666675520718944558L;
    @ApiModelProperty(value = "交易单号")
    private Long tradeId;

    @ApiModelProperty(value = "交易时间")
    private Long time;

    @ApiModelProperty(value = "产品代码")
    private String symbol;

    @ApiModelProperty(value = "交易价格")
    private String price;

    @ApiModelProperty(value = "交易数量")
    private String qty;

    @ApiModelProperty(value = "基础资产")
    private String baseAsset;

    @ApiModelProperty(value = "标价货币")
    private String quoteAsset;

    @ApiModelProperty(value = "买卖方向")
    private String side;

    @ApiModelProperty(value = "买手续费资产")
    private String feeAsset;

    @ApiModelProperty(value = "totalQuota")
    private String totalQuota;

    @ApiModelProperty(value = "fee")
    private String fee;

    @ApiModelProperty(value = "productName")
    private String productName;

    @ApiModelProperty(value = "maker/taker")
    private String role;
}
