package com.binance.mgs.account.authcenter.helper.qrcode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class QrCodeApiFactory {
    @Autowired
    private Map<String, QrCodeApi> qrCodeApiMap = new ConcurrentHashMap<>();

    public QrCodeApi get(String type) {
        return qrCodeApiMap.get(type);
    }
}
