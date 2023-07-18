package com.binance.mgs.account.fido.vo;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StartAuthPublicArg extends CommonArg {

    @ApiModelProperty("邮箱")
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

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;

    @ApiModelProperty("安全验证sessionId")
    private String sessionId;

    @ApiModelProperty("登录flowId")
    private String loginFlowId;

}
