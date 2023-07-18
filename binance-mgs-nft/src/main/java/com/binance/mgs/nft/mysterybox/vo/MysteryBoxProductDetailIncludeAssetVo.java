package com.binance.mgs.nft.mysterybox.vo;

import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MysteryBoxProductDetailIncludeAssetVo extends MysteryBoxProductDetailVo {

    private String userBalance = "0";

}

