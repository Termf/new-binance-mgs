package com.binance.mgs.account.account.enums;

public enum CaptchaType {
    gt("geetest"),
    bCAPTCHA("自研人机"),
    reCAPTCHA("google人机"),

    bCAPTCHA2("自研人机V2"),
    ;

    public String desc;

    CaptchaType(String desc) {
        this.desc = desc;
    }
}