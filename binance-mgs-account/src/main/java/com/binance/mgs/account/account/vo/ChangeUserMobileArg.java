package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("发送mobile激活码change")
@Data
@EqualsAndHashCode(callSuper = false)
public class ChangeUserMobileArg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = 3821894527125306971L;


    @ApiModelProperty("新手机号")
    @NotBlank
    private String newMobile;

    @ApiModelProperty("新手机代码")
    @NotBlank
    private String newMobileCode;

    @ApiModelProperty("新手机验证码")
    @NotBlank
    private String newMobileVerifyCode;

}
