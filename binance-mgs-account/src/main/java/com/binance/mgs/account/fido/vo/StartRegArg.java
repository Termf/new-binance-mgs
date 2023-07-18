package com.binance.mgs.account.fido.vo;

import com.binance.platform.mgs.base.vo.CommonArg;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class StartRegArg extends CommonArg {

    private String displayName;

}
