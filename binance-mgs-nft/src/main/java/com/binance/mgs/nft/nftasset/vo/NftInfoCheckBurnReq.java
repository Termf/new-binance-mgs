package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class NftInfoCheckBurnReq {
    private Long nftInfoId;
    private String creatorId;
    private boolean isOnChain;
    private Byte marketStatus;
    private Integer isOwner;
}
