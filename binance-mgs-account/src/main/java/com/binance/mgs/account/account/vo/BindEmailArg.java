package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@ApiModel("BindEmailArg")
@Getter
@Setter
public class BindEmailArg extends MultiCodeVerifyArg {

    @ApiModelProperty("邮箱")
    @NotNull
    private String email;
}
