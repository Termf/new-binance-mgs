package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("AuthorizeForNewProcessArg")
public class AuthorizeForNewProcessArg extends CommonArg {

    private static final long serialVersionUID = 1869989389783810297L;
    private String email;
    @NotNull
    private String code;

    @ApiModelProperty(value = "用于登陆绑定三方账户")
    private String registerToken;

    @ApiModelProperty(value = "登陆涞源，如果是ThreeParty则是三方登陆")
    private String threePartySource;

}
