package com.binance.mgs.account.account.vo;

import com.binance.account2fa.enums.MsgType;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@ApiModel("查询用户发送渠道(public)")
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryPublicSendChannelArg extends ValidateCodeArg {

    private String mobileCode;

    private String mobile;

    @ApiModelProperty(
            required = false,
            notes = "是否是resend"
    )
    private Boolean resend = false;

    private MsgType msgType = MsgType.TEXT;

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;
}
