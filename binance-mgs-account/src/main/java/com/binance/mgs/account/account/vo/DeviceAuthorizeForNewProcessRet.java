package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@ApiModel(description = "授权设备新版本Response", value = "授权设备新版本Response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAuthorizeForNewProcessRet {
    @ApiModelProperty("授权结果，true.授权成功")
    private boolean valid;
    @ApiModelProperty("UserDevice主键id")
    private Long id;
    @ApiModelProperty("设备id")
    private String deviceId;
    @ApiModelProperty("登录ip")
    private String loginIp;
    @ApiModelProperty("设备名称")
    private String deviceName;
    @ApiModelProperty("登陆所在地")
    private String locationCity;
    @ApiModelProperty("是否重复授权")
    private boolean retry = false;
    @ApiModelProperty("是否需要回答问题")
    private boolean needAnswerQuestion = false;
    @ApiModelProperty
    private String questionFlowId;
    @ApiModelProperty("授权成功后需要跳转的地址")
    private String callback;
    @ApiModelProperty("下发的新token")
    private String token;
    @ApiModelProperty("下发的新token 关联的csrftoken")
    private String csrfToken;
    @ApiModelProperty("userid")
    private Long userId;
    // oauth code
    private String code;

    private String refreshToken;

}
