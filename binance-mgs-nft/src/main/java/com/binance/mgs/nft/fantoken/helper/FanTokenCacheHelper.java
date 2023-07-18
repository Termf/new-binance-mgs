package com.binance.mgs.nft.fantoken.helper;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.nft.fantoken.constant.CallerTypeEnum;
import com.binance.nft.fantoken.ifae.*;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.QueryBannerByPageRequest;
import com.binance.nft.fantoken.request.QueryMainPageConfigRequest;
import com.binance.nft.fantoken.request.QueryNftMarketRequest;
import com.binance.nft.fantoken.request.QueryTeamByPageRequest;
import com.binance.nft.fantoken.request.QueryTeamInfoRequest;
import com.binance.nft.fantoken.request.QueryVoteByPageRequest;
import com.binance.nft.fantoken.request.QueryVoteInfoRequest;
import com.binance.nft.fantoken.request.category.CategoryDisplayRequest;
import com.binance.nft.fantoken.request.nftstaking.NftStakingDisplayRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.QueryNftMarketResponse;
import com.binance.nft.fantoken.response.nftstaking.NftStakingDisplayResponse;
import com.binance.nft.fantoken.response.QuickActionResponse;
import com.binance.nft.fantoken.vo.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <h1>Fan Token 相关的 JVM Cache</h1>
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class FanTokenCacheHelper implements ApplicationRunner {

    private final BaseHelper baseHelper;

    private final IFanTokenBannerManageAPI fanTokenBannerManageAPI;
    private final IFanTokenTeamManageAPI fanTokenTeamManageAPI;
    private final IFanTokenVoteManageAPI fanTokenVoteManageAPI;
    private final IFanTokenVoteUpdateManageApi voteUpdateManageApi;
    private final IFanTokenMainPageConfigManageAPI fanTokenMainPageConfigManageAPI;
    private final IFanTokenCategoryManagerApi categoryManagerApi;
    private final IFanTokenNftStakingManageApi nftStakingManageApi;

    private final FanTokenAsyncHelper fanTokenAsyncHelper;

    /**
     * <h2>all banner cache</h2>
     * */
    private final LoadingCache<Integer, CommonPageResponse<BannerVO>> fanTokenBannerLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(250)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Integer, CommonPageResponse<BannerVO>>() {
                        @Override
                        public CommonPageResponse<BannerVO> load(@NotNull Integer key) throws Exception {
                            // 请求对象中 rank 是 null, 需要在外部调用的地方自行排序
                            CommonPageRequest<QueryBannerByPageRequest> request =
                                    CommonPageRequest.<QueryBannerByPageRequest>builder()
                                            .page(1).size(250).params(new QueryBannerByPageRequest()).build();
                            APIResponse<CommonPageResponse<BannerVO>> response =
                                    fanTokenBannerManageAPI.queryBannerByPage(APIRequest.instance(request));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>main page config cache</h2>
     */
    private final LoadingCache<Integer, MainPageConfigVO> fanTokenMainPageConfigLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Integer, MainPageConfigVO>() {
                        @Override
                        public MainPageConfigVO load(@NotNull Integer key) throws Exception {

                            APIResponse<MainPageConfigVO> response = fanTokenMainPageConfigManageAPI.queryMainPageConfig();
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>all team cache</h2>
     * */
    private final LoadingCache<Integer, CommonPageResponse<TeamVO>> fanTokenTeamLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(250)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Integer, CommonPageResponse<TeamVO>>() {
                        @Override
                        public CommonPageResponse<TeamVO> load(@NotNull Integer key) throws Exception {
                            // 请求对象中 rank 是 null, 需要在外部调用的地方自行排序
                            CommonPageRequest<QueryTeamByPageRequest> request =
                                    CommonPageRequest.<QueryTeamByPageRequest>builder()
                                            .page(1).size(250).params(new QueryTeamByPageRequest()).build();
                            request.getParams().setCallerType(CallerTypeEnum.CUSTOM.getCallerType());

                            APIResponse<CommonPageResponse<TeamVO>> response =
                                    fanTokenTeamManageAPI.queryTeamByPage(APIRequest.instance(request));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>all vote cache</h2>
     * */
    private final LoadingCache<Integer, CommonPageResponse<VoteVO>> fanTokenVoteLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Integer, CommonPageResponse<VoteVO>>() {
                        @Override
                        public CommonPageResponse<VoteVO> load(@NotNull Integer key) throws Exception {
                            // 请求对象中 rank 是 null, 需要在外部调用的地方自行排序
                            CommonPageRequest<QueryVoteByPageRequest> request =
                                    CommonPageRequest.<QueryVoteByPageRequest>builder()
                                            .page(1).size(500).params(new QueryVoteByPageRequest()).build();
                            request.getParams().setCallerType(CallerTypeEnum.CUSTOM.getCallerType());

                            APIResponse<CommonPageResponse<VoteVO>> response =
                                    fanTokenVoteManageAPI.queryVoteByPage(APIRequest.instance(request));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>team info cache</h2>
     * */
    private final LoadingCache<String, TeamVO> fanTokenTeamInfoLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<String, TeamVO>() {
                        @Override
                        public TeamVO load(@NotNull String symbol) throws Exception {
                            APIResponse<TeamVO> response =
                                    fanTokenTeamManageAPI.queryTeamInfo(
                                            APIRequest.instance(QueryTeamInfoRequest.builder()
                                                    .symbol(symbol)
                                                    .callerType(CallerTypeEnum.CUSTOM.getCallerType())
                                                    .build())
                                    );
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>vote info cache</h2>
     * */
    private final LoadingCache<String, VoteVO> fanTokenVoteInfoLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<String, VoteVO>() {
                        @Override
                        public VoteVO load(@NotNull String voteId) throws Exception {
                            APIResponse<VoteVO> response = fanTokenVoteManageAPI.
                                    queryVoteInfo(APIRequest.instance(QueryVoteInfoRequest.builder().voteId(voteId).
                                            callerType(CallerTypeEnum.CUSTOM.getCallerType()).build()));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>vote_update info cache</h2>
     * */
    private final LoadingCache<String, List<VoteUpdateInfoVO>> fanTokenVoteUpdateInfoLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<String, List<VoteUpdateInfoVO>>() {
                        @Override
                        public List<VoteUpdateInfoVO> load(@NotNull String voteId) throws Exception {

                            APIResponse<List<VoteUpdateInfoVO>> response =
                                    voteUpdateManageApi.queryUpdateEventByVoteId(APIRequest.instance(voteId));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>Category Display cache</h2>
     * 以 teamId 为缓存的 key 获取当前 team 配置的盲盒信息
     * */
    private final LoadingCache<String, List<CategoryDisplayVO>> fanTokenCategoryDisplayLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<String, List<CategoryDisplayVO>>() {
                        @Override
                        public List<CategoryDisplayVO> load(@NotNull String teamId) throws Exception {

                            CategoryDisplayRequest request = CategoryDisplayRequest.builder().teamId(teamId).build();
                            APIResponse<List<CategoryDisplayVO>> response =
                                    categoryManagerApi.displayCategoryInfoByTeamId(APIRequest.instance(request));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>NFT 盲盒的售卖信息</h2>
     * */
    private final LoadingCache<QueryNftMarketRequest, QueryNftMarketResponse> fanTokenNftMarketLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<QueryNftMarketRequest, QueryNftMarketResponse>() {
                        @Override
                        public QueryNftMarketResponse load(@NotNull QueryNftMarketRequest request) throws Exception {
                            log.info("query nft market in cache helper, request param is : [{}]",
                                    JSON.toJSONString(request));
                            return fanTokenAsyncHelper.queryNftMarket(request);
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>NFT Staking Display</h2>
     * */
    private final LoadingCache<NftStakingDisplayRequest, NftStakingDisplayResponse> fanTokenNftStakingDisplayLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(20)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<NftStakingDisplayRequest, NftStakingDisplayResponse>() {
                        @Override
                        public NftStakingDisplayResponse load(@NotNull NftStakingDisplayRequest request) throws Exception {
                            APIResponse<NftStakingDisplayResponse> response =
                                    nftStakingManageApi.stakingDisplay(APIRequest.instance(request));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    /**
     * <h2>Quick Actions</h2>
     * */
    private final LoadingCache<Integer, List<QuickActionResponse>> fanTokenQuickActionLoadingCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Integer, List<QuickActionResponse>>() {
                        @Override
                        public List<QuickActionResponse> load(@NotNull Integer key) throws Exception {
                            QueryMainPageConfigRequest request = QueryMainPageConfigRequest.builder().isGrey(false).build();
                            APIResponse<List<QuickActionResponse>> response =
                                    fanTokenMainPageConfigManageAPI.queryQuickActionByMainPageConfig(APIRequest.instance(request));
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    public TeamVO queryTeamInfo(String symbol) throws ExecutionException {
        return fanTokenTeamInfoLoadingCache.get(symbol);
    }

    public List<VoteUpdateInfoVO> queryUpdateEventByVoteId(String voteId) throws ExecutionException {
        return fanTokenVoteUpdateInfoLoadingCache.get(voteId);
    }

    public VoteVO queryVoteInfo(String voteId) throws ExecutionException {
        return fanTokenVoteInfoLoadingCache.get(voteId);
    }

    public CommonPageResponse<BannerVO> queryBanner() throws ExecutionException {
        return fanTokenBannerLoadingCache.get(0);
    }

    public CommonPageResponse<TeamVO> queryTeam() throws ExecutionException {
        return fanTokenTeamLoadingCache.get(0);
    }

    public CommonPageResponse<VoteVO> queryVote() throws ExecutionException {
        return fanTokenVoteLoadingCache.get(0);
    }

    public MainPageConfigVO queryMainPageConfig() throws ExecutionException {
        return fanTokenMainPageConfigLoadingCache.get(0);
    }

    public List<CategoryDisplayVO> queryTeamCategoryInfo(String teamId) throws ExecutionException {
        return fanTokenCategoryDisplayLoadingCache.get(teamId);
    }

    public List<QuickActionResponse> queryQuickAction() throws ExecutionException {
        return fanTokenQuickActionLoadingCache.get(0);
    }

    /**
     * <h2>主动清除 CategoryDisplay 缓存</h2>
     * */
    public void invalidateCategoryDisplayCache(String teamId) {

        try {
            fanTokenCategoryDisplayLoadingCache.invalidate(teamId);
            log.info("exist empty data, invalidate category display loading cache: [{}]", teamId);
        } catch (Exception ex) {
            log.warn("invalidate CategoryDisplay cache has some error: [{}]", ex.getMessage(), ex);
        }
    }

    public QueryNftMarketResponse queryNftMarketLoadingCache(QueryNftMarketRequest request) throws ExecutionException {
        return fanTokenNftMarketLoadingCache.get(request);
    }

    public NftStakingDisplayResponse queryNftStakingDisplay(NftStakingDisplayRequest request) throws ExecutionException {
        return fanTokenNftStakingDisplayLoadingCache.get(request);
    }

    /**
     * <h2>初始化 cache</h2>
     * */
    public void init() throws ExecutionException {
        fanTokenBannerLoadingCache.get(0);
        fanTokenTeamLoadingCache.get(0);
        fanTokenVoteLoadingCache.get(0);
        fanTokenMainPageConfigLoadingCache.get(0);
        fanTokenQuickActionLoadingCache.get(0);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            init();
        } catch (Exception e) {
            log.error("FanTokenCacheHelper init error:",e);
        }
    }
}
