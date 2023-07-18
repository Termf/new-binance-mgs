package com.binance.mgs.account.config;

import com.binance.accountdefensecenter.core.bootstrap.AbstractCallAppCheckBootStrap;
import com.binance.accountdefensecenter.core.interceptor.CallAppCheckAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CallAppCheckAspectConfig extends AbstractCallAppCheckBootStrap {

    @Bean
    public CallAppCheckAspect callAppCheckAspect() {
        return new CallAppCheckAspect();
    }
}
