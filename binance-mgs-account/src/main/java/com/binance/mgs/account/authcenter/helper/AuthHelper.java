package com.binance.mgs.account.authcenter.helper;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserSecurityResetApi;
import com.binance.account.vo.reset.request.ResetPendingArg;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.request.UserIpRequest;
import com.binance.account2fa.api.User2FaApi;
import com.binance.account2fa.vo.request.InvalidatePasswordlessloginReq;
import com.binance.account2fa.vo.request.InvalidatePasswordlessloginResp;
import com.binance.authcenter.api.AuthApi;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.authcenter.vo.*;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.Md5Tools;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.version.VersionHelper;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.authcenter.dto.LoginContextDto;
import com.binance.mgs.account.authcenter.vo.LoginV3Arg;
import com.binance.mgs.account.authcenter.vo.LoginVerifyMfaArg;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import com.binance.mgs.account.authcenter.dto.HumanRecognitionMessage;
import com.binance.mgs.account.authcenter.dto.TokenDto;
import com.binance.mgs.account.authcenter.vo.LoginArg;
import com.binance.mgs.account.authcenter.vo.LoginV2Arg;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.utils.VersionUtil;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.binance.mgs.account.constant.BizType.LOGIN;

@Component
@Slf4j
public class AuthHelper extends BaseHelper {
    @Value("${human.recognition.bigdata.kafka.producer.topic:}")
    private String humanRecognitionTopic;

    @Value("${sub.domain.cookie:false}")
    private boolean needSubDomainCookie;
    @Value("${top.domain.cookie:true}")
    private boolean needTopDomainCookie;

    @Value("${login.banned.ios.version:2.11.0}")
    private String iosLoginBannedVersion;
    @Value("${login.banned.android.version:1.20.0}")
    private String androidLoginBannedVersion;

    @Resource
    private AuthApi authApi;
    @Resource
    @Lazy
    private AccountHelper accountHelper;
    @Resource
    private UserApi userApi;
    @Resource
    private UserSecurityResetApi userSecurityResetApi;
    @Resource
    private GeeTestHelper geeTestHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;

    @Resource(name = "bigDataKafkaTemplate")
    private KafkaTemplate bigDataKafkaTemplate;


    @Value("${need.force.check.gt.switch:true}")
    private boolean needForceCheckGtSwitch;


    @Autowired
    private User2FaApi user2FaApi;


    @Value("${enable.check.invalidate.passwordlesslogin.switch:true}")
    private boolean enableCheckInvalidatePasswordlessLogin;



    /**
     * 验证token是否有效 csrftoken不为null说明时从web端过来的，需要验证有效性
     *
     * @param request
     * @param response
     * @param token
     * @param csrfToken
     * @throws Exception
     */
    public void checkToken(HttpServletRequest request, HttpServletResponse response, String token, String csrfToken) throws Exception {
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        AuthRequest authRequest = new AuthRequest();
        authRequest.setToken(token);
        APIResponse<AuthResponse> apiResponse = authApi.auth(getInstance(authRequest));
        checkResponse(apiResponse);
        AuthResponse authResponse = apiResponse.getData();
        if (authResponse != null && StringUtils.isNotEmpty(authResponse.getUserId())) {
            // userId不为空说明token有效，若csrftoken不为空说明是web端过来，需再次验证csrftoken是否有效
            if (csrfToken == null || (csrfToken != null && isValidCsrfToken(request, authResponse, csrfToken))) {
                return;
            }
        }
        throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
    }

    private boolean isValidCsrfToken(HttpServletRequest request, AuthResponse authResponse, String csrfToken) {
        if (csrfToken != null && StringUtils.isNotEmpty(authResponse.getCsrfToken())
                && csrfToken.equalsIgnoreCase(Md5Tools.MD5(authResponse.getCsrfToken()))) {
            return true;
        }
        log.info("token={}, csrfToken expected: {}, given: {}", StringUtils.left(authResponse.getToken(), 12),
                Md5Tools.MD5(authResponse.getCsrfToken()), csrfToken);
        return false;
    }

