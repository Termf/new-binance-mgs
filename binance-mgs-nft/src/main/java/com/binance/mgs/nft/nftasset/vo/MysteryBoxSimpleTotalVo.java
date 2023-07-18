package com.binance.mgs.nft.nftasset.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.nft.assetservice.api.data.vo.NftAssetCountDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MysteryBoxSimpleTotalVo {

    private Page<MysteryBoxSimpleDto> mysteryBoxSimpleVoPage;
    private List<NftAssetCountDto> mysteryboxCount;

}
