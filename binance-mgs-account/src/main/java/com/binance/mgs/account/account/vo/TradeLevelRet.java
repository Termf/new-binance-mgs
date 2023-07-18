package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("交易等级详情")
public class TradeLevelRet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -4221519113816205131L;

    @ApiModelProperty("交易级别")
    private Integer level;

    @ApiModelProperty("BNB最低持仓额")
    private BigDecimal bnbFloor;

    @ApiModelProperty("BNB最高持仓额")
    private BigDecimal bnbCeil;

    @ApiModelProperty("BTC最低持仓数量")
    private BigDecimal btcFloor;

    @ApiModelProperty("BTC最高持仓数量")
    private BigDecimal btcCeil;

    @ApiModelProperty("被动方手续费")
    private BigDecimal makerCommission;

    @ApiModelProperty("主动方手续费")
    private BigDecimal takerCommission;

    @ApiModelProperty("老的被动方手续费")
    private BigDecimal oldMakerCommission;

    @ApiModelProperty("老的主动方手续费")
    private BigDecimal oldTakerCommission;

    @ApiModelProperty("买方交易手续费")
    private BigDecimal buyerCommission;

    @ApiModelProperty("卖方交易手续费")
    private BigDecimal sellerCommission;

    private BigDecimal btcBusdFloor;
    private BigDecimal btcBusdCeil;

    @ApiModelProperty("合约busd被动手续费")
    private BigDecimal busdMakerCommission;
    @ApiModelProperty("合约busd主动手续费")
    private BigDecimal busdTakerCommission;
}
