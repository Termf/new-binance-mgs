package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.tradeservice.vo.ArtistUserInfo;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProductInfoMgsVo implements Serializable {

    private Long productId;

    private String title;

    private String coverUrl;

    private String description;

    private List<String> tokenIds;

    private String contractAddress;

    private String network;

    @ApiModelProperty("status(0:pending list 1 listed 2 pending canclation 3 delisted 4 sold)")
    private Integer status;

    @ApiModelProperty("status(1 normal nft 2 unopened box 3 opened box)")
    private Integer nftType;

    @ApiModelProperty("rarity 0-4")
    private Integer rarity;

    @ApiModelProperty("0 fixed 1 auction")
    private Integer tradeType;

    @BigDecimal2String
    private BigDecimal amount;

    @BigDecimal2String
    private BigDecimal currentAmount;

    private String currency;

    private ArtistUserInfoMgs creator;

    private Date setEndTime;

    private Date setStartTime;
}

