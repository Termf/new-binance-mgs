package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonArg;
import com.binance.platform.mgs.enums.AccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel(description = "查询交易详情信息列表 request", value = "查询交易详情信息列表 request")
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryTradeDetailsArg extends CommonArg {
    /**
     *
     */
    private static final long serialVersionUID = -2230953930347157036L;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "订单Id")
    private Long orderId;

    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;
    @ApiModelProperty(required = false, notes = "账户类型，默认为MAIN主账户")
    private AccountType accountType = AccountType.MAIN;
}
