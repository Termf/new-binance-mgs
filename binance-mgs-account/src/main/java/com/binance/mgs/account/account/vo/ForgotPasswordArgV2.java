package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

@ApiModel("ForgotPasswordArgV2")
@Data
@EqualsAndHashCode(callSuper = false)
public class ForgotPasswordArgV2 extends ValidateCodeArg {
    @ApiModelProperty(value = "email")
    @Length(max = 255)
    private String email;
    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty("手机验证码")
    private String mobileVerifyCode;
    @ApiModelProperty("google验证码")
    private String googleVerifyCode;
    @ApiModelProperty("邮件验证码")
    private String emailVerifyCode;
    @ApiModelProperty("yubikey验证码")
    private String yubikeyVerifyCode;
}
