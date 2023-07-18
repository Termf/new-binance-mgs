package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class User2FaStatusArg extends CommonArg {

    @ApiModelProperty("移动端设备credentialId")
    private String credentialId;
    
}
