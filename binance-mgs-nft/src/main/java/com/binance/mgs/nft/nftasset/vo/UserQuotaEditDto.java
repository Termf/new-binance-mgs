package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.UserQuotaItemDto;
import com.binance.nft.assetservice.api.data.request.Security2faDto;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserQuotaEditDto implements Serializable {

    private Security2faDto security2faDto;

    private UserQuotaItemDto userQuotaItemDto;
}
