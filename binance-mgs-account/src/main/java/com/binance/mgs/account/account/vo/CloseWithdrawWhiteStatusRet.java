package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("关闭提现白名单")
@Data
@EqualsAndHashCode(callSuper = false)
public class CloseWithdrawWhiteStatusRet extends CommonArg {
    private static final long serialVersionUID = -7965164589335805544L;
}
