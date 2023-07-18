package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Description:
 *
 * @author alven
 * @since 2022/8/19
 */
@Data
public class SafePasswordVerifyArg extends CommonArg {
    @ApiModelProperty("密码")
    @NotBlank
    private String safePassword;
}
