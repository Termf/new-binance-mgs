package com.binance.mgs.account.account.vo;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class OauthUnbindUserArg {
    @NotBlank
    private String clientId;
}
