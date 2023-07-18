package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author rudy.c
 * @date 2022-03-01 17:26
 */
@Data
public class OneStepWithdrawalInfoRet {
    @ApiModelProperty("快捷提币状态")
    private Boolean status;
    @ApiModelProperty("每日限额")
    private BigDecimal dailyLimit;
    @ApiModelProperty("单次限额")
    private BigDecimal oneTimeLimit;
}
