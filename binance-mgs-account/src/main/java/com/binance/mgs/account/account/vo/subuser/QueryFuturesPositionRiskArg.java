package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class QueryFuturesPositionRiskArg {
    @NotBlank
    private String email;
}
