package com.binance.mgs.account.account.vo.google;

import lombok.Data;

import java.io.Serializable;

/**
 * {
 *   "name": "projects/770222260469/assessments/291eb659f0000000",
 *   "event": {
 *     "token": "*******dM_AufvNGuPr5CdA4oNYNfRcA6l2Jhogy4drodB0hUyc8A",
 *     "siteKey": "******AAAAAA-aEQBEGNVB-B45l0At*******",
 *     "userAgent": "",
 *     "userIpAddress": "",
 *     "expectedAction": "LOGIN"
 *   },
 *   "riskAnalysis": {
 *     "score": 0.9,  // 这个就是返回成功的分数
 *     "reasons": [
 *       "AUTOMATION"
 *     ]
 *   },
 *   "tokenProperties": {
 *     "valid": true,
 *     "invalidReason": "INVALID_REASON_UNSPECIFIED",
 *     "hostname": "",
 *     "action": "login",
 *     "createTime": "2021-06-22T08:21:51.638Z"
 *   }
 * }
 */
@Data
public class GoogleRecaptchaResponse implements Serializable {

    private static final long serialVersionUID = -2829467349141984304L;
    private String name;

    private GoogleRecaptchaEvent event;

   private GoogleRecaptchaRiskAnalysis riskAnalysis;

   private GoogleRecaptchaTokenProperties tokenProperties;
}
