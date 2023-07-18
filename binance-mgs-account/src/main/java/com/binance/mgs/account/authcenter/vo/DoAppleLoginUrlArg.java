package com.binance.mgs.account.authcenter.vo;

import com.binance.accountoauth.enums.ThirdOperatorEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("apple登录参数")
@Data
@EqualsAndHashCode(callSuper = false)
public class DoAppleLoginUrlArg{
    @NotBlank
    private String code;

    @NotBlank
    private String idToken;

    @ApiModelProperty(value = "安全验证sessionId")
    @NotBlank
    private String sessionId;

    @NotNull
    private ThirdOperatorEnum thirdOperatorEnum;

    @NotBlank
    private String redirectUrl;
}