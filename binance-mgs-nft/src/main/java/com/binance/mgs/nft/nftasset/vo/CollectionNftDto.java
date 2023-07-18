package com.binance.mgs.nft.nftasset.vo;

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
public class CollectionNftDto implements Serializable {
    @Long2String
    private Long nftInfoId;
    private String nftLink;
    private String zippedUrl;
    private String nftTitle;
    private String contractAddress;
    private String tokenId;
    private String collectionName;
    @Long2String
    private Long collectionId;

}
