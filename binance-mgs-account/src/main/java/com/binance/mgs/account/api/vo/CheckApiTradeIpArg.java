package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckApiTradeIpArg extends BaseApiArg {
    private static final long serialVersionUID = 1347188042736746490L;

    @NotNull
    @Min(1)
    @ApiModelProperty("keyId")
    private Long keyId;
}

