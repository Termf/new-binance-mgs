package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeConfirmRet implements Serializable {
    private static final long serialVersionUID = 3482418781183971901L;
    @ApiModelProperty(required = true, notes = "二维码标识,NEW:初始状态，SCAN:已扫码，CONFIRM：已确认登录，EXPIRED：已过期")
    private String status;
}
