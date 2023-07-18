package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("RefreshAccessTokenResp")
@Data
public class RefreshAccessTokenResp {

    @ApiModelProperty("下发的新token")
    private String token;
    @ApiModelProperty("下发的新token 关联的csrftoken")
    private String csrfToken;
    // oauth code
    private String code;

    private String refreshToken;
}
