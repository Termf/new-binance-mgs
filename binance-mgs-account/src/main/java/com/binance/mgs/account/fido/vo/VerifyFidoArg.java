package com.binance.mgs.account.fido.vo;

import com.binance.mgs.account.constant.FidoType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Description:
 *
 * @author alven
 * @since 2022/8/19
 */
@Data
public class VerifyFidoArg extends CommonArg {
    @NotNull
    @ApiModelProperty("fido类型")
    private FidoType fidoType;

    @NotNull
    @ApiModelProperty("fido code")
    private String code;
}