    /**
     * 登录
     *
     * @param request
     * @param loginArg
     * @return
     * @throws Exception
     */
    public LoginResponse login(HttpServletRequest request, LoginArg loginArg,boolean checkedVerifyCode) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(loginArg.getEmail());
        loginRequest.setPassword(loginArg.getPassword());
        loginRequest.setSafePassword(loginArg.getSafePassword());
        loginRequest.setAuthType(loginArg.getAuthType());
        loginRequest.setCode(loginArg.getVerifyCode());
        loginRequest.setVerifyCode(loginArg.getVerifyCode());
        loginRequest.setValidateCodeType(loginArg.getValidateCodeType());
        loginRequest.setVersionCode(VersionHelper.getVersion(request));
        loginRequest.setClientType(getClientType(request));
        loginRequest.setIp(WebUtils.getRequestIp());
        loginRequest.setGeetestChallenge(loginArg.getGeetestChallenge());
        loginRequest.setGeetestSeccode(loginArg.getGeetestSecCode());
        loginRequest.setGeetestValidate(loginArg.getGeetestValidate());
        loginRequest.setSerialNo(getCookieValue(request, Constant.COOKIE_SERIAL_NO));
        loginRequest.setCallback(request.getHeader("Referer"));
        if (send2NewPage()) {
            // 自定义发送到邮箱中的链接
            String customDeviceAuthorizeUrl =
                    String.format("%sgateway-api/v1/public/account/user-device/authorize?userId={userId}&code={code}", getBaseUrl());
            loginRequest.setCustomDeviceAuthorizeUrl(customDeviceAuthorizeUrl);
        }
        if (loginArg.getAuthType() == null) {
            loginRequest.setCheckedVerifyCode(checkedVerifyCode);
        } else {
            loginRequest.setOperationType(loginArg.getAuthType().getCode());
        }
        loginRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, loginArg.getEmail()));
        if (loginRequest.getDeviceInfo() != null) {
            String bncUuid = loginRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }
        if (isFromWeb()) {
            loginRequest.setToken(getTokenFromCookie());
            loginRequest.setCsrfToken(request.getHeader(Constant.HEADER_CSRFTOKEN));
        } else {
            loginRequest.setToken(request.getHeader(Constant.HEADER_TOKEN));
            loginRequest.setIsTerminal("true");
        }
        loginRequest.setIsNewLoginProcess(loginArg.getIsNewLoginProcess());
        APIResponse<LoginResponse> apiResponse = authApi.login(getInstance(loginRequest));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }


    /**
     * 登录V2
     *
     * @param request
     * @param loginArg
     * @return
     * @throws Exception
     */
    public LoginResponseV2 loginV2(HttpServletRequest request, LoginV2Arg loginArg,boolean checkedVerifyCode) throws Exception {

        invalidatePasswordlesslogin(request,loginArg.getEmail(),loginArg);
        LoginRequestV2 loginRequest = new LoginRequestV2();
        loginRequest.setEmail(loginArg.getEmail());
        loginRequest.setMobile(loginArg.getMobile());
        loginRequest.setMobileCode(loginArg.getMobileCode());
        loginRequest.setPassword(loginArg.getPassword());
        loginRequest.setSafePassword(loginArg.getSafePassword());
        loginRequest.setValidateCodeType(loginArg.getValidateCodeType());
        loginRequest.setVersionCode(VersionHelper.getVersion(request));
        loginRequest.setClientType(getClientType(request));
        loginRequest.setIp(WebUtils.getRequestIp());
        loginRequest.setGeetestChallenge(loginArg.getGeetestChallenge());
        loginRequest.setGeetestSeccode(loginArg.getGeetestSecCode());
        loginRequest.setGeetestValidate(loginArg.getGeetestValidate());
        loginRequest.setSerialNo(getCookieValue(request, Constant.COOKIE_SERIAL_NO));
        loginRequest.setCallback(request.getHeader("Referer"));
        loginRequest.setEmailVerifyCode(loginArg.getEmailVerifyCode());
        loginRequest.setMobileVerifyCode(loginArg.getMobileVerifyCode());
        loginRequest.setGoogleVerifyCode(loginArg.getGoogleVerifyCode());
        loginRequest.setYubikeyVerifyCode(loginArg.getYubikeyVerifyCode());
        loginRequest.setVerifyToken(loginArg.getVerifyToken());
        if (send2NewPage()) {
            // 自定义发送到邮箱中的链接
            String customDeviceAuthorizeUrl =
                    String.format("%sgateway-api/v1/public/account/user-device/authorize?userId={userId}&code={code}", getBaseUrl());
            loginRequest.setCustomDeviceAuthorizeUrl(customDeviceAuthorizeUrl);
        }
        // 验证通过之后设置标记
        loginRequest.setCheckedVerifyCode(checkedVerifyCode);
        loginRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, loginArg.getEmail()));
        loginRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        if (loginRequest.getDeviceInfo() != null) {
            UserOperationHelper.log("device-info", loginRequest.getDeviceInfo());
            String bncUuid = loginRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }
        if (isFromWeb()) {
            loginRequest.setToken(getTokenFromCookie());
            loginRequest.setCsrfToken(request.getHeader(Constant.HEADER_CSRFTOKEN));
        } else {
            loginRequest.setToken(request.getHeader(Constant.HEADER_TOKEN));
            loginRequest.setIsTerminal("true");
        }
        loginRequest.setIsNewLoginProcess(loginArg.getIsNewLoginProcess());
        loginRequest.setIsMobileUserVersion(true);
        APIResponse<LoginResponseV2> apiResponse = authApi.loginV2(getInstance(loginRequest));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    /**
     * 登录V2
     *
     * @param request
     * @param loginArg
     * @return
     * @throws Exception
     */
    public LoginResponseV3 loginV3(HttpServletRequest request, LoginV3Arg loginArg, boolean checkedVerifyCode) throws Exception {

        invalidatePasswordlesslogin(request,loginArg.getEmail(),loginArg);
        LoginRequestV3 loginRequest = new LoginRequestV3();
        loginRequest.setEmail(loginArg.getEmail());
        loginRequest.setMobile(loginArg.getMobile());
        loginRequest.setMobileCode(loginArg.getMobileCode());
        loginRequest.setPassword(loginArg.getPassword());
        loginRequest.setSafePassword(loginArg.getSafePassword());
        loginRequest.setFidoVerifyCode(loginArg.getFidoVerifyCode());
        loginRequest.setValidateCodeType(loginArg.getValidateCodeType());
        loginRequest.setVersionCode(VersionHelper.getVersion(request));
        loginRequest.setClientType(getClientType(request));
        loginRequest.setIp(WebUtils.getRequestIp());
        loginRequest.setGeetestChallenge(loginArg.getGeetestChallenge());
        loginRequest.setGeetestSeccode(loginArg.getGeetestSecCode());
        loginRequest.setGeetestValidate(loginArg.getGeetestValidate());
        loginRequest.setSerialNo(getCookieValue(request, Constant.COOKIE_SERIAL_NO));
        loginRequest.setCallback(request.getHeader("Referer"));
//        loginRequest.setEmailVerifyCode(loginArg.getEmailVerifyCode());
//        loginRequest.setMobileVerifyCode(loginArg.getMobileVerifyCode());
//        loginRequest.setGoogleVerifyCode(loginArg.getGoogleVerifyCode());
//        loginRequest.setYubikeyVerifyCode(loginArg.getYubikeyVerifyCode());
//        loginRequest.setVerifyToken(loginArg.getVerifyToken());
        if (send2NewPage()) {
            // 自定义发送到邮箱中的链接
            String customDeviceAuthorizeUrl =
                    String.format("%sgateway-api/v1/public/account/user-device/authorize?userId={userId}&code={code}", getBaseUrl());
            loginRequest.setCustomDeviceAuthorizeUrl(customDeviceAuthorizeUrl);
        }
        // 验证通过之后设置标记
        loginRequest.setCheckedVerifyCode(checkedVerifyCode);
        loginRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, loginArg.getEmail()));
        loginRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        if (loginRequest.getDeviceInfo() != null) {
            UserOperationHelper.log("device-info", loginRequest.getDeviceInfo());
            String bncUuid = loginRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }
        if (isFromWeb()) {
            loginRequest.setToken(getTokenFromCookie());
            loginRequest.setCsrfToken(request.getHeader(Constant.HEADER_CSRFTOKEN));
        } else {
            loginRequest.setToken(request.getHeader(Constant.HEADER_TOKEN));
            loginRequest.setIsTerminal("true");
        }
        loginRequest.setIsNewLoginProcess(loginArg.getIsNewLoginProcess());
        loginRequest.setIsMobileUserVersion(true);
        APIResponse<LoginResponseV3> apiResponse = authApi.loginV3(getInstance(loginRequest));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    /**
     * loginVerifyMfa
     *
     * @param request
     * @param loginArg
     * @return
     * @throws Exception
     */
    public LoginResponseV3 loginVerifyMfa(HttpServletRequest request, LoginVerifyMfaArg loginArg, Long userId, boolean newDeviceFlag) throws Exception {

//        invalidatePasswordlesslogin(request,loginArg.getEmail(),loginArg);
        LoginVerifyMfaRequest loginRequest = new LoginVerifyMfaRequest();
        loginRequest.setUserId(userId);
//        loginRequest.setEmail(loginArg.getEmail());
//        loginRequest.setMobile(loginArg.getMobile());
//        loginRequest.setMobileCode(loginArg.getMobileCode());
//        loginRequest.setPassword(loginArg.getPassword());
//        loginRequest.setSafePassword(loginArg.getSafePassword());
//        loginRequest.setValidateCodeType(loginArg.getValidateCodeType());
        loginRequest.setVersionCode(VersionHelper.getVersion(request));
        loginRequest.setClientType(getClientType(request));
        loginRequest.setIp(WebUtils.getRequestIp());
//        loginRequest.setGeetestChallenge(loginArg.getGeetestChallenge());
//        loginRequest.setGeetestSeccode(loginArg.getGeetestSecCode());
//        loginRequest.setGeetestValidate(loginArg.getGeetestValidate());
        loginRequest.setSerialNo(getCookieValue(request, Constant.COOKIE_SERIAL_NO));
        loginRequest.setCallback(request.getHeader("Referer"));
//        loginRequest.setEmailVerifyCode(loginArg.getEmailVerifyCode());
//        loginRequest.setMobileVerifyCode(loginArg.getMobileVerifyCode());
//        loginRequest.setGoogleVerifyCode(loginArg.getGoogleVerifyCode());
//        loginRequest.setYubikeyVerifyCode(loginArg.getYubikeyVerifyCode());
//        loginRequest.setVerifyToken(loginArg.getVerifyToken());
        loginRequest.setAlreadyCheckMFA(true);
        
        if (send2NewPage()) {
            // 自定义发送到邮箱中的链接
            String customDeviceAuthorizeUrl =
                    String.format("%sgateway-api/v1/public/account/user-device/authorize?userId={userId}&code={code}", getBaseUrl());
            loginRequest.setCustomDeviceAuthorizeUrl(customDeviceAuthorizeUrl);
        }
//        // 验证通过之后设置标记
//        loginRequest.setCheckedVerifyCode(checkedVerifyCode);
        loginRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, getUserEmail()));
        loginRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        if (loginRequest.getDeviceInfo() != null) {
            UserOperationHelper.log("device-info", loginRequest.getDeviceInfo());
            String bncUuid = loginRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }
        if (isFromWeb()) {
            loginRequest.setToken(getTokenFromCookie());
            loginRequest.setCsrfToken(request.getHeader(Constant.HEADER_CSRFTOKEN));
        } else {
            loginRequest.setToken(request.getHeader(Constant.HEADER_TOKEN));
            loginRequest.setIsTerminal("true");
        }
