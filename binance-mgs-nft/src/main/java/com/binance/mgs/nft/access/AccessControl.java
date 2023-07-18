package com.binance.mgs.nft.access;

import java.lang.annotation.*;

/**
 * 目前支持需要uid的接口
 * @author user
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessControl {
    /**
     * 事件
     * @return
     */
    AccessEvent event();

    /**
     * 网络类型
     * @return
     */
    String networkType() default "";
}
