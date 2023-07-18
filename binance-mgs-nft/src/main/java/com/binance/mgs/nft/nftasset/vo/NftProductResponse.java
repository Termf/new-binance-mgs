package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesInfoDto;
import com.binance.nft.assetservice.api.data.dto.SpecificationDto;
import com.binance.nft.assetservice.api.data.vo.CollectionInfoVo;
import com.binance.nft.assetservice.api.data.vo.NftProductVo;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class NftProductResponse {

    @Long2String
    private Long nftInfoId;
    private String creatorName;
    private String creatorUrl;
    @Long2String
    private Long creatorId;
    private String ownerName;
    private String ownerUrl;
    @Long2String
    private Long ownerId;
    private String nftTitle;
    private String description;
    @Long2String
    private Long quantity;
    private NftProductVo.MediaInfoVo mediaInfoVo;
    private String contractAddress;
    private String tokenId;
    private String network;
    private Byte status;
    private Byte nftType;
    private String soldFor;
    private String asset;
    private String rawUrl;
    private String coverUrl;
    private Byte category;
    private CollectionInfoVo collection;
    @Long2String
    private Long brandingId;
    @Long2String
    private Long serialsNo;
    private String serialsName;
    private String serialsAvatarUrl;
    private Long approveCount;
    private Integer forbiddenState;
    private SpecificationDto specificationDto;
    private List<NftAssetPropertiesInfoDto> properties;

    private BigDecimal royaltyFee;
    private BigDecimal platformFee;
}
