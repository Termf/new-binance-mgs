package com.binance.mgs.account.account.vo.new2fa;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.enums.MsgType;
import org.hibernate.validator.constraints.Length;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("SendPublicMobileVerifyCodeArg")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendPublicMobileVerifyCodeArg extends ValidateCodeArg {

    @ApiModelProperty(value = "email")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;


    @ApiModelProperty("短信模板类型：文本或者语音")
    private MsgType msgType= MsgType.TEXT;

    @ApiModelProperty("业务场景")
    private String bizScene;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;


    @ApiModelProperty("是否是新版本")
    private Boolean newRegisterVersion=false;

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;

    @ApiModelProperty(required = false, notes = "userChannel")
    private String userChannel;
}