//        loginRequest.setIsNewLoginProcess(loginArg.getIsNewLoginProcess());
        loginRequest.setIsMobileUserVersion(true);
        APIResponse<LoginResponseV3> apiResponse = authApi.loginVerifyMfa(getInstance(loginRequest));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }


    /**
     * 创建无意义的cookievalue
     *
     * @param token
     * @return
     */
    public String getDummyCookieValue(Object token) {
        String md5 = Md5Tools.MD5(UUID.randomUUID().toString());
        try {
            String tokenValue = (String) token;
            if (StringUtils.isNotEmpty(tokenValue)) {
                return StringUtils.substring(tokenValue, 0, tokenValue.lastIndexOf(".") + 1) + md5;
            }
        } catch (Exception e) {
            log.error("create dummy cookie error,token={}", token, e);
        }
        return md5;
    }
    @Async("simpleRequestAsync")
    public void loginWithIpNew(PnkLoginDto loginDto,String requestIp) {
        try {
            log.info("loginWithIpNew,userId={}", loginDto.getUserId());
            UserIpRequest request = new UserIpRequest();
            request.setIp(requestIp);
            request.setUserId(Long.valueOf(loginDto.getUserId()));
            userApi.saveUserIp(getInstance(request));
        } catch (Exception e) {
            log.warn("loginWithIpNew error", e);
        }
    }

    /**
     * 登出
     *
     * @param request
     */
    public void logout(HttpServletRequest request) {
        try {
            deleteAllCookiesWhileLogout();
            LogoutRequest logoutRequest = new LogoutRequest();
            logoutRequest.setToken(getToken());
            logoutRequest.setClientType(getClientType(request));
            APIResponse<LogoutResponse> apiResponse = authApi.logout(getInstance(logoutRequest));
            log.info("logout:{}", apiResponse);
        } catch (Exception e) {
            log.warn("logout fail", e);
        }
    }

    public void loginWithIpNew(String userId) {
        try {
            UserIpRequest request = new UserIpRequest();
            request.setIp(WebUtils.getRequestIp());
            request.setUserId(Long.valueOf(userId));
            userApi.saveUserIp(getInstance(request));
        } catch (Exception e) {
            log.warn("loginWithIpNew error", e);
        }
    }

    /**
     * 登出指定设备类型
     *
     * @param clientType
     */
    public void logoutByClientType(String clientType) {
        try {
            if (StringUtils.equalsAnyIgnoreCase(clientType, "web", "h5")) {
                deleteAllCookiesWhileLogout();
            }
            LogoutRequest logoutRequest = new LogoutRequest();
            logoutRequest.setUserId(getUserIdStr());
            logoutRequest.setClientType(clientType);
            APIResponse<LogoutResponse> apiResponse = authApi.logout(getInstance(logoutRequest));
            log.info("logout:{}", apiResponse);
        } catch (Exception e) {
            log.warn("logout fail", e);
        }
    }


    /**
     * 将某个指定的userid
     *
     * @param request
     */
    public void logoutForUserId(HttpServletRequest request, String userId) {
        try {
            deleteAllCookiesWhileLogout();
            LogoutRequest logoutRequest = new LogoutRequest();
            logoutRequest.setUserId(userId);
            logoutRequest.setClientType(getClientType(request));
            APIResponse<LogoutResponse> apiResponse = authApi.logout(getInstance(logoutRequest));
            log.info("logoutForUserId:  userId={},resp={}", userId, apiResponse);
        } catch (Exception e) {
            log.warn(String.format("logoutForUserId userId=%s fail", userId), e);
        }
    }

    /**
     * 退出所有终端
     *
     * @param userId
     */
    public void logoutAll(Long userId) {
        try {
            UserIdRequest userIdRequest = new UserIdRequest();
            userIdRequest.setUserId(userId);
            APIResponse<LogoutResponse> apiResponse = authApi.logoutAll(getInstance(userIdRequest));
            log.info("logoutAll:{}", apiResponse);
            deleteAllCookiesWhileLogout();
        } catch (Exception e) {
            log.warn("logoutAll fail", e);
        }
    }

    /**
     * 退出所有终端
     *
     * @param userId
     */
    public void logoutAllWithoutDeleteCookies(Long userId) {
        try {
            UserIdRequest userIdRequest = new UserIdRequest();
            userIdRequest.setUserId(userId);
            APIResponse<LogoutResponse> apiResponse = authApi.logoutAll(getInstance(userIdRequest));
            log.info("logoutAllWithoutDeleteCookies:{}", apiResponse);
        } catch (Exception e) {
            log.warn("logoutAllWithoutDeleteCookies fail", e);
        }
    }

    /***
     * 客户端扫码确认后，生成新的token下发
     *
     * @param clientType 生成qrcode的clientType
     */
    public CreateQrTokenResponse createQrToken(String clientType) {
        // 客户端固定从header中获取即可
        String token = WebUtils.getHttpServletRequest().getHeader(Constant.HEADER_TOKEN);
        CreateQrTokenRequest request = new CreateQrTokenRequest();
        request.setClientType(clientType);
        request.setToken(token);
        APIResponse<CreateQrTokenResponse> apiResponse = authApi.createQrToken(getInstance(request));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    /***
     * 根据设备临时授权码生成新的token下发
     *
     */
    public CreateDeviceTokenResponse createByDeviceToken() {
        CreateDeviceTokenRequest request = new CreateDeviceTokenRequest();
        request.setDeviceToken(getCookieValue(Constant.COOKIE_DEVICE_AUTH_CODE));
        APIResponse<CreateDeviceTokenResponse> apiResponse = authApi.createByDeviceToken(getInstance(request));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    public List<QrCodeLoginResponse> getQrCodeLoginList() {
        UserIdRequest request = new UserIdRequest();
        request.setUserId(getUserId());
        APIResponse<List<QrCodeLoginResponse>> apiResponse = authApi.getQrCodeLoginList(getInstance(request));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    /**
     * @param clientType 要删除的token设备类型
     */
    public void logoutQrCodeToken(String clientType) {
        LogoutQrCodeTokenRequest request = new LogoutQrCodeTokenRequest();
        request.setClientType(clientType);
        request.setUserId(getUserIdStr());
        APIResponse<Void> apiResponse = authApi.logoutQrCodeToken(getInstance(request));
        if (!isOk(apiResponse)) {
            log.warn("logoutQrCodeToken error:{}", JSON.toJSONString(apiResponse));
        }
    }

    /**
     * 判断重置流程是否正在审核中
     *
     * @param userId
     * @return
     */
    public boolean securityResetIsPending(Long userId) {
        ResetPendingArg resetPendingArgAPIRequest = new ResetPendingArg();
        resetPendingArgAPIRequest.setType(ResetPendingArg.ResetType.ENABLE);
        resetPendingArgAPIRequest.setUserId(userId);
        APIResponse<Boolean> apiResponse = userSecurityResetApi.securityResetIsPending(getInstance(resetPendingArgAPIRequest));
        if (apiResponse.getData() == null) {
            return false;
        } else {
            return apiResponse.getData();
        }
    }

    @Async("simpleRequestAsync")
    public void sendHumanRecognitionTopic(String userId, String clientId) {
        try {
            HumanRecognitionMessage humanRecognitionMessage = new HumanRecognitionMessage();
            humanRecognitionMessage.setClientId(clientId);
            humanRecognitionMessage.setUserId(userId);
            String message = JSON.toJSONString(humanRecognitionMessage);
            if (StringUtils.isNotBlank(humanRecognitionTopic)) {
                if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(clientId)) {
                    log.info("发送消息到人机识别系统,message={}", message);
                    bigDataKafkaTemplate.send(humanRecognitionTopic, message);
                } else {
                    log.warn("发送消息到人机识别系统失败，userId={},clientId={}", userId, clientId);
                }
            }
        } catch (Exception e) {
            log.warn("发送消息到人机识别系统失败,userId={}", userId, e);
        }
    }

    public String setAuthCookie(HttpServletRequest request, HttpServletResponse response, String token, String csrfToken) {
        // web端种cookie
        if (needTopDomainCookie) {
            BaseHelper.setCookie(request, response, true, Constant.COOKIE_TOKEN, token);
        }
        // cr00只写根域名
        BaseHelper.setCookie(request, response, true, Constant.COOKIE_NEW_CSRFTOKEN, csrfToken, false);
        // set dummy cookie
        BaseHelper.setCookie(request, response, true, Constant.COOKIE_MIX_TOKEN1, getDummyCookieValue(token));
        BaseHelper.setCookie(request, response, true, Constant.COOKIE_MIX_TOKEN2, getDummyCookieValue(token));
        BaseHelper.setCookie(request, response, true, Constant.COOKIE_MIX_TOKEN3, getDummyCookieValue(token));

        if (needSubDomainCookie) {
            BaseHelper.setCookie(request, response, false, Constant.COOKIE_TOKEN_V2, token);
//            BaseHelper.setCookie(request, response, false, Constant.COOKIE_NEW_CSRFTOKEN_V2, csrfToken, false);
            // 生成临时code
            TokenDto tokenDto = TokenDto.builder().token(token).csrfToken(csrfToken).build();
            return TokenHelper.createCode(tokenDto);
        }
        return null;
    }

    public String setRegisterTokenCookie(HttpServletRequest request, HttpServletResponse response, String registerToken,int maxAge) {
        // web端种cookie
        if (needTopDomainCookie) {
//            BaseHelper.setCookie(request, response, true, Constant.COOKIE_TOKEN, token);
              BaseHelper.setCookie(request, response, true, BinanceMgsAccountConstant.BINANCE_MGS_ACCOUNT_REGISTER_TOKEN, registerToken,maxAge,true);
        }
        // cr00只写根域名

        if (needSubDomainCookie) {
//            BaseHelper.setCookie(request, response, false, Constant.COOKIE_TOKEN_V2, token);
            BaseHelper.setCookie(request, response, false, BinanceMgsAccountConstant.BINANCE_MGS_ACCOUNT_REGISTER_TOKEN, registerToken,maxAge,true);
        }
        return null;
    }

    public String getRegisterTokenCookie() {
        return BaseHelper.getCookieValue(BinanceMgsAccountConstant.BINANCE_MGS_ACCOUNT_REGISTER_TOKEN);
    }


    public Long getUserIdByUserEmail() throws Exception {
        String email = getUserEmail();
        if (StringUtils.isBlank(email)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!isOk(emailApiResponse)) {
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            checkResponse(emailApiResponse);
        }
        return emailApiResponse.getData();
    }


    public String getUserEmailFromHeader() throws Exception {
        String email = getUserEmail();
        if (StringUtils.isBlank(email)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        return email;
    }

    public Map<String, LoginDeviceResponse> getValidLoginDevice() {
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(getUserId());
        Map<String, LoginDeviceResponse> map = new HashMap<>();
        APIResponse<List<LoginDeviceResponse>> apiResponse = authApi.listAllLoginDevice(getInstance(userIdRequest));
        if (!CollectionUtils.isEmpty(apiResponse.getData())) {
            apiResponse.getData().forEach(e -> map.put(e.getDeviceName(), e));
        }
        return map;
    }

    /**
     * 建议先针对会产生客诉和资损的版本（Android v1.20.0 & iOS v2.11.0）以下的版本，尽快开启封登录接口
     *
     * 等这个版本以下的活跃用户基本都消失之后，把封登录接口的版本限制提高到 (Android v1.27.0 & iOS v2.16.3)
     *
     * @return
     */
    public boolean isLoginBanned() {
        String currentVersion = VersionUtil.getVersionByClientType(getClientType());
        if (StringUtils.isNotBlank(currentVersion)) {
            if (isAndroid()) {
                if (VersionUtil.lowerVersionName(androidLoginBannedVersion, currentVersion)) {
                    // 判断是否低于某个版本
                    return true;
                }
            } else if (isIOS()) {
                if (VersionUtil.lowerVersionName(iosLoginBannedVersion, currentVersion)) {
                    // 判断是否低于某个版本
                    return true;
                }
            }
        }
        return false;
    }

    public Long getUserIdByToken(String token) throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setToken(token);
        APIResponse<AuthResponse> apiResponse = authApi.auth(getInstance(authRequest));
        checkResponse(apiResponse);
        AuthResponse authResponse = apiResponse.getData();
        if (authResponse != null && StringUtils.isNotEmpty(authResponse.getUserId())) {
            return Long.valueOf(authResponse.getUserId());
        }
        return null;
    }



    public void invalidatePasswordlesslogin(HttpServletRequest request,String email, LoginV2Arg loginArg)throws Exception{
        if(!enableCheckInvalidatePasswordlessLogin){
            log.warn("enableCheckInvalidatePasswordlessLogin=false");
            return;
        }
        if(StringUtils.isBlank(email)){
            log.warn("invalidatePasswordlesslogin,email is null");
            throw new BusinessException(AccountMgsErrorCode.PLEASE_USE_PASSWORD_LOGIN);
        }
        String enablePasswordlessLoginHeader = WebUtils.getHeader(BinanceMgsAccountConstant.ENABLE_PASSOWRDLESS_LOGIN_HEADER_NAME);
        boolean enablePasswordlessLoginVersion=org.apache.commons.lang3.StringUtils.isNotBlank(enablePasswordlessLoginHeader) && enablePasswordlessLoginHeader.equalsIgnoreCase("true");
        log.info("enablePasswordlessLoginHeader={},enablePasswordlessLoginVersion={}", enablePasswordlessLoginHeader, enablePasswordlessLoginVersion);
        //是否是二次验证
        Boolean verifyCodeFlag = !StringUtils.isAllBlank(loginArg.getMobileVerifyCode(), loginArg.getGoogleVerifyCode(), loginArg.getEmailVerifyCode(),
                loginArg.getYubikeyVerifyCode(), loginArg.getVerifyToken());
        if(verifyCodeFlag.booleanValue()){
            log.info("verifyCodeFlag=true");
            return;
        }
        //开始清理登录态
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!this.isOk(emailApiResponse)) {
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));


        InvalidatePasswordlessloginReq invalidatePasswordlessloginReq = new InvalidatePasswordlessloginReq();
        invalidatePasswordlessloginReq.setUserId(userId);
        APIResponse<InvalidatePasswordlessloginResp> invalidatePasswordlessloginRespAPIResponse = user2FaApi.invalidatePasswordlesslogin(getInstance(invalidatePasswordlessloginReq));
        if (!this.isOk(invalidatePasswordlessloginRespAPIResponse)) {
            log.warn("invalidatePasswordlesslogin failed,invalidatePasswordlessloginRespAPIResponse={}", JsonUtils.toJsonNotNullKey(invalidatePasswordlessloginRespAPIResponse));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(invalidatePasswordlessloginRespAPIResponse);
        }
        this.logout(request);
    }

    public void invalidatePasswordlesslogin(HttpServletRequest request,String email, LoginV3Arg loginArg)throws Exception{
        if(!enableCheckInvalidatePasswordlessLogin){
            log.warn("enableCheckInvalidatePasswordlessLogin=false");
            return;
        }
        if(StringUtils.isBlank(email)){
            log.warn("invalidatePasswordlesslogin,email is null");
            throw new BusinessException(AccountMgsErrorCode.PLEASE_USE_PASSWORD_LOGIN);
        }
        String enablePasswordlessLoginHeader = WebUtils.getHeader(BinanceMgsAccountConstant.ENABLE_PASSOWRDLESS_LOGIN_HEADER_NAME);
        boolean enablePasswordlessLoginVersion=org.apache.commons.lang3.StringUtils.isNotBlank(enablePasswordlessLoginHeader) && enablePasswordlessLoginHeader.equalsIgnoreCase("true");
        log.info("enablePasswordlessLoginHeader={},enablePasswordlessLoginVersion={}", enablePasswordlessLoginHeader, enablePasswordlessLoginVersion);
//        //是否是二次验证
//        Boolean verifyCodeFlag = !StringUtils.isAllBlank(loginArg.getMobileVerifyCode(), loginArg.getGoogleVerifyCode(), loginArg.getEmailVerifyCode(),
//                loginArg.getYubikeyVerifyCode(), loginArg.getVerifyToken());
//        if(verifyCodeFlag.booleanValue()){
//            log.info("verifyCodeFlag=true");
//            return;
//        }
        //开始清理登录态
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!this.isOk(emailApiResponse)) {
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));


        InvalidatePasswordlessloginReq invalidatePasswordlessloginReq = new InvalidatePasswordlessloginReq();
        invalidatePasswordlessloginReq.setUserId(userId);
        APIResponse<InvalidatePasswordlessloginResp> invalidatePasswordlessloginRespAPIResponse = user2FaApi.invalidatePasswordlesslogin(getInstance(invalidatePasswordlessloginReq));
        if (!this.isOk(invalidatePasswordlessloginRespAPIResponse)) {
            log.warn("invalidatePasswordlesslogin failed,invalidatePasswordlessloginRespAPIResponse={}", JsonUtils.toJsonNotNullKey(invalidatePasswordlessloginRespAPIResponse));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(invalidatePasswordlessloginRespAPIResponse);
        }
        this.logout(request);
    }

    /**
     * 清除web端半登录态cookie
     * @param request
     * @param response
     */
    public void delSerialNoCookie(HttpServletRequest request, HttpServletResponse response) {
        BaseHelper.delCookie(request, response, Constant.COOKIE_SERIAL_NO);
    }

    /**
     * 清除服务端半登录态缓存
     */
    public void delSerialNo(String serialNo) {
        DelSerialNoRequest request = new DelSerialNoRequest();
        request.setSerialNo(serialNo);
        APIResponse<Void> apiResponse = authApi.delSerialNo(getInstance(request));
        checkResponse(apiResponse);
    }                                           
}
