package com.binance.mgs.nft.market.converter;

import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.MultiRankingMgsResponse;
import com.binance.mgs.nft.market.vo.TopCreatorsMgsItem;
import com.binance.mgs.nft.market.vo.TopSalesMgsItem;
import com.binance.nft.market.vo.ranking.MultiRankingResponse;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class MultiRankingMgsResponseConverter {

    public static MultiRankingMgsResponse convert(APIResponse<MultiRankingResponse> ret, String password) {

        List<TopCreatorsMgsItem> creatorList = new ArrayList<>(ret.getData().getCreatorList().size());
        List<TopSalesMgsItem> salesList = new ArrayList<>(ret.getData().getSaleList().size());

        ret.getData().getCreatorList().forEach(topCreatorsItem -> {
                    TopCreatorsMgsItem mgsItem = new TopCreatorsMgsItem();
                    BeanUtils.copyProperties(topCreatorsItem, mgsItem);
                    if (topCreatorsItem.getCreatorId() != null){
                        mgsItem.setCreatorId(AesUtil.encrypt(topCreatorsItem.getCreatorId().toString(), password));
                    }else{
                        mgsItem.setCreatorId(null);
                    }
                    creatorList.add(mgsItem);
                }
        );

        ret.getData().getSaleList().forEach(topSalesItem -> {
            TopSalesMgsItem mgsItem = CopyBeanUtils.fastCopy(topSalesItem, TopSalesMgsItem.class);

            if (topSalesItem.getCreatorId()!=null){
                mgsItem.setCreatorId(AesUtil.encrypt(topSalesItem.getCreatorId().toString(), password));
            }else{
                mgsItem.setCreatorId(null);
            }

            salesList.add(mgsItem);
        });

        MultiRankingMgsResponse response = MultiRankingMgsResponse.builder()
                .collectionList(ret.getData().getCollectionList())
                .creatorList(creatorList)
                .saleList(salesList)
                .build();

        return response;
    }
}
