package com.binance.mgs.account.account.vo.marginRelated;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author sean w
 * @date 2021/10/20
 **/
@Data
public class BigDecimalWrapper {

    private BigDecimal value;

    public static BigDecimalWrapper of(BigDecimal value) {
        final BigDecimalWrapper response = new BigDecimalWrapper();
        response.setValue(value);
        return response;
    }
}
