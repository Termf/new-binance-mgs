package com.binance.mgs.nft.twofa;

import com.binance.account.vo.security.enums.BizSceneEnum;

import java.lang.annotation.*;

/**
 * 2FA
 * @author: allen.f
 * @date: 2021/9/22
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TwoFa {
    /**
     * 场景码
     * @return
     */
    BizSceneEnum scene();
}
