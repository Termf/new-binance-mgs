package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.request.Security2faDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAmountLimitEditDto implements Serializable {

    private Security2faDto security2faDto;

    private UserAmountLimitDto userQuotaItemDto;

}
