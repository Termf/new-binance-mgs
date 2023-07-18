package com.binance.mgs.account.account.vo.new2fa;

import com.binance.account2fa.enums.MsgType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

@ApiModel(description="发送短信")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendNewMobileVerifyCodeArg extends CommonArg {
    @ApiModelProperty("手机code")
    @NotEmpty
    private String mobileCode;
    @ApiModelProperty("手机号")
    @NotEmpty
    private String mobile;

    @ApiModelProperty("短信模板类型：文本或者语音")
    private MsgType msgType = MsgType.TEXT;


    @ApiModelProperty("是否是重新发送")
    private Boolean resend = false;

    @ApiModelProperty("业务场景")
    @NotEmpty
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
