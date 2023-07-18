package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MarginParentAndSubaccountSummaryRet {
    /**
     * 统计相关信息
     * */
    @ApiModelProperty("母账户净资产（单位：BTC")
    private String masterAccountNetAssetOfBtc;

    @ApiModelProperty("所有子账户总资产（单位：BTC")
    private String totalAssetOfBtc;

    @ApiModelProperty("所有子账户总负债（单位：BTC")
    private String totalLiabilityOfBtc;

    @ApiModelProperty("所有子账户净资产（单位：BTC")
    private String totalNetAssetOfBtc;
}
