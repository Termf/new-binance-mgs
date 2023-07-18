package com.binance.mgs.nft.trade.response;

import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import com.binance.nft.tradeservice.response.ProductDetailResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class ProductDetailExtendResponse extends ProductDetailResponse {

    private MysteryBoxProductDetailVo mysteryBoxProductDetailVo;

    private UserApproveInfo approve;

    private ReportVo reportVo;
}
