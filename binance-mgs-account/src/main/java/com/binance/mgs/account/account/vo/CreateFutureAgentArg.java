package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("创建future返佣")
public class CreateFutureAgentArg {

    @ApiModelProperty(value = "返佣码", required = true)
    private String agentCode;
}
