package com.binance.mgs.nft.access;


import java.lang.annotation.*;


/**
 * 编码校验
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DecodeCheck {

    String headerSignature();

    String privateKey();

    String[] checkParameter();
}
