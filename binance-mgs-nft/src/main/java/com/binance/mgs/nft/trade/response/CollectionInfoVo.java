package com.binance.mgs.nft.trade.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionInfoVo implements Serializable {

    private Long collectionId;

    private String collectionTitle;

    private Integer nftType;
}
