package com.binance.mgs.nft.common.controller.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GoogleRecaptchaRiskAnalysis implements Serializable {
    /**
     * secret
     */
    private Double score;
    /**
     * 前端返回
     */
    private String[] reasons;

}
