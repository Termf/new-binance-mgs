package com.binance.mgs.account.api.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BaseApiArg extends CommonArg {
    private static final long serialVersionUID = 7762672402262060228L;
    // @ApiModelProperty(required = false, notes = "设备信息 BASE64编码")
    // private String deviceInfo;
}

