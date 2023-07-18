package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonPageArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("分页获取用户设备登录信息日志Arg")
public class PageUserDeviceLogArg extends CommonPageArg {
    /**
     * 
     */
    private static final long serialVersionUID = 4385425556375192078L;
    @ApiModelProperty(value = "设备pk", required = true)
    @NotNull
    private Long devicePk;

}
