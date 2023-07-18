package com.binance.mgs.account.authcenter.vo;

import com.binance.accountoauth.enums.ThreePartyBindingEnum;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("DoBindUnbindThirdPartyArg")
@Data
public class DoBindUnbindThirdPartyArg {

    private String code;

    private String idToken;

    private String redirectUrl;

}
