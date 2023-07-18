package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.SpecificationDto;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxSimpleVo;
import com.binance.nft.assetservice.api.data.vo.NftAssetCountDto;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MysteryBoxSimpleDto {
    private String serialsNo;
    private String serialsName;
    private String zippedUrl;
    private List<MysteryBoxSimpleVo.ItemSimpleVo> itemSimpleVoList;
    private Byte nftType;
    private String quantity;
    private String freezeQuantity;
    private List<NftAssetCountDto> mysteryboxCount;
    private Byte assetStatus;
    private String network;
    @Long2String
    private Long secondMarketSellingDelay;
    @Long2String
    private Long duration;
    private SpecificationDto specificationDto;
}
