package com.binance.mgs.nft.market.request;

import lombok.Data;

@Data
public class PropertySearchMgsRequest {
    private String keyword;
    private Long collectionId;
    private String source;
}
