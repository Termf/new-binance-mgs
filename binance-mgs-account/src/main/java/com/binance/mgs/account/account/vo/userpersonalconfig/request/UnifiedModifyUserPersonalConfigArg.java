package com.binance.mgs.account.account.vo.userpersonalconfig.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class UnifiedModifyUserPersonalConfigArg {
    @NotBlank
    @ApiModelProperty("配置类型")
    private String configType;

    @NotBlank
    @ApiModelProperty("配置值")
    private String configValue;
}
