package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("绑定手机Arg")
@Getter
@Setter
public class BindMobileV2Arg extends MultiCodeVerifyArg {

    private static final long serialVersionUID = 6910076833119975017L;

    @ApiModelProperty("手机号")
    @NotBlank
    private String mobile;

    @ApiModelProperty("手机代码")
    @NotBlank
    private String mobileCode;
}
