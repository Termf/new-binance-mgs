package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.ranking.TopCollectionsItem;
import com.binance.nft.market.vo.ranking.TopSalesItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiRankingMgsResponse {

    private List<TopSalesMgsItem> saleList;

    private List<TopCollectionsItem> collectionList;

    private List<TopCreatorsMgsItem> creatorList;


}
