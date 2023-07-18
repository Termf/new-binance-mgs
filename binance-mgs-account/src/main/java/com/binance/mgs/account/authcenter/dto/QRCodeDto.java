package com.binance.mgs.account.authcenter.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class QRCodeDto {
    // 二维码类型
    private String type;
    // 生成二维码时前端发送的随机码，为了增加安全性获取二维码结果的时候要判断是否匹配
    private String random;
    // qrcode 状态
    private QRCodeStatus qrCodeStatus;
    // 创建二维码的ip
    private String createIp;
    // 创建二维码的clientType
    private String createClientType;
    // 创建二维码的设备信息
    private String deviceInfo;
    // 创建二维码的FVideoId
    private String fVideoId;
    // 创建二维码的设备信息
    private Map<String,String> deviceInfoMap;
    // 扫描二维码的ip
    private String scanIp;
    // 扫码确认后生成token csrftoken
    private String token;
    private String csrfToken;
    private Date expireDate;

    public enum Type {
        LOGIN
    }
}
