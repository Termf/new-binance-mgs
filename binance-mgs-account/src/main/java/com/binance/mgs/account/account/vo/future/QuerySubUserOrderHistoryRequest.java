package com.binance.mgs.account.account.vo.future;

import com.binance.futurestreamer.api.request.Pagination;
import com.binance.futurestreamer.constant.OrderType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("查询子账户委托历史订单请求")
public class QuerySubUserOrderHistoryRequest extends Pagination {


    @ApiModelProperty(required = true, value = "子账户邮箱")
    private String email;

    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "买卖方向")
    private String side;

    @ApiModelProperty(required = false, notes = "类型")
    private OrderType orderType;

    @ApiModelProperty(required = false, notes = "状态(NEW: 未成交, Partial Fill: 部分成交, Filled: 全部成交, Canceled: 已撤销)")
    private String status;

    @ApiModelProperty(required = false, notes = "是否隐藏 已撤销")
    private Boolean hideCancel;

    @ApiModelProperty(required = false, notes = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, notes = "标价货币")
    private String quoteAsset;

    @ApiModelProperty(required = false, notes = "clientOrderId")
    private String clientOrderId;
}
