package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("用户返佣情况汇总信息")
public class FiatTotalCommissionRet implements Serializable {
    private static final long serialVersionUID = 5953129244534857208L;

   @ApiModelProperty(value = "交易总佣金数")
   private BigDecimal total;

   @ApiModelProperty(value = "法币总佣金数")
   private BigDecimal fiatTotal;

   @ApiModelProperty(value = "总推荐人数")
   private Long agentCount;

}
