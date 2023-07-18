package com.binance.mgs.nft.nftasset.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxSerialsSimpleVo;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class MysteryBoxSimpleSerialsTotalVo implements Serializable {

    private List<MysteryBoxSimpleSerialsVo> mysteryBoxSerialsSimpleVo;
    private Integer count;
}
