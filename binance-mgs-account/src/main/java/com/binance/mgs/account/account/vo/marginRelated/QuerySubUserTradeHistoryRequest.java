package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.streamer.api.request.Pagination;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author sean w
 * @date 2021/9/29
 **/
@Data
@ApiModel(description = "查询子账户历史成交 request", value = "查询子账户历史成交 request")
public class QuerySubUserTradeHistoryRequest extends Pagination {

    private static final long serialVersionUID = -3514503598668237258L;

    @ApiModelProperty(required = false, value = "子账号邮箱")
    private String email;

    @ApiModelProperty(required = false, value = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, value = "结束时间")
    private Long endTime;

    @ApiModelProperty(required = false, value = "买卖方向")
    private String direction;

    @ApiModelProperty(required = false, value = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, value = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, value = "标价货币")
    private String quoteAsset;

    @ApiModelProperty(required = false, notes = "订单Id")
    private Long orderId;
}
