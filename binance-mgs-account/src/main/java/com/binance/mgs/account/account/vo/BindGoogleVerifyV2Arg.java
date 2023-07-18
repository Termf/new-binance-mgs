package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class BindGoogleVerifyV2Arg extends MultiCodeVerifyArg {

    private static final long serialVersionUID = 4386377450477080457L;

    @ApiModelProperty(value = "秘钥", required = true)
    @NotBlank
    private String secretKey;
}
