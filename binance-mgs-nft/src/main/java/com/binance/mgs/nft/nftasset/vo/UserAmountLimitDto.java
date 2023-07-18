package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.UserQuotaItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAmountLimitDto extends UserQuotaItemDto implements Serializable {


    private BigDecimal minAmount;

    private BigDecimal maxAmount;
}
