package com.binance.mgs.account.account.vo.accountMerge;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("MergeFlowArg")
@Data
public class MergeFlowArg {

    @ApiModelProperty(required = true, notes = "合并账号流程id")
    @NotNull
    private String flowId;
    
}
