package com.binance.mgs.account.account.vo;

import javax.validation.constraints.NotNull;

import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel("UserComplianceArg")
@Getter
@Setter
public class UserComplianceArg extends CommonArg {

    @ApiModelProperty("productLine")
    @NotNull
    private String productLine;

    @ApiModelProperty("operation")
    @NotNull
    private String operation;
}
