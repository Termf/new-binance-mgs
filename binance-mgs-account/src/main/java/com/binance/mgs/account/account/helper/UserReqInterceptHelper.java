package com.binance.mgs.account.account.helper;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class UserReqInterceptHelper {

    private static final String REQ_INTERCEPT_COUNT = "req_intercept_count";
    private static final String REQ_INTERCEPT_LOCK = "req_intercept_lock";


    public <T> void reqIntercept(String prefix, String key, int allowCount, long timeWindow, long lockTime) {
        this.reqIntercept(prefix, key, allowCount, timeWindow, lockTime, null);
    }

    /**
     * 在固定的时间窗口长度内
     *
     * @param prefix     key 的前缀
     * @param key        锁定的key
     * @param allowCount 允许次数
     * @param timeWindow 时间窗(固定开始) 单位 s
     * @param lockTime   禁用访问时长 单位 s，大于0时考虑拦截调用
     * @param reject     当达到锁定次数时可以做一个回调
     */
    public <T> void reqIntercept(String prefix, String key, int allowCount, long timeWindow, long lockTime, Consumer<Long> reject) {
        if (StringUtil.isBlank(key)) {
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        String lockKey = REQ_INTERCEPT_LOCK + prefix + ":" + key;
        String lockValue = RedisCacheUtils.get(lockKey);
        if (StringUtil.isNotBlank(lockValue)) {
            // 已经被禁止使用接口访问, 计算锁定剩余时间
            Long times = Long.valueOf(lockValue) - System.currentTimeMillis();
            log.warn("too many request intercept by key:{}, times:{}", lockKey, times);
            long hour = 0;
            long minute = 0;
            if (times > 0) {
                hour = times / TimeUnit.HOURS.toMillis(1);
                minute = (times - hour * TimeUnit.HOURS.toMillis(1)) / TimeUnit.MINUTES.toMillis(1);
                throw new BusinessException(MgsErrorCode.REQUEST_OVER_TIMES_PARAMS, new Object[]{hour, minute});
            } else {
                throw new BusinessException(MgsErrorCode.REQUEST_OVER_TIMES);
            }
        }
        long currentCount = RedisCacheUtils.increment(key, REQ_INTERCEPT_COUNT + prefix, 1, timeWindow, TimeUnit.SECONDS);
        if (currentCount > allowCount) {
            log.warn("too many requests for key:{} prefix:{} allowCount:{} currentCount:{}", key, prefix, allowCount, currentCount);
            if (lockTime > 0) {
                // 设置禁止继续调用的缓存
                String endTime = String.valueOf(System.currentTimeMillis() + lockTime * 1000);
                RedisCacheUtils.set(lockKey, endTime, lockTime);
                if (reject != null) {
                    reject.accept(currentCount);
                }
            }
        }
    }


}
