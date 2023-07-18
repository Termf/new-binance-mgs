package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.market.ifae.NftMysteryAllCollectionApi;
import com.binance.nft.market.request.allcollection.AllCollectionRecRequest;
import com.binance.nft.market.request.allcollection.AllCollectionSearchRequest;
import com.binance.nft.market.vo.AllCollectionItemVo;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class NftMysteryAllCollectionController {

    private final NftMysteryAllCollectionApi nftMysteryAllCollectionApi;

    private final BaseHelper baseHelper;

    @PostMapping("/friendly/nft/all-collection-page/search")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'mysteryAllCollectionSearch-'+#request")
    public CommonRet<CommonPageResponse<AllCollectionItemVo>> mysteryAllCollectionSearch(@RequestBody AllCollectionSearchRequest request) throws Exception {
        APIResponse<CommonPageResponse<AllCollectionItemVo>> response = nftMysteryAllCollectionApi.mysteryAllCollectionSearch(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<CommonPageResponse<AllCollectionItemVo>>(response.getData());
    }

    @PostMapping("/friendly/nft/all-collection-page/recommend-list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'mysteryAllCollectionRecommendList-'+#request")
    public CommonRet<CommonPageResponse<AllCollectionItemVo>> mysteryAllCollectionRecommendList(@RequestBody AllCollectionRecRequest request) throws Exception {
        Long userId = baseHelper.getUserId();
        log.info("mysteryAllCollectionRecommendList request: {} userId: {}", request, userId);
        request.setUserId(userId);
        APIResponse<CommonPageResponse<AllCollectionItemVo>> response = nftMysteryAllCollectionApi.mysteryAllCollectionRecommendList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<CommonPageResponse<AllCollectionItemVo>>(response.getData());
    }

}
