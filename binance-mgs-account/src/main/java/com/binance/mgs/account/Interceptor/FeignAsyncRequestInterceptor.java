package com.binance.mgs.account.Interceptor;

import com.binance.platform.env.EnvUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
@Log4j2
@Component
@ConditionalOnProperty(value = "feign.async.request.interceptor.enable", havingValue = "true")
public class FeignAsyncRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        try{
            Map<String, String> headMap= FeignAsyncHelper.getAndClearLocalHead();
            if(null!=headMap){
                for(Map.Entry<String, String> entry : headMap.entrySet()){
                    if(!EnvUtil.isProd()){
                        try{
                            log.info("FeignAsyncRequestInterceptor,key={},value={}", entry.getKey(),entry.getValue());
                        }catch (Exception e){
                            log.error("FeignAsyncRequestInterceptor,logfailed:", e);
                        }
                    }
                    requestTemplate.header(entry.getKey(),entry.getValue());
                }
            }
        }catch (Exception e){
            log.error("FeignAsyncRequestInterceptor,error",e);
        }


    }
}
