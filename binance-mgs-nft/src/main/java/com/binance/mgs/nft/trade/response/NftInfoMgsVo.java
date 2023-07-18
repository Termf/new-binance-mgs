package com.binance.mgs.nft.trade.response;

import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesInfoDto;
import com.binance.nft.tradeservice.vo.UserInfoVo;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftInfoMgsVo {
    private Long nftId;

    private String tokenId;

    private String rawUrl;

    private String coverUrl;

    private String contractAddress;

    private String mediaType;

    private Integer rawSize;

    private String specification;

    private Long duration;

    private Byte rarity;

    private ArtistUserMgsInfo creator;

    private UserInfoVo owner;

    private String network;

    @Long2String
    private Long itemId;

    private Integer numTokens;

    private Long approveCount;

    private List<NftAssetPropertiesInfoDto> properties;

}
