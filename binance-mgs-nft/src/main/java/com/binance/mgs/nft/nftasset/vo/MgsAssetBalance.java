package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.UserCashBalanceVo;
import lombok.Data;

@Data
public class MgsAssetBalance extends UserCashBalanceVo.AssetBalance {
    private Boolean hasFrozen = false;
}
