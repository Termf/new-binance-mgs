package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

@Data
public class ParentAndSubaccountSummaryRet {
    /**
     * 统计相关信息
     * */

    private String parentTotalMarginBalance;

    private String allSubAccountTotalMarginBalance;
}
