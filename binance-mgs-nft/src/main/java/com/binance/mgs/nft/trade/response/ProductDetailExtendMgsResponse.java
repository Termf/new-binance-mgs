package com.binance.mgs.nft.trade.response;

import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper=true)
public class ProductDetailExtendMgsResponse extends ProductDetailMgsResponse {

    private MysteryBoxProductDetailVo mysteryBoxProductDetailVo;

    private UserApproveInfo approve;

    private BigDecimal royaltyFee;

    private BigDecimal platformFee;

    private ReportVo reportVo;
}