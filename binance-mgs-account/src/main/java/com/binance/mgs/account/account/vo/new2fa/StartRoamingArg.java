package com.binance.mgs.account.account.vo.new2fa;

import javax.validation.constraints.NotNull;

import com.binance.account2fa.enums.AccountVerificationTwoEnum;
import com.binance.account2fa.enums.BizSceneEnum;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Kay.Zhao on 2022/11/25
 */
@Data
public class StartRoamingArg extends CommonArg {

    @ApiModelProperty("业务场景")
    @NotNull
    private BizSceneEnum bizScene;

    @ApiModelProperty("验证项")
    @NotNull
    private AccountVerificationTwoEnum verificationType;

    @ApiModelProperty("业务流水号，比如2fa流水号、风控挑战流水号")
    @NotNull
    private String businessFlowId;
    
}
