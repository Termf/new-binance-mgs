package com.binance.mgs.account.account.vo.mfa;

import com.binance.account2fa.enums.MsgType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * Description:
 *
 * @author alven
 * @since 2023/5/3
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SendPublicMfaMobileVerifyCodeArg {
    @ApiModelProperty("userChannel")
    private String userChannel;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;

    @ApiModelProperty("短信模板类型：文本或者语音")
    private MsgType msgType= MsgType.TEXT;

    @NotEmpty
    @ApiModelProperty("业务场景")
    private String bizScene;

    @NotEmpty
    @ApiModelProperty("Mfa流水号")
    private String bizNo;
}
