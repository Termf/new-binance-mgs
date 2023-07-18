package com.binance.mgs.nft.activity.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private Integer total;
    private Integer page;
    private List<T> records;
}
