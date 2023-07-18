package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class IsolatedMarginParentAndSubAccountSummaryRet {

    @ApiModelProperty("母账户净资产(单位: BTC)")
    private String parentAccountNetAssetOfBtc;

    @ApiModelProperty("所有子账户净资产(单位: BTC)")
    private String allSubAccountNetAssetOfBtc = "--";
}
