package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("SendBindEmailVerifyCodeArg")
public class SendBindEmailVerifyCodeArg  {



    @ApiModelProperty("email")
    @NotNull
    private String email;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;

}
