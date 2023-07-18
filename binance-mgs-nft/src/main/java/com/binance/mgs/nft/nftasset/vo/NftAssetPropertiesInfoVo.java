package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftAssetPropertiesInfoVo {
    private String propertyType;

    private String propertyName;

    private BigDecimal rate;

    private BigDecimal total;

}
