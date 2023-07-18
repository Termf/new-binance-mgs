package com.binance.mgs.account.account.vo.future;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("QueryPositionLimitConfigRet")
public class QueryPositionLimitConfigRet {

    @Data
    public static class PLPriceTable {

        @ApiModelProperty("symbol")
        private String symbol;

        @ApiModelProperty("每月续费费用 eg. 20")
        private String feePerMonth;

        @ApiModelProperty("每月续费费用 eg. 20")
        private String feeCurrentMonth;

        @ApiModelProperty("费用结算币种 eg. USDT")
        private String asset;
    }

    @ApiModelProperty("vip等级 eg. 1")
    private int vipLevel;

    @ApiModelProperty("上调比例(不带百分号) eg. 30 ")
    private int upRatio;

    @ApiModelProperty("支持该服务的symbol列表")
    private List<String> allSymbols;

    @ApiModelProperty("可上调的symbol 价目表 ")
    private List<PLPriceTable> canIncreaseSymbolsPriceTable;

    @ApiModelProperty("静态的全局价目表 ")
    private HashMap<String, String> globalPriceTable;

    @ApiModelProperty("可上调的symbol列表 ")
    private List<String> canIncreaseSymbols;

    @ApiModelProperty("可下调的symbol列表 ")
    private List<String> canDecreaseSymbols;

    private Map<String, Integer> symbolNotionalLimit;
}
