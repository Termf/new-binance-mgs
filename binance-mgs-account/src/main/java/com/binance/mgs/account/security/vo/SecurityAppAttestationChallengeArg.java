package com.binance.mgs.account.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(
        value = "SecurityAppAttestationChallengeArg",
        description = "Request parameters to generate new App Check security challenge"
)
public class SecurityAppAttestationChallengeArg {

    @NotBlank
    @ApiModelProperty(name = "deviceId", required = true)
    private String deviceId;
}
