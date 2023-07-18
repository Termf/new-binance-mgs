package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("是否使用BNB支付交易手续费开关")
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateCommissionStatusArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = 5419758629714566927L;
    @ApiModelProperty(value = "是否使用BNB支付交易手续费")
    private boolean isUseBnbFee;
}
