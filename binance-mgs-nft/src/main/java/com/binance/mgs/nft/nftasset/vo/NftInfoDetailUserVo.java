package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftInfoDetailUserVo {
    private boolean approve;

    private Integer isOwner;

    private Integer mysteryQuantity = 0;

    private List<NftBlockChainRefVo> chainRefDtoList;

    private ReportVo reportVo;
}
