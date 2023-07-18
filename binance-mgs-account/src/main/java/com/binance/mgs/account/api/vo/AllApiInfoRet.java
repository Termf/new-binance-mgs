package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("获取所有API信息")
public class AllApiInfoRet implements Serializable {
    private static final long serialVersionUID = -413388121776991098L;
    @ApiModelProperty("ip")
    private String currentIp;

    @ApiModelProperty("api信息列表")
    private List<ApiInfoRet> apiInfos;
}
