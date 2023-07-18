package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@ApiModel("一键注册用户注册确认")
public class OneButtonRegisterConfirmArg extends RegisterConfirmV2Arg {

    private static final long serialVersionUID = 7303495727569386654L;
    
    @ApiModelProperty(required = false, notes = "渠道")
    private String trackSource;

    @ApiModelProperty("一些额外参数")
    private Map<String, Object> externalData;

}
