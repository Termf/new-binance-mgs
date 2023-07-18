package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@ApiModel(description = "母账号激活子账号request")
@Data
public class SubUserActiveV2Arg extends CommonArg {

    private static final long serialVersionUID = 8395725384481648012L;

    @ApiModelProperty(value = "邮箱", required = true)
    @NotNull
    private Long subUserId;
    
    @ApiModelProperty(value = "激活验证码", required = true)
    @NotBlank
    private String emailVerifyCode;

    @ApiModelProperty("母账户手机验证码")
    private String parentMobileVerifyCode;
    @ApiModelProperty("母账户google验证码")
    private String parentGoogleVerifyCode;

}
