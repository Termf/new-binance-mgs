package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.mystery.api.vo.Artist;
import com.binance.nft.tradeservice.vo.ArtistUserInfo;
import com.binance.nft.tradeservice.vo.ProductInfoVo;
import com.binance.nft.tradeservice.vo.UserInfoVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductItemWithApprove extends ProductInfoVo {

    private UserApproveInfo userApproveInfo;

    private String mediaType;
}
