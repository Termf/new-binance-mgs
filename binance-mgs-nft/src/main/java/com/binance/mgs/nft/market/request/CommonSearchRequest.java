package com.binance.mgs.nft.market.request;

import lombok.Data;

@Data
public class CommonSearchRequest {
    private String keyword;
    private String source;
    private Integer assetType;
    private Integer count;
}
