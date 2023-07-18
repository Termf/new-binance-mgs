package com.binance.mgs.account.account.enums;

/**
 * 主要用于预防ddos攻击，辩识用户行为类型
 */
public enum UserPerformanceEnum {
    NORMAL_REGISTER("正常注册用户"),
    NORMAL_FORGET_PASS("正常忘记用户"),
    NORMAL_GET_2FA_LIST("正常用户获取2fa"),
    NOT_EXIST("用户不存在"),
    ILLEGAL_USER_INFO("无效身份参数"),// email mobile
    ILLEGAL_VERIFY_INFO("无效验证参数"), // password smsCode emailCode googleCode
    CAPTCHA_ILLEGAL("无效人机参数"),
    CAPTCHA_VERIFY_ERROR("人机验证失败"),
    SESSION_ID_ILLEGAL("fake sessionId");

    UserPerformanceEnum(String desc) {
    }
}
