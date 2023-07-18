package com.binance.mgs.account.account.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubUserBizType {
    MERCHANT("merchant", "merchant子账户"),
    CONNECT("connect", "connect子账户"),
    READ_ONLY("read_only", "readOnly子账户"),
    ;
    String bizType;
    String desc;

    public static SubUserBizType getByBizType(String bizType) {
        for (SubUserBizType value : SubUserBizType.values()) {
            if (value.getBizType().equals(bizType)) {
                return value;
            }
        }
        return null;
    }
}