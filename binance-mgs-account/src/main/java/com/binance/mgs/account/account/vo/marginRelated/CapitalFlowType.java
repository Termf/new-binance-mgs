package com.binance.mgs.account.account.vo.marginRelated;

/**
 * @author sean w
 * @date 2021/10/20
 **/

/**
 * 流水类型
 */
public enum CapitalFlowType {

    /**
     * 流水类型
     */
    TRANSFER("Transfer", "转账"),
    BORROW("Borrow", "借款"),
    REPAY("Repay", "还款"),
    BUY_INCOME("Buy-Trading Income", "买单-交易收入"),
    BUY_EXPENSE("Buy-Trading Expense", "买单-交易支出"),
    SELL_INCOME("Sell-Trading Income", "卖单-交易收入"),
    SELL_EXPENSE("Sell-Trading Expense", "卖单-交易支出"),
    TRADING_COMMISSION("Trading Commission", "交易手续费"),
    BUY_LIQUIDATION("Buy by Liquidation", "强平买入"),
    SELL_LIQUIDATION("Sell by Liquidation", "强平卖出"),
    REPAY_LIQUIDATION("Repay by Liquidation", "强平还款"),
    OTHER_LIQUIDATION("Other Liquidation", "其他强平"),
    LIQUIDATION_FEE("Liquidation Fee", "强平清算费用"),
    SMALL_BALANCE_CONVERT("Small Balance Convert", "小额兑换"),
    COMMISSION_RETURN("Commission Return", "手续费返还"),

            ;

    private String name;
    private String desc;

    CapitalFlowType(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
