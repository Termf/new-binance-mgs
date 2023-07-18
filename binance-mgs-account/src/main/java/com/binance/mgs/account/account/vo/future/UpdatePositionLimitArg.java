package com.binance.mgs.account.account.vo.future;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("UpdatePositionLimitArg")
public class UpdatePositionLimitArg {

    @NotBlank
    @ApiModelProperty("子账户邮箱")
    private String subUserEmail;

    @NotBlank
    @ApiModelProperty("交易对")
    private String symbol;

    @NotNull
    @ApiModelProperty("是否上调position-limit")
    private boolean increase;
}
