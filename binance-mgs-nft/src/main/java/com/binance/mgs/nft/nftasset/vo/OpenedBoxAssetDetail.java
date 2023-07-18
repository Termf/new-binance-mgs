package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.MysteryBoxItemVo;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class OpenedBoxAssetDetail implements Serializable {
    private MysteryBoxItemVo mysteryBoxItemVo;
    private MysteryBoxProductDetailVo mysteryBoxProductDetailVo;
}
