package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("设备授权Arg")
public class AuthorizeArg extends CommonArg {

    private static final long serialVersionUID = 1869989389783810297L;
    @NotNull
    private Long userId;
    @NotNull
    private String code;

}
