package com.binance.mgs.nft.market.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class CollectionInfo implements Serializable {
    @Long2String
    private Long layerId;
    private String layerName;
    private String category;
    @BigDecimal2String
    private BigDecimal royaltyFee;
    private String avatarUrl;
    private String nftProtocal;
    private Date createTime;
}
