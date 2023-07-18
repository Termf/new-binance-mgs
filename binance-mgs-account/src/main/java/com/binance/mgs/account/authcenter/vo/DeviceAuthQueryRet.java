package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@ApiModel("设备授权登录查询返回")
@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceAuthQueryRet implements Serializable {
    private static final long serialVersionUID = 8567954853192290824L;
    @ApiModelProperty(required = true, notes = "设备授权码状态,NEW:初始状态，CONFIRM：已确认登录，EXPIRED：已过期")
    private String status;
    // @ApiModelProperty(required = true, notes = "若非web端，则会返回token，若为web端不返回，后端直接写cookie即可")
    // private String token;
}
