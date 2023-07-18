package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author pengchenxue
 */
@Data
public class QueryFuturesAccountArg {
    @NotBlank
    private String email;
}
