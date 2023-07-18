package com.binance.mgs.account.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Apollo配置文件
 */
@Configuration
@Data
public class ApolloConfig {
    /**
     * 默认为使能
     */
    @Value("${matchbox.enabled:true}")
    private String mbxEnabled;

    @Value("${binance.current.site:MAIN}")
    private String currentSite;

    public Boolean isMainSite(){
        return "MAIN".equalsIgnoreCase(currentSite);
    }
}
