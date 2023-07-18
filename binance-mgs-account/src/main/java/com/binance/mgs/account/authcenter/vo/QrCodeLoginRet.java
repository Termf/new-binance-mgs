package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@ApiModel("扫码登录-查询所有通过扫码登录成功的信息")
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeLoginRet implements Serializable {
    private static final long serialVersionUID = -8183345366455406752L;
//    @ApiModelProperty("登录时间")
//    private String loginTime;
//    @ApiModelProperty("登录ip")
//    private String loginIp;
    @ApiModelProperty("登录设备类型,web:网页端，mac: Mac端，pc：PC端")
    private String tokenType;
}