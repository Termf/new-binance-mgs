package com.binance.mgs.nft.nftasset.vo;


import com.binance.nft.assetservice.api.data.vo.NftProfileAssetDto;
import com.binance.nft.market.vo.AssetMarketVo;
import com.binance.nft.market.vo.MarketBestOfferVO;
import com.binance.nft.market.vo.ranking.FloorPriceVo;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class NftProfileAssetVo implements Serializable {

    private NftProfileAssetDto nftInfo;
    private MysteryBoxProductSimpleRet productInfo;
    private boolean isCR7;

    private FloorPriceVo floorPriceVo;
    private MarketBestOfferVO bestOffer;
}
