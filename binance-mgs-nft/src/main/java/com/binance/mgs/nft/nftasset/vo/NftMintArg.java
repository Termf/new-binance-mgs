package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftMintArg implements Serializable {

    private Long userId;
    @NotNull
    private String nftProtocal;
    @NotNull
    private String nftSource;
    @NotNull
    private String nftTitle;
    @NotNull
    @Min(1L)
    @Max(128L)
    private Byte nftType;
    @NotNull
    private String nftUrl;
    @NotNull
    private Long quantity;
    @NotNull
    private String mediaType;
    @Min(0L)
    private Integer rawSize;
    @NotNull
    private String specification;
    @Min(0L)
    private Long duration;
    @Min(0L)
    @NotNull
    private BigDecimal mintFee;
    @NotNull
    private String asset;
    @NotNull
    private String description;

    @NotNull
    private long collectionId;
    @Builder.Default
    private String businessType="MINT";
    @NotNull
    private String network;

    private List<NftAssetPropertiesDto> properties;
}
