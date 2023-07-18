package com.binance.mgs.account.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(
        value = "SecurityAppAttestationArg",
        description = "Request parameters to verify Apple App Attestation"
)
public class SecurityAppAttestationArg {

    @NotBlank
    @ApiModelProperty(name = "attestation", required = true)
    private String attestation;
}
