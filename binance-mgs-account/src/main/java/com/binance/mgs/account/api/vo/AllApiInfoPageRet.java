package com.binance.mgs.account.api.vo;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("获取所有API信息")
public class AllApiInfoPageRet {
    @ApiModelProperty("ip")
    private String currentIp;

    @ApiModelProperty("api信息列表")
    private List<SubUserApiInfoRet> apiInfos;

    @ApiModelProperty("记录总数（分页用的）")
    private Long total;


}
