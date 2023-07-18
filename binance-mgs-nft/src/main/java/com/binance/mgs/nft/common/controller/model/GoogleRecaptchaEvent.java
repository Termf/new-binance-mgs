package com.binance.mgs.nft.common.controller.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
public class GoogleRecaptchaEvent implements Serializable {
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
