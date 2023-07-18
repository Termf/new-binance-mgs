package com.binance.mgs.nft.core.gray;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Configuration
public class NFTGrayConfiguration {
    @Value("${nft.grayFilter.urlPattern:/v1/private/*,/v1/public/*,/v1/friendly/*,/v2/private/*,/v2/public/*,/v2/friendly/*,/v3/private/*,/v3/public/*,/v3/friendly/*}")
    private String[] urlPattern;

    @Resource
    protected Environment env;

    @Bean
    public FilterRegistrationBean filterRegistrationBean(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        WebRequestGrayFilter webRequestGrayFilter = new WebRequestGrayFilter(
                env, discoveryClient, restTemplate
        );
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(webRequestGrayFilter);
        filterRegistrationBean.addUrlPatterns(urlPattern);
        filterRegistrationBean.setName("webRequestGrayFilter");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);//order的数值越小 则优先级越高
        return filterRegistrationBean;
    }

}
