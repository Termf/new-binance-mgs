package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesMgsVo {
    @Long2String
    private Long itemId;

    private String name;

    private Integer amount;

    private Integer rarity;

    private String probability;

    private String coverImg;

    private String description;
}
