package com.binance.mgs.nft.common.cache;

import com.binance.master.utils.DateUtils;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.RandomUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheConfiguration {
    private final MgsNftProperties mgsNftProperties;
    private ScheduledThreadPoolExecutor executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
    @Value("${redisson.nodes}")
    private String nodes;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useClusterServers().addNodeAddress(nodes);
        config.setCodec(StringCodec.INSTANCE);
        return Redisson.create(config);
    }

    @PostConstruct
    public void init() {
        loadLocalcacheConfig();
        initHotDataConfig();
        monitorApolloChanges();
    }

    private void initHotDataConfig() {
        if (MapUtils.isEmpty(mgsNftProperties.getCacheConfigMap())) {
            return;
        }
        if(CollectionUtils.isNotEmpty(executorService.getQueue()) ||executorService.getActiveCount() > 0) {
            log.warn("initHotDataConfig reset executorService");
            executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            executorService.shutdown();
            executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        }

        mgsNftProperties.getCacheConfigMap().entrySet().stream().forEach(e -> {
            MgsNftProperties.LocalCacheConfig config = e.getValue();
            executorService.scheduleWithFixedDelay(() -> {
                long start = DateUtils.getNewUTCTimeMillis();
                CacheUtils.cacheTopRequest(redissonClient(), e.getKey(),
                        config.getMaxSize(), config.getMinQps());
                log.warn("initHotDataConfig schedule {} cost {}", e.getKey(), DateUtils.getNewUTCTimeMillis() - start);
            }, RandomUtils.nextInt(1000, 3000), config.getInterval(), TimeUnit.MILLISECONDS);
            log.warn("initHotDataConfig set schedule {} {}", e.getKey(), config);
        });
    }

    private void loadLocalcacheConfig() {
        if (MapUtils.isEmpty(mgsNftProperties.getCacheConfigMap())) {
            return;
        }

        mgsNftProperties.getCacheConfigMap().entrySet().stream().forEach(e -> {
            CacheUtils.register(e.getKey(), e.getValue());
            log.warn("loadLocalcacheConfig {} ok", e.getKey());
        });
    }

    private void monitorApolloChanges() {
        ConfigService.getAppConfig().addChangeListener(changeEvent -> {
            log.warn("Apollo was changed for namespace {}", changeEvent.getNamespace());
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                if (change.getPropertyName().contains("cache.config")) {
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(3000L);
                        } catch (InterruptedException e) {
                        }

                        loadLocalcacheConfig();

                        initHotDataConfig();

                        log.warn("localcaches are reload because of {} change", key);
                    });
                }
            }
        });
    }
}
