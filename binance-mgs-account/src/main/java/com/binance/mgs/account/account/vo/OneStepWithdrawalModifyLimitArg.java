package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author rudy.c
 * @date 2022-03-21 17:09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OneStepWithdrawalModifyLimitArg extends MultiCodeVerifyArg {
    @ApiModelProperty(value = "单次限额", required = true)
    @NotNull
    @Min(0)
    private BigDecimal oneTimeLimit;
}
