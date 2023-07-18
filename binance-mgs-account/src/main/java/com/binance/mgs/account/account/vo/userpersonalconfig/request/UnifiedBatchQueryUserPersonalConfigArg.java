package com.binance.mgs.account.account.vo.userpersonalconfig.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@ApiModel
public class UnifiedBatchQueryUserPersonalConfigArg {


    @ApiModelProperty("配置类型")
    @NotEmpty
    @Size(max = 5)
    private List<String> configTypes;
}
