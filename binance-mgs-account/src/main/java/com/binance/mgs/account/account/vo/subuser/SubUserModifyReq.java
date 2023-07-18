package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author pengchenxue
 */
@Data
public class SubUserModifyReq {
    @NotNull
    private Long subAccountUserId;
    @NotNull
    private String modifyEmail;
    @NotNull
    private String authType;
    @NotNull
    private String code;
    @ApiModelProperty("子账号邮箱验证码")
    private String emailVerifyCode;
}
