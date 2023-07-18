package com.binance.mgs.account.account.vo.future;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("QueryPositionLimitAdjustmentArg")
public class QueryPositionLimitAdjustmentArg {

    @NotBlank
    @ApiModelProperty("子账户邮箱")
    private String subUserEmail;
}
