package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizeForNewProcessV2Arg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = -2109784645843306006L;

    @ApiModelProperty(value = "email")
    private String email;
    @ApiModelProperty(value = "mobileCode")
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    private String mobile;

    @ApiModelProperty(value = "用于登陆绑定三方账户")
    private String registerToken;

    @ApiModelProperty(value = "登陆涞源，如果是ThreeParty则是三方登陆")
    private String threePartySource;

    @ApiModelProperty(value = "fido 2fa token")
    private String verifyTokenFIDO;

    @ApiModelProperty(value = "otp 2fa token")
    private String verifyTokenOTP;
}
