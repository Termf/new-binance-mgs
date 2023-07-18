package com.binance.mgs.account.account.vo.google;

import lombok.Data;

import java.io.Serializable;

@Data
public class GoogleRecaptchaRiskAnalysis implements Serializable {
    private static final long serialVersionUID = -3907956098604831128L;
    /**
     * secret
     */
    private Double score;
    /**
     * 前端返回
     */
    private String[] reasons;

}
