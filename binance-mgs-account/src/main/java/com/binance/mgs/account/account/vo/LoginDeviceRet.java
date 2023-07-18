package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class LoginDeviceRet implements Serializable {
    private static final long serialVersionUID = -7532210712992196223L;
    @ApiModelProperty("设备类型")
    private String clientType;
    @ApiModelProperty("设备名称")
    private String deviceName;
    @ApiModelProperty("登陆时间")
    private String loginTime;
    @ApiModelProperty("登陆ip")
    private String loginIp;
    @ApiModelProperty("登陆地点")
    private String locationCity;
}
