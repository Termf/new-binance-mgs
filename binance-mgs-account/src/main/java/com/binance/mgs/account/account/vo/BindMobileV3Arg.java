package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("绑定手机ArgV3,接入了MFA")
@Getter
@Setter
public class BindMobileV3Arg extends CommonArg {

    @ApiModelProperty("手机号")
    @NotBlank
    private String mobile;

    @ApiModelProperty("手机代码")
    @NotBlank
    private String mobileCode;

    @ApiModelProperty("手机验证码")
    @NotBlank
    private String mobileVerifyCode;

}
