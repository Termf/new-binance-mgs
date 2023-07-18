package com.binance.mgs.account.authcenter.helper.qrcode;


import com.binance.mgs.account.authcenter.dto.QRCodeDto;
import com.binance.mgs.account.authcenter.vo.QrCodeContentRet;

public interface QrCodeApi {

    void doConfirm(QRCodeDto dto);

    void setAdditionInfo(QrCodeContentRet ret, QRCodeDto qrCodeDto);

}
