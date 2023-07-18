package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class ChangeGoogleVerifyArg extends CommonArg {


    @ApiModelProperty(value = "秘钥", required = true)
    @NotBlank
    private String secretKey;

    @ApiModelProperty("google验证码")
    private String googleVerifyCode;
}
