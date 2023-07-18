package com.binance.mgs.account.advice;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DdoS预校验
 * 目前只能基于用户标识、设备标识做一些次数统计的拦截
 * 基于业务内部的DDoS逻辑需要单独判断
 *
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/08/26
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DDoSPreMonitor {

    /**
     * 默认则提取方法名称
     */
    String action() default "";
}
