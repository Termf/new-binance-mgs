package com.binance.mgs.account.account.vo;

import com.binance.account.vo.security.enums.MsgType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ApiModel("发送绑定手机验证码Arg")
public class SendBindMobileVerifyCodeArg extends CommonArg {

    private static final long serialVersionUID = 1937072660150984696L;


    @ApiModelProperty("手机号")
    @NotBlank
    private String mobile;

    @ApiModelProperty("手机代码")
    @NotBlank
    private String mobileCode;

    @ApiModelProperty("短信模板类型：文本或者语音")
    private MsgType msgType = MsgType.TEXT;


    @ApiModelProperty("是否是重新发送")
    private Boolean resend = false;

    @ApiModelProperty(required = false, notes = "userChannel")
    private String userChannel;

}
