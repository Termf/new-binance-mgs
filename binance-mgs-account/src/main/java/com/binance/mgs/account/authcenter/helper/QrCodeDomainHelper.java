package com.binance.mgs.account.authcenter.helper;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserApi;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.user.response.GetUserEmailResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.Md5Tools;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.mgs.account.authcenter.dto.DomainTemplate;
import com.binance.mgs.account.authcenter.dto.TokenDto;
import com.binance.mgs.account.authcenter.vo.CreateQrCodeUrlArg;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.component.domain.OpsDomainNamespaceConfig;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.utils.DomainUtils;
import com.binance.platform.mgs.utils.PKGenarator;
import com.ctrip.framework.apollo.ConfigService;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class QrCodeDomainHelper extends BaseHelper {
    private static final String APP_OPS_CONFIG_NAMESPACE = "ops.domain";

    private static final String APP_BIZ_CONFIG_NAMESPACE = "biz.app";

    private static final String APOLLO_DOMAIN_KEY = "dynamic-config.all.template";
    private static final String WEB_VIEW_URL_MAPPING = "webViewWebUrlMapping";

    private static final String DOMAIN_REQUEST_TYPE = "DOMAIN";
    private static final String WEBVIEW_REQUEST_TYPE = "WEBVIEW";

    private LoadingCache<String, Map<String, DomainTemplate.ApiDomainInfo>> apiDomainCache = CacheBuilder
            .newBuilder().maximumSize(100).refreshAfterWrite(120, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Map<String, DomainTemplate.ApiDomainInfo>>() {
                @Override
                public Map<String, DomainTemplate.ApiDomainInfo> load(String key) throws Exception {
                    List<DomainTemplate.ApiDomainInfo> apiDomainInfoList = getApiDomainInfo();
                    if (Objects.equals(DOMAIN_REQUEST_TYPE, key)) {
                        return apiDomainInfoList.stream().collect(Collectors.toMap(DomainTemplate.ApiDomainInfo::getApiDomain, Function.identity(), (a, b) -> a));
                    }
                    return apiDomainInfoList.stream().collect(Collectors.toMap(DomainTemplate.ApiDomainInfo::getWebviewDomain, Function.identity(), (a, b) -> a));
                }
            });

    public String getDomainByType(CreateQrCodeUrlArg req) throws ExecutionException {
        String domain = DomainUtils.getDomain();
        if (req.getDomainType() == 1) {
            return domain;
        }

        DomainTemplate.ApiDomainInfo ret;
        if (req.getRequestType() == 1) {
            ret = apiDomainCache.get(DOMAIN_REQUEST_TYPE).get(domain);
        } else {
            ret = apiDomainCache.get(WEBVIEW_REQUEST_TYPE).get(domain);
        }
        if (ret == null) {
            return domain;
        }
        if (req.getDomainType() == 2) {
            return StringUtils.isNotBlank(ret.getWebviewDomain()) ? ret.getWebviewDomain() : domain;
        }

        return StringUtils.isNotBlank(ret.getWebDomain()) ? ret.getWebDomain() : getWebDomainFromMapping(ret.getWebviewDomain(), domain);
    }

    private String getWebDomainFromMapping(String webView, String domain) {
        if (StringUtils.isBlank(webView)) {
            return domain;
        }
        String webUrlMapping = ConfigService.getConfig(APP_BIZ_CONFIG_NAMESPACE).getProperty(WEB_VIEW_URL_MAPPING, null);
        JSONObject json = JSONObject.parseObject(webUrlMapping);
        String webUrl = json.getString(webView);
        return StringUtils.isBlank(webUrl) ? domain : webUrl;
    }

    public static List<DomainTemplate.ApiDomainInfo> getApiDomainInfo() {
        String domainConfigJsonStr = ConfigService.getConfig(APP_OPS_CONFIG_NAMESPACE).getProperty(APOLLO_DOMAIN_KEY, null);
        if (StringUtils.isBlank(domainConfigJsonStr)) {
            log.error("getApiDomainInfo load domain failed. json String is empty");
            return Collections.emptyList();
        }
        DomainTemplate domainInfos = JsonUtils.toObj(domainConfigJsonStr, DomainTemplate.class);
        if (domainInfos == null || domainInfos.getDomains() == null || domainInfos.getDomains().getApiAllDomain() == null) {
            log.error("getApiDomainInfo load domain failed. result error json = {}", domainConfigJsonStr);
            return Collections.emptyList();
        }
        log.info("etApiDomainInfo load domain success. data = {}", JsonUtils.toJsonNotNullKey(domainInfos.getDomains().getApiAllDomain()));
        return domainInfos.getDomains().getApiAllDomain();
    }
}
