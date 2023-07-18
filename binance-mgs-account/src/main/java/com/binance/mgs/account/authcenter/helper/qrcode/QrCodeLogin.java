package com.binance.mgs.account.authcenter.helper.qrcode;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserDeviceApi;
import com.binance.account.vo.device.request.AddUserDeviceForQRCodeLoginRequest;
import com.binance.account.vo.device.request.CheckNewDeviceIpRequest;
import com.binance.account.vo.device.response.AddUserDeviceForQRCodeLoginResponse;
import com.binance.account.vo.device.response.CheckNewDeviceIpResponse;
import com.binance.authcenter.vo.CreateQrTokenResponse;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.authcenter.dto.QRCodeDto;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.vo.QrCodeContentRet;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("LOGIN")
@Slf4j
public class QrCodeLogin implements QrCodeApi {
    @Resource
    private AuthHelper authHelper;
    @Resource
    private UserDeviceApi userDeviceApi;
    @Resource
    private CrowdinHelper crowdinHelper;

    @Override
    public void doConfirm(QRCodeDto dto) {
        // 访问authcenter，根据app端token，生成web端的token
        CreateQrTokenResponse tokenResponse = authHelper.createQrToken(dto.getCreateClientType());
        if (tokenResponse != null) {
            dto.setToken(tokenResponse.getToken());
            dto.setCsrfToken(tokenResponse.getCsrfToken());
            AddUserDeviceForQRCodeLoginRequest request = new AddUserDeviceForQRCodeLoginRequest();
            request.setIp(dto.getCreateIp());
            request.setUserId(authHelper.getUserId());
            request.setDeviceInfo(dto.getDeviceInfoMap());
            request.setScannedClientType(dto.getCreateClientType());
            log.info("AddUserDeviceForQRCodeLoginReq={}", JSON.toJSONString(request));
            APIResponse<AddUserDeviceForQRCodeLoginResponse> apiResponse = userDeviceApi.addDeviceForQRCodeLogin(authHelper.getInstance(request));
            log.info("AddUserDeviceForQRCodeLoginResponse={}", JSON.toJSONString(apiResponse));
        } else {
            log.error("扫码确认生成token失败");
        }
    }

    @Override
    public void setAdditionInfo(QrCodeContentRet ret, QRCodeDto qrCodeDto) {
        // 判断是否新设备
        CheckNewDeviceIpRequest checkNewDeviceIpRequest = new CheckNewDeviceIpRequest();
        checkNewDeviceIpRequest.setIp(qrCodeDto.getCreateIp());
        checkNewDeviceIpRequest.setUserId(authHelper.getUserId());
        checkNewDeviceIpRequest.setDeviceInfo(qrCodeDto.getDeviceInfoMap());
        APIResponse<CheckNewDeviceIpResponse> apiResponse = userDeviceApi.checkNewDeviceIp(authHelper.getInstance(checkNewDeviceIpRequest));
        log.info("CheckNewDeviceIpResponse = {}", JSON.toJSONString(apiResponse));
        if (apiResponse.getData() != null && apiResponse.getData().isNewDeviceIp()) {
            ret.getConfirmContent().setMessage(crowdinHelper.getMessageByKey("qr-login-message", authHelper.getLanguage()));
        }
    }
}
