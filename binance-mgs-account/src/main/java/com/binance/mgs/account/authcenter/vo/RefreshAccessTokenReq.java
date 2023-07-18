package com.binance.mgs.account.authcenter.vo;

import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@ApiModel("RefreshAccessTokenReq")
@Data
public class RefreshAccessTokenReq extends ValidateCodeArg {


    @ApiModelProperty("用来续期登录态的token")
    @NotBlank
    private String refreshToken;

    @ApiModelProperty("手机验证码")
    private String mobileVerifyCode;
    @ApiModelProperty("google验证码")
    private String googleVerifyCode;
    @ApiModelProperty("邮件验证码")
    private String emailVerifyCode;
    @ApiModelProperty("yubikey验证码")
    private String yubikeyVerifyCode;
    @ApiModelProperty("fido验证码")
    private String fidoVerifyCode;

    @ApiModelProperty("校验2fa的token")
    private String verifyToken;

}
