package com.binance.mgs.account.account.vo.new2fa;

import javax.validation.constraints.NotNull;

import com.binance.account2fa.enums.BizSceneEnum;
import org.hibernate.validator.constraints.Length;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("SendPublicEmailVerifyCodeArg")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendPublicEmailVerifyCodeArg extends ValidateCodeArg {
    private static final long serialVersionUID = -5339241585994756757L;

    @ApiModelProperty(value = "email")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty("业务场景")
    @NotNull
    private BizSceneEnum bizScene;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;


    @ApiModelProperty("是否是新版本")
    private Boolean newRegisterVersion=false;

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;
}
