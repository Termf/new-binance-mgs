package com.binance.mgs.account.account.vo.subuser;

import com.binance.margin.isolated.api.profit.request.PeriodType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@ApiModel("查询子账户盈亏请求参数")
@Data
public class QuerySubUserIsolatedMarginProfitArg {

    @ApiModelProperty("子账户邮箱")
    private String subUserEmail;

    @ApiModelProperty("币对")
    private String symbol;

    @ApiModelProperty("统计维度")
    @NotNull
    private PeriodType periodType = PeriodType.TODAY;
}
