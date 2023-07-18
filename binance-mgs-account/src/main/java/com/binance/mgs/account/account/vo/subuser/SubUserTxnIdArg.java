package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by pcx
 */
@Data
public class SubUserTxnIdArg {
    @NotNull
    private Long txnId;
}
