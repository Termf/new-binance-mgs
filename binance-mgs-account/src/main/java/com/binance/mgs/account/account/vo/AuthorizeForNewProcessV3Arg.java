package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author rudy.c
 * @date 2023-03-02 16:41
 */
@Data
public class AuthorizeForNewProcessV3Arg extends CommonArg {
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
}
