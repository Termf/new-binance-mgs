package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueryFuturesAccountSummaryArg {
    @NotNull
    private Integer page;
    @NotNull
    private Integer rows;

    private String email;
    @ApiModelProperty(notes = "子账户开启状态,1:开启; 0:未开启")
    private String isSubUserEnabled;
}
