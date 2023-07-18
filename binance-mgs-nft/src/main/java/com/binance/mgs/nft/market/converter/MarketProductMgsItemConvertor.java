package com.binance.mgs.nft.market.converter;

import com.binance.master.commons.SearchResult;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.MarketProductMgsItem;
import com.binance.mgs.nft.market.vo.UserInfoMgsVo;
import com.binance.nft.market.vo.MarketProductItem;

import java.util.ArrayList;
import java.util.List;

public class MarketProductMgsItemConvertor {
    public static SearchResult<MarketProductMgsItem> convert(SearchResult<MarketProductItem> resp, String password) {

        List<MarketProductMgsItem> marketProductMgsItemList = new ArrayList<>(resp.getRows().size());

        resp.getRows().forEach(marketProductItem -> {
                    MarketProductMgsItem item = CopyBeanUtils.fastCopy(marketProductItem, MarketProductMgsItem.class);
                    item.setCreator(CopyBeanUtils.fastCopy(marketProductItem.getCreator(), UserInfoMgsVo.class));
                    item.setOwner(CopyBeanUtils.fastCopy(marketProductItem.getOwner(), UserInfoMgsVo.class));

                    item.getCreator().setUserId(AesUtil.encrypt(marketProductItem.getCreator().getUserId().toString(), password));
                    item.getOwner().setUserId(AesUtil.encrypt(marketProductItem.getOwner().getUserId().toString(), password));
                    marketProductMgsItemList.add(item);
                }
        );

        return new SearchResult(marketProductMgsItemList, resp.getTotal());
    }
}
