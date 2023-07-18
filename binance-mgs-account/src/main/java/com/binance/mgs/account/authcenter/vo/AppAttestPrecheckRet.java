package com.binance.mgs.account.authcenter.vo;

import lombok.Data;

@Data
public class AppAttestPrecheckRet {
    private String validationType;
    private String challenge;
}
