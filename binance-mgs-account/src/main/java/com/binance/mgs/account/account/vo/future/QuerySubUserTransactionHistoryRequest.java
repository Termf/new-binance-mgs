package com.binance.mgs.account.account.vo.future;

import com.binance.futurestreamer.api.request.Pagination;
import com.binance.futurestreamer.constant.BalanceType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("查询子账号下的资金流水")
public class QuerySubUserTransactionHistoryRequest extends Pagination {

    @ApiModelProperty(required = true, value = "子账户邮箱")
    private String email;

    @ApiModelProperty(required = false, value = "流水单号")
    private Long tranId;

    @ApiModelProperty(required = false, value = "产品代码")
    private String asset;

    @ApiModelProperty(required = false, value = "更改类型")
    private BalanceType balanceType;

    @ApiModelProperty(required = false, value = "更改描述")
    private String balanceInfo;

    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;
}
