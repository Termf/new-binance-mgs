package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserAssetBalanceRet implements Serializable {

    @Long2String
    private Long userId;
    private BigDecimal totalBtcValue;
    private BigDecimal totalFiatValue;
    private String fiatName;
    private List<AssetBalance> assetBalanceList;

    @Data
    public static class AssetBalance {
        @ApiModelProperty
        private String asset;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal free = BigDecimal.ZERO;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal total = BigDecimal.ZERO;
        @ApiModelProperty
        private String logoUrl;
        @ApiModelProperty
        @BigDecimal2String
        private BigDecimal totalFiatValue = BigDecimal.ZERO;
    }

}
