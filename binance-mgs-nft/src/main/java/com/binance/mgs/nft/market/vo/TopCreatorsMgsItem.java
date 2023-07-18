package com.binance.mgs.nft.market.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCreatorsMgsItem {

    @BigDecimal2String
    private BigDecimal volume;

    private Integer salesCount;

    private Integer itemsCount;

    private String creatorId;

    private Long creatorIdOrig;

    private String nickName;

    private String avatarUrl;

    private Integer rank;

    private Integer fansCount;

    private Integer followRelation;
}
