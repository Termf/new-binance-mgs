package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2020/10/20
 */
@Data
public class QueryDeliveryAccountAssetRiskSummaryVo {
    private String email;
    private Boolean isSubUserEnabled;
    private String walletBalance; //钱包余额
    private String unrealizedProfit; //持仓未实现盈亏
    private String marginBalance; //保证金余额
    private String maintenanceMargin; //维持保证金
}
