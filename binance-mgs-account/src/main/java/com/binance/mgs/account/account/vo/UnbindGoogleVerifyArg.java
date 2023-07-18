package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UnbindGoogleVerifyArg extends CommonArg {

    private static final long serialVersionUID = 9017198027522014688L;

    @ApiModelProperty("密码")
    @NotNull
    private String password;

    @ApiModelProperty("谷歌验证码")
    @NotNull
    private Integer googleCode;

}
