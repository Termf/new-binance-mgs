package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.MysteryBoxSimpleVo;
import com.binance.nft.assetservice.api.data.vo.NftAssetCountDto;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MysteryBoxSimpleRet implements Serializable {

    private String serialsNo;
    private String serialsName;
    private String zippedUrl;
    private List<MysteryBoxSimpleVo.ItemSimpleVo> itemSimpleVoList;
    private Byte nftType;
    private List<NftAssetCountDto> mysteryboxCount;
    private Integer quantity;
    private Byte assetStatus;

    private String network;
}
