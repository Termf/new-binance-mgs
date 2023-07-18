package com.binance.mgs.nft.access;

import com.binance.nft.bnbgtwservice.common.enums.BusinessSceneEnum;

import java.lang.annotation.*;

/**
 * 部分充值接口需要kyc校验
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KycForDeposit {

    BusinessSceneEnum scene() default BusinessSceneEnum.DEFAULT;
}
