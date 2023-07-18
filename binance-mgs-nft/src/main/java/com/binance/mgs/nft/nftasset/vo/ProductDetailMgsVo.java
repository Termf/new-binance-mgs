package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailMgsVo {
    @Long2String
    private Long id;

    @Long2String
    private Long nftId;

    @Long2String
    private Long productNo;

    private String title;

    private Integer category;

    private Integer nftType;

    private Integer tradeType;

    @BigDecimal2String
    private BigDecimal amount;

    @BigDecimal2String
    private BigDecimal maxAmount;

    @BigDecimal2String
    private BigDecimal stepAmount;

    private String currency;

    private Date setStartTime;

    private Date setEndTime;

    private Integer status;

    private Integer batchNum;

    private Integer stockNum;

    private Integer leftStockNum;

    private String coverUrl;

    private String description;

    @Long2String
    private Long creatorId;

    @Long2String
    private Long listerId;

    private Date listTime;

    private Integer listType;

    private Integer source;

    @BigDecimal2String
    private BigDecimal currentAmount;

    private CategoryMgsVo categoryVo;

    private Long maxAmountUserId;

    private List<NftBlockChainRefVo> tokenList;

    private Date createTime;

    private Long ownerId;

    private Boolean adminOwner;

    private Boolean productLocked;

    private List<Integer> openListPlatforms;
}
