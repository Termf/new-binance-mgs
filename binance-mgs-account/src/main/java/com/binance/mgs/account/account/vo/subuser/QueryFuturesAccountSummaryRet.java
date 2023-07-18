package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import java.util.List;

@Data
public class QueryFuturesAccountSummaryRet {
    private List<QueryFuturesAccountAssetRiskSummaryVo> subAccountList;
}
