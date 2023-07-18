package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("用户返佣详情信息")
public class CommissionDetailRet implements Serializable {

    private static final long serialVersionUID = -7789219463337136045L;
   @ApiModelProperty(value = "总推荐人数")
   private Long agentCount;
    @ApiModelProperty(value = "推荐人信息")
    private List<Agent> agents;
   @ApiModelProperty(value = "佣金信息")
   private List<Commission> commissions;

   @Data
   public static class Commission{
        private long tranId;
        private String agent;
        private String commission;
        private Date time;
        private String asset;
        private String email;
    }

    @Data
    public static class Agent{
        private Date time;
        private String email;
    }
}