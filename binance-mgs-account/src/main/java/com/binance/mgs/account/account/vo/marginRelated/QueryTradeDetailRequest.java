package com.binance.mgs.account.account.vo.marginRelated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author sean w
 * @date 2021/12/10
 **/
@ApiModel(description = "查询交易详情request", value = "查询交易详情request")
@Data
public class QueryTradeDetailRequest implements Serializable {

    private static final long serialVersionUID = -4512206079797637005L;
    @ApiModelProperty(required = true, value = "子账户邮箱")
    @NotNull
    private String email;

    @ApiModelProperty(required = true, notes = "订单Id")
    @NotNull
    private Long orderId;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;
}
