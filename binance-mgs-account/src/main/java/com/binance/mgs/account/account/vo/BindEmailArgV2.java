package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@ApiModel("BindEmailArgV2")
@Getter
@Setter
public class BindEmailArgV2 extends CommonArg {

    @ApiModelProperty("邮箱")
    @NotNull
    private String email;

    @ApiModelProperty("邮箱验证码")
    @NotNull
    private String emailVerifyCode;
}
