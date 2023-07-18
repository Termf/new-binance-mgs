package com.binance.mgs.account.account.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author freeman
 * US需求使用
 */
@Data
public class IPForbiddenRet implements Serializable {
    /**
     * 国家是否在黑名单
     */
    private Boolean isCountryForbidden;
    /**
     * 国家某个地区是否被禁止
     */
    private Boolean isRegionForbidden;
    /**
     * 提醒内容
     */
    private String message;
}
