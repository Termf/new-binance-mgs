package com.binance.mgs.nft.market.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.market.converter.MultiRankingMgsResponseConverter;
import com.binance.mgs.nft.market.converter.TopSalesMgsItemConvertor;
import com.binance.mgs.nft.market.proxy.HomepageCacheProxy;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.mgs.nft.market.vo.MultiRankingMgsResponse;
import com.binance.mgs.nft.market.vo.TopCreatorsMgsItem;
import com.binance.mgs.nft.market.vo.TopSalesMgsItem;
import com.binance.mgs.nft.nftasset.controller.helper.UserFollowHelper;
import com.binance.nft.market.ifae.NftLiteApi;
import com.binance.nft.market.ifae.NftRankingApi;
import com.binance.nft.market.request.CollectionRankRequest;
import com.binance.nft.market.request.TopCollectionRankRequest;
import com.binance.nft.market.vo.ranking.*;
import com.binance.nft.tradeservice.utils.Switcher;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping({"/v1/public/nft/","/v1/friendly/nft/"})
@RestController
@RequiredArgsConstructor
@Slf4j
public class NftRankingController {

    private final NftRankingApi nftRankingApi;

    private final NftLiteApi nftLiteApi;
    private final HomepageCacheProxy homepageCacheProxy;
    private final UserFollowHelper userFollowHelper;
    private final BaseHelper baseHelper;
    private final MarketCacheProxy marketCacheProxy;
    private final MgsNftProperties mgsNftProperties;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @ApolloJsonValue("${market.ranking.creators.black.list:[]}")
    private Set<Long> rankingCreatorsBlackList;

    @GetMapping("/ranking/top-sales/{period}/{limit}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<TopSalesMgsItem>> topSalesRanking(@PathVariable("period") Integer period, @PathVariable("limit") Integer limit) throws Exception {
        APIResponse<RankingResponse<TopSalesItem>> ret = nftRankingApi.topSalesRanking(period, limit);
        RankingResponse<TopSalesMgsItem> response =  TopSalesMgsItemConvertor.convert(ret, AES_PASSWORD);
        return new CommonRet<>(response);
    }

    @GetMapping("/ranking/top-collections/{period}/{limit}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<TopCollectionsItem>> topCollectionsRanking(@PathVariable("period") Integer period, @PathVariable("limit") Integer limit) throws Exception {
        APIResponse<RankingResponse<TopCollectionsItem>> ret = nftRankingApi.topCollectionsRanking(period, limit);
        return new CommonRet<>(ret.getData());
    }

    @GetMapping("/ranking/top-creators/{period}/{limit}")
    public CommonRet<RankingResponse<TopCreatorsMgsItem>> topCreatorsRanking(@PathVariable("period") Integer period, @PathVariable("limit") Integer limit) throws Exception {
        Long userId = baseHelper.getUserId();
        RankingResponse<TopCreatorsMgsItem> response = homepageCacheProxy.topCreatorsRanking(period, limit * 2);
        if(CollectionUtils.isNotEmpty(response.getList())) {
            List<Long> creatorIds = response.getList().stream()
                    .map(TopCreatorsMgsItem::getCreatorIdOrig)
                    .filter(uid -> !Objects.equals(userId, uid))
                    .collect(Collectors.toList());
            Map<Long, Boolean> followMap = userFollowHelper.queryFollow(creatorIds, userId);
            List<TopCreatorsMgsItem> list = response.getList().stream().filter(f->!rankingCreatorsBlackList.contains(f.getCreatorIdOrig())).map(o -> {
                Integer followRelation = Switcher.<Integer>Case().when(Objects.equals(userId, o.getCreatorIdOrig())).then(2)
                        .when(followMap.getOrDefault(o.getCreatorIdOrig(), false)).then(1)
                        .end(0);
                TopCreatorsMgsItem vo = CopyBeanUtils.fastCopy(o, TopCreatorsMgsItem.class);
                vo.setFollowRelation(followRelation);
                vo.setCreatorIdOrig(null);
                return vo;
            }).sorted(Comparator.comparing(TopCreatorsMgsItem::getRank)).collect(Collectors.toList());
            for (TopCreatorsMgsItem item: list) {
                item.setRank(list.indexOf(item) + 1);
            }
            Date updateTime = response.getUpdateTime();
            response = new RankingResponse<>();
            response.setList(list.stream().limit(limit).collect(Collectors.toList()));
            response.setUpdateTime(updateTime);
        }
        return new CommonRet<>(response);
    }


