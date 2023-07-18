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
@ApiModel("查询子账户委托历史订单请求")
public class QuerySubUserOrderHistoryRequest extends Pagination {

    private static final long serialVersionUID = 5236508960022469644L;

    @ApiModelProperty(required = false, value = "子账号邮箱")
    private String email;

    @ApiModelProperty(required = false, value = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, value = "结束时间")
    private Long endTime;

    @ApiModelProperty(required = false, value = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, value = "买卖方向")
    private String direction;

    @ApiModelProperty(required = false, value = "状态(NEW: 未成交, Partial Fill: 部分成交, Filled: 全部成交, Canceled: 已撤销)")
    private String status;

    @ApiModelProperty(required = false, value = "是否隐藏 已撤销")
    private Boolean hideCancel;

    @ApiModelProperty(required = false, value = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, value = "标价货币")
    private String quoteAsset;
}
