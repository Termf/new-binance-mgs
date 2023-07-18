package com.binance.mgs.nft.mysterybox.helper;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.nft.market.ifae.NftMarketCollectionApi;
import com.binance.nft.market.request.CollectionDetailRequest;
import com.binance.nft.market.vo.MysteryBoxCollectionForMiniProgram;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.mgs.nft.mysterybox.vo.RarityVo;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.nft.assetservice.api.IUserInfoApi;
import com.binance.nft.common.data.MappingProductType;
import com.binance.nft.common.data.ProductStatusMappingHelper;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxAdminApi;
import com.binance.nft.mystery.api.iface.INFTMysteryBoxQueryApi;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.mystery.api.vo.*;
import com.binance.nft.mystery.api.vo.MysteryBoxDetailQueryType;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import com.binance.nft.mystery.api.vo.MysteryBoxProductVo;
import com.binance.nft.mystery.api.vo.QueryMysteryBoxDetailRequest;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysteryBoxCacheHelper implements ApplicationRunner {

    private final BaseHelper baseHelper;

    private final NFTMysteryBoxApi nftMysteryBoxApi;

    private final NFTMysteryBoxAdminApi nftMysteryBoxAdminApi;

    private final NftMarketCollectionApi nftMarketCollectionApi;

    private final HashMap<Integer, String> DEFAULT_RARITIES = new HashMap<Integer, String>(){
        {
            put(0,"SSR");
            put(1, "SR");
            put(2, "R");
            put(3, "N");
        }
    };

    private final INFTMysteryBoxQueryApi inftMysteryBoxQueryApi;

    private final IUserInfoApi iUserInfoApi;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public void init() throws ExecutionException {
        mysteryBoxProductsLoadingCache.get(0L);
        mysteryBoxProductsLoadingCache.get(1L);
    }


    /**
     * mystery-box商品缓存
     */
    private final LoadingCache<Long, List<MysteryBoxProductVo>> mysteryBoxProductsLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(4)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(60, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, List<MysteryBoxProductVo>>() {
                        @Override
                        public List<MysteryBoxProductVo> load(Long isGray) throws Exception {
                            APIResponse<List<MysteryBoxProductVo>> listAllAvailableMysteryBox = nftMysteryBoxApi.listAllAvailableMysteryBox();
                            baseHelper.checkResponse(listAllAvailableMysteryBox);
                            List<MysteryBoxProductVo> mysteryBoxProductVos = listAllAvailableMysteryBox.getData();
                            if (isGray == 0) {
                                mysteryBoxProductVos = mysteryBoxProductVos.stream().filter(
                                        mysteryBoxProductVo -> !mysteryBoxProductVo.getIsGray()
                                ).collect(Collectors.toList());
                            }
                            mysteryBoxProductVos.forEach(
                                    mysteryBoxProductVo ->
                                            mysteryBoxProductVo.setMappingStatus(ProductStatusMappingHelper
                                                    .mappingStatus(MappingProductType.MYSTERY_BOX.name(), mysteryBoxProductVo.getStartTime(), mysteryBoxProductVo.getEndTime(), mysteryBoxProductVo.getStatus()))
                            );
                            List<MysteryBoxProductVo> mysteryBoxProductVosResult = Lists.newArrayList();
                            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(mysteryBoxProductVos)) {
                                Map<Integer, List<MysteryBoxProductVo>> productMap = mysteryBoxProductVos.stream()
                                        .collect(Collectors.groupingBy(MysteryBoxProductVo::getMappingStatus));
                                List<MysteryBoxProductVo> removeOnSell = productMap.remove(0);
                                if (CollectionUtils.isNotEmpty(removeOnSell)) {
                                    removeOnSell.sort(Comparator.comparing(MysteryBoxProductVo::getStartTime));
                                    mysteryBoxProductVosResult.addAll(removeOnSell);
                                }
                                List<MysteryBoxProductVo> removeNotStart = productMap.remove(-1);
                                if (CollectionUtils.isNotEmpty(removeNotStart)) {
                                    removeNotStart.sort(Comparator.comparing(MysteryBoxProductVo::getStartTime));
                                    mysteryBoxProductVosResult.addAll(removeNotStart);
                                }
                                List<MysteryBoxProductVo> remainingProduct = productMap.values().stream().filter(CollectionUtils::isNotEmpty).reduce(
                                        Lists.newArrayList(), (l1, l2) -> {
                                            l1.addAll(l2);
                                            return l1;
                                        }
                                );
                                if (CollectionUtils.isNotEmpty(remainingProduct)) {
                                    remainingProduct.sort(Comparator.comparing(MysteryBoxProductVo::getStartTime, Comparator.reverseOrder()));
                                    mysteryBoxProductVosResult.addAll(remainingProduct);
                                }
                            }
                            return mysteryBoxProductVosResult;
                        }
                    }, Executors.newFixedThreadPool(1)));


    /**
     * mystery-box商品缓存
     */
    private final LoadingCache<Long, List<MysteryBoxProductVo>> mysteryBoxOnSellProductsLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(2)
                    .refreshAfterWrite(30, TimeUnit.MINUTES)
                    .expireAfterAccess(60, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, List<MysteryBoxProductVo>>() {
                        @Override
                        public List<MysteryBoxProductVo> load(Long ignore) throws Exception {
                            APIResponse<List<MysteryBoxProductVo>> listAllAvailableMysteryBox = nftMysteryBoxApi.listOnSellAvailableMysteryBox();
                            baseHelper.checkResponse(listAllAvailableMysteryBox);
                            List<MysteryBoxProductVo> mysteryBoxProductVosResult = listAllAvailableMysteryBox.getData();
                            return mysteryBoxProductVosResult;
                        }
                    }, Executors.newFixedThreadPool(1)));


    /**
     * mystery-box商品缓存
     */
    private final LoadingCache<Long, String> mysteryBoxProductCache =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .refreshAfterWrite(1, TimeUnit.SECONDS)
                    .expireAfterAccess(60, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, String>() {
                        @Override
                        public String load(Long productId) throws Exception {
                            APIResponse<MysteryBoxProductDetailVo> mysteryBoxProductDetailVoAPIResponse
                                    = nftMysteryBoxApi.queryMysteryBoxDetail(APIRequest.instance(
                                    QueryMysteryBoxDetailRequest.builder()
                                            .productId(productId)
                                            .mysteryBoxDetailQueryType(MysteryBoxDetailQueryType.PRODUCT_ID)
                                            .build()));
                            baseHelper.checkResponse(mysteryBoxProductDetailVoAPIResponse);
                            MysteryBoxProductDetailVo detailVoAPIResponseData = mysteryBoxProductDetailVoAPIResponse.getData();
                            return JsonUtils.toJsonHasNullKey(detailVoAPIResponseData);
                        }
                    }, Executors.newFixedThreadPool(1)));

    LoadingCache<Long, List<RarityVo>> SERIAL_RARITY_LIST_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, List<RarityVo>>() {
                        @Override
                        public List<RarityVo> load(Long collectionId) throws Exception {
                            CommonPageRequest request = CommonPageRequest.builder().size(100).params(MysteryBoxConfigVo.builder().serialsNo(collectionId).build()).build();
                            APIResponse<CommonPageResponse<MysteryBoxConfigVo>> mysteryConfig = nftMysteryBoxAdminApi.listMysteryBoxConfig(APIRequest.instance(request));
                            baseHelper.checkResponse(mysteryConfig);
                            if (mysteryConfig.getData().getTotal() == 0) {
                                throw new Exception("invoking listMysteryBoxConfig method: result is empty");
                            }
                            List<RarityVo> rarityVos = new ArrayList<>();
                            rarityVos.add(RarityVo.builder().value(-1).name("Unopened").build());
                            Map<Integer, MysteryBoxConfigVo> mysteryBoxConfigVos = mysteryConfig.getData().getData().stream().collect(Collectors.toMap(MysteryBoxConfigVo::getRarity, Function.identity(), (v1,v2) -> v1));
                            for (Integer key : mysteryBoxConfigVos.keySet()) {
                                MysteryBoxConfigVo mysteryBoxConfigVo = mysteryBoxConfigVos.get(key);
                                rarityVos.add(RarityVo.builder()
                                        .value(mysteryBoxConfigVo.getRarity())
                                        .name(DEFAULT_RARITIES.get(mysteryBoxConfigVo.getRarity()))
                                        .build());
                            }
                            return rarityVos;
                        }
                    }, Executors.newFixedThreadPool(1)));



    /**
     * mystery-box collection detail cache
     */
    private final LoadingCache<Long, MysteryBoxCollectionForMiniProgram> collectionsDetailLoadingCache =
            CacheBuilder.newBuilder()
                    .initialCapacity(50)
                    .maximumSize(1_000)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(2, TimeUnit.HOURS)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, MysteryBoxCollectionForMiniProgram>() {

                        @Override
                        public MysteryBoxCollectionForMiniProgram load(Long serial_no) throws Exception {
                            Map<Long, MysteryBoxCollectionForMiniProgram> res = procCollectionsDetail(Arrays.asList(serial_no));
                            for (Long key : res.keySet()) {
                                return res.get(key);
                            }
                            throw new BusinessException("collectionsDetailLoadingCache: collection is empty");
                        }

                        @Override
                        public Map<Long, MysteryBoxCollectionForMiniProgram> loadAll(Iterable<? extends Long> keys) throws Exception {
                            List<Long> serials_no = ((Set<Long>) keys).stream().collect(Collectors.toList());
                            return procCollectionsDetail(serials_no);
                        }
                    }, Executors.newFixedThreadPool(1)));


    private Map<Long, MysteryBoxCollectionForMiniProgram> procCollectionsDetail(List<Long> serials_no) throws Exception {
        if (CollectionUtils.isEmpty(serials_no)) {
            return MapUtils.EMPTY_MAP;
        }
        List<CollectionDetailRequest> requests = serials_no.stream().map(t->CollectionDetailRequest.builder().layerId(t).build()).collect(Collectors.toList());
        APIResponse<List<MysteryBoxCollectionForMiniProgram>> response = nftMarketCollectionApi.getMysteryDetailByCollectionIds(APIRequest.instance(requests));
        baseHelper.checkResponse(response);
        return ListUtils.emptyIfNull(response.getData()).stream().collect(Collectors.toMap(MysteryBoxCollectionForMiniProgram::getSerialsNo, Function.identity(), (v1, v2)->v1));
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public List<MysteryBoxCollectionForMiniProgram> getCollectionsDetail(int page, int size, List<Long> serials_no) {
        try {
            int offset = (page - 1) * size;
            Map<Long, MysteryBoxCollectionForMiniProgram> collectionMaps = collectionsDetailLoadingCache.getAll(serials_no);
            List<MysteryBoxCollectionForMiniProgram> res = collectionMaps.values().stream()
                    .sorted(Comparator.comparing(MysteryBoxCollectionForMiniProgram::getCreateDate).reversed())
                    .skip(offset).limit(size).collect(Collectors.toList());
            return res;
        } catch (Exception e) {
            log.error("MysteryBoxCacheHelper:getCollectionsDetail:", e);
            return Collections.emptyList();
        }
    }

    public List<MysteryBoxProductVo> getMysteryBoxProductVo(Boolean isGray) throws ExecutionException {
        return mysteryBoxProductsLoadingCache.get(isGray?1L:0L);
    }


    public List<MysteryBoxProductVo> getOnSellMysteryBoxProductVo() throws ExecutionException {
        return mysteryBoxOnSellProductsLoadingCache.get(0L);
    }

    public MysteryBoxProductDetailVo getProductDetailVo(Long productId) throws ExecutionException {
        String productDetail = mysteryBoxProductCache.get(productId);
        return JsonUtils.toObj(productDetail, MysteryBoxProductDetailVo.class);
    }

    public List<RarityVo> getSerialsRarityVo(Long collectionId) {
        try {
            if (collectionId == null){
                throw new IllegalArgumentException("Unsupported Collection");
            }
            return SERIAL_RARITY_LIST_CACHE.get(collectionId);
        } catch (Exception e) {
            log.error("getSerialsRarityVo error:", e);
            List<RarityVo> rarityVos = new ArrayList<>();
            rarityVos.add(RarityVo.builder().value(-1).name("Unopened").build());
            rarityVos.add(RarityVo.builder().value(0).name("SSR").build());
            rarityVos.add(RarityVo.builder().value(1).name("SR").build());
            rarityVos.add(RarityVo.builder().value(2).name("R").build());
            rarityVos.add(RarityVo.builder().value(3).name("N").build());
            return rarityVos;
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            init();
        }catch (Exception e){
            log.error("MysteryBoxCacheHelper init error:",e);
        }
    }
}
