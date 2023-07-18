package com.binance.mgs.account.account.vo.accountMerge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("GiveUpMergeArg")
@Data
public class GiveUpMergeArg {

    @ApiModelProperty("合并账号流程id")
    private String flowId;

    @ApiModelProperty("步骤，比如CONFIRM_INFO、GIVE_UP_ASSET")
    private String step;
    
}
