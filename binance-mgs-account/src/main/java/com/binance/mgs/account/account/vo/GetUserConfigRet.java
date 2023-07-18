package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GetUserConfigRet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2937481578238644074L;

    @ApiModelProperty(value = "配置项类型名")
    private String configType;

    @ApiModelProperty(value = "配置项名称值")
    private String configName;

}
