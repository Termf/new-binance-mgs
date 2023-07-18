package com.binance.mgs.nft.trade.response;

import com.binance.mgs.nft.mysterybox.helper.MysteryBoxCacheHelper;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxItemVo;
import com.binance.nft.assetservice.api.data.vo.NftInfoDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCollectionVo implements Serializable {

    private List<NftInfoDto> nftInfos;

    private String title;

    private String url;

    private Integer count;

    private String contractAddress;

    private String tokenId;

}
