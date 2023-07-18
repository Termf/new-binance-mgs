package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueryManagerSubUserAssetDetailArg {
    @ApiModelProperty(required = true, notes = "托管账号email")
    @NotNull
    private String managerSubUserEmail;
}
