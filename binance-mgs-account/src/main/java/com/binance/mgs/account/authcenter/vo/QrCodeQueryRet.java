package com.binance.mgs.account.authcenter.vo;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("扫码登录查询返回")
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeQueryRet implements Serializable {
    private static final long serialVersionUID = -2000769039461572843L;
    @ApiModelProperty(required = true, notes = "二维码标识,NEW:初始状态，SCAN:已扫码，CONFIRM：已确认登录，EXPIRED：已过期")
    private String status;
    @ApiModelProperty(required = true, notes = "若非web端，则会返回token，若为web端不返回，后端直接写cookie即可")
    private String token;
    @ApiModelProperty(required = true, notes = "临时code")
    private String code;

    @ApiModelProperty(notes = "只有登录成功后，才会返回bncLocation")
    private String bncLocation;
}