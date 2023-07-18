package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class BindGoogleVerifyArg extends CommonArg {

    private static final long serialVersionUID = 4386377450477080457L;

    @ApiModelProperty(value = "密码", required = true)
    @NotBlank
    private String password;

    @ApiModelProperty(value = "Google验证码", required = true)
    @NotNull
    private Integer googleCode;

    @ApiModelProperty(value = "秘钥", required = true)
    @NotBlank
    private String secretKey;

    @ApiModelProperty(value="短信验证码",required = true)
    private String smsCode;
}
