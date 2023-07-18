package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ModifyWithdrawalWhitelistArg {

    @ApiModelProperty(value = "用户数据提现白名单时间")
    private String withdrawalWhiteTime;
}
