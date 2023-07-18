package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class UnbindMobileArg extends CommonArg {

    private static final long serialVersionUID = 6163763530444184494L;

    @ApiModelProperty("密码")
    @NotBlank
    private String password;

    @ApiModelProperty("短信验证码")
    @NotBlank
    private String smsCode;

}
