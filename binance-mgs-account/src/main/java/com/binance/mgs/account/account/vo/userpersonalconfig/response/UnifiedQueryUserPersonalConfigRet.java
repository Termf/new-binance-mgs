package com.binance.mgs.account.account.vo.userpersonalconfig.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class UnifiedQueryUserPersonalConfigRet {
    @ApiModelProperty("配置类型")
    private String configType;
    @ApiModelProperty("配置值")
    private String configValue;
}
