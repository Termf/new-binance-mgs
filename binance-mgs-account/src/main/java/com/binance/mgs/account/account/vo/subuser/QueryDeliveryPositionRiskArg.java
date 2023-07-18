package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class QueryDeliveryPositionRiskArg {
    @NotBlank
    @Email
    private String email;
    private String pair;
}
