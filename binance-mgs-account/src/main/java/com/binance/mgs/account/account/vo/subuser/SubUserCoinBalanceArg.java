package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/11/9.
 */
@Data
public class SubUserCoinBalanceArg {
    @NotNull
    private String coin;
    private String email;
}
