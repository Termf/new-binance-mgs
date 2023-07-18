package com.binance.mgs.nft.fantoken.helper;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
public class CacheProperty {

    private static final String FAN_TOKEN_CACHE_KEY = "nft.fan-token.cache.enabled";

    @Value("${nft.fan-token.cache.enabled:false}")
    private boolean enabled;

    @ApolloConfigChangeListener(interestedKeys = FAN_TOKEN_CACHE_KEY)
    private void onChange(ConfigChangeEvent event) {
        log.info("nft.fan-token.cache.enabled changed: [{}]", System.currentTimeMillis());
        ConfigChange change = event.getChange(FAN_TOKEN_CACHE_KEY);
        log.info("found cache key change: [{}], [{}], [{}], [{}]",
                change.getPropertyName(), change.getOldValue(), change.getNewValue(),
                change.getChangeType());
        enabled = Boolean.parseBoolean(change.getNewValue());
    }
}
