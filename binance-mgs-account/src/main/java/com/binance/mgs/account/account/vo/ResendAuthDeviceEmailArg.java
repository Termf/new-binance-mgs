package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("设备授权Arg")
public class ResendAuthDeviceEmailArg {

    @ApiModelProperty(notes = "账号")
    private String email;

}
