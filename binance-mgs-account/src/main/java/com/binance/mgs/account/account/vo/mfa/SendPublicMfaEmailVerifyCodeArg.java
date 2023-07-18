package com.binance.mgs.account.account.vo.mfa;

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
public class SendPublicMfaEmailVerifyCodeArg {
    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;

    @NotEmpty
    @ApiModelProperty("业务场景")
    private String bizScene;

    @NotEmpty
    @ApiModelProperty("Mfa流水号")
    private String bizNo;
}
