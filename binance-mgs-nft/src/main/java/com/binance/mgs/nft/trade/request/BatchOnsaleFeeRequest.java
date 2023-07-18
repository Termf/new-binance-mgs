package com.binance.mgs.nft.trade.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: felix
 * @date: 24.8.22
 * @description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOnsaleFeeRequest {

    private Integer tradeType;

    private List<Long> nftIds;

}
