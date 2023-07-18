package com.binance.mgs.nft.market.proxy;

import com.binance.master.commons.SearchResult;
import com.binance.master.constant.Constant;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.common.cache.CacheSeneEnum;
import com.binance.mgs.nft.common.cache.CacheUtils;
import com.binance.mgs.nft.common.helper.RedisHelper;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.market.request.CommonSearchRequest;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.MarketSuggestionListMgs;
import com.binance.mgs.nft.mysterybox.helper.MysteryBoxCacheHelper;
import com.binance.mgs.nft.nftasset.controller.helper.UserFollowHelper;
import com.binance.nft.market.ifae.*;
import com.binance.nft.market.request.AssetMarketRequest;
import com.binance.nft.market.request.MarketProductRequest;
import com.binance.nft.market.request.MysteryProductQueryArg;
import com.binance.nft.market.request.SuggestionSearchRequest;
import com.binance.nft.market.vo.*;
import com.binance.nft.mystery.api.vo.MysteryBoxProductVo;
import com.binance.nft.tradeservice.utils.Switcher;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MarketCacheProxy {

    @Resource
    private ProductMarketApi productMarketApi;

    @Resource
    private ProductTagApi productTagApi;

    @Resource
    private MysteryMarketPlaceApi mysteryMarketPlaceApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private MysteryBoxCacheHelper mysteryBoxCacheHelper;

    @Resource
    private SuggestionApi suggestionApi;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private AssetMarketApi assetMarketApi;
    @Resource
    private MgsNftProperties mgsNftProperties;
    @Resource
    private RedisHelper redisHelper;
    @Resource
    private UserFollowHelper userFollowHelper;

    private LoadingCache<Integer, Map<String, Long>> STOCK_LOCK_CACHE = CacheBuilder.<Integer, Map<String, Long>>newBuilder()
            .initialCapacity(1)
            .maximumSize(1)
            .refreshAfterWrite(5, TimeUnit.SECONDS)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build(CacheLoader.asyncReloading(new CacheLoader<Integer, Map<String, Long>>() {
                @Override
                public Map<String, Long> load(Integer key) throws Exception {
                    return redisHelper.getLockFlagList();
                }
            }, Executors.newFixedThreadPool(1)));


    public SearchResult<MarketProductItem> productList(MarketProductRequest request) throws Exception {
        TypeReference typeReference = new TypeReference<SearchResult<MarketProductItem>>() {};
        Function<MarketProductRequest, SearchResult<MarketProductItem>> function = req -> {
            APIResponse<SearchResult<MarketProductItem>> response = null;
            try {
                response = productMarketApi.productList(APIRequest.instance(request));
            } catch (Exception e) {
                log.warn("productList error", e);
            }
            baseHelper.checkResponse(response);
            return response.getData();
        };

        return CacheUtils.<MarketProductRequest, SearchResult<MarketProductItem>>getData(redissonClient, request, CacheSeneEnum.SEARCH_REGULAR,
                request.getPage() < 5, typeReference, function);
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public List<MysterySimpleListItem> mysterySimpleList() throws Exception {
        APIResponse<List<MysterySimpleListItem>> response = mysteryMarketPlaceApi.simpleList();
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public CommonPageResponse<MysteryProductItemVo> mysteryList(CommonPageRequest<MysteryProductQueryArg> request) throws Exception {

        TypeReference typeReference = new TypeReference<CommonPageResponse<MysteryProductItemVo>>() {};
        Function<CommonPageRequest<MysteryProductQueryArg>, CommonPageResponse<MysteryProductItemVo>> function = req -> {
            // 处理盲盒关键字 交换为serialNo
            exchangeMysteryKeyword(request.getParams());

            APIResponse<CommonPageResponse<MysteryProductItemVo>> response = null;
            try {
                response = mysteryMarketPlaceApi.mysteryList(request);
            } catch (Exception e) {
                log.error("mysteryList error", e);
            }
            baseHelper.checkResponse(response);
            return response.getData();
        };

        return CacheUtils.<CommonPageRequest<MysteryProductQueryArg>, CommonPageResponse<MysteryProductItemVo>>getData(redissonClient, request, CacheSeneEnum.SEARCH_MYSTERY,
                request.getPage() < 8, typeReference, function);
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'sl-'+#keyword")
    public MarketSuggestionListMgs suggestionList(String keyword, String password) throws Exception {
        APIResponse<MarketSuggestionList> detail = suggestionApi.suggestionList(
                APIRequest.instance(keyword));
        baseHelper.checkResponse(detail);

        List<MarketSuggestionListMgs.NFTCreators> mgsCreators = new ArrayList<>(detail.getData().getNftCreatorsList().size());
        if (detail.getData().getNftCreatorsList() != null && !detail.getData().getNftCreatorsList().isEmpty()) {
            detail.getData().getNftCreatorsList().forEach(nftCreator -> {
                String userIdStr = null;
                if (nftCreator.getCreator_id() != null) {
                    userIdStr = AesUtil.encrypt(nftCreator.getCreator_id().toString(), password);
                }
                MarketSuggestionListMgs.NFTCreators creator = MarketSuggestionListMgs.NFTCreators.builder()
                        .creator_name(nftCreator.getCreator_name())
                        .creator_id(userIdStr)
                        .build();
                mgsCreators.add(creator);
            });
        }

        MarketSuggestionListMgs result = MarketSuggestionListMgs.builder()
                .mysteryCollectionList(detail.getData().getMysteryCollectionList())
                .nftCollectionList(detail.getData().getNftCollectionList())
                .nftCreatorsList(mgsCreators).build();
        return result;
    }

    public MarketSuggestionListMgs suggestionListV2(CommonSearchRequest request, Long userId, String password, String scenesType) {
        log.info("suggestionListV2 scenesType:{},request:{}", scenesType, JsonUtils.toJsonNotNullKey(request));
        String keyword = Optional.ofNullable(request).map(CommonSearchRequest::getKeyword).orElse("");
        if (StringUtils.isEmpty(keyword)) {
            return MarketSuggestionListMgs.builder().build();
        }

        SuggestionSearchRequest searchRequest = new SuggestionSearchRequest();
        searchRequest.setKeyword(request.getKeyword());
        searchRequest.setUserId(userId);
        searchRequest.setSource(request.getSource());
        searchRequest.setScenesType(scenesType);

        TypeReference typeReference = new TypeReference<MarketSuggestionList>() {};
        Function<SuggestionSearchRequest, MarketSuggestionList> function = req -> {
            APIResponse<MarketSuggestionList> response = null;
            try {
                response = suggestionApi.suggestionListV2(APIRequest.instance(searchRequest));
            } catch (Exception e) {
                log.error("suggestionListV2 error", e);
            }
            baseHelper.checkResponse(response);
            return response.getData();
        };
        String cacheKey = CacheUtils.generateKey(request.getKeyword() + request.getSource() + scenesType);
        MarketSuggestionList detail = CacheUtils.<SuggestionSearchRequest, MarketSuggestionList>getData(redissonClient, searchRequest, CacheSeneEnum.SUGGESTION_SEARCH,
                Boolean.TRUE, typeReference, function, cacheKey);

        //follow relation
        List<Long> creatorIds = detail.getNftCreatorsList().stream()
                .map(t-> t.getCreator_id())
                .filter(uid -> !Objects.equals(userId, uid))
                .collect(Collectors.toList());
        Map<Long, Boolean> followMap = userFollowHelper.queryFollow(creatorIds, userId);

        List<MarketSuggestionListMgs.NFTCreators> mgsCreators = new ArrayList<>(detail.getNftCreatorsList().size());
        if (detail.getNftCreatorsList() != null && !detail.getNftCreatorsList().isEmpty()) {
            detail.getNftCreatorsList().forEach(nftCreator -> {
                String userIdStr = null;
                if (nftCreator.getCreator_id() != null) {
                    userIdStr = AesUtil.encrypt(nftCreator.getCreator_id().toString(), password);
                }
                //follow relation
                Integer followRelation = Switcher.<Integer>Case().when(Objects.equals(userId, nftCreator.getCreator_id())).then(2)
                        .when(followMap.getOrDefault(nftCreator.getCreator_id(), false)).then(1)
                        .end(0);

                MarketSuggestionListMgs.NFTCreators creator = MarketSuggestionListMgs.NFTCreators.builder()
                        .creator_name(nftCreator.getCreator_name())
                        .creator_id(userIdStr)
                        .avatarUrl(nftCreator.getAvatarUrl())
                        .followerCount(nftCreator.getFollowerCount())
                        .followRelation(followRelation)
                        .build();
                mgsCreators.add(creator);
            });
        }

        MarketSuggestionListMgs result = MarketSuggestionListMgs.builder()
                .mysteryCollectionList(detail.getMysteryCollectionList())
                .nftCollectionList(detail.getNftCollectionList())
                .nftItemList(detail.getNftItemList())
                .nftCreatorsList(mgsCreators).build();
        return result;
    }

    public SearchResult<AssetMarketVo> assetList(AssetMarketRequest request, Long userId) throws Exception {
        List<Long> whitelist = mgsNftProperties.getAssetListV2Whitelist();
        Integer abPercent = mgsNftProperties.getAssetListV2AbPercent();
        boolean isUseNewApi = isUseNewApi(userId, whitelist, abPercent);
        TypeReference typeReference = new TypeReference<SearchResult<AssetMarketVo>>() {};

        Function<AssetMarketRequest, SearchResult<AssetMarketVo>> function = req -> {
            APIResponse<SearchResult<AssetMarketVo>> response = null;
            try {
                if (isUseNewApi) {
                    response = assetMarketApi.assetListV2(APIRequest.instance(request));
                }else {
                    response = assetMarketApi.assetList(APIRequest.instance(request));
                }
            } catch (Exception e) {
                log.warn("assetList error", e);
            }
            baseHelper.checkResponse(response);
            return response.getData();
        };
        CacheSeneEnum scene = isUseNewApi ? CacheSeneEnum.SEARCH_ASSET_V2 : CacheSeneEnum.SEARCH_ASSET;
        return CacheUtils.<AssetMarketRequest, SearchResult<AssetMarketVo>>getData(redissonClient, request, scene,
                request.getPage() < 5, typeReference, function);
    }

    private void exchangeMysteryKeyword(MysteryProductQueryArg queryArg) {
        String keyword = Optional.ofNullable(queryArg.getKeyword())
                .map(String::toLowerCase)
                .orElse(null);
        if(StringUtils.isBlank(keyword)) {
            return;
        }

        try {
            List<MysteryBoxProductVo> mysteryBoxProductVos = mysteryBoxCacheHelper.getMysteryBoxProductVo(isGray());
            if(CollectionUtils.isEmpty(mysteryBoxProductVos)) {
                return;
            }

            List<Long> serialNos = mysteryBoxProductVos.stream()
                    .filter(vo -> vo.getName().toLowerCase().contains(keyword)
                    ||vo.getSubTitle().toLowerCase().contains(keyword))
                    .map(MysteryBoxProductVo::getSerialsNo)
                    .collect(Collectors.toList());
            if(CollectionUtils.isEmpty(serialNos)) {
                return;
            }

            serialNos = CollectionUtils.isEmpty(queryArg.getSerialNo()) ? serialNos :
                    (List<Long>) CollectionUtils.intersection(serialNos, queryArg.getSerialNo());
            if(CollectionUtils.isEmpty(serialNos)) {
                return;
            }
//            if (CollectionUtils.isEmpty(queryArg.getSerialNo()) || (!CollectionUtils.isEmpty(queryArg.getSerialNo()) && (!StringUtils.isEmpty(keyword)))){
            if (CollectionUtils.isEmpty(queryArg.getSerialNo())){
                queryArg.setSearchKeyWord(keyword);
            }

            queryArg.setSerialNo(serialNos);
            queryArg.setKeyword(StringUtils.EMPTY);
        } catch (Exception e) {
            log.error("getMysteryBoxProductVo error", e);
        }

    }

    public Boolean isGray() {
        String env = WebUtils.getHeader(Constant.GRAY_ENV_HEADER);
        return StringUtils.isNotBlank(env) && !"normal".equals(env);

    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public List<ProductTagInfoVo> productTagList(Integer env) {
        APIResponse<List<ProductTagInfoVo>> response = productTagApi.productTagList(APIRequest.instance(env));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public boolean containsDex(Long userId) {
        userId = Optional.ofNullable(userId).orElse(99L);
        Map<String, Object> data = new HashMap<>(mgsNftProperties.getCommonConfig());

        if(userId != 99L && Objects.nonNull(data) &&data.containsKey("aggregatorWhitelist")) {
            String whitelist = (String) data.get("aggregatorWhitelist");
            if(whitelist.indexOf(userId.toString()) > 0) return true;
        }
        if(Objects.nonNull(data) &&data.containsKey("aggregatorAbPercent")) {
            Number number = (Number) data.get("aggregatorAbPercent");
            return (userId % 100) < number.intValue();
        }

        return false;
    }

    public boolean checkStockAvailable(Long productId) {
        if(Objects.isNull(productId) ||Objects.equals(productId, 0L)) return true;
        Map<String, Long> map = Collections.EMPTY_MAP;
        try {
            map = STOCK_LOCK_CACHE.get(0);
            log.debug("checkStockAvailable {} {}", map, productId);
            Long ts = map.get(String.valueOf(productId));
            if(Objects.isNull(ts)) return true;
            return ts.compareTo(System.currentTimeMillis()) <= 0;
        } catch (Exception e) {
        }
        return true;
    }

    public boolean isUseNewApi(Long userId, List<Long> searchWhitelist,Integer searchAbPercent) {
        userId = Optional.ofNullable(userId).orElse(99L);
        if(CollectionUtils.isNotEmpty(searchWhitelist) && searchWhitelist.contains(userId)) {
            return true;
        }
        if(Objects.nonNull(searchAbPercent)) {
            return (userId % 100) < searchAbPercent;
        }
        return false;
    }
}
