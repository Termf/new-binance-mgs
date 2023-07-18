package com.binance.mgs.account.api.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@EqualsAndHashCode(callSuper = false)
public class EnableApiCreateArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = 5406490070581687783L;
    @ApiModelProperty(required = true)
    @NotEmpty
    private String uuid;
}

