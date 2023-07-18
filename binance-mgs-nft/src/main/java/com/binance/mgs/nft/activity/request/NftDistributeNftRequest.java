package com.binance.mgs.nft.activity.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftDistributeNftRequest implements Serializable {
    /**
     * 领取Code
     */
    private String code;
}
