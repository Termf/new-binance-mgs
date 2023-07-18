package com.binance.mgs.account.account.vo.subuser;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by Fei.Huang on 2018/11/23.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("子账户持仓")
public class SubUserAssetListRet extends UserAssetRet {
    // BTC估值
    private BigDecimal btcValue;
}
