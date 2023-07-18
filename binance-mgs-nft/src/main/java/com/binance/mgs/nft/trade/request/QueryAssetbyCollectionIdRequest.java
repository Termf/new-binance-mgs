package com.binance.mgs.nft.trade.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAssetbyCollectionIdRequest implements Serializable {
    @NotNull
    private Long collectionId;
}
