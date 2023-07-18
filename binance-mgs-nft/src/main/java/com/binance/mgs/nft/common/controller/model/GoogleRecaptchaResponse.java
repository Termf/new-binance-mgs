package com.binance.mgs.nft.common.controller.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class GoogleRecaptchaResponse implements Serializable {

    private String name;

    private GoogleRecaptchaEvent event;

   private GoogleRecaptchaRiskAnalysis riskAnalysis;

   private GoogleRecaptchaTokenProperties tokenProperties;
}
