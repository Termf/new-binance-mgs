package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeCreateArg extends CommonArg {
    private static final long serialVersionUID = 3793264604046381311L;
    @ApiModelProperty(required = true, notes = "随机码")
    @NotBlank
    @Pattern(regexp = "[0-9a-f\\-]{30,60}")
    private String random;
}
