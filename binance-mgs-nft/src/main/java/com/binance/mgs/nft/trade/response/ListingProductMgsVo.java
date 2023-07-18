package com.binance.mgs.nft.trade.response;

import com.binance.nft.tradeservice.vo.ListingProductVO;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ListingProductMgsVo {

    @Long2String
    private Long productId;

    private String title;

    private String description;

    @ApiModelProperty("上架申请时间")
    private Date createTime;
    @ApiModelProperty("上架时间")
    private Date listTime;

    private Date setStartTime;
    private Date setEndTime;

    @Long2String
    private Long listUserId;

    private String currency;
    private BigDecimal amount;
    private BigDecimal stepAmount;
    private BigDecimal maxAmount;

    @Long2String
    private Long creatorId;

    private String creatorName;

    @ApiModelProperty("status(0:pending list 1 listed 2 pending canclation 3 delisted 4 sold 5 Delisted 6 Rejected 7 Expired)")
    private Integer status;

    @ApiModelProperty("status(1 normal nft 2 unopened box 3 opened box)")
    private Integer nftType;
    private Integer tradeType;

    @Long2String
    private Long nftId;

    private Date exposureTime;

    @Long2String
    private Long collectionId;

    private String collectionName;

    private String coverUrl;

    private List<Platform> openListInfo;

    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    private Boolean unreadFlag;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Platform {
        private Integer platform;
        @BigDecimal2String
        private BigDecimal amount;
    }
}
