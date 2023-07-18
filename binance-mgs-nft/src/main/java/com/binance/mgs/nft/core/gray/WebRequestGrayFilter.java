package com.binance.mgs.nft.core.gray;

import com.binance.platform.common.RpcContext;
import com.binance.platform.common.EnvUtil;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class WebRequestGrayFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebRequestGrayFilter.class);

    private static final String META_FLOW_FLAG = "flowflag";
    public static final String GRAY_ENV_HEADER = "X-GRAY-ENV";

    private Environment env;

    private String applicationName;

    public final String normalHeader;

    private final AtomicLong forwardCount = new AtomicLong(0);

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate;

    public WebRequestGrayFilter(Environment env, DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.applicationName = env.getProperty("spring.application.name");
        this.normalHeader = env.getProperty("nft.mgs.flow.match.normalHeader", "normal");
        this.env = env;
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    @SneakyThrows
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) {
        boolean shouldFilter = env.getProperty("nft.mgs.flow.match.shouldFilter", Boolean.class, true);
        if (shouldFilter) {
            filterMatchedNodes(httpServletRequest, httpServletResponse, filterChain);
        } else {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }
    }

    private void filterMatchedNodes(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws Exception {
        String currFlowFlag = EnvUtil.getFlowFlag();
        String envFlagHeader = getEnvFlagHeader(httpServletRequest);
        if (!isEnvMatch(currFlowFlag, envFlagHeader)) {
            ServiceInstance instance = filterByFlag(envFlagHeader);
            if (instance != null) {
                LOGGER.info("will forward request to right instance count={} envFlagHeader={} currentFlowFlag={}", forwardCount.incrementAndGet(), envFlagHeader, currFlowFlag);
                forwardToCorrectInstance(httpServletRequest, httpServletResponse, instance);
                return;
            }
            LOGGER.error("there is no matched envFlagHeader={} current flag={}", envFlagHeader, currFlowFlag);
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private void forwardToCorrectInstance(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ServiceInstance instance) throws IOException {
        // build http redirectUrl
        String homePageUrl = ((EurekaServiceInstance) instance).getInstanceInfo().getHomePageUrl();
        String requestUri = httpServletRequest.getRequestURI();
        if (homePageUrl.endsWith("/") && requestUri.startsWith("/")) {
            requestUri = requestUri.substring(1);
        }
        String redirectUrl = homePageUrl + requestUri;
        LOGGER.info("the redirectUrl url is {} ", redirectUrl);
        String queryString = httpServletRequest.getQueryString();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(queryString)) {
            redirectUrl += "?" + queryString;
        }

        // build http method
        String method = httpServletRequest.getMethod();

        // build http header
        Enumeration<String> requestHeaderNames = httpServletRequest.getHeaderNames();
        MultiValueMap<String, String> multiValueMap = new HttpHeaders();
        while (requestHeaderNames.hasMoreElements()) {
            String nextElement = requestHeaderNames.nextElement();
            String value = httpServletRequest.getHeader(nextElement);
            multiValueMap.add(nextElement, value);
        }

        // set response header
        httpServletResponse.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        // do redirect request
        if (HttpMethod.GET.name().equalsIgnoreCase(method)) {
            HttpEntity<byte[]> httpEntity = new HttpEntity<>(multiValueMap);
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(redirectUrl, HttpMethod.GET, httpEntity, byte[].class);
            writeResponse(httpServletResponse, responseEntity);
        } else if (HttpMethod.POST.name().equalsIgnoreCase(method)) {
            byte[] bytes = StreamUtils.copyToByteArray(httpServletRequest.getInputStream());
            HttpEntity<byte[]> httpEntity = new HttpEntity<>(bytes, multiValueMap);
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(redirectUrl, HttpMethod.POST, httpEntity, byte[].class);
            writeResponse(httpServletResponse, responseEntity);
        }
    }

    private ServiceInstance filterByFlag(String envFlagHeader) {
        List<ServiceInstance> appInstances = discoveryClient.getInstances(applicationName);
        List<ServiceInstance> filteredList = appInstances.stream().filter(ins -> StringUtils.equals(envFlagHeader, ins.getMetadata().get(META_FLOW_FLAG)))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(filteredList)) {
            int random = ThreadLocalRandom.current().nextInt(filteredList.size());
            return filteredList.get(random);
        } else {
            return null;
        }
    }

    private boolean isEnvMatch(String currentFlowFlag, String envFlagHeader) {
        return StringUtils.equals(currentFlowFlag, envFlagHeader);
    }

    private void writeResponse(HttpServletResponse httpServletResponse, ResponseEntity<byte[]> responseEntity) throws IOException {
        byte[] forObject = responseEntity.getBody();
        assert forObject != null;
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();
        outputStream.write(forObject);
        try {
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    private String getEnvFlagHeader(HttpServletRequest httpServletRequest) {
        String envHeader = httpServletRequest.getHeader(GRAY_ENV_HEADER);
        if (envHeader == null) {
            envHeader = RpcContext.getContext().get(GRAY_ENV_HEADER);
        }
        if (normalHeader.equals(envHeader)) {
            envHeader = null;
        }
        return envHeader;
    }
}
