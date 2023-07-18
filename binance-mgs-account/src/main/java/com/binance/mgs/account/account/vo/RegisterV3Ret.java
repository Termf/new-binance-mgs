package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("RegisterV3Ret")
public class RegisterV3Ret  {
    /**
     * 
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(required = true, notes = "账号")
    private String email;

    @ApiModelProperty(required = true, notes = "交易账号")
    private Long tradingAccount;

    @ApiModelProperty("下发的新token")
    private String token;
    @ApiModelProperty("下发的新token 关联的csrftoken")
    private String csrfToken;
    // oauth code
    private String code;

    private String refreshToken;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}
