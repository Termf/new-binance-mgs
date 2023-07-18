package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("用户返佣情况汇总信息")
public class TotalCommissionRet implements Serializable {

    private static final long serialVersionUID = -7789219463337136045L;
   @ApiModelProperty(value = "总佣金数")
   private BigDecimal total;
   @ApiModelProperty(value = "总推荐人数")
   private Long agentCount;

}