package com.binance.mgs.account.account.vo;

import javax.validation.constraints.NotNull;

import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2020/9/30
 */
@Getter
@Setter
@ApiModel("用户自定义配置项")
public class UserConfigTypeArg extends CommonArg {

    @ApiModelProperty(required = true, notes = "配置类型")
    @NotNull
    private String type;
}
