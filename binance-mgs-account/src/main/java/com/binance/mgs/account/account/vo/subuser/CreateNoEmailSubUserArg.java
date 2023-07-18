package com.binance.mgs.account.account.vo.subuser;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * Created by pcx
 */
@Data
public class CreateNoEmailSubUserArg {
    @NotNull
    private String userName;
    @NotNull
    private String password;
    @NotNull
    private String confirmPassword;
}
