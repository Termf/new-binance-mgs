package com.binance.mgs.account.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RedisSlidingWindow {

    private @NonNull RedisTemplate<String, Object> redisTemplate;
    /*
     * 时间间隔内的次数限制
     */
    private @NonNull long limit;
    /*
     * 时间间隔
     */
    private @NonNull long timout;
    private @NonNull TimeUnit unit;


    /**
     * 增长访问数
     *
     * @param key
     */
    public void increase(String key) {
        long curTs = System.currentTimeMillis();
        long maxScore = curTs - unit.toMillis(timout);
//        redisTemplate.multi();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, maxScore);//按score清除过期成员
        redisTemplate.opsForZSet().add(key, "ts:"+curTs, curTs);
        redisTemplate.expire(key, timout, unit); //更新过期时间
//        redisTemplate.exec();
    }


    public long getTtl(String key) {
        return Optional.ofNullable(redisTemplate.getExpire(key, unit)).orElse(-1L);
    }


    public boolean checkOverLimit(String key) {
        return Optional.ofNullable(redisTemplate.opsForZSet().zCard(key)).orElse(0L).compareTo(limit) >= 0;
    }
}
