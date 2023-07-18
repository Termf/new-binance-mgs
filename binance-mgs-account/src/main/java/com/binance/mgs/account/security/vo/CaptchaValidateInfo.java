package com.binance.mgs.account.security.vo;

import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Data
public class CaptchaValidateInfo {
    private String captchaType;
    private String bizType;
    private String email;
    private String mobile;
    private String mobileCode;
    private String refreshToken;
    private int status;

    private String subUserEmail;
    private String idToken;
    private String userId;
    private String validateId;
    private String rejectMsg;
}
