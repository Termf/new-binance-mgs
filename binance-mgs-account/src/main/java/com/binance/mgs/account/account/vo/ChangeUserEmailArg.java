package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@ApiModel("发送邮件激活码change")
@Data
@EqualsAndHashCode(callSuper = false)
public class ChangeUserEmailArg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = 3811894527125306971L;


    @ApiModelProperty("新邮箱")
    @NotBlank
    private String newEmail;

    @ApiModelProperty("新邮件验证码")
    @NotBlank
    private String newEmailVerifyCode;

}
