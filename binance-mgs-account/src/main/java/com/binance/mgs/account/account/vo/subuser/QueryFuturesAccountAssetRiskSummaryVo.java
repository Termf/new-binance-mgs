package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

@Data
public class QueryFuturesAccountAssetRiskSummaryVo {
    private String email;
    private Boolean isSubUserEnabled;
    private String totalInitialMargin;
    private String totalMaintenanceMargin;
    private String totalWalletBalance;
    private String totalUnrealizedProfit;
    private String totalMarginBalance;
    private String totalPositionInitialMargin;
    private String totalOpenOrderInitialMargin;
}
