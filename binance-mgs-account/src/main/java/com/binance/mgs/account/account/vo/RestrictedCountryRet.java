package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("受限国家/地区")
public class RestrictedCountryRet implements Serializable {

    @ApiModelProperty("是否受限")
    private boolean restricted;

    @ApiModelProperty("country code")
    private String countryCode;

}