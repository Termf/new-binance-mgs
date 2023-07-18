package com.binance.mgs.account.account.vo.enterprise;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author dana.d
 */
@ApiModel("注册角色子账户Request")
@Data
public class CreateEnterpriseRoleAccountArg {
    @ApiModelProperty(required = true, notes = "邮箱")
    @NotBlank
    private String email;

    @ApiModelProperty(required = true, notes = "密码")
    @NotBlank
    private String password;

    @ApiModelProperty(required = true, notes = "确认密码")
    @NotBlank
    private String confirmPassword;

    @ApiModelProperty(required = false, notes = "子账号备注")
    private String remark;

    @ApiModelProperty(required = true, notes = "角色id")
    @NotNull
    private Long roleId;

    @ApiModelProperty(required = true, notes = "子账号邮箱激活验证码")
    @NotBlank
    private String emailVerifyCode;

    @ApiModelProperty("操作人账户手机验证码")
    private String operatorMobileVerifyCode;

    @ApiModelProperty("操作人账户google验证码")
    private String operatorGoogleVerifyCode;
}
