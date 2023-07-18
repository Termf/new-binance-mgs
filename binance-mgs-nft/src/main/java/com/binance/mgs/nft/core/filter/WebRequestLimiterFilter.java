package com.binance.mgs.nft.core.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.ErrorCode;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.RequestIpUtil;
import com.binance.master.utils.WebUtils;
import com.binance.master.web.handlers.MessageHelper;
import com.binance.mgs.nft.common.helper.RedisHelper;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.core.redis.RedisCommonConfig;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.monitor.Monitors;
import com.binance.platform.openfeign.body.CustomBodyServletRequestWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.util.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class WebRequestLimiterFilter extends OncePerRequestFilter {
    private static final String REQ_BURUSH_WINDOW_KEY = "nft:brush:%s:%s";
    private static volatile Map<String, AtomicInteger> valueMap = new ConcurrentHashMap<>();
    private static final AtomicLong scheduleTime = new AtomicLong();
    protected static final Cache<String, Integer> bcache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(6000)
            .expireAfterWrite(600, TimeUnit.SECONDS)
            .build();

    protected static final Cache<String, Integer> scache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(3000)
            .expireAfterWrite(65, TimeUnit.SECONDS)
            .build();

    private static ScheduledThreadPoolExecutor executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);

    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(16);
    private final RedisHelper redisHelper;
    private final MgsNftProperties mgsNftProperties;
    private final MessageHelper messageHelper;
    private final BaseHelper baseHelper;

    @SneakyThrows
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) {
        String requestUri = httpServletRequest.getRequestURI();
        requestUri = requestUri.replace("/v1/public/nft/", "")
                .replace("/v1/private/nft/", "")
                .replace("/v1/friendly/nft/", "");
        HttpServletRequest requestWrapper = httpServletRequest;
        if(mgsNftProperties.getReqLogList().contains(requestUri)) {
            Object param = httpServletRequest.getParameterMap();
            if (HttpMethod.POST.name().equals(httpServletRequest.getMethod()) && !httpServletRequest.getContentType().equals(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                // 重新生成ServletRequest  这个新的 ServletRequest 获取流时会将流的数据重写进流里面
                CustomBodyServletRequestWrapper customequestWrapper = new CustomBodyServletRequestWrapper(httpServletRequest);
                param = new String(customequestWrapper.getBody());
                requestWrapper = customequestWrapper;
            }
            String ip = RequestIpUtil.getIpAddress(requestWrapper);
            log.info("custome request {} {} {}", requestUri, param, ip);
        }

        if(!mgsNftProperties.getRateLimiterMap().containsKey(requestUri)) {
            filterChain.doFilter(requestWrapper, httpServletResponse);
            return;
        }

        String key = WebUtils.getHeader(Constant.HEADER_USER_ID);
        if (StringUtils.isBlank(key)) {
            key = RequestIpUtil.getIpAddress(requestWrapper);
        }

        try {
            do {
                //白名单
                if(checkWhiteList(requestUri, key)) break;

                //系统维护
                checkApiMaintence(requestUri);

                //请求计数
                recordBrushApiRequest(requestWrapper, requestUri);

                //本地限流
                doLocalRateLimit(requestUri, key);

                // 刷接口校验
                if(!doBrushApiLimit(requestUri, key)) break;

                //redis限流
                MgsNftProperties.ParamFlowRuleConfig config = mgsNftProperties.getRateLimiterMap().get(requestUri);
                int limit = config.getExcludes().contains(key) ? config.getExcludeLimit() : config.getLimit();
                doRedisRateLimit(requestUri, key, limit);
            } while (false);
        } catch (BusinessException e) {
            ErrorCode errorCode = e.getErrorCode();
            writeResponse(httpServletResponse, errorCode);
            return;
        } catch (Exception e) {
            log.warn("ratelimiter error", e);
        }

        filterChain.doFilter(requestWrapper, httpServletResponse);
    }

    private boolean checkWhiteList(String requestUri, String key) {
        MgsNftProperties.ParamFlowRuleConfig config = mgsNftProperties.getRateLimiterMap().get(requestUri);
        return SetUtils.emptyIfNull(config.getWhitelist()).contains(key);
    }

    private boolean doBrushApiLimit(String requestUri, String key) {
        if(!mgsNftProperties.getBrushApiConfig().containsKey(requestUri)) {
            return true;
        }
        MgsNftProperties.BrushParamConfig config = mgsNftProperties.getBrushApiConfig().get(requestUri);
        Integer bCount = Optional.ofNullable(bcache.getIfPresent(requestUri + "-" + key)).orElse(0);
        boolean lv2Limited = config.getThresholds()[1] != -1 && bCount >= config.getThresholds()[1];
        if(lv2Limited) {
            log.info("doBrushApiLimit lv2Limited 1 {} {} {}",requestUri, key, config.getLimits()[1]);
            doRedisRateLimit(requestUri, key, config.getLimits()[1]);
            return false;
        }
        Integer sCount = Optional.ofNullable(scache.getIfPresent(requestUri + "-" + key)).orElse(0);
        lv2Limited = config.getThresholds()[3] != -1 && sCount >= config.getThresholds()[3];
        if(lv2Limited) {
            log.info("doBrushApiLimit lv2Limited 2 {} {} {}",requestUri, key, config.getLimits()[1]);
            doRedisRateLimit(requestUri, key, config.getLimits()[1]);
            return false;
        }

        if((config.getThresholds()[0] != -1 && bCount >= config.getThresholds()[0])
                ||(config.getThresholds()[2] != -1 &&sCount >= config.getThresholds()[2])) {
            log.info("doBrushApiLimit lv1Limited 3 {} {} {}",requestUri, key, config.getLimits()[0]);
            doRedisRateLimit(requestUri, key, config.getLimits()[0]);
            return false;
        }
        return true;
    }

    private void recordBrushApiRequest(HttpServletRequest requestWrapper, String requestUri) {
        if(!mgsNftProperties.getBrushApiConfig().containsKey(requestUri)) {
            return;
        }
        String ip = RequestIpUtil.getIpAddress(requestWrapper);
        mgsNftProperties.getBrushWindows().stream().forEach(w -> {
            Long curWin = DateUtils.getNewUTCTimeMillis() / 1000 / 60 / w;
            String winKey = String.format(REQ_BURUSH_WINDOW_KEY, curWin, requestUri);
            String newKey = winKey + "-" + ip;
            valueMap.putIfAbsent(newKey, new AtomicInteger(0));
            valueMap.get(newKey).incrementAndGet();
            Long timeMs = DateUtils.getNewUTCTimeMillis();
            Long timeS = timeMs/1000L;
            Long prevTime = scheduleTime.getAndSet(timeS);
            if(!Objects.equals(timeS, prevTime)) {
                Long nextMs = (timeS + 1) * 1000L;
                executorService.schedule(() -> {
                    try {
                        Map<String, List<Object>> dataMap = new HashMap<>();
                        Map<String, AtomicInteger> localMap = valueMap;
                        Map<String, AtomicInteger> newValueMap = new ConcurrentHashMap<>();
                        valueMap = newValueMap;
                        localMap.entrySet().stream().forEach(e -> {
                            int count = e.getValue().getAndUpdate(v -> 0);
                            if(count == 0) return;
                            int index = e.getKey().lastIndexOf("-");
                            String key = e.getKey().substring(0, index);
                            String member = e.getKey().substring(index + 1);
                            List<Object> list = dataMap.getOrDefault(key, Lists.newArrayList());
                            list.add(member);
                            list.add(count);
                            dataMap.put(key, list);
                        });

                        dataMap.entrySet().stream().forEach(e -> {
                            ListUtils.partition(e.getValue(), 50).forEach(l -> {
                                redisHelper.recordBrushApi(e.getKey(), l.toArray());
                                log.info("recordBrushApiRequest {} {}", e.getKey(), l.size());
                            });
                        });
                    } catch (Exception e) {
                        log.warn("recordBrushApiRequest error", e);
                    }
                }, nextMs - timeMs, TimeUnit.MILLISECONDS);
            }
        });
    }

    private void checkApiMaintence(String requestUri) {
        MgsNftProperties.ParamFlowRuleConfig config = mgsNftProperties.getRateLimiterMap().get(requestUri);
        if(config != null && config.getLimit() == 0) {
            throw new BusinessException(GeneralCode.API_MAINTENANCE);
        }
    }



    private void writeResponse(HttpServletResponse httpServletResponse, ErrorCode errorCode) throws IOException {
        String message = messageHelper.getMessage(errorCode);
        CommonRet<Void> commonRet = new CommonRet<>();
        commonRet.setCode(errorCode.getCode());
        commonRet.setMessage(message);
        byte[] data = JsonUtils.toJsonHasNullKey(commonRet).getBytes(StandardCharsets.UTF_8);

        httpServletResponse.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();
        outputStream.write(data);
        try {
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    private void doLocalRateLimit(String requestUri, String key) {
        Entry entry = null;
        try {
            entry = SphU.entry(requestUri, EntryType.IN, 1, key);
        } catch (BlockException e) {
            Monitors.count("mgs_nft_block", "uri", requestUri, "type", "local");
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        } finally {
            if (entry != null) {
                entry.exit(1, key);
            }
        }
    }

    @SneakyThrows
    private void doRedisRateLimit(String uri, String key, int limit) {
        if(limit == 0) {
            Monitors.count("mgs_nft_block", "uri", uri, "type", "redis");
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
        String limiterKey = String.format(RedisCommonConfig.MGS_RATE_LIMITER_KEY, uri, key, DateUtils.getNewUTCTimeMillis()/1000);
        // 异步调用 + size 监控 保护redis
        if(executor.getQueue().size() > 1000) {
            return;
        }
        Future<Long> future = executor.submit(() -> {
            return redisHelper.incrAndGet(limiterKey, 3000L);
        });
        Long count = null;
        try {
            count = future.get(300L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {}

        if(count == null) return;

        if(count == -1L ||count > limit) {
            if(count > limit * 10) {
                log.warn("mgs redis block overlimit warn {} {} {}", uri, key, count);
            }
            Monitors.count("mgs_nft_block", "uri", uri, "type", "redis");
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
    }
}
