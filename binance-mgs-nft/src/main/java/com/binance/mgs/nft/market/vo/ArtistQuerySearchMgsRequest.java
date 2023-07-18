package com.binance.mgs.nft.market.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;


@Data
public class ArtistQuerySearchMgsRequest {

    @NotNull
    private String creatorId;

    private Integer status;

    private Integer tradeType;

    private Integer reSale;

    private String currency;

    //rangeparam
    @DecimalMin("0")
    private BigDecimal amountFrom;

    @DecimalMin("0")
    private BigDecimal amountTo;

    //search
    private String keyword;

    private String orderBy = "set_end_time";

    private Integer orderType = 1;

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

