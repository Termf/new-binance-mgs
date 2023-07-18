package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

@Data
public class QueryFuturesAccountAssetRiskVo {
    private String asset;

    private String walletBalance;

    private String unrealizedProfit;

    private String marginBalance;

    private String maintenanceMargin;

    private String initialMargin;

    private String positionInitialMargin;

    private String openOrderInitialMargin;

    private String maxWithdrawAmount;
}
