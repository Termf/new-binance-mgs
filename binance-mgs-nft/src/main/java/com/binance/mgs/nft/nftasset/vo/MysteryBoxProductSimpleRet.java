package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class MysteryBoxProductSimpleRet implements Serializable {

    private String nftUrl;
    private Byte rarity;
    private Byte nftType;
    private Byte marketStatus;
    private Integer freezeQty;
    private Integer quantity;

    private String productId;
    private String productName;
    private Integer status;
    private Integer totalOnSale;
    private Integer totalListed;

    private Byte offerType;
    private Byte bidStatus;
    private String asset;
    private String startPrice;
    private String price;

    @Long2String
    private Long secondMarketSellingDelay;

    private Date remaining;//endDate
    private Date setStartTime;
    private Date setEndTime;
    @Long2String
    private Long timestamp;

    private String network;
    private List<Integer> openListPlatforms;
}
