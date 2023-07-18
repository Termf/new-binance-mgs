package com.binance.mgs.account.account.vo.google;

import lombok.Data;

import java.io.Serializable;

@Data
public class GoogleRecaptchaEvent implements Serializable {
    private static final long serialVersionUID = 4697856879438579730L;
    /**
     * secret
     */
    private String token;
    /**
     * 前端返回
     */
    private String siteKey;

    private String userAgent;

    private String userIpAddress;

    private String expectedAction;

}
