package com.binance.mgs.account.account.vo;

import com.binance.account.vo.security.enums.MsgType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("发送短信")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendMobileVerifyCodeArg extends CommonArg {

    /**
     *
     */
    private static final long serialVersionUID = 5419758629714566927L;
    @ApiModelProperty(value = "邮箱,web端无需传值，会根据cookie里的值自动反推获取")
    private String email;

    @ApiModelProperty("短信模板类型：文本或者语音")
    private MsgType msgType = MsgType.TEXT;


    @ApiModelProperty("是否是重新发送")
    private Boolean resend = false;

    @ApiModelProperty(required = false, notes = "userChannel")
    private String userChannel;
}
