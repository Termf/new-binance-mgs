package com.binance.mgs.nft.nftasset.response;

import com.binance.mgs.nft.nftasset.vo.MysteryBoxMgsVo;
import com.binance.mgs.nft.nftasset.vo.NftInfoDetailMgsVo;
import com.binance.mgs.nft.nftasset.vo.ProductDetailMgsVo;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class NftAssetDetailResponse implements Serializable {

    private NftInfoDetailMgsVo nftInfoDetailMgsVo;

    private ProductDetailMgsVo productDetailMgsVo;

    private MysteryBoxMgsVo mysteryBoxMgsVo;

    @Long2String
    private Long timestamp;

    private List<FeeVO> royaltyFees;

    private List<FeeVO> platformFees;

    private boolean isAdminOwner;

    private boolean isCR7;

    private boolean redeemable;

    private String redeemRewardName;

    private Long redemptionTime;

    private Integer dexFlag;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeVO{
        private Integer productSource;
        private BigDecimal rate;
        private BigDecimal vipRate;
    }
}

