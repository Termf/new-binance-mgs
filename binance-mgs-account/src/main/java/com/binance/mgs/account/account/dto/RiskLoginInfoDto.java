package com.binance.mgs.account.account.dto;

import com.binance.master.commons.ToString;
import lombok.Data;

/**
 * Created by yewei on 2021/3/8 5:24 下午
 * login的时候，发给风控的数据
 */
@Data
public class RiskLoginInfoDto extends ToString {
    private Long userId;
    private String email;
    private String mobileCode;
    private String mobile;
    private String ip;
    //扫码登录source=qr_code_login
    //正常登录source=login
    private String source;

    public static String QR_CODE_LOGIN = "qr_code_login";
    public static String MOBILE_LOGIN = "mobile_login";
    public static String MOBILE_LOGIN_2FA = "mobile_login_2fa";
    public static String EMAIL_LOGIN = "email_login";
    public static String EMAIL_LOGIN_2FA = "email_login_2fa";

    public static String typeTo2FA(String currentType){
        if(currentType.equals(MOBILE_LOGIN)){
            return MOBILE_LOGIN_2FA;
        }
        if(currentType.equals(EMAIL_LOGIN)){
            return EMAIL_LOGIN_2FA;
        }
        return currentType;
    }
}
