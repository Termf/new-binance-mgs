package com.binance.mgs.account.authcenter.vo;

import com.binance.accountoauth.enums.ThreePartyBindingEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;

@ApiModel("DoBindUnbindThreePartyArg")
@Data
public class DoBindUnbindThreePartyArg {

    private String code;

    private String idToken;

    private String redirectUrl;

    @NotNull
    private ThreePartyBindingEnum threePartyBindingEnum;

}
