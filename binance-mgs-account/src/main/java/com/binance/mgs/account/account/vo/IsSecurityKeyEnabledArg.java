package com.binance.mgs.account.account.vo;

import com.binance.account.common.enums.SecurityKeyApplicationScenario;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@ApiModel("查询Security Key 在当前场景是否启用")
@Getter
@Setter
public class IsSecurityKeyEnabledArg {

    @ApiModelProperty("Security Key 应用场景")
    @NotNull
    private SecurityKeyApplicationScenario scenario;

}
