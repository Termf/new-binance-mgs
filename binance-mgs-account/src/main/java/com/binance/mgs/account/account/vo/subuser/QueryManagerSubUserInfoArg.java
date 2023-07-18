package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
@Data
public class QueryManagerSubUserInfoArg {
    @ApiModelProperty(required = true, notes = "托管账号email")
    private String managerSubUserEmail;

    private String isSubUserEnabled;

    @ApiModelProperty(required = true, notes = "page")
    @NotNull
    private Integer page=1;

    @ApiModelProperty(required = true, notes = "size")
    @NotNull
    private Integer size=50;
}
