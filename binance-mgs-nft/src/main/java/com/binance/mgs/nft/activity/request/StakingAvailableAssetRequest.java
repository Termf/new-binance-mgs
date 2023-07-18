package com.binance.mgs.nft.activity.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: felix
 * @date: 26.10.22
 * @description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingAvailableAssetRequest {

    private String activityId;
}
