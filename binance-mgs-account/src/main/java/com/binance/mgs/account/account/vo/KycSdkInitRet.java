package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class KycSdkInitRet implements Serializable {

    private static final long serialVersionUID = -6167407022933326898L;

    @ApiModelProperty("初始化JUMIO SDK的API KEY")
    private String apiKey;

    @ApiModelProperty("初始化JUMIO SDK的API SECRET")
    private String apiSecret;

    @ApiModelProperty("业务流水号")
    private String merchantReference;

    @ApiModelProperty("用户流水号")
    private String userReference;

    @ApiModelProperty("callback 地址")
    private String callBack;

}