    @GetMapping("/ranking/top-creators-follows/{period}/{limit}")
    public CommonRet<RankingResponse<TopCreatorsMgsItem>> topCreatorsFollowsRanking(@PathVariable("period") Integer period, @PathVariable("limit") Integer limit) throws Exception {
        Long userId = baseHelper.getUserId();
        RankingResponse<TopCreatorsMgsItem> response = homepageCacheProxy.topCreatorsFollowsRanking(period, limit);
        if(CollectionUtils.isNotEmpty(response.getList())) {
            List<Long> creatorIds = response.getList().stream()
                    .map(TopCreatorsMgsItem::getCreatorIdOrig)
                    .filter(uid -> !Objects.equals(userId, uid))
                    .collect(Collectors.toList());
            Map<Long, Boolean> followMap = userFollowHelper.queryFollow(creatorIds, userId);
            List<TopCreatorsMgsItem> list = response.getList().stream().map(o -> {
                Integer followRelation = Switcher.<Integer>Case().when(Objects.equals(userId, o.getCreatorIdOrig())).then(2)
                        .when(followMap.getOrDefault(o.getCreatorIdOrig(), false)).then(1)
                        .end(0);
                TopCreatorsMgsItem vo = CopyBeanUtils.fastCopy(o, TopCreatorsMgsItem.class);
                vo.setFollowRelation(followRelation);
                vo.setCreatorIdOrig(null);
                return vo;
            }).collect(Collectors.toList());
            Date updateTime = response.getUpdateTime();
            response = new RankingResponse<>();
            response.setList(list);
            response.setUpdateTime(updateTime);
        }
        return new CommonRet<>(response);
    }


    @GetMapping("/ranking/multiple")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<MultiRankingMgsResponse> multipleRanking() throws Exception {
        APIResponse<MultiRankingResponse> ret = nftRankingApi.multiRanking();
        MultiRankingMgsResponse response = MultiRankingMgsResponseConverter.convert(ret, AES_PASSWORD);
        return new CommonRet<>(response);
    }

    @GetMapping("/ranking/lite/multiple")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<RankingResponse<TopCollectionsItem>> liteCollectionsRanking()  {

        APIResponse<RankingResponse<TopCollectionsItem>> response = nftLiteApi.topCollectionsRanking(7, 10);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/ranking/top-collections-v2")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<SearchResult<TopCollectionVo>> topCollectionsRankingV2(@Valid @RequestBody TopCollectionRankRequest request) throws Exception {
        if (StringUtils.isEmpty(request.getNetwork()) || StringUtils.isEmpty(request.getPeriod()) || StringUtils.isEmpty(request.getSortType())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        Long userId = baseHelper.getUserId();
        boolean supportEth = marketCacheProxy.containsDex(userId);
        request.setSupportEth(supportEth);

        List<Long> whitelist = mgsNftProperties.getAssetListV2Whitelist();
        Integer abPercent = mgsNftProperties.getRankingV2AbPercent();
        boolean isUseNewApi = marketCacheProxy.isUseNewApi(userId, whitelist, abPercent);

        APIResponse<SearchResult<TopCollectionVo>> response;
        if (isUseNewApi) {
            CollectionRankRequest requestNew = CopyBeanUtils.fastCopy(request, CollectionRankRequest.class);
            response = nftRankingApi.collectionsRanking(APIRequest.instance(requestNew));
        } else {
            response = nftRankingApi.topCollectionsRankingV2(APIRequest.instance(request));
        }
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>trend ranking</h2>
     * */
    @PostMapping("/ranking/trend-collection")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<SearchResult<TopCollectionVo>> queryNftTrendRanking(@RequestBody CollectionRankRequest request) {

        request.setCount(request.getRows());
        APIResponse<SearchResult<TopCollectionVo>> apiResponse = nftRankingApi.queryNftTrendRanking(APIRequest.instance(
                request));
        baseHelper.checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @GetMapping("/ranking/top-collections-v2/{network}/{period}/{sortType}/{count}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<List<TopCollectionVo>> topCollectionsRankingV2(@PathVariable("network") String network, @PathVariable("period") String period, @PathVariable("sortType") String sortType, @PathVariable("count") Integer count) throws Exception {
        if (StringUtils.isEmpty(network) || StringUtils.isEmpty(period) || StringUtils.isEmpty(sortType)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        Long userId = baseHelper.getUserId();
        boolean supportEth = marketCacheProxy.containsDex(userId);
        APIResponse<SearchResult<TopCollectionVo>> response = nftRankingApi.topCollectionsRankingV2(APIRequest.instance(TopCollectionRankRequest.builder()
                .network(network)
                .period(period)
                .sortType(sortType)
                .count(count)
                .page(1)
                .rows(100)
                .supportEth(supportEth)
                .build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData().getRows());
    }
}
