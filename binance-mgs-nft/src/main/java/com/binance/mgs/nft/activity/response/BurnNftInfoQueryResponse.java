package com.binance.mgs.nft.activity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class BurnNftInfoQueryResponse {
    private List<BurnNftInfoVo> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurnNftInfoVo {
        private String title;
        private String coverUrl;
        private String contractAddress;
        private String tokenId;
        private Long nftId;
        private Integer nftType;
    }
}
