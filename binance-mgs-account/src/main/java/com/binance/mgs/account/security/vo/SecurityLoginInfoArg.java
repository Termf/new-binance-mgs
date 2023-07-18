package com.binance.mgs.account.security.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SecurityLoginInfoArg extends ValidateCodeArg {

    @NotBlank
    @ApiModelProperty(required = true, allowableValues = "login,register,forget_password,create_apikey,refresh_access_token,third_login")
    private String bizType;

    @ApiModelProperty(name = "email")
    private String email;

    @ApiModelProperty(name = "mobile")
    private String mobile;

    @ApiModelProperty(name = "mobileCode")
    private String mobileCode;
}
