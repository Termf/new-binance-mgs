package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class BindGoogleVerifyV3Arg extends CommonArg {

    private static final long serialVersionUID = 4386377450477080457L;

    @ApiModelProperty(value = "秘钥", required = true)
    @NotBlank
    private String secretKey;

    @ApiModelProperty("google验证码")
    private String googleVerifyCode;
}
