package com.binance.mgs.nft.market.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSalesMgsItem {

    private Long nftId;

    private Integer nftType;

    private String coverUrl;

    private String title;

    @BigDecimal2String
    private BigDecimal price;

    private String currency;

    private String network;

    private Date tradeTime;

    private String creatorId;

    private String nickName;

    private String avatarUrl;

    private Integer rank;

}

