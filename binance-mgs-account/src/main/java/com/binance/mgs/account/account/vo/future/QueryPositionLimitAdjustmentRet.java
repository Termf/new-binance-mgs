package com.binance.mgs.account.account.vo.future;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("QueryPositionLimitAdjustmentRet")
public class QueryPositionLimitAdjustmentRet {

    @ApiModelProperty("symbol")
    private String symbol;

    @ApiModelProperty("是否正在使用上调服务")
    private boolean isUsingService;

    @ApiModelProperty("上调比例(不带百分号) eg. 30 ")
    private int upRatio;

    @ApiModelProperty("服务结束时间")
    private long endTime;

    @ApiModelProperty("是否自动续期")
    private boolean autoRenew;

    @ApiModelProperty("服务结束时间")
    private long lastAdjustTime;
}
