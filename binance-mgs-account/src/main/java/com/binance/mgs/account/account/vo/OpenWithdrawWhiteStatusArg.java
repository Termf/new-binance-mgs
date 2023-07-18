package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("开启提现白名单")
@Data
@EqualsAndHashCode(callSuper = false)
public class OpenWithdrawWhiteStatusArg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = 5783999760783222865L;

}
