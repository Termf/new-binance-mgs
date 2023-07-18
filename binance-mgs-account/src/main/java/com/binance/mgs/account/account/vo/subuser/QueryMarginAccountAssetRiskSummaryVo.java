package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class QueryMarginAccountAssetRiskSummaryVo {
    private String email;
    private Boolean isSubUserEnabled;
    @ApiModelProperty("风险率")
    private String marginLevel;

    @ApiModelProperty("子账户总资产（单位：BTC")
    private String totalAssetOfBtc;

    @ApiModelProperty("子账户总负债（单位：BTC")
    private String totalLiabilityOfBtc;

    @ApiModelProperty("子账户净资产（单位：BTC")
    private String totalNetAssetOfBtc;

    @ApiModelProperty("flexLine信用额度子账号")
    private Boolean isFlexLineCreditUser = false;

    @ApiModelProperty("flexLine交易子账号")
    private Boolean isFlexLineTradingUser = false;
}
