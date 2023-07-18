package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("开启关闭站内转账Arg")
public class FastWithdrawSwitchArg extends CommonArg {

    private static final long serialVersionUID = 6627167942046309156L;
    
    @ApiModelProperty(required = true, notes = "是否开启站内转账")
    private Boolean enableFastWithdraw;
}
