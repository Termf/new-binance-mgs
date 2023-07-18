package com.binance.mgs.account.authcenter.dto;

import lombok.Data;

@Data
public class DeviceAuthCodeDto {
    // qrcode 状态
    private DeviceAuthCodeStatus deviceAuthCodeStatus;
    // 创建二维码的ip
    private String createIp;
    // 创建二维码的clientType
    private String createClientType;
    // 创建二维码的设备信息
    private String deviceInfo;
    // 扫描二维码的ip
    private String authIp;
    // 扫码确认后生成token csrftoken
    private String token;
    private String csrfToken;
}
