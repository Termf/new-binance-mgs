package com.binance.mgs.account.account.vo.userpersonalconfig.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class UnifiedQueryUserPersonalConfigArg {

    @ApiModelProperty("配置类型")
    @NotBlank
    private String configType;
}
