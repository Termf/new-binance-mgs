package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("ApiKeyRiskAgreementArg")
public class ApiKeyRiskDisclaimerArg {
    @NotBlank
    @Length(max = 20)
    private String configValue; // opt-in or opt-out
}
