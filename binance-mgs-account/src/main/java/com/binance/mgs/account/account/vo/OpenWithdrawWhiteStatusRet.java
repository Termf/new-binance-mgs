package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("开启提现白名单")
@Data
@EqualsAndHashCode(callSuper = false)
public class OpenWithdrawWhiteStatusRet extends CommonArg {
    private static final long serialVersionUID = 5838180601216458587L;
}
