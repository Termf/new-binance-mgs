package com.binance.mgs.account.account.helper;

import com.binance.master.error.BusinessException;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Kay.Zhao on 2021/2/10
 */
@Component
@Slf4j
public class CheckHelper {

    @Value("#{'${voice.sms.notSupport.countryCode:AX,BL,BQ,BV,CU,CW,DM,EH,GG,GS,GW,HM,IO,IR,JE,KP,KZ,MF,MK,PF,PN,RE,SC,SJ,SS,SX,SY,TF,UM,WF,XK}'.split(',')}")
    private List<String> voiceSmsNotSupportCountryCode;

    /**
     * 检查mobileCode是否支持发送语言短信验证码
     * @param mobileCode
     */
    public void assetIfSupportVoiceSms(String mobileCode) {
        if (StringUtils.isNotBlank(mobileCode) && voiceSmsNotSupportCountryCode.contains(mobileCode.toUpperCase())) {
            throw new BusinessException(AccountMgsErrorCode.VOICE_SMS_NOT_SUPPORT);
        }        
    }
}
