package com.binance.mgs.account.account.vo.subuser;

import com.binance.account.vo.subuser.enums.MarginPeriodType;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;


/**
 * @author pengchenxue
 */
@Data
public class QueryMarginAccountArg {
    @NotBlank
    private String email;

    private MarginPeriodType marginPeriodType;
}
