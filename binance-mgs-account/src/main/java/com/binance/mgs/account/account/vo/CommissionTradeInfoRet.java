package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("commission交易详情")
public class CommissionTradeInfoRet implements Serializable {

    private static final long serialVersionUID = 6003308079360471410L;
    @ApiModelProperty("交易量")
    private List<CommissionTrade> trades;
    @ApiModelProperty("距下一等级差距")
    private CommissionNext next;
    @ApiModelProperty("当前费率")
    private UserCommissionGas user;

    @ApiModel("用户交易量信息")
    @Data
    public static class CommissionTrade implements Serializable {


        private static final long serialVersionUID = 4056804639604730172L;
        // @ApiModelProperty("用户Id")
        // private Long userId;

        @ApiModelProperty("用户折算BTC交易量")
        private String btc;

        @ApiModelProperty("用户折算BUSD交易量")
        private String busd;

        @ApiModelProperty("用户BNB持仓")
        private String bnb;

        @ApiModelProperty("统计时间戳")
        private long date;

    }

    @ApiModel("用户距离下一等级信息")
    @Data
    public static class CommissionNext implements Serializable {


        private static final long serialVersionUID = 8016228856262407944L;
        @ApiModelProperty("下一等级")
        private String nextLevel;

        @ApiModelProperty("距离下一等级需要BTC")
        private String nextBtc;

        @ApiModelProperty("距离下一等级需要BUSD")
        private String nextBusd;

        @ApiModelProperty("距离下一等级需要BNB")
        private String nextBnb;
    }

    @ApiModel("用户交易等级费率信息")
    @Data
    public static class UserCommissionGas implements Serializable {

        private static final long serialVersionUID = 137477282845983810L;
        @ApiModelProperty("用户交易等级")
        private String level;

        @ApiModelProperty("gas1值")
        private String gasRate;// gas_rate取gas_rate1的值

        @ApiModelProperty("主动单手续费")
        private String takerCommission;

        @ApiModelProperty("被动单手续费")
        private String makerCommission;

        @ApiModelProperty("30天交易量折算BTC汇总")
        private String btc30;

        @ApiModelProperty("30天交易量折算BUSD汇总")
        private String busd30;

        @ApiModelProperty("BNB持仓")
        private String bnb;

    }
}
