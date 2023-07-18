package com.binance.mgs.account.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(
        value = "SecurityAppAttestCheckArg",
        description = "Request parameters to check Apple App Attestation"
)
public class SecurityAppAttestCheckArg {
    @ApiModelProperty(value = "sessionId", required = true)
    @NotBlank
    private String sessionId;

    @ApiModelProperty(value = "assertion", required = true)
    @NotBlank
    private String assertion;
}
