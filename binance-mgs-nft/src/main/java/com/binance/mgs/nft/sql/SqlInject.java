package com.binance.mgs.nft.sql;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqlInject {


    /**
     * 方法参数
     * @return
     */
    String[] params();

}
