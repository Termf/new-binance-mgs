package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.streamer.api.request.Pagination;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author sean w
 * @date 2021/10/9
 **/
@Data
@ApiModel("查询子账户委资金流水请求")
public class QuerySubUserCapitalFlowRequest extends Pagination {

    private static final long serialVersionUID = 1694621125883199584L;

    @ApiModelProperty(required = true,name = "子账号邮箱")
    private String email;

    @ApiModelProperty("资产名称")
    private String asset;

    @ApiModelProperty("交易对")
    private String symbol;

    @ApiModelProperty("资金流水类型")
    private CapitalFlowType type;

    @ApiModelProperty(value = "开始时间", required = true)
    @NotNull(message = "开始时间不能为空")
    private Long startTime;

    @ApiModelProperty(value = "结束时间 开始时间和结束时间不能超过3个月", required = true)
    @NotNull(message = "结束时间不能为空")
    private Long endTime;
}
