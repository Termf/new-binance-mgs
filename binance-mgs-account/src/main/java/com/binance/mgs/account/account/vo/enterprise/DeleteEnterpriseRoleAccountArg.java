package com.binance.mgs.account.account.vo.enterprise;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author dana.d
 */
@ApiModel("删除企业角色账户Request")
@Data
public class DeleteEnterpriseRoleAccountArg {

    @ApiModelProperty("企业角色账号userId")
    @NotNull
    private Long roleAccountUserId;

    @ApiModelProperty("操作人账户手机验证码")
    private String operatorMobileVerifyCode;

    @ApiModelProperty("操作人账户google验证码")
    private String operatorGoogleVerifyCode;
}
