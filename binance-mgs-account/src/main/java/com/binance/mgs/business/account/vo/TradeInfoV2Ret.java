package com.binance.mgs.business.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("交易详情加入")
public class TradeInfoV2Ret extends TradeInfoRet{

    @ApiModelProperty("当前费率")
    private UserGas user;

    @ApiModel("用户交易等级费率信息")
    @Data
    public static class UserGas extends TradeInfoRet.UserGas {

        private static final long serialVersionUID = -7049401512470334752L;

        @ApiModelProperty("折扣后主动方手续费")
        private String bnbTakerCommission;

        @ApiModelProperty("折扣后被动方手续费")
        private String bnbMakerCommission;

    }

}
