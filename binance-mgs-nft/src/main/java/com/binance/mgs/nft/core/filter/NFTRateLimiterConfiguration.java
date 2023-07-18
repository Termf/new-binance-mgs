package com.binance.mgs.nft.core.filter;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.binance.master.utils.DateUtils;
import com.binance.master.web.handlers.MessageHelper;
import com.binance.mgs.nft.common.helper.RedisHelper;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.RandomUtils;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NFTRateLimiterConfiguration {
    private ScheduledThreadPoolExecutor executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private static final String REQ_BURUSH_WINDOW_KEY = "nft:brush:%s:%s";

    private final RedisHelper redisHelper;
    private final RedissonClient redissonClient;
    private final MgsNftProperties mgsNftProperties;
    private final MessageHelper messageHelper;
    private final BaseHelper baseHelper;
    @Bean
    public FilterRegistrationBean filterLimiterRegistrationBean() {
        WebRequestLimiterFilter webRequestLimiterFilter = new WebRequestLimiterFilter(redisHelper, mgsNftProperties, messageHelper, baseHelper);
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(webRequestLimiterFilter);
        filterRegistrationBean.addUrlPatterns("/v1/*");
        filterRegistrationBean.setName("webRequestLimiterFilter");
        filterRegistrationBean.setOrder(Integer.MAX_VALUE);
        return filterRegistrationBean;
    }


    @PostConstruct
    public void init() {
        loadRateLimiterRules();
        initLoadBrushApiData();
        monitorApolloChanges();
    }

    private void loadRateLimiterRules() {
        if (MapUtils.isEmpty(mgsNftProperties.getRateLimiterMap())) {
            return;
        }

        List<ParamFlowRule> rules = mgsNftProperties.getRateLimiterMap().entrySet().stream().map(e -> {
            String resource = e.getKey();
            MgsNftProperties.ParamFlowRuleConfig config = e.getValue();

            ParamFlowRule rule = new ParamFlowRule();
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            rule.setCount(config.getLimit());
            rule.setResource(resource);
            rule.setParamIdx(0);
            rule.setDurationInSec(1);

            List<ParamFlowItem> items = SetUtils.emptyIfNull(config.getExcludes()).stream()
                    .map(r -> {
                        ParamFlowItem item = new ParamFlowItem();
                        item.setClassType(String.class.getTypeName());
                        item.setCount(config.getExcludeLimit());
                        item.setObject(r);
                        return item;
                    }).collect(Collectors.toList());
            rule.setParamFlowItemList(items);

            return rule;
        }).collect(Collectors.toList());

        ParamFlowRuleManager.loadRules(rules);
    }

    private void monitorApolloChanges() {
        ConfigService.getAppConfig().addChangeListener(changeEvent -> {
            log.warn("Apollo was changed for namespace {}", changeEvent.getNamespace());
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                if (change.getPropertyName().contains("ratelimiter")) {
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(3000L);
                        } catch (InterruptedException e) {
                        }
                        loadRateLimiterRules();
                        log.info("folw rules are reload because of {} change", key);
                    });
                } else if (change.getPropertyName().contains("brush")) {
                    initLoadBrushApiData();
                }
            }
        });
    }




    private void initLoadBrushApiData() {
        if(CollectionUtils.isNotEmpty(executorService.getQueue()) ||executorService.getActiveCount() > 0) {
            log.warn("initLoadBrushApiData reset executorService");
            executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            executorService.shutdown();
            executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        }
        if (MapUtils.isEmpty(mgsNftProperties.getBrushApiConfig())) {
            return;
        }

        mgsNftProperties.getBrushApiConfig().entrySet().stream().forEach(e -> {
            MgsNftProperties.BrushParamConfig config = e.getValue();
            executorService.scheduleWithFixedDelay(() -> {
                long start = DateUtils.getNewUTCTimeMillis();
                IntStream.range(0,2).forEach(i -> {
                    int w = mgsNftProperties.getBrushWindows().get(i);
                    Long curWin = DateUtils.getNewUTCTimeMillis() / 1000 / 60 / w;
                    String winKey = String.format(REQ_BURUSH_WINDOW_KEY, curWin - 1, e.getKey());
                    RScoredSortedSet rs = redissonClient.getScoredSortedSet(winKey);
                    int min = config.getThresholds()[i * 2];
                    Collection<ScoredEntry<String>> collection = rs.entryRangeReversed(min, true, 1000_000.0, false, 0, config.getSizes()[i]);
                    if(rs.remainTimeToLive() == -1L) {
                        rs.expireAsync(w, TimeUnit.MINUTES);
                    }
                    if(CollectionUtils.isEmpty(collection)) return;
                    Map<String, Integer> dataMap = collection.stream()
                            .collect(Collectors.toMap(entry -> e.getKey() + "-" + entry.getValue(),
                                    entry -> entry.getScore().intValue()));
                    Cache cache = i == 0 ? WebRequestLimiterFilter.bcache : WebRequestLimiterFilter.scache;
                    cache.putAll(dataMap);

                    Double m = collection.stream().limit(1).findAny().map(ScoredEntry::getScore).get();
                    Double n = collection.stream().skip(collection.size()-1).limit(1).findAny().map(ScoredEntry::getScore).get();
                    log.info("initLoadBrushApiData win {} uri {} data {} {}", w, e.getKey(), m, n);
                });
                log.warn("initLoadBrushApiData schedule {} cost {}", e.getKey(), DateUtils.getNewUTCTimeMillis() - start);
            }, RandomUtils.nextInt(1000, 3000), 1000 * 60, TimeUnit.MILLISECONDS);
            log.warn("initLoadBrushApiData set schedule {} {}", e.getKey(), config);
        });
    }
}
