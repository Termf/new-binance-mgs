package com.binance.mgs.nft.market.converter;

import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.TopSalesMgsItem;
import com.binance.nft.market.vo.ranking.RankingResponse;
import com.binance.nft.market.vo.ranking.TopSalesItem;

import java.util.ArrayList;
import java.util.List;

public class TopSalesMgsItemConvertor {

    public static RankingResponse<TopSalesMgsItem> convert(APIResponse<RankingResponse<TopSalesItem>> ret, String password) {

        List<TopSalesMgsItem> topSalesMgsItemList = new ArrayList<>(ret.getData().getList().size());
        ret.getData().getList().forEach(topSalesItem -> {
            TopSalesMgsItem item = CopyBeanUtils.fastCopy(topSalesItem, TopSalesMgsItem.class);

            if (topSalesItem.getCreatorId() != null){
                item.setCreatorId(AesUtil.encrypt(topSalesItem.getCreatorId().toString(), password));
            }else{
                item.setCreatorId(null);
            }
            topSalesMgsItemList.add(item);

        });

        RankingResponse<TopSalesMgsItem> response = new RankingResponse();
        response.setUpdateTime(ret.getData().getUpdateTime());
        response.setList(topSalesMgsItemList);
        return response;
    }
}
