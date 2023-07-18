package com.binance.mgs.account.account.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * BigDecimalWrapper
 *
 * <p>Copyright (C) 上海比捷网络科技有限公司.</p>
 *
 * @author YueYouqian
 * @since 1.0
 */
@Data
public class BigDecimalWrapper {
    private BigDecimal value;

    public static BigDecimalWrapper of(BigDecimal value) {
        final BigDecimalWrapper response = new BigDecimalWrapper();
        response.setValue(value);
        return response;
    }
}
