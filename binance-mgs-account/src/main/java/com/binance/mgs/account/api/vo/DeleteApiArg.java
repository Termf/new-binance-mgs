package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@EqualsAndHashCode(callSuper = false)
public class DeleteApiArg extends BaseApiArg {
    /**
     * 
     */
    private static final long serialVersionUID = 1347188042736746490L;

    @ApiModelProperty("keyId")
    private Long keyId;

    @ApiModelProperty(required = true)
    @NotEmpty
    private String apiKey;


}

