package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.market.converter.HomePageResponseConvertor;
import com.binance.mgs.nft.market.proxy.HomepageCacheProxy;
import com.binance.mgs.nft.market.vo.HomeArtistMgsVo;
import com.binance.mgs.nft.market.vo.HomePageMgsResponse;
import com.binance.mgs.nft.market.vo.TopicMgs;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.mgs.nft.nftasset.controller.helper.UserFollowHelper;
import com.binance.nft.assetservice.api.data.vo.ItemsApproveInfo;
import com.binance.nft.common.data.ProductStatusMappingHelper;
import com.binance.nft.common.data.ProductTimeHelper;
import com.binance.nft.market.ifae.NftMarketApi;
import com.binance.nft.market.ifae.NftMarketArtistApi;
import com.binance.nft.market.ifae.NftMarketCollectionApi;
import com.binance.nft.market.request.CollectionFloorPriceRequest;
import com.binance.nft.market.request.HomeArtistRequest;
import com.binance.nft.market.request.HomeCollectionRequest;
import com.binance.nft.market.request.HomepageGroupCollectionRequest;
import com.binance.nft.market.vo.BannerType;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.market.vo.artist.HomeArtistVo;
import com.binance.nft.market.vo.collection.FloorPriceDetailVo;
import com.binance.nft.market.vo.collection.HomeCollectionDetailVo;
import com.binance.nft.market.vo.collection.HomeCollectionVo;
import com.binance.nft.market.vo.homepage.*;
import com.binance.nft.tradeservice.utils.Switcher;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class HomePageController implements ApplicationRunner {

    private final NftMarketApi marketApi;

    private final BaseHelper baseHelper;

    private final CrowdinHelper crowdinHelper;

    private final ApproveHelper approveHelper;

    private final NftMarketArtistApi nftMarketArtistApi;

    private final HomepageCacheProxy homepageCacheProxy;

    private final UserFollowHelper userFollowHelper;

    private final NftMarketCollectionApi nftMarketCollectionApi;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    public void init() throws ExecutionException {
        homePageResponseLocalCache.get(Boolean.TRUE);
        homePageResponseLocalCache.get(Boolean.FALSE);
        marketplaceBannerLocalCache.get(Boolean.TRUE);
        marketplaceBannerLocalCache.get(Boolean.FALSE);
    }

    /**
     * mystery-box商品缓存
     */
    private final LoadingCache<Boolean, String> homePageResponseLocalCache =
            CacheBuilder.newBuilder()
                    .maximumSize(2)
                    .refreshAfterWrite(10, TimeUnit.SECONDS)
                    .expireAfterAccess(60, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Boolean, String>() {
                        @Override
                        public String load(Boolean gray) throws Exception {
                            APIResponse<HomePageResponse> homePageResponseAPIResponse = marketApi.homepageBanners(
                                    APIRequest.instance(gray)
                            );
                            baseHelper.checkResponse(homePageResponseAPIResponse);
                            HomePageResponse homePageResponse = homePageResponseAPIResponse.getData();
                            return JsonUtils.toJsonHasNullKey(homePageResponse);
                        }
                    }, executorService));


    /**
     * mystery-box商品缓存
     */
    private final LoadingCache<Boolean, List<Banner>> marketplaceBannerLocalCache =
            CacheBuilder.newBuilder()
                    .maximumSize(2)
                    .refreshAfterWrite(10, TimeUnit.SECONDS)
                    .expireAfterAccess(60, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Boolean, List<Banner>>() {
                        @Override
                        public List<Banner> load(Boolean gray) throws Exception {
                            APIResponse<List<Banner>> listAPIResponse = marketApi.marketplaceBanners(
                                    APIRequest.instance(gray)
                            );
                            baseHelper.checkResponse(listAPIResponse);
                            return listAPIResponse.getData();
                        }
                    }, executorService));


    @GetMapping({"/public/nft/homepage", "/friendly/nft/homepage"})
    public CommonRet<HomePageMgsResponse> homePageContent(HttpServletRequest request) {
        try {
            Long userId = baseHelper.getUserId();
            String homePageResponseString = homePageResponseLocalCache.get(isGray(request));
            HomePageResponse homePageResponse = JsonUtils.parse(homePageResponseString, HomePageResponse.class);
            List<Banner> banners = homePageResponse.getBanners();
            refreshStatusAndDuration(banners);
            Map<Long, ItemsApproveInfo> approveInfoMap =
                    getApproveInfoByProductIds(homePageResponse.getTopics(), userId);
            Optional.of(homePageResponse).map(
                    HomePageResponse::getTopics
            ).orElse(Collections.emptyList())
                    .forEach(
                            topic -> {
                                String title = topic.getTitle();
                                String messageByKey = crowdinHelper.getMessageByKey(title, baseHelper.getLanguage());
                                topic.setTitle(messageByKey);
                                Optional.of(topic).map(
                                        Topic::getItems
                                ).orElse(Collections.emptyList())
                                        .forEach(topicItem -> {
//                                            topicItem.setMappingStatus(ProductStatusMappingHelper.mappingStatus(topicItem.getType(), topicItem.getStartTime(), topicItem.getEndTime(), topicItem.getStatus()));
                                            ItemsApproveInfo info = approveInfoMap.get(topicItem.getProductId());
                                            if (info != null) {
                                                topicItem.setApprove(UserApproveInfo.builder()
                                                        .approve(info.isApprove())
                                                        .count(info.getCount())
                                                        .build());
                                            }
                                        });
//                                CollectionUtils.filter(topic.getItems(), item -> Objects.isNull(item.getMappingStatus()) || item.getMappingStatus() < 1);
                            }
                    );

            Optional.of(homePageResponse).map(
                    HomePageResponse::getBanners
            ).orElse(Collections.emptyList())
                    .forEach(
                            banner -> {
                                String title = banner.getTitle();
                                title = crowdinHelper.getMessageByKey(title, baseHelper.getLanguage());
                                banner.setTitle(title);
                                String subTitle = banner.getSubTitle();
                                subTitle = crowdinHelper.getMessageByKey(subTitle, baseHelper.getLanguage());
                                banner.setSubTitle(subTitle);
                            }
                    );
            HomePageMgsResponse response = HomePageResponseConvertor.convert(homePageResponse, AES_PASSWORD);
            return new CommonRet<>(response);
        } catch (Exception e) {
            log.error("load home page error:", e);
            return new CommonRet<>();
        }
    }



    @GetMapping("/public/nft/homepage/banners")
    public CommonRet<List<Banner>> homePageBanners(HttpServletRequest request) {
        try {
            String homePageResponseString = homePageResponseLocalCache.get(isGray(request));
            HomePageResponse homePageResponse = JsonUtils.parse(homePageResponseString, HomePageResponse.class);
            List<Banner> banners = homePageResponse.getBanners();
            refreshStatusAndDuration(banners);

            Optional.of(homePageResponse).map(
                    HomePageResponse::getBanners
            ).orElse(Collections.emptyList())
                    .forEach(
                            banner -> {
                                String title = banner.getTitle();
                                title = crowdinHelper.getMessageByKey(title, baseHelper.getLanguage());
                                banner.setTitle(title);
                                String subTitle = banner.getSubTitle();
                                subTitle = crowdinHelper.getMessageByKey(subTitle, baseHelper.getLanguage());
                                banner.setSubTitle(subTitle);
                            }
                    );
            return new CommonRet<>(banners);
        } catch (Exception e) {
            log.error("load home page error:", e);
            return new CommonRet<>();
        }
    }

    @GetMapping( "/friendly/nft/homepage/topics")
    public CommonRet<List<TopicMgs>> homePageTopics(HttpServletRequest request) {
        try {
            Long userId = baseHelper.getUserId();
            String homePageResponseString = homePageResponseLocalCache.get(isGray(request));
            HomePageResponse homePageResponse = JsonUtils.parse(homePageResponseString, HomePageResponse.class);
            Map<Long, ItemsApproveInfo> approveInfoMap =
                    getApproveInfoByProductIds(homePageResponse.getTopics(), userId);
            Optional.of(homePageResponse).map(
                    HomePageResponse::getTopics
            ).orElse(Collections.emptyList())
                    .forEach(
                            topic -> {
                                String title = topic.getTitle();
                                String messageByKey = crowdinHelper.getMessageByKey(title, baseHelper.getLanguage());
                                topic.setTitle(messageByKey);
                                Optional.of(topic).map(
                                        Topic::getItems
                                ).orElse(Collections.emptyList())
                                        .forEach(topicItem -> {
                                            //                                            topicItem.setMappingStatus(ProductStatusMappingHelper.mappingStatus(topicItem.getType(), topicItem.getStartTime(), topicItem.getEndTime(), topicItem.getStatus()));
                                            ItemsApproveInfo info = approveInfoMap.get(topicItem.getProductId());
                                            if (info != null) {
                                                topicItem.setApprove(UserApproveInfo.builder()
                                                        .approve(info.isApprove())
                                                        .count(info.getCount())
                                                        .build());
                                            }
                                        });
                            }
                    );
            HomePageMgsResponse response = HomePageResponseConvertor.convert(homePageResponse, AES_PASSWORD);
            return new CommonRet<>(response.getTopics());
        } catch (Exception e) {
            log.error("load home page error:", e);
            return new CommonRet<>();
        }
    }

    @GetMapping("/public/nft/home-collection")
    public CommonRet<List<HomeArtistVo>> collectionList(HttpServletRequest httpServletRequest) {

        HomeCollectionRequest request = new HomeCollectionRequest();
        request.setGray(isGray(httpServletRequest));
        APIResponse<List<HomeCollectionVo>> response = nftMarketCollectionApi
                .findCollectionListByCondition(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet(response.getData());
    }

    /**
     * <h2>主页分组 collection 信息</h2>
     * */
    @GetMapping("/friendly/nft/homepage-group-collection")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE, key = "'homeGroupCollection-'+ #groupType")
    public CommonRet<List<HomeCollectionDetailVo>> findHomepageGroupCollection(@RequestParam("groupType") String groupType, HttpServletRequest httpServletRequest) {

        HomepageGroupCollectionRequest request = new HomepageGroupCollectionRequest();
        request.setGray(isGray(httpServletRequest));
        request.setGroupType(groupType);
        APIResponse<List<HomeCollectionDetailVo>> response = nftMarketCollectionApi.findHomepageGroupCollection(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/public/nft/home-collection-detail")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE, key = "'collectionListDetail-'+#nftType+'-'+#pageSize")
    public CommonRet<List<HomeCollectionDetailVo>> collectionListDetail(@RequestParam("nftType") Integer nftType, @RequestParam(value = "pageSize", defaultValue = "16") Integer pageSize, HttpServletRequest httpServletRequest) {
        HomeCollectionRequest request = new HomeCollectionRequest();
        request.setGray(isGray(httpServletRequest));
        request.setNftType(nftType);
        Integer requestPageSize = (pageSize <= 0 || pageSize > 30) ? 16 : pageSize;
        request.setPageSize(requestPageSize);
        APIResponse<List<HomeCollectionDetailVo>> response = nftMarketCollectionApi
                .findCollectionListDetailByCondition(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet(response.getData());
    }

    @GetMapping({"/public/nft/home-artist","/friendly/nft/home-artist"})
    public CommonRet<CommonPageResponse<HomeArtistMgsVo>> homeArtist(HttpServletRequest httpServletRequest) {
        Long userId = baseHelper.getUserId();

        HomeArtistRequest request = new HomeArtistRequest();
        request.setGray(isGray(httpServletRequest));
        request.setPageSize(30);

        CommonPageResponse<HomeArtistMgsVo> result = homepageCacheProxy.homeArtist(request);
        if(CollectionUtils.isNotEmpty(result.getData())) {
            List<Long> creatorIds = result.getData().stream()
                    .map(HomeArtistMgsVo::getCreatorId)
                    .filter(uid -> !Objects.equals(userId, uid))
                    .collect(Collectors.toList());
            Map<Long, Boolean> followMap = userFollowHelper.queryFollow(creatorIds, userId);
            List<HomeArtistMgsVo> list = result.getData().stream().map(o -> {
                Integer followRelation = Switcher.<Integer>Case().when(Objects.equals(userId, o.getCreatorId())).then(2)
                        .when(followMap.getOrDefault(o.getCreatorId(), false)).then(1)
                        .end(0);
                HomeArtistMgsVo vo = CopyBeanUtils.fastCopy(o, HomeArtistMgsVo.class);
                vo.setFollowRelation(followRelation);
                vo.setCreatorId(null);
                return vo;
            }).collect(Collectors.toList());
            result = new CommonPageResponse<>(result.getPage(), result.getSize(), result.getTotal(), list);
        }
        return new CommonRet(result);
    }

    @SneakyThrows
    @GetMapping("/public/nft/homepage/announce")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public CommonRet<HomepageAnnounceResponse> homePageContent(Integer cursor) {
        APIResponse<HomepageAnnounceResponse> homepageAnnounceResponseAPIResponse =
                marketApi.homepageAnnounce(APIRequest.instance(HomepageAnnounceRequest.builder().cursor(cursor).build()));
        com.binance.nftcore.utils.lambda.check.BaseHelper.checkResponse(homepageAnnounceResponseAPIResponse);
        return new CommonRet<>(homepageAnnounceResponseAPIResponse.getData());
    }

    @PostMapping("/public/nft/collection/floor-price")
    public CommonRet<Map<Long, FloorPriceDetailVo>> getCollectionFloorPriceByCollectionIds(@RequestBody List<CollectionFloorPriceRequest> request) {
        APIResponse<Map<Long, FloorPriceDetailVo>> response = nftMarketCollectionApi.getCollectionFloorPriceByCollectionIds(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet(response.getData());
    }

    private void refreshStatusAndDuration(List<Banner> banners) {
        if (CollectionUtils.isNotEmpty(banners)) {
            banners.stream().filter(
                    banner -> StringUtils.isNotBlank(banner.getType())
            ).forEach(banner -> {
                banner.setDuration(ProductTimeHelper.getDuration(banner.getStartTime(), banner.getEndTime()).toString());
                banner.setMappingStatus(ProductStatusMappingHelper.mappingStatus(
                        banner.getType(), banner.getStartTime(), banner.getEndTime(), banner.getStatus()));
            });
        }
    }

    private Map<Long, ItemsApproveInfo> getApproveInfoByProductIds(List<Topic> topics, Long userId) {

        List<Long> productIds = new ArrayList<>();
        for (Topic topic : topics) {
            List<TopicItem> items = topic.getItems();
            productIds.addAll(Optional.ofNullable(items)
                    .map(item -> item.stream().map(x -> x.getProductId()).distinct().collect(Collectors.toList()))
                    .orElse(Collections.emptyList()));
        }
        List<ItemsApproveInfo> infoList = approveHelper.queryApproveInfo(productIds, userId);
        if (CollectionUtils.isEmpty(infoList)) {
            return Collections.emptyMap();
        }
        return infoList.stream().collect(
                Collectors.toMap(ItemsApproveInfo::getProductId, Function.identity(), (v1, v2) -> v1));
    }

    @GetMapping("/public/nft/marketplace")
    public CommonRet<List<Banner>> marketplaceBanner(HttpServletRequest request) {
        try {
            List<Banner> banners = marketplaceBannerLocalCache.get(isGray(request));
            if (CollectionUtils.isNotEmpty(banners)) {
                // 计算时间最新 更好 不需要做 深copy区分
                banners.forEach(banner -> banner.setType(BannerType.REGULAR.name()));
                refreshStatusAndDuration(banners);
            }
            return new CommonRet<>(banners);
        } catch (Exception e) {
            log.error("load marketplace error:", e);
            return new CommonRet<>();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            init();
        } catch (Exception e) {
            log.error("HomePageController init error:", e);
        }
    }


    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);

    }
}
