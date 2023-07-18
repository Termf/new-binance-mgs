package com.binance.mgs.nft.nftasset.vo;

import com.binance.master.commons.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionNamingVo implements Serializable {

    private List<CollectionStatisticsDto> statisticsDtoList;
    private SearchResult<CollectionNftDto> nftPage;
}
