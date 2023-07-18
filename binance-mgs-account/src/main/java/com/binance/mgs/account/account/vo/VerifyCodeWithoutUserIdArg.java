package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Description:
 *
 * @author alven
 * @since 2023/3/8
 */
@Data
public class VerifyCodeWithoutUserIdArg extends ValidateCodeArg {
    @ApiModelProperty("手机号")
    private String mobile;

    @ApiModelProperty("手机代码")
    private String mobileCode;

    @ApiModelProperty("邮箱")
    private String email;

    @NotNull
    @ApiModelProperty("验证码")
    private String verifyCode;

    @NotNull
    @ApiModelProperty("验证类型")
    private String verifyType;
}
