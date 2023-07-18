package com.binance.mgs.account.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckApiKycArg extends BaseApiArg {
    private static final long serialVersionUID = -5534691091227970270L;
    private Boolean isUpdate = false;
}

