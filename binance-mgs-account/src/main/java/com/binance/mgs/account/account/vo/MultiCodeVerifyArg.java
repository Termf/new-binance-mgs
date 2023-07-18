package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: mingming.sheng
 * @Date: 2020/4/29 4:18 下午
 */
@Data
public class MultiCodeVerifyArg extends CommonArg {
    private static final long serialVersionUID = -706386303074058369L;

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
    @ApiModelProperty("fido外部验证码")
    private String fidoExternalVerifyCode;
    @ApiModelProperty("passkeys验证码")
    private String passkeysVerifyCode;
    @ApiModelProperty("roaming验证码")
    private String roamingVerifyCode;

}
