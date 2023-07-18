package com.binance.mgs.account.account.vo.future;

import com.binance.futurestreamer.api.request.Pagination;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class QueryTradeDetailRequest extends Pagination {

    private static final long serialVersionUID = -7490302842674809097L;

    @NotNull
    @ApiModelProperty(required = true, notes = "orderId")
    private Long orderId;

    @ApiModelProperty(required = true, notes = "当前订单归属的userId")
    private Long userId;

}
