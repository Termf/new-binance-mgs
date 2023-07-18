package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel(description = "查询子账户历史成交 request", value = "查询子账户历史成交 request")
@Data
@EqualsAndHashCode(callSuper = false)
public class SubQueryUserTradeArg extends QueryUserTradeArg {
    private static final long serialVersionUID = 7452525187974127811L;
    /**
     *
     */

    @ApiModelProperty(required = false, notes = "子账号，不传默认查询所有子账号信息")
    private String userId;
}
