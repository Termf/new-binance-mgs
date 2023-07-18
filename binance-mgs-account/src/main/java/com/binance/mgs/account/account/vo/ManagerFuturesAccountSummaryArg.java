package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author rudy.chen
 * @date 2021-08-11 14:09
 */
@Data
public class ManagerFuturesAccountSummaryArg {
    @ApiModelProperty(value = "托管子账户邮箱", required = true)
    @NotEmpty
    private String email;
}
