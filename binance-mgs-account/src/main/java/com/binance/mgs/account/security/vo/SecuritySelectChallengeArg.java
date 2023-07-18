package com.binance.mgs.account.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel(value = "SecuritySelectChallengeArg")
public class SecuritySelectChallengeArg {

    @NotNull
    @ApiModelProperty(required = true)
    private List<String> supportedTypes;
}
