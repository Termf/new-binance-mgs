package com.binance.mgs.nft.core.config;

import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Getter
@Configuration
public class MgsNftProperties {

    @ApolloJsonValue("${mgs.nft.ratelimiter:}")
    private Map<String, ParamFlowRuleConfig> rateLimiterMap = new HashMap<>();
    @ApolloJsonValue("${mgs.nft.distribute.whitelist:[]}")
    private Set<Long> distributeWhitelist = new HashSet<>();

    @ApolloJsonValue("${mgs.nft.cache.config:{}}")
    private Map<String, LocalCacheConfig> cacheConfigMap;
    @ApolloJsonValue("${mgs.nft.api.request.log:[]}")
    private List<String> reqLogList;
    @ApolloJsonValue("${mgs.nft.common.config:{}}")
    private Map<String, Object> commonConfig;
    @ApolloJsonValue("${mgs.nft.common.config.whitelist:[]}")
    private Set<Long> commonWhitelist;

    // api防刷接口
    // api防刷时间窗口
    @ApolloJsonValue("${mgs.nft.api.request.brush.windows:[60,5]}")
    private List<Integer> brushWindows;
    // api防刷时间窗口
    @ApolloJsonValue("${mgs.nft.api.request.brush.config:{}}")
    private Map<String, BrushParamConfig> brushApiConfig;
    @ApolloJsonValue("${mgs.nft.attr.collection.blacklist:[]}")
    private List<Long> collectionBlackList;

    @ApolloJsonValue("${mgs.nft.suggestion.search.whitelist:[]}")
    private List<Long> searchWhitelist;

    @Value("${mgs.nft.suggestion.search.ab.percent:0}")
    private Integer searchAbPercent;
    @ApolloJsonValue("${mgs.nft.profile.white.list:[]}")
    private List<Long> profileWhitelist;
    @ApolloJsonValue("${mgs.nft.profile.white.segment:10}")
    private Integer profileWhiteSegment;

    @ApolloJsonValue("${mgs.nft.asset.list.v2.whitelist:[]}")
    private List<Long> assetListV2Whitelist;
    @Value("${mgs.nft.asset.list.v2.ab.percent:0}")
    private Integer assetListV2AbPercent;
    @ApolloJsonValue("${mgs.nft.activity.staking.collection-mapping:{\"688009096191889409\":685810189391933440}}")
    private Map<Long, Long> stakingCollectionMapping;
    @ApolloJsonValue("${nft.dexasset.expose.collection.whitelist:[]}")
    private List<Long> exposeDexAssetCollections;

    @Value("${mgs.nft.ranking.v2.ab.percent:0}")
    private Integer rankingV2AbPercent;

    @Data
    public static class ParamFlowRuleConfig {
        private Integer limit;
        private Integer excludeLimit;
        private Set<String> excludes = new HashSet<>();
        private Set<String> whitelist;
    }

    @Data
    public static class BrushParamConfig {
        //big-lv1,big-lv2,s-lv1,s-lv2
        private int[] thresholds = new int[]{-1,-1,-1,-1};
        private int[] sizes = new int[]{100,50};
        //lv1limit, lv2limit
        private int[] limits = new int[]{10,5};
    }

    @Data
    public static class LocalCacheConfig {
        private Integer minSize = 100;
        private Integer maxSize = 10000;
        //hotdata localcache time
        private Integer expire = 5;
        //hot sync 5-10s
        private Integer minQps = 10;
        //间隔时间 ms
        private Integer interval = 3000;

        private Long redisExpire = 15L;
    }
}
