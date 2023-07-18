package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ApiModel("用户注册邮箱确认新流程")
public class RegisterConfirmForNewProcessArg {
    @ApiModelProperty(value = "email")
    @NotBlank
    @Length(max = 255)
    private String email;
    @ApiModelProperty(value = "校验码")
    @NotBlank
    private String verifyCode;

}
