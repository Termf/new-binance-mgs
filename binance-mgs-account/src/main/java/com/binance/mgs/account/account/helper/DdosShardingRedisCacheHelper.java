package com.binance.mgs.account.account.helper;

import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.security.vo.CaptchaValidateInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 为了ddos攻击专门的redis集群
 */
@Slf4j
@Getter
@Component
public class DdosShardingRedisCacheHelper {

    @Value("${ddos.ban.ip.expire.seconds:550}")
    private int ddosBanIpExpireSeconds;
    @Value("${ddos.redis.incr.limit:1000}")
    private long ddosRedisIncrLimit;
    @Value("${verify.cache.expire.minutes:30}")
    private int verifyCacheExpireMinutes;

    public Long ipVisitCount(String ip, String type) {
        try {
            String key = StringUtils.joinWith(":", CacheConstant.ACCOUNT_DDOS_IP_COUNT_PREFIX, type, ip);
            //超过一定次数不再递增了
            Integer oldCount = ShardingRedisCacheUtils.get(key, Integer.class);
            if (oldCount != null && oldCount > ddosRedisIncrLimit) {
                log.warn("ipVisitCount incr over limit={} key={} ip={}", ddosRedisIncrLimit, key, WebUtils.getRequestIp());
                return Long.valueOf(oldCount);
            }
            Long count = ShardingRedisCacheUtils.increment(key, 1);
            Long expire = ShardingRedisCacheUtils.getExpire(key);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null == expire || -1L == expire) {
                ShardingRedisCacheUtils.expire(key, ddosBanIpExpireSeconds, TimeUnit.SECONDS);
                expire = ShardingRedisCacheUtils.getExpire(key);
            }
            log.info("ipVisitCount expire={},count={}", expire, count);
            return count;
        } catch (Exception e) {
            log.error("ipCount error", e);
        }
        return 0L;
    }

    /**
     * 超过上限则不在计数
     *
     * @param ip
     * @param upperLimit
     * @return
     */
    public Long ipVisitCountWithUpperLimit(String ip, long upperLimit) {
        try {
            String key = CacheConstant.ACCOUNT_DDOS_IP_COUNT_PREFIX + ":" + ip;
            //超过一定次数不再递增了
            Integer oldCount = ShardingRedisCacheUtils.get(key, Integer.class);
            if (oldCount != null && oldCount > upperLimit) {
                log.warn("qrCodeIpVisitCount incr over limit={} key={} ip={}", upperLimit, key, WebUtils.getRequestIp());
                return Long.valueOf(oldCount);
            }
            Long count = ShardingRedisCacheUtils.increment(key, 1);
            Long expire = ShardingRedisCacheUtils.getExpire(key);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null == expire || -1L == expire) {
                ShardingRedisCacheUtils.expire(key, ddosBanIpExpireSeconds, TimeUnit.SECONDS);
                expire = ShardingRedisCacheUtils.getExpire(key);
            }
            log.info("qrCodeIpVisitCount expire={},count={}", expire, count);
            return count;
        } catch (Exception e) {
            log.error("ipCount error", e);
        }
        return 0L;
    }

    public Long subAccountActionCount(Long parentUserId, String action, int expireTime) {
        try {
            String key = StringUtils.joinWith(":", CacheConstant.ACCOUNT_DDOS_SUB_ACCOUNT_ACTION_FREQUENCY_PREFIX, action, parentUserId);
            //超过一定次数不再递增了
            Integer oldCount = ShardingRedisCacheUtils.get(key,Integer.class);
            if (oldCount != null && oldCount > ddosRedisIncrLimit) {
                log.warn("subAccountActionCount incr over limit={} key={} ip={}", ddosRedisIncrLimit, key, WebUtils.getRequestIp());
                return Long.valueOf(oldCount);
            }
            Long count = ShardingRedisCacheUtils.increment(key, 1);
            Long expire = ShardingRedisCacheUtils.getExpire(key);
            if (null == expire || -1L == expire) {
                ShardingRedisCacheUtils.expire(key, expireTime, TimeUnit.SECONDS);
            }
            log.info("subAccountActionWithExpireTime parentUserId={}, action={},expire={},count={}", parentUserId, action, expire, count);
            return count;
        } catch (Exception e) {
            log.error("subAccountActionWithExpireTime count error", e);
        }
        return 0L;
    }

    /**
     * 统计人机token使用次数
     * 一般最多5条命
     */
    public Long incrCaptchaTokenCount(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
        try {
            String key = CacheConstant.ACCOUNT_DDOS_CAPTCHA_TOKEN_COUNT_PREFIX + ":" + token;
            //超过一定次数不再递增了
            Integer oldCount = ShardingRedisCacheUtils.get(key, Integer.class);
            if (oldCount != null && oldCount > ddosRedisIncrLimit) {
                log.warn("captcha token incr over limit={} key={} ip={}", ddosRedisIncrLimit, key, WebUtils.getRequestIp());
                return Long.valueOf(oldCount);
            }
            Long count = ShardingRedisCacheUtils.increment(key, 1);
            Long expire = ShardingRedisCacheUtils.getExpire(key);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null == expire || -1L == expire) {
                ShardingRedisCacheUtils.expire(key, verifyCacheExpireMinutes, TimeUnit.MINUTES);
                expire = ShardingRedisCacheUtils.getExpire(key);
            }
            log.info("incrGtForbiddenCache key={},expire={},count={}", key, expire, count);
            return count;
        } catch (Exception e) {
            log.error("incrGtForbiddenCache error", e);
            return 0L;
        }
    }

    /**
     * 设置人机验证是否通过
     */
    public void setVerifyResult(String token, boolean isSuccess) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
        try {
            String key = CacheConstant.ACCOUNT_DDOS_VERIFY_CAPTCHA_CACHE_PREFIX + ":" + token;
            ShardingRedisCacheUtils.set(key, isSuccess, verifyCacheExpireMinutes * 60);
        } catch (Exception e) {
            log.error("setVerifyResult error", e);
        }
    }

    /**
     * 获取人机验证是否通过
     */
    public boolean getVerifyResult(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            String key = CacheConstant.ACCOUNT_DDOS_VERIFY_CAPTCHA_CACHE_PREFIX + ":" + token;
            Boolean value = ShardingRedisCacheUtils.get(key, Boolean.class);
            log.info("getVerifyResult value = {}", value);
            return BooleanUtils.isTrue(value);
        } catch (Exception e) {
            log.error("getVerifyResult error", e);
        }
        return true;
    }


    public void setValidateInfo(String key, CaptchaValidateInfo value, long expireTime) {
        String cacheKey = CacheConstant.ACCOUNT_DDOS_SESSION_ID_PREFIX + ":" + key;
        String jsonStr = JsonUtils.toJsonNotNullKey(value);
        ShardingRedisCacheUtils.set(cacheKey, jsonStr, expireTime);
    }

    public CaptchaValidateInfo getValidateInfo(String key) {
        try {
            String cacheKey = CacheConstant.ACCOUNT_DDOS_SESSION_ID_PREFIX + ":" + key;
            String jsonStr = ShardingRedisCacheUtils.get(cacheKey);
            if (StringUtils.isBlank(jsonStr)) {
                return null;
            }
            return JsonUtils.toObj(jsonStr, CaptchaValidateInfo.class);
        } catch (Exception e) {
            log.info("parse obj exception");
            return null;
        }
    }
}
