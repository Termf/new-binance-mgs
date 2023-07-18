package com.binance.mgs.account.account.vo.new2fa;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author rudy.c
 * @date 2023-04-14 16:57
 */
@Data
public class CheckManageMFAArg extends CommonArg {
    @ApiModelProperty(value = "业务场景", required = true)
    @NotEmpty
    private String bizScene;
}
