package com.binance.mgs.account.authcenter.controller;

import com.binance.account.api.UserDeviceApi;
import com.binance.account.vo.device.request.FindMostSimilarUserDeviceRequest;
import com.binance.account.vo.device.response.FindMostSimilarUserDeviceResponse;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.authcenter.api.AuthApi;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.authcenter.vo.RefreshAccessTokenByRefreshTokenReq;
import com.binance.authcenter.vo.RefreshAccessTokenByRefreshTokenResp;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.authcenter.AuthCenterBaseAction;
import com.binance.mgs.account.authcenter.dto.TokenDto;
import com.binance.mgs.account.authcenter.helper.TokenHelper;
import com.binance.mgs.account.authcenter.vo.LoginCallbackArg;
import com.binance.mgs.account.authcenter.vo.RefreshAccessTokenMfaReq;
import com.binance.mgs.account.authcenter.vo.RefreshAccessTokenReq;
import com.binance.mgs.account.authcenter.vo.RefreshAccessTokenResp;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.service.RiskService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.utils.DomainUtils;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;

import static com.binance.mgs.account.constant.BizType.FORGET_PASSWORD;
import static com.binance.mgs.account.constant.BizType.REFRESH_ACCESS_TOKEN;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class TokenController extends AuthCenterBaseAction {
    @Value("${sub.domain.cookie:false}")
    private boolean needSubDomainCookie;
    @Resource
    private TokenHelper tokenHelper;

    @Resource
    private AuthApi authApi;

    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;

    @Autowired
    private UserDeviceApi userDeviceApi;

    @Resource
    private AccountHelper accountHelper;

    @Value("${force.check.access.token.switch:false}")
    private boolean forceCheckAccessTokenSwitch;
    
    @Resource
    private RiskService riskService;

    /**
     * 用于登录, 需要携带登录态再跳转回相应的域名对应的页面
     */
    @GetMapping(value = "/public/authcenter/callback")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken", "csrftoken"},
            forwardedCookies = {"p20t", "cr20", "cr00"}, specialQueryKeys = "csrftoken")
    @DDoSPreMonitor(action = "TokenController.callback")
    public CommonRet<String> callback(@Valid LoginCallbackArg loginCallbackArg, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        log.info("callback parameter = {}, origin url = {}", loginCallbackArg,  DomainUtils.getFullDomain());
        if (!needSubDomainCookie) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return ok();
        }
        // 根据code获取token
        TokenDto tokenDto = TokenHelper.getToken(loginCallbackArg.getCode());
        if (tokenDto != null) {
            // code使用完之后马上删掉
            TokenHelper.delCode(loginCallbackArg.getCode());
            // 校验子域名是否在白名单中
            tokenHelper.checkWhitelistSubDomain(request, response);
            // 验证csrftoken
            tokenHelper.checkCsrfToken(request,response,tokenDto);
            // 设置token
            BaseHelper.setCookie(request, response, false, Constant.COOKIE_TOKEN_V2, tokenDto.getToken());
        } else {
            // code已失效
            log.info("code = {} expire", loginCallbackArg.getCode());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        String callback =loginCallbackArg.getCallback();
        if(StringUtils.isNotBlank(callback)){
            // check callback.
            tokenHelper.checkCallback(callback, response);
            // 兜底方案,跳转到首页
            //callback = String.format("https://www.%s/%s/", DomainUtils.getTopDomain(request),baseHelper.getLanguage());
            response.sendRedirect(callback);
        }
        return ok();
    }

    /**
     * 已登录时, 使用该接口换取code后调用callback将登录态写入cookie
     */
    @PostMapping(value = "/private/authcenter/code")
    public CommonRet<String> getCode(HttpServletRequest request) throws Exception {
        // 生成临时code
        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(BaseHelper.getCookieValue(request, Constant.COOKIE_TOKEN_V2));
        tokenDto.setCsrfToken(BaseHelper.getCookieValue(request, Constant.COOKIE_NEW_CSRFTOKEN_V2));
        return ok(TokenHelper.createCode(tokenDto));
    }


    @PostMapping(value = "/transient/authcenter/refresh/accesstoken")
    @UserOperation(name = "续期登录态", logDeviceOperation = true, eventName = "refreshAccessToken", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "TokenController.refreshAccessToken")
    @AntiBotCaptchaValidate(bizType = {REFRESH_ACCESS_TOKEN})
    @AccountDefenseResource(name="TokenController.refreshAccessToken")
    public CommonRet<RefreshAccessTokenResp> refreshAccessToken(HttpServletRequest request, HttpServletResponse response,@Valid @RequestBody RefreshAccessTokenReq refreshAccessTokenReq) throws Exception {
        //检查人机验证
        accountHelper.verifyCodeCacheAndBanIp(refreshAccessTokenReq, FORGET_PASSWORD);


        //获取userid
        Long currentUserId = getUserId();
        if (null != currentUserId && forceCheckAccessTokenSwitch) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }

        String oldRefreshToken=refreshAccessTokenReq.getRefreshToken();
        Long userId = ShardingRedisCacheUtils.get(oldRefreshToken, Long.class, CacheConstant.ACCOUNT_REFRESH_TOKEN_KEY);
        if (null == userId) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));

        String fvideoId=request.getHeader(LocalLogKeys.FVIDEO_ID);
        if(org.apache.commons.lang3.StringUtils.isBlank(fvideoId)){
            fvideoId=request.getHeader(LocalLogKeys.FVIDEO_ID_APP);
        }
        if(StringUtils.isBlank(fvideoId)){
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        if(!baseHelper.isIOS() && !baseHelper.isAndroid()){
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        //check设备
        Map<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getUserEmail());
        // EX-24827 记录设备信息到user-operation-log
        UserOperationHelper.log("device-info", deviceInfo);
        
        FindMostSimilarUserDeviceRequest findMostSimilarUserDeviceRequest = new FindMostSimilarUserDeviceRequest();
        findMostSimilarUserDeviceRequest.setAgentType(baseHelper.getClientType());
        findMostSimilarUserDeviceRequest.setContent(deviceInfo);
        findMostSimilarUserDeviceRequest.setUserId(userId);
        findMostSimilarUserDeviceRequest.setFvideoId(fvideoId);
        APIResponse<FindMostSimilarUserDeviceResponse> apiResponse = userDeviceApi.findMostSimilarUserDevice(getInstance(findMostSimilarUserDeviceRequest));
        checkResponse(apiResponse);


        FindMostSimilarUserDeviceResponse findMostSimilarUserDeviceResponse=apiResponse.getData();
        if(!findMostSimilarUserDeviceResponse.isSame()){
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }


        RefreshAccessTokenByRefreshTokenReq refreshAccessTokenByRefreshTokenReq = new RefreshAccessTokenByRefreshTokenReq();
        refreshAccessTokenByRefreshTokenReq.setRefreshToken(refreshAccessTokenReq.getRefreshToken());
        refreshAccessTokenByRefreshTokenReq.setClientType(baseHelper.getClientType());
        refreshAccessTokenByRefreshTokenReq.setEmailVerifyCode(refreshAccessTokenReq.getEmailVerifyCode());
        refreshAccessTokenByRefreshTokenReq.setMobileVerifyCode(refreshAccessTokenReq.getMobileVerifyCode());
        refreshAccessTokenByRefreshTokenReq.setGoogleVerifyCode(refreshAccessTokenReq.getGoogleVerifyCode());
        refreshAccessTokenByRefreshTokenReq.setYubikeyVerifyCode(refreshAccessTokenReq.getYubikeyVerifyCode());
        refreshAccessTokenByRefreshTokenReq.setFidoVerifyCode(refreshAccessTokenReq.getFidoVerifyCode());
        refreshAccessTokenByRefreshTokenReq.setVerifyToken(refreshAccessTokenReq.getVerifyToken());

        APIResponse<RefreshAccessTokenByRefreshTokenResp> refreshAccessTokenByRefreshTokenRespAPIResponse = authApi.refreshAccessTokenByRefreshToken(getInstance(refreshAccessTokenByRefreshTokenReq));
        checkResponse(refreshAccessTokenByRefreshTokenRespAPIResponse);

        RefreshAccessTokenResp refreshAccessTokenResp=new RefreshAccessTokenResp();
        refreshAccessTokenResp.setToken(refreshAccessTokenByRefreshTokenRespAPIResponse.getData().getToken());
        refreshAccessTokenResp.setCsrfToken(refreshAccessTokenByRefreshTokenRespAPIResponse.getData().getCsrfToken());
        refreshAccessTokenResp.setRefreshToken(refreshAccessTokenByRefreshTokenRespAPIResponse.getData().getRefreshToken());
        refreshAccessTokenResp.setCode(null);

        CommonRet<RefreshAccessTokenResp> ret = new CommonRet<>();
        ret.setData(refreshAccessTokenResp);
        return ret;
    }

    @PostMapping(value = "/transient/authcenter/refresh/accesstokenWithMfa")
    @UserOperation(name = "续期登录态", logDeviceOperation = true, eventName = "refreshAccessToken", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "TokenController.refreshAccessTokenMfa")
    @AccountDefenseResource(name="TokenController.refreshAccessToken")
    public CommonRet<RefreshAccessTokenResp> refreshAccessTokenMfa(HttpServletRequest request, HttpServletResponse response,@Valid @RequestBody RefreshAccessTokenMfaReq refreshAccessTokenReq) throws Exception {
        
        //获取userid
        Long currentUserId = getUserId();
        if (null != currentUserId && forceCheckAccessTokenSwitch) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }

        String oldRefreshToken=refreshAccessTokenReq.getRefreshToken();
        Long userId = ShardingRedisCacheUtils.get(oldRefreshToken, Long.class, CacheConstant.ACCOUNT_REFRESH_TOKEN_KEY);
        if (null == userId) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
        
        String fvideoId=request.getHeader(LocalLogKeys.FVIDEO_ID);
        if(org.apache.commons.lang3.StringUtils.isBlank(fvideoId)){
            fvideoId=request.getHeader(LocalLogKeys.FVIDEO_ID_APP);
        }
        if(StringUtils.isBlank(fvideoId)){
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        if(!baseHelper.isIOS() && !baseHelper.isAndroid()){
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        //check设备
        Map<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getUserEmail());
        
        // mfa流程，调用风控，异步超时流程
        riskService.getRiskChallengeTimeOut(userId, deviceInfo, BizSceneEnum.REFRESH_ACCESSTOKEN.name());
        
        // EX-24827 记录设备信息到user-operation-log
        UserOperationHelper.log("device-info", deviceInfo);

        FindMostSimilarUserDeviceRequest findMostSimilarUserDeviceRequest = new FindMostSimilarUserDeviceRequest();
        findMostSimilarUserDeviceRequest.setAgentType(baseHelper.getClientType());
        findMostSimilarUserDeviceRequest.setContent(deviceInfo);
        findMostSimilarUserDeviceRequest.setUserId(userId);
        findMostSimilarUserDeviceRequest.setFvideoId(fvideoId);
        APIResponse<FindMostSimilarUserDeviceResponse> apiResponse = userDeviceApi.findMostSimilarUserDevice(getInstance(findMostSimilarUserDeviceRequest));
        checkResponse(apiResponse);


        FindMostSimilarUserDeviceResponse findMostSimilarUserDeviceResponse=apiResponse.getData();
        if(!findMostSimilarUserDeviceResponse.isSame()){
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }

        RefreshAccessTokenByRefreshTokenReq refreshAccessTokenByRefreshTokenReq = new RefreshAccessTokenByRefreshTokenReq();
        refreshAccessTokenByRefreshTokenReq.setRefreshToken(refreshAccessTokenReq.getRefreshToken());
        refreshAccessTokenByRefreshTokenReq.setClientType(baseHelper.getClientType());
        refreshAccessTokenByRefreshTokenReq.setAlreadyCheckMFA(true);

        APIResponse<RefreshAccessTokenByRefreshTokenResp> refreshAccessTokenByRefreshTokenRespAPIResponse = authApi.refreshAccessTokenByRefreshToken(getInstance(refreshAccessTokenByRefreshTokenReq));
        checkResponse(refreshAccessTokenByRefreshTokenRespAPIResponse);

        RefreshAccessTokenResp refreshAccessTokenResp=new RefreshAccessTokenResp();
        refreshAccessTokenResp.setToken(refreshAccessTokenByRefreshTokenRespAPIResponse.getData().getToken());
        refreshAccessTokenResp.setCsrfToken(refreshAccessTokenByRefreshTokenRespAPIResponse.getData().getCsrfToken());
        refreshAccessTokenResp.setRefreshToken(refreshAccessTokenByRefreshTokenRespAPIResponse.getData().getRefreshToken());
        refreshAccessTokenResp.setCode(null);

        CommonRet<RefreshAccessTokenResp> ret = new CommonRet<>();
        ret.setData(refreshAccessTokenResp);
        return ret;
    }
}
