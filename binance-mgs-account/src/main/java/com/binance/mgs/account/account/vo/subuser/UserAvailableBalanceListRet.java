package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("子账户各币种资产资产详情")
@Data
public class UserAvailableBalanceListRet {

    private String coin;

    private String free;

    @ApiModelProperty("是否为ETF币种")
    private boolean etf;
}
