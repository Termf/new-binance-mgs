package com.binance.mgs.account.account.vo.new2fa;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Kay.Zhao on 2022/3/1
 */
@Data
public class Verify2FaCodeArg extends MultiCodeVerifyArg {

    @ApiModelProperty("业务场景")
    @NotNull
    private String bizScene;

    @ApiModelProperty("requestId")
    private String requestId;
    
}
