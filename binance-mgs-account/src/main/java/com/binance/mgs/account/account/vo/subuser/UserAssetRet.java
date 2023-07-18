package com.binance.mgs.account.account.vo.subuser;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户持仓")
public class UserAssetRet implements Serializable {
    private static final long serialVersionUID = -9189584321404190118L;
    @ApiModelProperty
    private String asset;
    @ApiModelProperty
    private String assetName;
    @ApiModelProperty
    private BigDecimal free;
    @ApiModelProperty
    private BigDecimal locked;
    @ApiModelProperty
    private BigDecimal freeze;
    @ApiModelProperty
    private BigDecimal withdrawing;
    @ApiModelProperty
    private BigDecimal ipoing;
    @ApiModelProperty
    private BigDecimal ipoable;
    @ApiModelProperty
    private BigDecimal storage;
    @ApiModelProperty
    private Boolean forceStatus;
    @ApiModelProperty
    private Boolean resetAddressStatus;
    @ApiModelProperty
    private Date modifiedAt;
    @ApiModelProperty
    private Date createdAt;
    @ApiModelProperty
    private int test;
    @ApiModelProperty
    private Boolean sameAddress;
    @ApiModelProperty
    private Boolean depositTipStatus;
    @ApiModelProperty
    private String assetLabel;
    @ApiModelProperty
    private String assetLabelEn;
    @ApiModelProperty
    private String depositTip;
    @ApiModelProperty
    private String depositTipEn;
    @ApiModelProperty
    private String depositTipCn;
    @ApiModelProperty
    private String chargeDescCn;
    @ApiModelProperty
    private String chargeDescEn;
    @ApiModelProperty
    private Boolean isLegalMoney;
    @ApiModelProperty
    private String logoUrl;
    @ApiModelProperty
    private String assetDetail;
}
