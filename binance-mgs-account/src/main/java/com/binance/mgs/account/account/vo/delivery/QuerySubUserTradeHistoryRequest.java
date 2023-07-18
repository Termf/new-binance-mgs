package com.binance.mgs.account.account.vo.delivery;

import com.binance.deliverystreamer.api.request.Pagination;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("下查询子账户历史订单")
public class QuerySubUserTradeHistoryRequest extends Pagination {


    @ApiModelProperty(required = true, value = "子账户邮箱")
    private String email;

    @ApiModelProperty(required = false, notes = "操作行为")
    private String action;

    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;

    @ApiModelProperty(required = false, notes = "订单Id")
    private Long orderId;

    @ApiModelProperty(required = false, notes = "买卖方向")
    private String side;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, notes = "标价货币")
    private String quoteAsset;
}
