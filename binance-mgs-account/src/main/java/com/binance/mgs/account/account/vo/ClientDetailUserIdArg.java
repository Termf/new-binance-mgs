package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ClientDetailUserIdArg {

    @ApiModelProperty(required = false, notes = "第三方编号")
    private String clientId;
}
