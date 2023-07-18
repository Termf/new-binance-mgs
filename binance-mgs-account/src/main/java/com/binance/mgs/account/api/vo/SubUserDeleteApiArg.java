package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class SubUserDeleteApiArg {
    @NotNull
    @Length(max = 100)
    private String subUserEmail;

    @ApiModelProperty("keyId")
    private Long keyId;

    @ApiModelProperty(required = true)
    @NotEmpty
    private String apiKey;


}

