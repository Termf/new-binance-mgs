package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel("扫码登录-扫码、授权、查询")
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeArg extends CommonArg {
    private static final long serialVersionUID = 7181190031292049690L;
    @ApiModelProperty(required = true, notes = "二维码标识")
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9\\-]{16,60}")
    private String qrCode;
}
