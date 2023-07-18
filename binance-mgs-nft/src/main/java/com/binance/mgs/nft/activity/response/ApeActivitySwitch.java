package com.binance.mgs.nft.activity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author: felix
 * @date: 26.10.22
 * @description:
 */
@Data
@AllArgsConstructor
@Builder
public class ApeActivitySwitch {

    private boolean isDisplay;

    private String RoAE;
}
