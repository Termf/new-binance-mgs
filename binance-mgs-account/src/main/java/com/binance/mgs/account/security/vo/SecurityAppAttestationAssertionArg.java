package com.binance.mgs.account.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(
        value = "SecurityAppAttestationAssertionArg",
        description = "Request parameters to verify Apple App Attestation Assertion"
)
public class SecurityAppAttestationAssertionArg {

    @NotBlank
    @ApiModelProperty(name = "assertion", required = true)
    private String assertion;
}
