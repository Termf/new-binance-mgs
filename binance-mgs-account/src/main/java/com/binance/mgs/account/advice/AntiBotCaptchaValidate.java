package com.binance.mgs.account.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AntiBotCaptchaValidate {
    /**
     * 校验场景
     */
    String[] bizType() default {};

    /**
     * 方法名
     */
    String name() default "";
}
