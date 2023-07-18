package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLimitCountDto implements Serializable {
    //默认改为c
    private Integer limitMintCount = 0;

    private Integer remainMintCount = 0;

    private Integer limitOnsaleCount = 0;

    private Integer remainOnSaleCount = 0;
}
