package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author rudy.c
 * @date 2021-09-13 20:04
 */
@Data
public class CreateSubUserV2Arg {
    @ApiModelProperty(value = "子账号邮箱", required = true)
    @NotEmpty
    private String email;
    @ApiModelProperty(value = "密码", required = true)
    @NotEmpty
    private String password;
    @ApiModelProperty(value = "子账号邮箱激活验证码", required = true)
    @NotEmpty
    private String emailVerifyCode;

    @ApiModelProperty("母账户手机验证码")
    private String parentMobileVerifyCode;
    @ApiModelProperty("母账户google验证码")
    private String parentGoogleVerifyCode;

    public String getEmail() {
        if(email == null) {
            return email;
        }
        return email.trim().toLowerCase();
    }
}
