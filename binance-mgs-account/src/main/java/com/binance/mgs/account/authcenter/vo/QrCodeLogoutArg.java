package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("扫码登录-删除通过扫码方式授权的token")
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeLogoutArg extends CommonArg {

    private static final long serialVersionUID = 7109125395226016257L;
    @ApiModelProperty(required = true, notes = "要删除token的设备类型")
    @NotBlank
    private String tokenType;
}
