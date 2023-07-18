package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("commission交易详情加入")
public class CommissionTradeInfoV2Ret extends CommissionTradeInfoRet {

    @ApiModelProperty("当前费率")
    private UserCommissionGas user;

    @ApiModel("用户交易等级费率信息")
    @Data
    public static class UserCommissionGas extends CommissionTradeInfoRet.UserCommissionGas {

        private static final long serialVersionUID = -7049401512470334762L;

        @ApiModelProperty("折扣后主动方手续费")
        private String bnbTakerCommission;

        @ApiModelProperty("折扣后被动方手续费")
        private String bnbMakerCommission;

    }

}
