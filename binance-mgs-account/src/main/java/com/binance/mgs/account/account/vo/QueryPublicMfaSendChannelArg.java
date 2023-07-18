package com.binance.mgs.account.account.vo;

import com.binance.account2fa.enums.MsgType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * Description:
 *
 * @author alven
 * @since 2023/5/9
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryPublicMfaSendChannelArg {
    @NotEmpty
    @ApiModelProperty("业务场景")
    private String bizScene;

    @NotEmpty
    @ApiModelProperty("Mfa流水号")
    private String bizNo;
    
    private Boolean resend = false;

    private MsgType msgType = MsgType.TEXT;
}
