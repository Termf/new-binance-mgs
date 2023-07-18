package com.binance.mgs.nft.trade.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: felix
 * @date: 15.8.22
 * @description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOffsaleRequest {

    private List<Long> productIds;
}
