package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.NftAssetPersonalVo;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class NftAssetPersonalRet implements Serializable {

    private NftAssetPersonalVo nftInfo;
    private MysteryBoxProductSimpleRet productInfo;

}
