package com.binance.mgs.account.constant;

public class BizType {
    public static final String LOGIN = "login";
    public static final String REGISTER = "register";
    public static final String FORGET_PASSWORD = "forget_password";
    public static final String CREATE_APIKEY = "create_apikey";
    public static final String REFRESH_ACCESS_TOKEN = "refresh_access_token";
    public static final String THIRD_LOGIN = "third_login";

    public static boolean isMatch(String type) {
        return type.equals(LOGIN) || type.equals(REGISTER) || type.equals(FORGET_PASSWORD) || type.equals(CREATE_APIKEY) || type.equals(REFRESH_ACCESS_TOKEN) || type.equals(THIRD_LOGIN);
    }
}
