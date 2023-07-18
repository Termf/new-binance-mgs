package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.MysteryBoxSerialVo;
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
public class UnopenedBoxAssetDetail implements Serializable {
    private MysteryBoxSerialVo mysteryBoxSerialVo;
    private MysteryBoxProductDetailVo mysteryBoxProductDetailVo;
}
