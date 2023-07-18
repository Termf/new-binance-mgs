package com.binance.mgs.account.account.vo;

import com.binance.account2fa.enums.MsgType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@ApiModel("查询用户发送渠道(protect)")
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryProtectSendChannelArg {

    private String mobileCode;

    private String mobile;

    @ApiModelProperty(
            required = false,
            notes = "是否是resend"
    )
    private Boolean resend = false;

    private MsgType msgType = MsgType.TEXT;
}
