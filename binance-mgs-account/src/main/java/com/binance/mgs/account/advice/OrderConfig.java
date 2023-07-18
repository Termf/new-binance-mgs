package com.binance.mgs.account.advice;

/**
 * 数字越大优先级越低
 * 数字相同按照代码行序
 * @author Men Huatao (alex.men@binance.com)
 * @date 10/19/22
 */
public class OrderConfig {
    public static final int DDoSPreMonitorAspect_ORDER = -3;
    public static final int AntiBotCaptchaValidate_ORDER = -2;
    public static final int AccountDefenseResource_ORDER = -1;
    public static final int UserOperation_ORDER = 0;
    public static final int ApiKeyKycCheck_ORDER = 1;
    public static final int SubAccountForbidden_ORDER = 1;
    public static final int OTPSendLimit_ORDER = 10;
}
