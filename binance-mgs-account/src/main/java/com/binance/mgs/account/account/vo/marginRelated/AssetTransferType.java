package com.binance.mgs.account.account.vo.marginRelated;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sean w
 * @date 2021/10/20
 **/
public enum AssetTransferType {

    ROLL_IN(-60, -51, false),
    ROLL_OUT(-61, -52, true),
    BORROW(71, -53, false),
    REPAY(72, -54, true),

    // 交易买卖
    BUY_INCOME(133, 133, false),
    BUY_EXPENSE(134, 134, false),
    SELL_EXPENSE(135, 135, true),
    SELL_INCOME(136, 136, true),

    // 交易手续费
    TRADING_COMMISSION(137, 137, true),

    // 强平买卖
    BUY_LIQUIDATION_BASE(129, 129, false),
    BUY_LIQUIDATION_QUOTE(130, 130, false),
    SELL_LIQUIDATION_BASE(131, 131, true),
    SELL_LIQUIDATION_QUOTE(132, 132, true),

    REPAY_LIQUIDATION(-62, -55, true),
    OTHER_LIQUIDATION(-63, -56, true),
    LIQUIDATION_FEE(-26, -57, true),
    SMALL_BALANCE_CONVERT(80, 80, false),
    COMMISSION_RETURN(6, 6, false),

            ;
    private int marginCode;
    private int isolatedCode;
    private boolean negative;

    public static AssetTransferType getByType(int code) {
        for (AssetTransferType transferType : values()) {
            if (transferType.getIsolatedCode() == code || transferType.getMarginCode() == code) {
                return transferType;
            }
        }
        return null;
    }

    public CapitalFlowType getCapitalFlowType() {
        if (this == ROLL_IN || this == ROLL_OUT) {
            return CapitalFlowType.TRANSFER;
        } else if (this == BUY_LIQUIDATION_BASE || this == BUY_LIQUIDATION_QUOTE) {
            return CapitalFlowType.BUY_LIQUIDATION;
        } else if (this == SELL_LIQUIDATION_BASE || this == SELL_LIQUIDATION_QUOTE) {
            return CapitalFlowType.SELL_LIQUIDATION;
        }
        return CapitalFlowType.valueOf(this.name());
    }

    public static List<Integer> getByCapitalFlowType(CapitalFlowType type, boolean isIsolated) {
        if (isIsolated) {
            if (type == null) {
                return Arrays.stream(AssetTransferType.values())
                        .map(AssetTransferType::getIsolatedCode).collect(Collectors.toList());
            }
            if (type == CapitalFlowType.TRANSFER) {
                return Lists.newArrayList(ROLL_IN.isolatedCode, ROLL_OUT.isolatedCode);
            } else if (type == CapitalFlowType.BUY_LIQUIDATION) {
                return Lists.newArrayList(BUY_LIQUIDATION_BASE.isolatedCode, BUY_LIQUIDATION_QUOTE.isolatedCode);
            } else if (type == CapitalFlowType.SELL_LIQUIDATION) {
                return Lists.newArrayList(SELL_LIQUIDATION_BASE.isolatedCode, SELL_LIQUIDATION_QUOTE.isolatedCode);
            } else {
                return Lists.newArrayList(AssetTransferType.valueOf(type.name()).isolatedCode);
            }
        } else {
            if (type == null) {
                return Arrays.stream(AssetTransferType.values())
                        .map(AssetTransferType::getMarginCode).collect(Collectors.toList());
            }
            if (type == CapitalFlowType.TRANSFER) {
                return Lists.newArrayList(ROLL_IN.marginCode, ROLL_OUT.marginCode);
            } else if (type == CapitalFlowType.BUY_LIQUIDATION) {
                return Lists.newArrayList(BUY_LIQUIDATION_BASE.marginCode, BUY_LIQUIDATION_QUOTE.marginCode);
            } else if (type == CapitalFlowType.SELL_LIQUIDATION) {
                return Lists.newArrayList(SELL_LIQUIDATION_BASE.marginCode, SELL_LIQUIDATION_QUOTE.marginCode);
            } else {
                return Lists.newArrayList(AssetTransferType.valueOf(type.name()).marginCode);
            }
        }
    }

    public int getMarginCode() {
        return marginCode;
    }

    public int getIsolatedCode() {
        return isolatedCode;
    }

    public boolean isNegative() {
        return negative;
    }

    AssetTransferType(int marginCode, int isolatedCode, boolean negative) {
        this.marginCode = marginCode;
        this.isolatedCode = isolatedCode;
        this.negative = negative;
    }
}
