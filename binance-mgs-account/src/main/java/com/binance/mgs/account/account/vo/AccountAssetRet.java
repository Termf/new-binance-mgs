package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.enums.AccountType;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * copy from {@link com.binance.mgs.business.asset.vo.WalletBalanceRet}
 *
 * @author kvicii
 * @date 2021/06/08
 */
@Data
@ApiModel
public class AccountAssetRet implements Serializable {
    private static final long serialVersionUID = -189276674637931891L;
    @ApiModelProperty("账户类型")
    private AccountType accountType;
    @ApiModelProperty("是否已激活")
    private boolean activate;
    @ApiModelProperty("账户余额")
    @BigDecimal2String
    private BigDecimal balance = BigDecimal.ZERO;
    @ApiModelProperty("账户余额时间戳")
    private Long time;
    @ApiModelProperty("资产余额详情")
    private List<AssetBalance> assetBalances;

    @Data
    public static class AssetBalance {
        @ApiModelProperty
        private String asset;
        @ApiModelProperty
        private String assetName;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal free = BigDecimal.ZERO;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal locked = BigDecimal.ZERO;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal freeze = BigDecimal.ZERO;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal withdrawing = BigDecimal.ZERO;
        @ApiModelProperty
        private String logoUrl;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal transferBtc = BigDecimal.ZERO;
    }
}
