package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.market.vo.UserApproveInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductItemWithApproveMgs extends ProductInfoMgsVo {

    private UserApproveInfo userApproveInfo;

    private String mediaType;

    private Date timestamp;
}
