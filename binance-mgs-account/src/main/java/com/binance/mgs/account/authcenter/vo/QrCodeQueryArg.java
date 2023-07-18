package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel("扫码登录结果查询")
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeQueryArg extends CommonArg {
    private static final long serialVersionUID = -5062753637132248348L;
    @ApiModelProperty(required = true, notes = "二维码标识")
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9\\-]{16,60}")
    private String qrCode;
    @ApiModelProperty(required = true, notes = "生成二维码时传给后端的随机码")
    @NotBlank
    @Pattern(regexp = "[0-9a-f\\-]{30,60}")
    private String random;
}
