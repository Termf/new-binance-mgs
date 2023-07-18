package com.binance.mgs.account.constant;

import org.apache.commons.lang3.StringUtils;

public enum QRCodeActionType {
    CONFIRM,
    DEEPLINK;

    public static boolean isConfirm(String type) {
        return StringUtils.equals(type, CONFIRM.name());
    }

    public static boolean isDeepLink(String type) {
        return StringUtils.equals(type, DEEPLINK.name());
    }
}
