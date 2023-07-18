package com.binance.mgs.nft.event;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.nft.market.ifae.NFTEventConfigApi;
import com.binance.nft.market.vo.event.EventConfigBo;
import com.binance.nft.market.vo.event.EventConfigDetailRequest;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.EnvUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequestMapping("/v1/public/nft/event")
@RestController
@RequiredArgsConstructor
public class EventConfigController {

    private final NFTEventConfigApi eventConfigApi;

    /**
     * mystery-box商品缓存
     */
    private final LoadingCache<EventConfigDetailRequest, EventConfigBo> eventConfigBoCache =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(60, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<EventConfigDetailRequest, EventConfigBo>() {
                        @Override
                        public EventConfigBo load(@NotNull EventConfigDetailRequest eventConfigDetailRequest) throws Exception {
                            final String envFlag = EnvUtil.getEnvFlag();
                            RpcContext.getContext().set("X-GRAY-ENV", envFlag);
                            final APIResponse<EventConfigBo> eventConfig = eventConfigApi.getEventConfig(
                                    APIRequest.instance(eventConfigDetailRequest)
                            );
                            BaseHelper.checkResponse(eventConfig);
                            return eventConfig.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    @SneakyThrows
    @GetMapping("detail/{eventNumber}")
    public CommonRet<EventConfigBo> eventDetail(@PathVariable String eventNumber, @RequestParam(required = false) Integer isDev, HttpServletRequest request) {
        final boolean isPreview = Objects.nonNull(isDev) && isDev == 1;
        EventConfigDetailRequest eventConfigDetailRequest = new EventConfigDetailRequest();
        eventConfigDetailRequest.setIsGray(isGray(request));
        eventConfigDetailRequest.setIsPreview(isPreview);
        eventConfigDetailRequest.setEventNumber(Long.valueOf(eventNumber));
        EventConfigBo eventConfigBo;
        if (isPreview) {
            final APIResponse<EventConfigBo> eventConfig = eventConfigApi.getEventConfig(
                    APIRequest.instance(eventConfigDetailRequest)
            );
            BaseHelper.checkResponse(eventConfig);
            eventConfigBo = eventConfig.getData();
        } else {
            eventConfigBo = eventConfigBoCache.get(eventConfigDetailRequest);
        }
        if (eventConfigBo.getStatus() == 0 && !isPreview) {
            throw new IllegalArgumentException("Event status error!");
        }
        refreshStatus(eventConfigBo);
        return new CommonRet<>(eventConfigBo);
    }

    @SneakyThrows
    @GetMapping("detail/event-key/{eventKey}")
    public CommonRet<EventConfigBo> eventDetailByKey(@PathVariable String eventKey, @RequestParam(required = false) Integer isDev, HttpServletRequest request) {
        final boolean isPreview = Objects.nonNull(isDev) && isDev == 1;
        EventConfigDetailRequest eventConfigDetailRequest = new EventConfigDetailRequest();
        eventConfigDetailRequest.setIsGray(isGray(request));
        eventConfigDetailRequest.setIsPreview(isPreview);
        eventConfigDetailRequest.setEventKey(eventKey);
        EventConfigBo eventConfigBo;
        if (isPreview) {
            final APIResponse<EventConfigBo> eventConfig = eventConfigApi.getEventConfig(
                    APIRequest.instance(eventConfigDetailRequest)
            );
            BaseHelper.checkResponse(eventConfig);
            eventConfigBo = eventConfig.getData();
        } else {
            eventConfigBo = eventConfigBoCache.get(eventConfigDetailRequest);
        }
        if (eventConfigBo.getStatus() == 0 && !isPreview) {
            throw new IllegalArgumentException("Event status error!");
        }
        refreshStatus(eventConfigBo);
        return new CommonRet<>(eventConfigBo);
    }

    private void refreshStatus(EventConfigBo eventConfig) {
        final Date eventStartTime = new Date(eventConfig.getEventStartTime());
        final Duration startBetween = Duration.between(eventStartTime.toInstant(), new Date().toInstant());
        final Date eventEndTime = new Date(eventConfig.getEventEndTime());
        final Duration endBetween = Duration.between(new Date().toInstant(), eventEndTime.toInstant());
        if (startBetween.isNegative()) {
            eventConfig.setStatus(1);
        } else if (endBetween.isNegative()) {
            eventConfig.setStatus(3);
        } else {
            eventConfig.setStatus(2);
        }
    }

    private Boolean isPreview(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && envFlag.endsWith("binance.com");
    }

    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);

    }

}
