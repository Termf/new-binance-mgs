package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MarketProductMgsItem implements Serializable {

    @Long2String
    private Long productId;

    private String title;

    private String coverUrl;

    private Integer tradeType;

    private Integer nftType;

    @BigDecimal2String
    private BigDecimal amount;

    private String currency;

    private Date setStartTime;

    private Date setEndTime;

    private Long timestamp;

    private Integer rarity;

    @ApiModelProperty("status(0:pending list 1 listed 2 pending canclation 3 delisted 4 sold 5 Delisted 6 Rejected 7 Expired)")
    private Integer status;

    private UserInfoMgsVo owner;

    private UserInfoMgsVo creator;

    private String mediaType;

    private Integer favorites;

    private String network;

    private UserApproveInfo approve;

    private Integer verified;

    private String collectionId;

    private String collectionName;

}
