package com.binance.mgs.nft.market.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MarketProductReq {
    //eqparam
    private Integer category;

    private Integer status;

    private Integer tradeType;

    private String currency;

    private String mediaType;

    private Long creatorId;

    private Integer reSale;

    private String network;

    //inparam
    private List<Long> productIds;

    private List<Integer> categorys;

    private List<String> properties;

    private List<Integer> statusList;

    //rangeparam
    @DecimalMin("0")
    private BigDecimal amountFrom;

    @DecimalMin("0")
    private BigDecimal amountTo;

    //search
    private String keyword;

    private String orderBy = "list_time";

    private Integer orderType = 1;

    private Long userId;

    private Long collectionId;

    @ApiModelProperty
    @NotNull
    @Min(1)
    @Max(100)
    private Integer page;

    @ApiModelProperty
    @NotNull
    @Min(1)
    @Max(100)
    private Integer rows;

    public Integer getOffset() {
        return (page - 1) * rows;
    }
}
