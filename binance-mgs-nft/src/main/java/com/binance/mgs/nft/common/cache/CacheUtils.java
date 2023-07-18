package com.binance.mgs.nft.common.cache;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.common.constant.MgsNftConstants;
import com.binance.mgs.nft.common.helper.RedisHelper;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.platform.monitor.Monitors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.util.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CacheUtils {
    //scene time
    private static final String REQ_COUNT_WINDOW_KEY = "mgs:nft:req:count:%s:%s";

    private static Logger log = LoggerFactory.getLogger(CacheUtils.class);
    private static Map<String, Cache> CACHE_MAP = new ConcurrentHashMap<>();
    private static ScheduledThreadPoolExecutor executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    private static volatile Map<String, AtomicInteger> valueMap = new ConcurrentHashMap<>();
    private static final AtomicLong scheduleTime = new AtomicLong();
    private static final Map<String, MgsNftProperties.LocalCacheConfig> cacheConfigMap = new ConcurrentHashMap<>();

    static synchronized void register(String scene, MgsNftProperties.LocalCacheConfig cacheConfig) {
        cacheConfigMap.put(scene, cacheConfig);
        Cache preCache = CACHE_MAP.get(scene);
        final Cache cache = Caffeine.newBuilder()
                .initialCapacity(cacheConfig.getMinSize())
                .maximumSize(cacheConfig.getMaxSize())
                .expireAfterWrite(cacheConfig.getExpire(), TimeUnit.SECONDS)
                .build();
        if(preCache != null) {
            cache.putAll(preCache.asMap());
        }
        CACHE_MAP.put(scene, cache);
    }

    public static <V> void putAll(String scene, Map<String, String> map) {
        Cache cache = CACHE_MAP.get(scene);
        if(cache == null) return;
        map.entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
                .forEach(e -> cache.put(e.getKey().substring(e.getKey().lastIndexOf(":")+1), e.getValue()));
    }

    private static <K,V> V get(String scene, K key) {
        Cache<K,V> cache = CACHE_MAP.get(scene);
        if(cache == null) return null;
        return cache.getIfPresent(key);
    }

    private static void del(String scene, String key) {
        Cache<String,String> cache = CACHE_MAP.get(scene);
        if(cache == null) return;
        cache.invalidate(key);
    }

    public static String generateKey(Object params) {
        String json = JsonUtils.toJsonNotNullKey(params);
        return hash(json, 31) + "_" + hash(json, 131);
    }

    private static long hash(String str, long seed) {
        long hash = 0;
        for(int i=0;i<str.length();i++) {
            hash = hash * seed + str.charAt(i);
        }

        return (hash & 0x7fffffff);
    }


    private static void recordRequest(RedissonClient redissonClient, CacheSeneEnum scene, String key) {
        String newKey = scene.name() + "-" + key;
        valueMap.putIfAbsent(newKey, new AtomicInteger(0));
        valueMap.get(newKey).incrementAndGet();
        Long timeMs = DateUtils.getNewUTCTimeMillis();
        Long timeS = timeMs/1000L;
        Long prevTime = scheduleTime.getAndSet(timeS);
        if(!Objects.equals(timeS, prevTime)) {
            Long nextMs = (timeS + 1) * 1000L;
            executorService.schedule(() -> {
                long ts = DateUtils.getNewUTCTimeMillis() / 1000;
                long ls = ts % 10;
                Long win10 = ts - ls;
                Long win5 = (ts - ls + 5) < ts ? (ts - ls + 5) : (ts - ls - 5);
                Map<String, List<Object>> reqMap = new HashMap<>();

                Map<String, AtomicInteger> localMap = valueMap;
                Map<String, AtomicInteger> newValueMap = new ConcurrentHashMap<>();
                valueMap = newValueMap;
                localMap.entrySet().stream().forEach(e -> {
                    int count = e.getValue().getAndUpdate(v -> 0);
                    if(count == 0) return;
                    String k = e.getKey();
                    int index = k.lastIndexOf("-");
                    String sc = k.substring(0, index);
                    String paramKey = k.substring(index + 1);

                    String win10Key = String.format(REQ_COUNT_WINDOW_KEY, sc, win10);
                    List<Object> list10 = reqMap.getOrDefault(win10Key, Lists.newArrayList());
                    list10.add(paramKey);
                    list10.add(count);
                    reqMap.put(win10Key, list10);

                    String win5Key = String.format(REQ_COUNT_WINDOW_KEY, sc, win5);
                    List<Object> list5 = reqMap.getOrDefault(win5Key, Lists.newArrayList());
                    list5.add(paramKey);
                    list5.add(count);
                    reqMap.put(win5Key, list5);
                });

                reqMap.entrySet().stream().forEach(e -> {
                    ListUtils.partition(e.getValue(), 50).forEach(l -> {
                        RedisHelper.recordApiCount(e.getKey(), l.toArray());
                        log.info("recordRequest {} {}", e.getKey(), l.size());
                    });
                });
            }, nextMs - timeMs, TimeUnit.MILLISECONDS);
        }
    }

//    private static void doRecordRequest(RedissonClient redissonClient, String scene, String key, int count) {
//        long ts = DateUtils.getNewUTCTimeMillis() / 1000;
//        long ls = ts % 10;
//        Long win10 = ts - ls;
//        Long win5 = (ts - ls + 5) < ts ? (ts - ls + 5) : (ts - ls - 5);
//        RScoredSortedSet rs10 = redissonClient.getScoredSortedSet(String.format(REQ_COUNT_WINDOW_KEY, scene, win10));
//        RScoredSortedSet rs5 = redissonClient.getScoredSortedSet(String.format(REQ_COUNT_WINDOW_KEY, scene, win5));
//        boolean exist10 = rs10.isExists();
//        boolean exist5 = rs5.isExists();
//        rs10.addScoreAsync(key, count);
//        rs5.addScoreAsync(key, count);
//        if(!exist10) rs10.expireAsync(20L, TimeUnit.SECONDS);
//        if(!exist5) rs5.expireAsync(20L, TimeUnit.SECONDS);
//        log.info("recordRequest {} {} count {}", win10, win5, count);
//    }



    public static void cacheTopRequest(RedissonClient redissonClient, String scene, int limit, int min) {
        long ts = DateUtils.getNewUTCTimeMillis() / 1000;
        long ls = ts % 10;
        // 最近5-10s热点数据
        Long win10 = ls < 5 ? ts - ls - 5 : ts - ls;
        //最近10-15s热点数据
//        Long win10 = ls < 5 ? ts - ls - 10 : ts - ls - 5;
        RScoredSortedSet<String> rs10 = redissonClient.<String>getScoredSortedSet(String.format(REQ_COUNT_WINDOW_KEY, scene, win10));

        int total = 0;
        AtomicReference<Double> max = new AtomicReference<>(Double.MAX_VALUE);
        while (total < limit) {
            Collection<ScoredEntry<String>> list = rs10.entryRangeReversed(min, true, max.get(), false, 0, 1000);
            if(CollectionUtils.isEmpty(list)) break;
            List<String> keyList = list.stream().map(e -> {
                max.set(e.getScore());
                return e.getValue();
            }).collect(Collectors.toList());

            ListUtils.partition(keyList, 100).stream().forEach(l -> {
                List<String> keys = l.stream().map(k -> String.format(MgsNftConstants.NFT_REMOTE_CACHE_KEY, scene, k)).collect(Collectors.toList());
                Map<String, String> map = redissonClient.<String, String>getBuckets().get(keys.toArray(new String[keys.size()]));
                if(Objects.nonNull(map) && !map.isEmpty()) {
                    putAll(scene, map);
                }
            });

            total += list.size();
            if(list.size() < 1000) break;
        }

        if(rs10.remainTimeToLive() == -1L) {
            rs10.expire(20L, TimeUnit.SECONDS);
        }
        log.info("topRequest {} {} {}", win10, scene, total);
    }

    public static <T, R> R getData(RedissonClient redissonClient, T request, CacheSeneEnum scene,
                                   boolean cache, TypeReference<R> typeReference, Function<T, R> function) {
        String key = CacheUtils.generateKey(request);
        return getData(redissonClient, request, scene, cache, typeReference, function, key);
    }

    public static <T,R> R getData(RedissonClient redissonClient, T request, CacheSeneEnum scene,
                                  boolean cache, TypeReference<R> typeReference, Function<T, R> function, String key) {
        if(!cache) {
            Monitors.count("cacheApi", "scene", scene.name(), "type", "skip");
            log.info("cacheApi {} {} {} {}", "scene", scene.name(), "type", "skip");
            return function.apply(request);
        }

        // record times
        recordRequest(redissonClient, scene, key);


        String cacheData = CacheUtils.<String, String>get(scene.name(), key);
        if(Objects.nonNull(cacheData)) {
            Monitors.count("cacheApi", "scene", scene.name(), "type", "local");
            log.info("cacheApi {} {} {} {}", "scene", scene.name(), "type", "local");
            return JsonUtils.parse(cacheData, typeReference);
        }


        RBucket<String> rbucket = redissonClient.getBucket(String.format(MgsNftConstants.NFT_REMOTE_CACHE_KEY, scene.name(), key));
        R result = getByRedis(rbucket, scene, typeReference);
        if(Objects.nonNull(result)) return result;


        RLock rLock = redissonClient.getFairLock(String.format(MgsNftConstants.NFT_BREAKDOWN_LOCK_KEY, key));
        boolean locked = false;
        try {
            locked = rLock.tryLock(1200L, 3000L, TimeUnit.MILLISECONDS);
            if(locked) {
                result = getByRedis(rbucket, scene, typeReference);
                if(Objects.nonNull(result)) return result;

                return breakdown(function, request, scene, rbucket);
            } else {
                return getByRedis(rbucket, scene, typeReference);
            }
        } catch (BusinessException e) {
            log.warn("getData breakdown error", e);
            throw e;
        } catch (InterruptedException e) {
            log.warn("getData breakdown error", e);
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        } catch (Exception e) {
            log.warn("getData breakdown error", e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        } finally {
            if(locked) {
                rLock.unlockAsync();
            }
        }
    }


    private static <R> R getByRedis(RBucket<String> rbucket, CacheSeneEnum scene, TypeReference<R> typeReference) {
        if(Objects.isNull(rbucket)){
            return null;
        }
        String data = rbucket.get();
        if(Objects.nonNull(data)) {
            Monitors.count("cacheApi", "scene", scene.name(), "type", "redis");
            log.info("cacheApi {} {} {} {}", "scene", scene.name(), "type", "redis");
            return JsonUtils.parse(data, typeReference);
        }
        return null;
    }

    private static <T,R> R breakdown(Function<T, R> function, T request, CacheSeneEnum scene, RBucket<String> rbucket) {
        R result = function.apply(request);
        // todo
        log.debug("breakdown result,{}",JsonUtils.toJsonNotNullKey(result));

        Monitors.count("cacheApi", "scene", scene.name(), "type", "breakdown");
        log.info("cacheApi {} {} {} {}", "scene", scene.name(), "type", "breakdown");
        MgsNftProperties.LocalCacheConfig cacheConfig = cacheConfigMap.get(scene.name());
        Long expire = Optional.ofNullable(cacheConfig).map(MgsNftProperties.LocalCacheConfig::getRedisExpire).orElse(15L);
        if(Objects.nonNull(result)) {
            rbucket.setAsync(JsonUtils.toJsonNotNullKey(result), expire, TimeUnit.SECONDS);
        } else {
            rbucket.setAsync(JsonUtils.toJsonNotNullKey(Collections.EMPTY_MAP), 5, TimeUnit.SECONDS);
        }
        return result;
    }


    public static <T> void invalidCacheData(T req, CacheSeneEnum seneEnum, RedissonClient redissonClient) {
        String key = CacheUtils.generateKey(req);
        String cacheKey = String.format(MgsNftConstants.NFT_REMOTE_CACHE_KEY, seneEnum.name(), key);
        redissonClient.getBucket(cacheKey).deleteAsync();
        log.info("edit invalidCacheData {}", cacheKey);
        del(seneEnum.name(), key);
    }
}
