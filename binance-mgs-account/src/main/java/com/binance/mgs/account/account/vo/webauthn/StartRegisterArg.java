package com.binance.mgs.account.account.vo.webauthn;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class StartRegisterArg extends CommonArg {

    @ApiModelProperty("绑定域名")
    @NotNull
    private String origin;

    @ApiModelProperty("别名")
    private String nickname;

}
