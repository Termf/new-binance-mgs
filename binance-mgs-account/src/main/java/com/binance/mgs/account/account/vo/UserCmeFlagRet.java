package com.binance.mgs.account.account.vo;

import com.binance.master.commons.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserCmeFlagRet extends ToString {

    private static final long serialVersionUID = -4529616444748346926L;

    @ApiModelProperty("是否需要弹窗进行CME认证确认, false: 不需要, true: 需要")
    private boolean cmeConfirm = false;

}
