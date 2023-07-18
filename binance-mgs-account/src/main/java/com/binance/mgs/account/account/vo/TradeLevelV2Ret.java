package com.binance.mgs.account.account.vo;

import com.binance.report.vo.user.TradeLevelPropertiesResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel("交易等级详情，加入折扣手续费费率")
public class TradeLevelV2Ret {

    @ApiModelProperty("折扣、任务开始时间")
    private TradeLevelPropertiesResponse properties;

    @ApiModelProperty("各等级数据，浮点数换成字符串")
    private List<TradeLevelStringVo> levels;

    @Data
    @ApiModel("交易等级，浮点数换成字符串")
    public static class TradeLevelStringVo {

        @ApiModelProperty("交易级别")
        private Integer level;

        @ApiModelProperty("BNB最低持仓额")
        private String bnbFloor;

        @ApiModelProperty("BNB最高持仓额")
        private String bnbCeil;

        @ApiModelProperty("BTC最低持仓数量")
        private String btcFloor;

        @ApiModelProperty("BTC最高持仓数量")
        private String btcCeil;

        @ApiModelProperty("被动方手续费")
        private String makerCommission;

        @ApiModelProperty("老的被动方手续费")
        private String oldMakerCommission;

        @ApiModelProperty("主动方手续费")
        private String takerCommission;

        @ApiModelProperty("老的主动方手续费")
        private String oldTakerCommission;

        @ApiModelProperty("买方交易手续费")
        private String buyerCommission;

        @ApiModelProperty("卖方交易手续费")
        private String sellerCommission;

        @ApiModelProperty("折扣后被动方手续费")
        private String bnbMakerCommission;

        @ApiModelProperty("折扣后主动方手续费")
        private String bnbTakerCommission;

        private String btcBusdFloor;
        private String btcBusdCeil;

    }

    @Data
    @ApiModel("折扣、任务开始时间")
    public static class TradeLevelPropertiesResponse {

        @ApiModelProperty("交易折扣")
        private String gasRate;

        @ApiModelProperty("交易量和持仓任务统计时间")
        private String task1Time;

        @ApiModelProperty("等级计算任务时间")
        private String task2Time;

    }

}
