package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by yangyang on 2019/7/11.
 */
@Data
@ApiModel("创建返佣推荐比例")
public class SaveUserAgentRateArg implements Serializable{


    @ApiModelProperty(required = true, notes = "被推荐人返佣比例")
    @NotNull
    private BigDecimal referralRate;

    private String label;
}
