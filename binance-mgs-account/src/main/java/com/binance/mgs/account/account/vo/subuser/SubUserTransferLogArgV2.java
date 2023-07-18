package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 */
@Data
public class SubUserTransferLogArgV2 {
    @NotNull
    private Integer page;
    @NotNull
    private Integer rows;
    private String userId;
    @ApiModelProperty("划转方(to:划入方;from:划出方;默认查所有)")
    private String transfers;
    @ApiModelProperty("查询划转起始时间")
    private Long startTime;
    @ApiModelProperty("查询划转结束时间")
    private Long endTime;
}
