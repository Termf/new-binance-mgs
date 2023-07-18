package com.binance.mgs.nft.market.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author joy
 * @date 2022/12/6 15:21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileAssetFilterRequest {
    private Long userId;
    private Long profileUserId;
    private String profileStrId;
    private String currency;
    private String keyword;
    private String orderBy = "set_end_time";
    private Integer orderType = 1;
    private List<Long> collections;
    private List<Integer> assetType;
    private List<Integer> ownerType;
    private List<Integer> tradeType;
    private List<Integer> rarities;
    private List<String> networks;
    private List<Integer> productSource;

    /**
     * viewType
     * 1.card
     * 2.list
     */
    private Integer viewType;

    /**
     * 0.unverify
     * 1.verify
     */
    private Integer verifyType;

    private boolean isOwner;

    @ApiModelProperty
    @NotNull
    @Min(1)
    @Max(100)
    private Integer page;

    @ApiModelProperty
    @NotNull
    @Min(1)
    @Max(100)
    private Integer pageSize;
}
