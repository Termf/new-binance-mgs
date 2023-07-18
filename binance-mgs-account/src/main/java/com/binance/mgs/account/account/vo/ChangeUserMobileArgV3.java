package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@ApiModel("发送mobile激活码changeV3,接入MFA")
@Data
@EqualsAndHashCode(callSuper = false)
public class ChangeUserMobileArgV3 extends CommonArg {

    @ApiModelProperty("新手机号")
    @NotBlank
    private String newMobile;

    @ApiModelProperty("新手机代码")
    @NotBlank
    private String newMobileCode;

    @ApiModelProperty("新手机验证码")
    @NotBlank
    private String newMobileVerifyCode;

}
