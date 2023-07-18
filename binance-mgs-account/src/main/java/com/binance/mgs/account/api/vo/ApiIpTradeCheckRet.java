package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ApiIpTradeCheckRet{

    private static final long serialVersionUID = -2097537566488269783L;

    @ApiModelProperty("api id")
    private String id;

    @ApiModelProperty("检查是否合格")
    private boolean checkPass;

    @ApiModelProperty("交易权限过期时间")
    private Date tradeExpireTime;
}
