package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("删除用户设备登录信息日志Arg")
public class DeleteDeviceArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = 3194014343104875880L;
    @ApiModelProperty(value = "设备pk", required = true)
    @NotNull
    private Long devicePk;
    @ApiModelProperty(value = "唯一设备标识", required = false)
    private String deviceId;
}
