package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@ApiModel
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ActiveUserV2Ret implements Serializable {


    @ApiModelProperty(required = true, notes = "用户id")
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

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}
