package com.binance.mgs.account.account.vo.future;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("查询子账户委托订单")
public class QuerySubUserOpenOrderRequest {
    @ApiModelProperty("产品代码")
    private String symbol;
    
    @ApiModelProperty(required = true,name = "子账号邮箱")
    private String email;
}
