package com.binance.mgs.account.account.vo.delivery;

import com.binance.deliverystreamer.api.request.Pagination;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class QueryTradeDetailRequest extends Pagination {

    private static final long serialVersionUID = 2068810512758073326L;
    @NotNull
    @ApiModelProperty(required = true, notes = "orderId")
    private Long orderId;

    @ApiModelProperty( notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = true, notes = "当前订单归属的userId")
    private Long userId;
}
