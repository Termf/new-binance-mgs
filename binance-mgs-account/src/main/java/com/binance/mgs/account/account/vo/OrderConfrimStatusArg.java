package com.binance.mgs.account.account.vo;

import com.binance.account.common.enums.OrderConfirmType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@ApiModel("用户下单确认状态Request")
@Data
public class OrderConfrimStatusArg extends CommonArg {
    private static final long serialVersionUID = -3100988708105275305L;
    @ApiModelProperty("类型")
    @NotNull
    private OrderConfirmType orderConfirmType;

    @ApiModelProperty("true:启用 false 停用")
    private boolean status;
}
