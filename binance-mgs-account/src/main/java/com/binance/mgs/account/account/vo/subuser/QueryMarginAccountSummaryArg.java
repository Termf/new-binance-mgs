package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueryMarginAccountSummaryArg {
    @NotNull
    private Integer page;
    @NotNull
    private Integer rows;

    private String email;
    private String isSubUserEnabled;
}
