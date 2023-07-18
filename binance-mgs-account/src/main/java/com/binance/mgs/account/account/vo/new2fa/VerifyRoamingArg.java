package com.binance.mgs.account.account.vo.new2fa;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Kay.Zhao on 2022/11/25
 */
@Data
public class VerifyRoamingArg extends CommonArg {

    @ApiModelProperty("roamingFlowId")
    @NotNull
    private String roamingFlowId;

    private String fidoVerifyCode;
    
}
