package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftBlockChainRefVo {
    @Long2String
    private Long nftInfoId;

    private String contractAddress;

    private String tokenId;

    private String network;
}
