package com.binance.mgs.nft.trade.response;

import com.binance.nft.mystery.api.vo.query.common.MysteryBoxConfigDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAssetbyCollectionIdResponse implements Serializable {

    private List<ItemCollectionVo> items;

    private Integer nftType;

}
