package com.binance.mgs.account.account.vo.oauth;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OauthGetBindUserArg {

    @NotBlank
    private String clientId;

}
