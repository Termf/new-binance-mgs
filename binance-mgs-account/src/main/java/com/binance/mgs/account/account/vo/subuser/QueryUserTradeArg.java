package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonPageArg;
import com.binance.platform.mgs.enums.AccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel(description = "查询用户历史成交 request", value = "查询用户历史成交 request")
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryUserTradeArg extends CommonPageArg {
    private static final long serialVersionUID = 6957412634440582914L;
    /**
     *
     */


    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;

    @ApiModelProperty(required = false, notes = "订单Id")
    private Long orderId;

    @ApiModelProperty(required = false, notes = "买卖方向")
    private String direction;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, notes = "标价货币")
    private String quoteAsset;
    @ApiModelProperty(required = false, notes = "账户类型，默认为MAIN主账户")
    private AccountType accountType = AccountType.MAIN;

}
