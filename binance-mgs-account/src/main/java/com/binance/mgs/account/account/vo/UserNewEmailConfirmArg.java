package com.binance.mgs.account.account.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserNewEmailConfirmArg {

    @NotBlank
    private String flowId;
    @NotBlank
    private String email;
    @NotBlank
    private String pwd;

    @NotBlank
    private String newSafePwd;

    @NotBlank
    private String sign;

}
