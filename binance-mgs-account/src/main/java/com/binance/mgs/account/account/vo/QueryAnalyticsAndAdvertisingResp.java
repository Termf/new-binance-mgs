package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel
public class QueryAnalyticsAndAdvertisingResp {

    @ApiModelProperty(value = "用户数据是否用于分析")
    private boolean analytics;

    @ApiModelProperty(value = "用户数据是否用于广告")
    private boolean advertising;
}
