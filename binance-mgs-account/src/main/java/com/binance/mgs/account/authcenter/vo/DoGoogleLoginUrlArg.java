package com.binance.mgs.account.authcenter.vo;

import com.binance.accountoauth.enums.ThirdOperatorEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("google登录参数")
@Data
public class DoGoogleLoginUrlArg{
    private static final long serialVersionUID = 786169372777989179L;

    @NotBlank
    private String idToken;

    @ApiModelProperty(value = "安全验证sessionId")
    @NotBlank
    private String sessionId;

    @NotNull
    private ThirdOperatorEnum thirdOperatorEnum;
}