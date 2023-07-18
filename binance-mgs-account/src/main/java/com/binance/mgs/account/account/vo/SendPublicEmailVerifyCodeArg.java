package com.binance.mgs.account.account.vo;

import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

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
