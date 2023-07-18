package com.binance.mgs.account.ddos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Data
public class IpLimitDdosCommonRequest {
    private String ip;

    private int batchSendSize;

    private String domain;

    private String app;

    private String reason;
}
