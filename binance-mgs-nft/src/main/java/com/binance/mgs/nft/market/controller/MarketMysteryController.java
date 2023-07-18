package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIResponse;
import com.binance.master.utils.DateUtils;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.nft.market.ifae.MysteryMarketPlaceApi;
import com.binance.nft.market.request.MysteryProductQueryArg;
import com.binance.nft.market.vo.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RequestMapping("/v1/public/nft/market-mystery")
@RestController
@RequiredArgsConstructor
public class MarketMysteryController {

    private final MarketCacheProxy marketCacheProxy;
    private final MysteryMarketPlaceApi mysteryMarketPlaceApi;
    private final BaseHelper baseHelper;

    @PostMapping("/simple-list")
    @GetMapping("/simple-list")
    public CommonRet<List<MysterySimpleListItem>> mysterySimpleList() throws Exception {
        List<MysterySimpleListItem> resp = marketCacheProxy.mysterySimpleList();
        Long timestamp = DateUtils.getNewUTCTimeMillis();
        if(CollectionUtils.isNotEmpty(resp)) {
            resp.forEach(litem -> ListUtils.emptyIfNull(litem.getItemList()).forEach(item -> item.setTimestamp(timestamp)));
        }
        return new CommonRet<>(resp);
    }

    @PostMapping("/mystery-list")
    public CommonRet<CommonPageResponse<MysteryProductItemVo>> mysteryList(@Valid @RequestBody CommonPageRequest<MysteryProductQueryArg> request) throws Exception {
        final Integer size = request.getSize();
        if (size == null || size != 16){
            return new CommonRet<>();
        }
        CommonPageResponse<MysteryProductItemVo> resp = marketCacheProxy.mysteryList(request);
        Long timestamp = DateUtils.getNewUTCTimeMillis();
        if(CollectionUtils.isNotEmpty(resp.getData())) {
            resp.getData().forEach(item -> item.setTimestamp(timestamp));
        }
        return new CommonRet<>(resp);
    }


    @GetMapping("/recommend")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public CommonRet<List<MysteryRecommendItem>> recommendList() throws Exception {
        APIResponse<List<MysteryRecommendItem>> response = mysteryMarketPlaceApi.recommendList();
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

}
