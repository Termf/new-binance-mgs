package com.binance.mgs.nft.common.helper;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.binance.nft.tradeservice.constant.Constants;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisHelper implements InitializingBean {
    private static String brushHashScript;
    private static String brushHashSha1;
    private static RScript brushHash;
    private static String apiCacheHashScript;
    private static String apiCacheHashSha1;
    private static RScript apiCacheHash;
    @Resource
    private RedissonClient redissonClient;

    @SneakyThrows
    public static void recordApiCount(String key, Object...args) {
        apiCacheHash.evalShaAsync(RScript.Mode.READ_WRITE, apiCacheHashSha1, RScript.ReturnType.BOOLEAN,
                Arrays.asList(key), args);
    }

    @SneakyThrows
    public void recordBrushApi(String key, Object...args) {
        brushHash.evalShaAsync(RScript.Mode.READ_WRITE, brushHashSha1, RScript.ReturnType.BOOLEAN,
                Arrays.asList(key), args);
    }

    public Long incrAndGet(String key, Long expire) {
        try {
            long val = redissonClient.getAtomicLong(key).incrementAndGet();
            if(val == 1L) {
                redissonClient.getAtomicLong(key).expireAsync(expire, TimeUnit.MILLISECONDS);
            }
            return val;
        } catch (Exception e) {
            log.error("incrAndGet error", e);
        }
        return -1L;
    }


    public Map<String, Long> getLockFlagList() {
        String key = Constants.TRADE_PRODUCT_STOCK_LOCK_LIST_KEY;
        Long now = System.currentTimeMillis();
        Collection<ScoredEntry<String>> entries = redissonClient.<String>getScoredSortedSet(key)
                .entryRangeReversed(now.doubleValue(), true, now.doubleValue() + 10 * 60 * 1000, true, 0, 2000);
        if(CollectionUtils.isEmpty(entries)) return Collections.emptyMap();
        Map<String, Long> result = new HashMap<>(entries.size() * 4 / 3);
        entries.stream().forEach(e -> result.put(e.getValue(), e.getScore().longValue()));
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        brushHashScript = new ResourceScriptSource(new ClassPathResource("script/brush-api.lua")).getScriptAsString();
        brushHash = redissonClient.getScript(IntegerCodec.INSTANCE);
        brushHashSha1 = brushHash.scriptLoad(brushHashScript);
        apiCacheHashScript = new ResourceScriptSource(new ClassPathResource("script/api-cache.lua")).getScriptAsString();
        apiCacheHash = redissonClient.getScript(IntegerCodec.INSTANCE);
        apiCacheHashSha1 = brushHash.scriptLoad(apiCacheHashScript);

    }
}
