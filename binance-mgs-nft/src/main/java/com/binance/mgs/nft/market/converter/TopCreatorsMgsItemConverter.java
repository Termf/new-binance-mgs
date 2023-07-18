package com.binance.mgs.nft.market.converter;

import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.TopCreatorsMgsItem;
import com.binance.nft.market.vo.ranking.RankingResponse;
import com.binance.nft.market.vo.ranking.TopCreatorsItem;

import java.util.ArrayList;
import java.util.List;


public class TopCreatorsMgsItemConverter {
    public static RankingResponse<TopCreatorsMgsItem> convert(APIResponse<RankingResponse<TopCreatorsItem>> response, String password) {

        List<TopCreatorsMgsItem> topCreatorsMgsItemList = new ArrayList<>(response.getData().getList().size());

        response.getData().getList().forEach(topCreatorsItem -> {
            TopCreatorsMgsItem item = TopCreatorsMgsItem.builder()
                    .volume(topCreatorsItem.getVolume())
                    .salesCount(topCreatorsItem.getSalesCount())
                    .itemsCount(topCreatorsItem.getItemsCount())
                    .avatarUrl(topCreatorsItem.getAvatarUrl())
                    .nickName(topCreatorsItem.getNickName())
                    .creatorIdOrig(topCreatorsItem.getCreatorId())
                    .fansCount(topCreatorsItem.getFansCount())
                    .rank(topCreatorsItem.getRank())
                    .build();

            if (topCreatorsItem.getCreatorId() != null){
                item.setCreatorId(AesUtil.encrypt(topCreatorsItem.getCreatorId().toString(), password));
            }
            topCreatorsMgsItemList.add(item);
        });

        RankingResponse rankingResponse = new RankingResponse();
        rankingResponse.setList(topCreatorsMgsItemList);
        rankingResponse.setUpdateTime(response.getData().getUpdateTime());

        return rankingResponse;
    }
}
