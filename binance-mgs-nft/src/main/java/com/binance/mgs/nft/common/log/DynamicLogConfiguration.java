package com.binance.mgs.nft.common.log;

import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class DynamicLogConfiguration {

    private static final String LOGLEVEL_PREFIX = "service.log.level";
    @Value("${service.log.level:WARN}")
    private String level;

    @PostConstruct
    public void init() {
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5L);
                modifyLogLevel(level);
                monitorApolloChanges();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void monitorApolloChanges() {
        ConfigService.getAppConfig().addChangeListener(changeEvent -> {
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                if (change.getPropertyName().equals(LOGLEVEL_PREFIX)) {
                    log.warn("Apollo was changed for {}", LOGLEVEL_PREFIX);
                    String levelName = change.getNewValue();
                    modifyLogLevel(levelName);
                    log.warn("log level change finish {}", levelName);
                }
            }
        });
    }

    private void modifyLogLevel(String levelName) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration configuration = context.getConfiguration();
        Map<String, LoggerConfig> configMap = configuration.getLoggers();
        configMap.values().forEach(c -> c.setLevel(Level.getLevel(levelName)));
        context.updateLoggers();
    }

}
