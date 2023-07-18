package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import java.util.List;


@Data
public class QueryFuturesAccountRet {
    /**
     * 基础信息
     * */

    private String email;

    private boolean canTrade;

    private boolean canDeposit;

    private boolean canWithdraw;

    private Integer feeTier;

    private Long updateTime;

    private String asset;


    /**
     * 统计相关信息
     * */
    private String totalInitialMargin;

    private String totalMaintenanceMargin;

    private String totalWalletBalance;

    private String totalUnrealizedProfit;

    private String totalMarginBalance;

    private String totalPositionInitialMargin;

    private String totalOpenOrderInitialMargin;

    private String maxWithdrawAmount;

    private List<QueryFuturesAccountAssetRiskVo> assets;


}
