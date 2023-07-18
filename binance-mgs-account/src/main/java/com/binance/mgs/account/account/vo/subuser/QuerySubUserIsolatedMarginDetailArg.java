package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@ApiModel("子账户逐仓详情请求参数")
@Data
public class QuerySubUserIsolatedMarginDetailArg {

    @ApiModelProperty("子账户邮箱")
    @NotEmpty
    private String subUserEmail;

    @ApiModelProperty("币对")
    private String symbols;

    @ApiModelProperty("仅显示创建的")
    private boolean onlyCreated = false;
}
