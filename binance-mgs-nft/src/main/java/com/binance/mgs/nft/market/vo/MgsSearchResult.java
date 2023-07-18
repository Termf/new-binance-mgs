package com.binance.mgs.nft.market.vo;

import com.binance.master.commons.SearchResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MgsSearchResult<T> extends SearchResult<T> {
    private Boolean hasMore;

    public MgsSearchResult(List<T> rows, long total, Boolean hasMore) {
        super(rows, total);
        this.hasMore = hasMore;
    }

    public MgsSearchResult(List<T> rows, long total, int page, int pageSize) {
        super(rows, total);
        this.hasMore = page * pageSize < total;
    }
}
