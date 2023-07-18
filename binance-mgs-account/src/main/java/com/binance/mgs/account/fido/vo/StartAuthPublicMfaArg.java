package com.binance.mgs.account.fido.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * Description:
 *
 * @author alven
 * @since 2023/5/3
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StartAuthPublicMfaArg extends CommonArg {
    @NotEmpty
    @ApiModelProperty("业务场景")
    private String bizScene;

    @NotEmpty
    @ApiModelProperty("mfa流水号")
    private String bizNo;
}
