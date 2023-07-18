package com.binance.mgs.account.api.vo;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by pcx
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubUserGetAllApiInfoArg {

    @Length(max = 200)
    private String apiName;


    @Length(max = 100)
    private String subUserEmail;

    @Length(max = 100)
    private String apiKey;

    @ApiModelProperty(value = "页数，从1开始", required = true)
    @NotNull
    @Min(value = 1, message = "min value is 1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页行数", required = true)
    @NotNull
    @Min(value = 1, message = "min value is 1")
    @Max(value = 500, message = "max value is 500")
    private Integer rows = 20;

}

