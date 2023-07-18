package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.validator.CreationActionGroup;
import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCreateArg implements Serializable {

    @Long2String
    private Long userId;
    @Long2String
    private Long collectionId;
    @Long2String
    private Long brandId;
    /**
     * Fixed once created
     */
    @NotNull(groups = CreationActionGroup.class)
    @Length(max = 32)
    private String collectionName;
    @NotNull(groups = CreationActionGroup.class)
    @Length(max = 10)
    private String symbol;
    @NotNull(groups = CreationActionGroup.class)
    private Integer category;
    // 1 - BSC 2 - ERC
    @NotNull(groups = CreationActionGroup.class)
    private Integer networkType;
    @NotNull(groups = CreationActionGroup.class)
    @DecimalMax("0.1")
    @DecimalMin("0.01")
    @BigDecimal2String
    private BigDecimal royaltyFeeRate;

    @Min(0)
    private BigDecimal contractOnChainFee;
    private String asset;
    private Integer source;
    private String walletAddress;
    private Byte status;

    /**
     * Changeable
     */
    private Boolean needNewContract;
    private Byte verifyType;
    private String logoUrl;
    private String bannerUrl;
    private UserSocialLinkVo userSocialLinkUrl;
    @Length(max = 500)
    @NotBlank
    private String description;

}
