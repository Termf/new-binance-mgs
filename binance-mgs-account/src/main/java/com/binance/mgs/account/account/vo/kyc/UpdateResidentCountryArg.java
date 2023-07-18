package com.binance.mgs.account.account.vo.kyc;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("UpdateResidentCountryRequest")
public class UpdateResidentCountryArg {

    @ApiModelProperty("国家码简写")
    @NotBlank
    private String code;
}
