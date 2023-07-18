package com.binance.mgs.account.account.vo.new2fa;

import java.util.HashMap;
import java.util.Map;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.enums.MsgType;
import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("发送短信")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendMobileVerifyCodeArg extends CommonArg {

    @ApiModelProperty(value = "邮箱,web端无需传值，会根据cookie里的值自动反推获取")
    private String email;

    @ApiModelProperty("手机code")
    private String mobileCode;
    @ApiModelProperty("手机号")
    private String mobile;

    @ApiModelProperty("短信模板类型：文本或者语音")
    private MsgType msgType = MsgType.TEXT;


    @ApiModelProperty("是否是重新发送")
    private Boolean resend = false;

    @ApiModelProperty("业务场景")
    private String bizScene;

    @ApiModelProperty(required = false, notes = "userChannel")
    private String userChannel;

    /**
     * 各场景所需参数Key
     * api_key_manage:apiName
     */
    @ApiModelProperty("邮件所需参数")
    private Map<String, Object> params = new HashMap<>();
}
