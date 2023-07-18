package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ApiModel
@Data
public class QueryApiKeyWhitelistArg {

    @NotNull
    @Min(1)
    @ApiModelProperty("keyId")
    private Long keyId;
}
