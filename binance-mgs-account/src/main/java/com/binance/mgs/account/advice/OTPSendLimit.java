package com.binance.mgs.account.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rudy.c
 * @date 2023-03-31 10:32
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OTPSendLimit {
    String OTP_TYPE_SMS = "SMS";
    String OTP_TYPE_EMAIL = "EMAIL";
    String otpType() default OTP_TYPE_SMS;
}
