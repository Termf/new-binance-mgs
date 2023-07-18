package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckSubUserApiTradeIpArg extends BaseApiArg {
    private static final long serialVersionUID = 1347188042736746490L;

    @NotNull
    @Length(max = 100)
    private String subUserEmail;

    @ApiModelProperty("keyId")
    @NotNull
    @Min(1)
    private Long keyId;
}

