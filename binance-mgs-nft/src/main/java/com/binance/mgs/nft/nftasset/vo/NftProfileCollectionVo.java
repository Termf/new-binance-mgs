package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.market.vo.collection.DailyTradePriceVo;
import com.binance.nft.market.vo.collection.FloorPriceDetailVo;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftProfileCollectionVo implements Serializable {
    @Long2String
    private Long layerId;
    private String layerName;
    private Byte layerStatus;

    private String description;
    private String avatarUrl;
    private String bannerUrl;
    private Byte verifyType;
    private Byte banned;
    //系列类型，1：regular nft；2：mystery box
    private Integer collectionType;

    private String freezeReason;
    private String bannedReason;
    private boolean isCreator;

    private Integer forSale;
    private Integer total;


    FloorPriceDetailVo floorPrice;
    DailyTradePriceVo dailyTradePrice;

    // cr7 nft/mysterybox
    private Byte cr7;
}
