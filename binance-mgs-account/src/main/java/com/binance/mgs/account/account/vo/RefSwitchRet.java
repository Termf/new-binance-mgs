package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("推广返佣开关")
public class RefSwitchRet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6844661793932170411L;

    @ApiModelProperty(value = "推广返佣开关")
    private boolean refSwitch;

    @ApiModelProperty(value = "APP推广返佣开关")
    private boolean refSwitchApp;

}
