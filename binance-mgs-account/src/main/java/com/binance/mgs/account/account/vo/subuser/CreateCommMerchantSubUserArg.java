package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Created by Kay.Zhao on 2021/2/2
 */
@ApiModel("merchant母账户注册子账户Request")
@Data
public class CreateCommMerchantSubUserArg {


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

    @ApiModelProperty(required = true, notes = "子账户业务类型,参考：com.binance.mgs.account.account.enums.SubUserBizType")
    @NotNull
    private String subUserBizType;

    @ApiModelProperty(required = true, notes = "子账号邮箱激活验证码")
    @NotNull
    private String emailVerifyCode;

    @ApiModelProperty(notes ="母账户手机验证码")
    private String parentMobileVerifyCode;

    @ApiModelProperty(notes ="母账户google验证码")
    private String parentGoogleVerifyCode;
}