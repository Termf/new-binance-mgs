package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
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
public class NftInfoDetailMgsVo {
    @Long2String
    private Long nftId;

    private boolean isOnChain;

    private boolean canBurn;

    private String tokenId;

    private Byte nftType;

    private String nftTitle;

    private String rawUrl;

    private String coverUrl;

    private String contractAddress;

    private String mediaType;

    private String description;

    private Byte nftStatus;

    private Integer rawSize;

    private String specification;

    private Integer forbiddenState;

    private boolean forbiddenWithdraw;

    private boolean forbiddenTrade;

    @Long2String
    private Long collectionId;

    private String collectionName;

    private Integer verified;

    private Boolean canView;

    private String avatarUrl;

    @Long2String
    private Long duration;

    private Byte rarity;

    private String soldFor;
    private String asset;

    private UserInfoArtistMgsVo creator;

    private UserInfoArtistMgsVo owner;

    private String network;

    @Long2String
    private Long approveCount;

    private boolean approve;

    private Integer isOwner;

    private List<NftAssetPropertiesInfoVo> properties;

    @Long2String
    private Long serialsNo;

    private String serialsName;

    private List<NftBlockChainRefVo> chainRefDtoList;

    private Byte marketStatus;

    @Long2String
    private Long itemId;

    private Integer numTokens;

    private Integer mysteryQuantity = 0;

    private ReportVo reportVo;

    private boolean isAdminOwner;

    private Byte banned;

    private Integer openRarity;

    private Integer openRarityCount;
}
