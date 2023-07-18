package com.binance.mgs.account.oauth.helper;

import com.binance.master.utils.RedisCacheUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.constant.CacheKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Component
@Slf4j
public class OauthHelper extends BaseHelper {
    private static final long OAUTH_STATE_TIMEOUT = 10 * 60L;
    @Value("${oauth.redirect.url:http://localhost:9024/v1/public/oauth/callback}")
    private String oauthRedirectUrl;
    @Value("${oauth.login.url:https://account.devfdg.net/en/login}")
    private String oauthLoginUrl;
    @Value("${oauth.login.url.cn:https://account.binancezh.com/en/login}")
    private String oauthLoginUrlCn;
    @Value("${oauth.client.scope:user:email,user:address}")
    private String oauthClientScope;
    @Value("${oauth.client.id:ug-client}")
    private String clientId;

    /*
     * Save state to redis, key = value = state
     */
    public static void saveState(String state, String callback) {
        RedisCacheUtils.set(CacheKey.getOauthState(state), String.format("%s:%s", state, callback),
                OAUTH_STATE_TIMEOUT);
    }


    /*
     * http://localhost:9050/oauth/authorize?client_id=unity-client&redirect_uri=http%3a%2f%2flocalhost%
     * 3a8080%2funity%2fdashboard.htm&response_type=code&scope=read
     */
    public String getAuthorizeCodeUri(String state, String lang, String region) throws UnsupportedEncodingException {
        log.debug("getAuthorizeCodeUri input param [state : {} lang : {} region : {}]", state, lang, region);
        String redirect = URLEncoder.encode(oauthRedirectUrl, "UTF-8");
        String baseUrl = oauthLoginUrl;
        if (StringUtils.equalsIgnoreCase("cn", region)) {
            // 中国区域名
            baseUrl = oauthLoginUrlCn;
        }
        return String.format("%s/%s/oauth/authorize?response_type=code&scope=%s&client_id=%s&redirect_uri=%s&state=%s",
                baseUrl, lang, oauthClientScope, clientId, redirect, state);
    }

}
