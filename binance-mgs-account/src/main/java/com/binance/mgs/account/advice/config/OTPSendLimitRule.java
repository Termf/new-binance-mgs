package com.binance.mgs.account.advice.config;

import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.advice.OTPSendLimit;
import lombok.Data;

/**
 * @author rudy.c
 * @date 2023-03-31 10:39
 */
@Data
public class OTPSendLimitRule {
    /**
     * otp类型，SMS 或者EMAIL
     */
    private String otpType;
    /**
     * 场景
     */
    private String bizScene;
    /**
     * 限制时间，单位秒
     */
    private Long duration;
    /**
     * 限制值
     */
    private Long limit;

    public boolean validate() {
        if(StringUtils.isBlank(otpType)) {
            return false;
        }
        if(!OTPSendLimit.OTP_TYPE_SMS.equalsIgnoreCase(otpType) && !OTPSendLimit.OTP_TYPE_EMAIL.equalsIgnoreCase(otpType)) {
            return false;
        }
        if(StringUtils.isBlank(bizScene)) {
            return false;
        }
        if(duration == null || duration <= 0) {
            return false;
        }
        if(limit == null || limit <= 0) {
            return false;
        }
        return true;
    }
}
