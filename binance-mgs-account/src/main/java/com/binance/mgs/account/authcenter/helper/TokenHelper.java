package com.binance.mgs.account.authcenter.helper;

import com.binance.account.api.UserApi;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.user.response.GetUserEmailResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.Md5Tools;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.mgs.account.authcenter.dto.TokenDto;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.component.domain.OpsDomainNamespaceConfig;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.utils.PKGenarator;
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
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TokenHelper extends BaseHelper {
    //code有效期30s
    private static final long CODE_TIMEOUT = 30;
    @Value("${subdomains.csrftoken.check.enable:true}")
    private boolean checkCsrfTokenEnable;
    @Value("${subdomains.whitelist.check.enable:true}")
    private boolean checkDomainWhiteList;
    @Autowired
    private OpsDomainNamespaceConfig opsDomainNamespaceConfig;
    @Resource
    private UserApi userApi;

    @Value("${callback.subdomains.whitelist.check.switch:false}")
    private boolean checkCallbackWhiteListSwitch;

    @Value("${callback.subdomains.whitelist.throw.exception.switch:false}")
    private boolean checkCallbackWhiteListThrowsExceptionSwitch;

    @Value("${callback.crlf.injection.throw.exception.switch:false}")
    private boolean checkCallbackCrlfInjectionThrowsExceptionSwitch;

    @Value("#{'${callback.crlf.injection.check.list:Set-Cookie,X-Frame-Options,X-XSS-Protection,X-Content-Type-Options,X-Download-Options,Referrer-Policy,Strict-Transport-Security,Content-Security-Policy}'.split(',')}")
    private List<String> callbackCrlfInjectionCheckList;

    @Value("${remove.request.csrf.token.switch:false}")
    private boolean removeRequestCsrfTokenSwitch;

    /**
     * 生成临时code
     * 
     * @return
     */
    public static String createCode(TokenDto token) {
        // code有效期30s
        String code = PKGenarator.getId();
        RedisCacheUtils.set(CacheKey.getTokenCode(code), token, CODE_TIMEOUT);
        return code;
    }

    public static TokenDto getToken(String code) {
        return RedisCacheUtils.get(CacheKey.getTokenCode(code), TokenDto.class);
    }
    public static void delCode(String code) {
        RedisCacheUtils.del(CacheKey.getTokenCode(code));
    }

    public boolean checkWhitelistSubDomain(HttpServletRequest request, HttpServletResponse response) throws MalformedURLException {
        if (!checkDomainWhiteList) {
            // 关闭则不校验，临时方案，上线后没问题删掉该开关
            return true;
        }
        StringBuffer requestURL = request.getRequestURL();
        String tempContextUrl = requestURL.delete(requestURL.length() - request.getRequestURI().length(), requestURL.length()).append("/").toString();
        URL url = new URL(tempContextUrl);
        String host = url.getHost();
        log.info("url host={}", host);
        int lastSecondDot = host.lastIndexOf(".", host.lastIndexOf(".") - 1);
        String domainPrefix = host.substring(0, lastSecondDot);
        String domainSuffix = host.substring(lastSecondDot + 1);
        if (!opsDomainNamespaceConfig.getWhitelistSubDomainPrefix().contains(domainPrefix)
                || !opsDomainNamespaceConfig.getWhitelistSubDomainSuffix().contains(domainSuffix)) {
            log.warn("illegal subDomain,requestURL={},domainPrefix={} domainSuffix={}", requestURL, domainPrefix, domainSuffix);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            throw new BusinessException(AccountMgsErrorCode.ILLEGAL_DOMAIN);
        }
        return true;
    }

    /**
     * 验证csrftokne
     *  @param request
     * @param response
     * @param tokenDto
     * @return
     */
    public void checkCsrfToken(HttpServletRequest request, HttpServletResponse response, TokenDto tokenDto) {
        String csrfToken = request.getHeader(Constant.HEADER_CSRFTOKEN);
        if (removeRequestCsrfTokenSwitch && StringUtils.isBlank(csrfToken)) {
            log.info("checkCsrfToken, csrfToken = {}", csrfToken);
            return;
        }
        if(StringUtils.isBlank(csrfToken)){
            // 前端从header或者参数中进行传递
            csrfToken = request.getParameter(Constant.HEADER_CSRFTOKEN);
        }
        String csrfTokenInCookie = getCookieValue(request,Constant.COOKIE_NEW_CSRFTOKEN);
        String csrfTokenInRedis = tokenDto.getCsrfToken();
        if (csrfToken == null || StringUtils.isEmpty(csrfTokenInCookie)
                || !csrfToken.equalsIgnoreCase(Md5Tools.MD5(csrfTokenInCookie))
        ||!csrfToken.equalsIgnoreCase(Md5Tools.MD5(csrfTokenInRedis))) {
            log.warn("csrftoken is invalid, csrfToken={},cookieCsrfToken={}",csrfToken,StringUtils.left(csrfTokenInCookie,12));
            if(checkCsrfTokenEnable) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
            }
        }
    }


    public String checkRefreshTokenAndGetEmail(String refreshToken) throws Exception{
        Long userId = ShardingRedisCacheUtils.get(refreshToken, Long.class, CacheConstant.ACCOUNT_REFRESH_TOKEN_KEY);
        if (null == userId) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<GetUserEmailResponse> emailApiResponse = userApi.getUserEmailByUserId(getInstance(userIdRequest));
        checkResponse(emailApiResponse);
        if (emailApiResponse.getData() == null||StringUtils.isBlank(emailApiResponse.getData().getEmail())) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        String email = emailApiResponse.getData().getEmail();
        return email;
    }

    public Long checkRefreshTokenAndGetUserId(String refreshToken) throws Exception{
        Long userId = ShardingRedisCacheUtils.get(refreshToken, Long.class, CacheConstant.ACCOUNT_REFRESH_TOKEN_KEY);
        if (null == userId) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        return userId;
    }

    public void checkCallback(String callback, HttpServletResponse response) {
        if (!checkCallbackWhiteListSwitch) {
            return;
        }

        try {
            String[] callbackArr = callback.split(",");
            if (callbackArr.length > 1) {
                log.warn("illegal callback, callbackArr = {}", Arrays.toString(callbackArr));
                if (checkCallbackWhiteListThrowsExceptionSwitch) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    throw new BusinessException(AccountMgsErrorCode.ILLEGAL_DOMAIN);
                }
            }

            URL url = new URL(callback);
            String host = url.getHost();
            log.info("callback host = {}", host);
            int lastSecondDot = host.lastIndexOf(".", host.lastIndexOf(".") - 1);
            String domainPrefix = host.substring(0, lastSecondDot);
            String domainSuffix = host.substring(lastSecondDot + 1);
            if (!opsDomainNamespaceConfig.getWhitelistSubDomainPrefix().contains(domainPrefix) || !opsDomainNamespaceConfig.getWhitelistSubDomainSuffix().contains(domainSuffix)) {
                log.warn("illegal subDomain, callback = {},domainPrefix = {} domainSuffix = {}", callback, domainPrefix, domainSuffix);
                if (checkCallbackWhiteListThrowsExceptionSwitch) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    throw new BusinessException(AccountMgsErrorCode.ILLEGAL_DOMAIN);
                }
            }
        } catch (Exception e) {
            log.error("checkCallback error: ", e);
            if (checkCallbackWhiteListThrowsExceptionSwitch) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                throw new BusinessException(AccountMgsErrorCode.ILLEGAL_DOMAIN);
            }
        }

        if (callback.contains("\n") || callback.contains("\r\n")) {
            for (String header : callbackCrlfInjectionCheckList) {
                if (StringUtils.containsIgnoreCase(callback, header)) {
                    log.info("header injection check, callback = {}", callback);
                    if (checkCallbackCrlfInjectionThrowsExceptionSwitch) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        throw new BusinessException(AccountMgsErrorCode.ILLEGAL_DOMAIN);
                    }
                }
            }
        }
    }
}
