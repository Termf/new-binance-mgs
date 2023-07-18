package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserApi;
import com.binance.account.api.UserDeviceApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.common.enums.UserSecurityResetType;
import com.binance.account.vo.device.request.ResendAuthorizeDeviceEmailRequest;
import com.binance.account.vo.device.request.UserDeviceAuthorizeRequest;
import com.binance.account.vo.device.request.VerifyAuthDeviceCodeRequest;
import com.binance.account.vo.device.response.ResendAuthorizeDeviceEmailResponse;
import com.binance.account.vo.device.response.UserDeviceAuthorizeResponse;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.request.VerificationTwoV3Request;
import com.binance.account.vo.security.response.VerificationTwoV3Response;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.response.GetUserEmailResponse;
import com.binance.accountoauth.api.AccountOauthApi;
import com.binance.accountoauth.vo.oauth.request.DoLoginBindThreePartyRequest;
import com.binance.authcenter.vo.DeviceAuthorizeRequest;
import com.binance.authcenter.vo.DeviceAuthorizeResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.vo.AuthorizeArg;
import com.binance.mgs.account.account.vo.AuthorizeForNewProcessArg;
import com.binance.mgs.account.account.vo.AuthorizeForNewProcessV2Arg;
import com.binance.mgs.account.account.vo.AuthorizeForNewProcessV3Arg;
import com.binance.mgs.account.account.vo.DeviceAuthorizeForNewProcessRet;
import com.binance.mgs.account.account.vo.ResendAuthDeviceEmailArg;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.helper.DeviceAuthHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.mgs.account.service.Account2FaService;
import com.binance.mgs.account.service.RiskService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.utils.StringUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@RestController
@Slf4j
public class EmailController extends AccountBaseAction {
    @Resource
    private UserSecurityApi userSecurityApi;
    @Resource
    private UserApi userApi;
    @Resource
    private UserDeviceApi userDeviceApi;
    @Resource
    private RiskHelper riskHelper;
    @Resource
    private DeviceAuthHelper deviceAuthHelper;
    @Resource
    private AuthHelper authHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    private AccountOauthApi accountOauthApi;
    @Autowired
    private Account2FaService account2FaService;
    @Autowired
    private RiskService riskService;

    @Value("${DDos.ip.limit.count:10}")
    private int ipLimitCount;
    @Value("${new.authorize.banIp.check.switch:false}")
    private boolean newAuthorizeBanIpCheck;// 新设备授权场景banIp检查

    @Value("${need.device.security.check.switch:false}")
    private boolean needDeviceSecurityCheck;// 新设备授权场景安全检查

    /**
     * 邮件验证
     *
     * @param request
     * @param response
     * @param authorizeArg
     * @return
     * @throws Exception
     */
    @GetMapping("/v1/public/account/user-device/authorize")
    @UserOperation(name = "授权设备", eventName = "authorizeDevice", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"}, forwardedCookies = {"p20t","cr20","d91f"})
    @DDoSPreMonitor(action = "deviceAuthorize")
    public CommonRet<Void> authorize(HttpServletRequest request, HttpServletResponse response,
                                     @Validated AuthorizeArg authorizeArg) throws Exception {
        log.info("authorize:userid={}", authorizeArg.getUserId());
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(authorizeArg.getUserId());
        APIResponse<GetUserEmailResponse> emailApiResponse = userApi.getUserEmailByUserId(getInstance(userIdRequest));
        if (emailApiResponse == null || emailApiResponse.getData() == null
                || StringUtils.isBlank(emailApiResponse.getData().getEmail())) {
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));

            if (baseHelper.send2NewPage()) {
                baseHelper.sendRedirect(String.format("/%s/user/device-authorize-result", baseHelper.getLanguage())
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
                return new CommonRet<>();
            } else {
                baseHelper.sendRedirect("/device_authorize_result.html"
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
                return new CommonRet<>();
            }
        }

        if (authorizeArg.getCode().length() != 32) {
            log.warn("code length  error");
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            if (baseHelper.send2NewPage()) {
                baseHelper.sendRedirect(String.format("/%s/user/device-authorize-result", baseHelper.getLanguage())
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
                return new CommonRet<>();
            } else {
                baseHelper.sendRedirect("/device_authorize_result.html"
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
                return new CommonRet<>();
            }
        }


        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, authorizeArg.getUserId()));
        riskHelper.frequentGrantDeviceSendMq(authorizeArg.getUserId(), emailApiResponse.getData().getEmail());
        //检查验证码是否有效
        VerifyAuthDeviceCodeRequest verifyAuthDeviceCodeRequest = new VerifyAuthDeviceCodeRequest();
        verifyAuthDeviceCodeRequest.setUserId(authorizeArg.getUserId());
        verifyAuthDeviceCodeRequest.setCode(authorizeArg.getCode());
        APIResponse<Boolean> verfifyAuthDeviceResp = userDeviceApi.verifyAuthDeviceCode(getInstance(verifyAuthDeviceCodeRequest));
        if (!baseHelper.isOk(verfifyAuthDeviceResp)) {
            log.warn("code is illegal,verfifyAuthDeviceResp={}", JsonUtils.toJsonNotNullKey(verfifyAuthDeviceResp));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            if (baseHelper.send2NewPage()) {
                baseHelper.sendRedirect(String.format("/%s/user/device-authorize-result", baseHelper.getLanguage())
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
                return new CommonRet<>();
            } else {
                baseHelper.sendRedirect("/device_authorize_result.html"
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
                return new CommonRet<>();
            }
        }


        UserDeviceAuthorizeRequest userDeviceAuthorizeRequest = new UserDeviceAuthorizeRequest();
        userDeviceAuthorizeRequest.setCode(authorizeArg.getCode());
        APIResponse<UserDeviceAuthorizeResponse> authorizeResponse =
                userDeviceApi.authorizeDevice(getInstance(userDeviceAuthorizeRequest));

        // 需要回答问题
        if (authorizeResponse.getData() != null && !authorizeResponse.getData().isValid()
                && authorizeResponse.getData().isNeedAnswerQuestion()) {
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            String questionUrl = String.format("/%s/question/%s/%s", baseHelper.getLanguage(),
                    UserSecurityResetType.authDevice.name(),
                    StringUtils.trimToEmpty(authorizeResponse.getData().getQuestionFlowId()));
            log.info("redirect user to answer question,url: {}, uid: {}, flowId: {}", questionUrl,
                    authorizeArg.getUserId(), authorizeResponse.getData().getQuestionFlowId());
            baseHelper.sendRedirect(questionUrl);
        }

        if (authorizeResponse.getData() != null && authorizeResponse.getData().isValid()) {
            UserDeviceAuthorizeResponse deviceAuthorize = authorizeResponse.getData();
            if (deviceAuthorize.getId() != null) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.DEVICE_PK, deviceAuthorize.getId()));
            }
            // 授权成功，跳转登陆页面
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.TRUE.toString()));
            if (baseHelper.send2NewPage()) {
                deviceAuthHelper.auth(String.valueOf(authorizeArg.getUserId()));
                if (StringUtils.contains(deviceAuthorize.getCallback(), "client_id")) {
                    // 临时判断下，如果callback包含client_id就说明是之前是从第三方过来的oauth登录方式，
                    log.info("新设备授权跳转到refer地址={}", deviceAuthorize.getCallback());
                    response.sendRedirect(deviceAuthorize.getCallback());
                } else {
                    baseHelper.sendRedirect(String.format("/%s/user/device-authorize-result", baseHelper.getLanguage())
                            + StringUtil.buildGetParamStr(ImmutableMap.of("loginIp",
                            StringUtils.trimToEmpty(deviceAuthorize.getLoginIp()), "deviceName",
                            StringUtils.trimToEmpty(deviceAuthorize.getDeviceName()), "locationCity",
                            StringUtils.trimToEmpty(deviceAuthorize.getLocationCity()), "success", true)));
                }
            } else {
                baseHelper.sendRedirect("/device_authorize_result.html" + StringUtil.buildGetParamStr(
                        ImmutableMap.of("loginIp", StringUtils.trimToEmpty(deviceAuthorize.getLoginIp()), "deviceName",
                                StringUtils.trimToEmpty(deviceAuthorize.getDeviceName()), "locationCity",
                                StringUtils.trimToEmpty(deviceAuthorize.getLocationCity()), "success", true)));
            }
        } else {
            log.warn("Authorize failed, the link is invalid or expired, email:{}, code:{}",
                    emailApiResponse.getData().getEmail(), authorizeArg.getCode());
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            if (baseHelper.send2NewPage()) {
                baseHelper.sendRedirect(String.format("/%s/user/device-authorize-result", baseHelper.getLanguage())
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
            } else {
                baseHelper.sendRedirect("/device_authorize_result.html"
                        + StringUtil.buildGetParamStr(ImmutableMap.of("success", false, "msg", "fail")));
            }
        }
        return new CommonRet<>();
    }


    /**
     * 邮件验证
     *
     * @param request
     * @param response
     * @param authorizeArg
     * @return
     * @throws Exception
     */
    @PostMapping("/v1/protect/account/user-device/authorizeForNewProcess")
    @UserOperation(name = "授权设备", logDeviceOperation = true, eventName = "authorizeDevice", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "authorizeForNewProcess")
    public CommonRet<DeviceAuthorizeForNewProcessRet> authorizeForNewProcess(HttpServletRequest request,
                                                                             HttpServletResponse response, @Validated @RequestBody AuthorizeForNewProcessArg authorizeArg) throws Exception {
        log.info("authorizeForNewProcess before :email={}", authorizeArg.getEmail());
        // replace email
        authorizeArg.setEmail(authHelper.getUserEmailFromHeader());
        log.info("authorizeForNewProcess after :email={}", authorizeArg.getEmail());
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(authorizeArg.getEmail());
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!baseHelper.isOk(emailApiResponse)) {
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, null, authorizeArg.getEmail());
        if(MapUtils.isNotEmpty(deviceInfo)) {
            UserOperationHelper.log("device-info", deviceInfo);
        }
        riskHelper.frequentGrantDeviceSendMq(userId, authorizeArg.getEmail());
        //检查验证码是否有效
        VerifyAuthDeviceCodeRequest verifyAuthDeviceCodeRequest = new VerifyAuthDeviceCodeRequest();
        verifyAuthDeviceCodeRequest.setUserId(userId);
        verifyAuthDeviceCodeRequest.setCode(authorizeArg.getCode());
        APIResponse<Boolean> verfifyAuthDeviceResp = userDeviceApi.verifyAuthDeviceCode(getInstance(verifyAuthDeviceCodeRequest));
        if (!baseHelper.isOk(verfifyAuthDeviceResp)) {
            log.warn("code is illegal,verfifyAuthDeviceResp={}", JsonUtils.toJsonNotNullKey(verfifyAuthDeviceResp));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(verfifyAuthDeviceResp);
        }

        DeviceAuthorizeRequest userDeviceAuthorizeRequest = new DeviceAuthorizeRequest();
        userDeviceAuthorizeRequest.setCode(authorizeArg.getCode() + userId);
        userDeviceAuthorizeRequest.setClientType(baseHelper.getClientType());
        APIResponse<DeviceAuthorizeResponse> authorizeResponse =
                authApi.authorizeDevice(getInstance(userDeviceAuthorizeRequest));
        CommonRet<DeviceAuthorizeForNewProcessRet> finalResult = new CommonRet<>();
        DeviceAuthorizeForNewProcessRet deviceRet = new DeviceAuthorizeForNewProcessRet();
        deviceRet.setUserId(userId);
        // 需要回答问题
        if (authorizeResponse.getData() != null && !authorizeResponse.getData().isValid()
                && authorizeResponse.getData().isNeedAnswerQuestion()) {
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            log.info("redirect user to answer question, uid: {}, flowId: {}", userId,
                    authorizeResponse.getData().getQuestionFlowId());
            BeanUtils.copyProperties(authorizeResponse.getData(), deviceRet);
            finalResult.setData(deviceRet);
            return finalResult;
        }

        if (authorizeResponse.getData() != null && authorizeResponse.getData().isValid()) {
            DeviceAuthorizeResponse deviceAuthorize = authorizeResponse.getData();
            if (deviceAuthorize.getId() != null) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.DEVICE_PK, deviceAuthorize.getId()));
            }
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.TRUE.toString()));
            BeanUtils.copyProperties(authorizeResponse.getData(), deviceRet);
            if (baseHelper.isFromWeb()) {
                deviceRet.setToken(null);
                deviceRet.setCsrfToken(null);
                finalResult.setData(deviceRet);
                authHelper.setAuthCookie(request, response, authorizeResponse.getData().getToken(),
                        authorizeResponse.getData().getCsrfToken());
                // } else {
                // 客户端例如mac，pc，返回token
                // data.setToken(qrCodeDto.getToken());
            } else {
                finalResult.setData(deviceRet);
            }
            //绑定三方账户 todo check下是否需要添加
            String registerToken = baseHelper.isFromWeb()?authHelper.getRegisterTokenCookie():authorizeArg.getRegisterToken();
            if (StringUtils.isNotBlank(registerToken) && StringUtils.isNotBlank(authorizeArg.getThreePartySource()) && BinanceMgsAccountConstant.THREE_PARTY_SOURCE.equals(authorizeArg.getThreePartySource())){
                try {
                    DoLoginBindThreePartyRequest doLoginBindThreePartyRequest = new DoLoginBindThreePartyRequest();
                    doLoginBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
                    doLoginBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, authorizeArg.getEmail()));
                    doLoginBindThreePartyRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
                    doLoginBindThreePartyRequest.setRegisterToken(registerToken);
                    doLoginBindThreePartyRequest.setUserId(Long.valueOf(userId));
                    APIResponse<Boolean> bindThreePartyRes = accountOauthApi.doLoginBindThreeParty(getInstance(doLoginBindThreePartyRequest));
                    log.info("accountOauthApi.doLoginBindThreeParty.userId:{},bindThreePartyRes:{}",userId,JsonUtils.toJsonHasNullKey(bindThreePartyRes));
                }catch (Exception e){
                    log.error("accountOauthApi.doLoginBindThreeParty.userId:{},error:{}",userId,e);
                }
                authHelper.setRegisterTokenCookie(request,response,registerToken,0);
            }
            return finalResult;
        } else {
            log.warn("Authorize failed, the link is invalid or expired, email:{}, code:{}", authorizeArg.getEmail(),
                    authorizeArg.getCode());
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(authorizeResponse);
        }
        return new CommonRet<>();
    }


    /**
     * 邮件验证
     *
     * @param request
     * @param response
     * @param authorizeArg
     * @return
     * @throws Exception
     */
    @PostMapping("/v2/protect/account/user-device/authorizeForNewProcess")
    @UserOperation(name = "授权设备", logDeviceOperation = true, eventName = "authorizeDevice", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "authorizeForNewProcessV2")
    public CommonRet<DeviceAuthorizeForNewProcessRet> authorizeForNewProcessV2(HttpServletRequest request,
                                                                               HttpServletResponse response, @Validated @RequestBody AuthorizeForNewProcessV2Arg authorizeArg) throws Exception {
        log.info("authorizeForNewProcessV2 before :email={},mobile={}", authorizeArg.getEmail(), authorizeArg.getMobile());
        // replace email
        authorizeArg.setEmail(authHelper.getUserEmailFromHeader());
        log.info("authorizeForNewProcessV2 after :email={}", authorizeArg.getEmail());
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(authorizeArg.getEmail());
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!baseHelper.isOk(emailApiResponse)) {
            String ip = WebUtils.getRequestIp();
            if (newAuthorizeBanIpCheck) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("authorizeForNewProcessV2 fake email=%s", authorizeArg.getEmail()));
            }
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, null, authorizeArg.getEmail());
        if(MapUtils.isNotEmpty(deviceInfo)) {
            UserOperationHelper.log("device-info", deviceInfo);
        }
        riskHelper.frequentGrantDeviceSendMq(userId, authorizeArg.getEmail());

        String deviceSecurityCheck =  ddosCacheSeviceHelper.getEmailCacheWithOperationEnum(userId.toString(),DdosOperationEnum.DEVICE_AUTH);

        if (StringUtils.isNotBlank(deviceSecurityCheck) && needDeviceSecurityCheck) {
            throw new BusinessException(AccountMgsErrorCode.PLEASE_CONTACT_CUSTOMER_SERVICE);
        }
        //检查验证码是否有效
        if (StringUtils.isNotBlank(authorizeArg.getVerifyTokenFIDO()) && StringUtils.isNotBlank(authorizeArg.getVerifyTokenOTP())) {
            try {
                log.info("authorizeForNewProcess verify new 2fa start");
                account2FaService.verify2FaToken(userId, com.binance.account2fa.enums.BizSceneEnum.AUTHORIZE_NEW_DEVICE_FIDO.name(), authorizeArg.getVerifyTokenFIDO());
                account2FaService.verify2FaToken(userId, com.binance.account2fa.enums.BizSceneEnum.AUTHORIZE_NEW_DEVICE_OTP.name(), authorizeArg.getVerifyTokenOTP());
            } catch (Exception e) {
                log.error("authorizeForNewProcess verify new 2fa failed ", e);
                UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
                throw e;
            }
        } else {
            log.info("authorizeForNewProcess verify old 2fa start");
            VerificationTwoV3Request verificationTwoV3Request = new VerificationTwoV3Request();
            verificationTwoV3Request.setUserId(userId);
            verificationTwoV3Request.setBizScene(BizSceneEnum.AUTHORIZE_NEW_DEVICE);
            verificationTwoV3Request.setEmailVerifyCode(authorizeArg.getEmailVerifyCode());
            verificationTwoV3Request.setMobileVerifyCode(authorizeArg.getMobileVerifyCode());
            verificationTwoV3Request.setGoogleVerifyCode(authorizeArg.getGoogleVerifyCode());
            verificationTwoV3Request.setYubikeyVerifyCode(authorizeArg.getYubikeyVerifyCode());
            APIResponse<VerificationTwoV3Response> verificationTwoV3ResponseAPIResponse = userSecurityApi.verificationsTwoV3(getInstance(verificationTwoV3Request));
            if (!baseHelper.isOk(verificationTwoV3ResponseAPIResponse)) {
                log.warn("verifyCode is illegal,verificationTwoV3ResponseAPIResponse={}", JsonUtils.toJsonNotNullKey(verificationTwoV3ResponseAPIResponse));
                UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
                checkResponse(verificationTwoV3ResponseAPIResponse);
            }
        }

        DeviceAuthorizeRequest userDeviceAuthorizeRequest = new DeviceAuthorizeRequest();
        userDeviceAuthorizeRequest.setCode(userId.toString());
        userDeviceAuthorizeRequest.setClientType(baseHelper.getClientType());
        APIResponse<DeviceAuthorizeResponse> authorizeResponse =
                authApi.authorizeDevice(getInstance(userDeviceAuthorizeRequest));
        CommonRet<DeviceAuthorizeForNewProcessRet> finalResult = new CommonRet<>();
        DeviceAuthorizeForNewProcessRet deviceRet = new DeviceAuthorizeForNewProcessRet();
        deviceRet.setUserId(userId);
        // 需要回答问题
        if (authorizeResponse.getData() != null && !authorizeResponse.getData().isValid()
                && authorizeResponse.getData().isNeedAnswerQuestion()) {
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            log.info("redirect user to answer question, uid: {}, flowId: {}", userId,
                    authorizeResponse.getData().getQuestionFlowId());
            BeanUtils.copyProperties(authorizeResponse.getData(), deviceRet);
            finalResult.setData(deviceRet);
            return finalResult;
        }

        if (authorizeResponse.getData() != null && authorizeResponse.getData().isValid()) {
            DeviceAuthorizeResponse deviceAuthorize = authorizeResponse.getData();
            if (deviceAuthorize.getId() != null) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.DEVICE_PK, deviceAuthorize.getId()));
            }
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.TRUE.toString()));
            BeanUtils.copyProperties(authorizeResponse.getData(), deviceRet);
            if (baseHelper.isFromWeb()) {
                deviceRet.setToken(null);
                deviceRet.setCsrfToken(null);
                finalResult.setData(deviceRet);
                String code = authHelper.setAuthCookie(request, response, authorizeResponse.getData().getToken(),
                        authorizeResponse.getData().getCsrfToken());
                deviceRet.setCode(code);
                // } else {
                // 客户端例如mac，pc，返回token
                // data.setToken(qrCodeDto.getToken());
                deviceRet.setRefreshToken(null);
            } else {
                finalResult.setData(deviceRet);
            }
            //绑定三方账户
            String registerToken = baseHelper.isFromWeb()?authHelper.getRegisterTokenCookie():authorizeArg.getRegisterToken();
            if (StringUtils.isNotBlank(registerToken) && StringUtils.isNotBlank(authorizeArg.getThreePartySource()) && BinanceMgsAccountConstant.THREE_PARTY_SOURCE.equals(authorizeArg.getThreePartySource())){
                try {
                    DoLoginBindThreePartyRequest doLoginBindThreePartyRequest = new DoLoginBindThreePartyRequest();
                    doLoginBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
                    doLoginBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, authorizeArg.getEmail()));
                    doLoginBindThreePartyRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
                    doLoginBindThreePartyRequest.setRegisterToken(registerToken);
                    doLoginBindThreePartyRequest.setUserId(Long.valueOf(userId));
                    APIResponse<Boolean> bindThreePartyRes = accountOauthApi.doLoginBindThreeParty(getInstance(doLoginBindThreePartyRequest));
                    log.info("accountOauthApi.doLoginBindThreeParty.userId:{},bindThreePartyRes:{}",userId,JsonUtils.toJsonHasNullKey(bindThreePartyRes));
                }catch (Exception e){
                    log.error("accountOauthApi.doLoginBindThreeParty.userId:{},error:{}",userId,e);
                }
                authHelper.setRegisterTokenCookie(request,response,registerToken,0);
            }
            return finalResult;
        } else {
            log.warn("Authorize failed, the link is invalid or expired, email:{}", authorizeArg.getEmail());
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(authorizeResponse);
        }
        return new CommonRet<>();
    }


    /**
     * 重发设备授权邮件
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/v1/protect/account/user-device/resendAuthorizeDeviceEmail")
    @UserOperation(name = "重发设备授权邮件", eventName = "resendAuthorizeDeviceEmail", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    public CommonRet<ResendAuthorizeDeviceEmailResponse> resendAuthorizeDeviceEmail(HttpServletRequest request,
                                                                                    HttpServletResponse response, @RequestBody @Validated ResendAuthDeviceEmailArg resendAuthDeviceEmailArg)
            throws Exception {
        log.info("resendAuthorizeDeviceEmail before :email={}", resendAuthDeviceEmailArg.getEmail());
        // replace email
        resendAuthDeviceEmailArg.setEmail(authHelper.getUserEmailFromHeader());
        log.info("resendAuthorizeDeviceEmail after:email={}", resendAuthDeviceEmailArg.getEmail());
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, getLoginUserId()));
        ResendAuthorizeDeviceEmailRequest resendAuthorizeDeviceEmailRequest = new ResendAuthorizeDeviceEmailRequest();
        resendAuthorizeDeviceEmailRequest.setEmail(resendAuthDeviceEmailArg.getEmail());
        HashMap<String, String> deviceInfo = logDeviceInfo();
        resendAuthorizeDeviceEmailRequest.setDeviceInfo(deviceInfo);
        APIResponse<ResendAuthorizeDeviceEmailResponse> responseAPIResponse =
                userDeviceApi.resendAuthorizeDeviceEmail(getInstance(resendAuthorizeDeviceEmailRequest));
        if (!baseHelper.isOk(responseAPIResponse)) {
            log.warn("resendAuthorizeDeviceEmail error={}", JsonUtils.toJsonNotNullKey(responseAPIResponse));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(responseAPIResponse);
        }
        CommonRet<ResendAuthorizeDeviceEmailResponse> result = new CommonRet<ResendAuthorizeDeviceEmailResponse>();
        result.setData(responseAPIResponse.getData());
        return result;
    }

    /**
     * 登记设备信息
     *
     * @return
     */
    private HashMap<String, String> logDeviceInfo() {
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getUserEmail());
        if (deviceInfo == null) {
            deviceInfo = Maps.newHashMap();
        }
        UserOperationHelper.log(ImmutableMap.of("device", deviceInfo));
        return deviceInfo;
    }

    @PostMapping("/v3/protect/account/user-device/authorizeForNewProcess")
    @UserOperation(name = "授权设备", logDeviceOperation = true, eventName = "authorizeDevice", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "authorizeForNewProcessV2")
    public CommonRet<DeviceAuthorizeForNewProcessRet> authorizeForNewProcessV3(HttpServletRequest request,
                                                                               HttpServletResponse response, @Validated @RequestBody AuthorizeForNewProcessV3Arg authorizeArg) throws Exception {
        log.info("authorizeForNewProcessV3 before :email={},mobile={}", authorizeArg.getEmail(), authorizeArg.getMobile());
        // replace email
        authorizeArg.setEmail(authHelper.getUserEmailFromHeader());
        log.info("authorizeForNewProcessV3 after :email={}", authorizeArg.getEmail());
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(authorizeArg.getEmail());
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!baseHelper.isOk(emailApiResponse)) {
            String ip = WebUtils.getRequestIp();
            if (newAuthorizeBanIpCheck) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("authorizeForNewProcessV3 fake email=%s", authorizeArg.getEmail()));
            }
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, null, authorizeArg.getEmail());
        if(MapUtils.isNotEmpty(deviceInfo)) {
            UserOperationHelper.log("device-info", deviceInfo);
        }
        riskHelper.frequentGrantDeviceSendMq(userId, authorizeArg.getEmail());

        String deviceSecurityCheck =  ddosCacheSeviceHelper.getEmailCacheWithOperationEnum(userId.toString(),DdosOperationEnum.DEVICE_AUTH);

        if (StringUtils.isNotBlank(deviceSecurityCheck) && needDeviceSecurityCheck) {
            throw new BusinessException(AccountMgsErrorCode.PLEASE_CONTACT_CUSTOMER_SERVICE);
        }

        // 检查MFA
        riskService.getRiskChallengeTimeOut(userId, deviceInfo, com.binance.account2fa.enums.BizSceneEnum.AUTHORIZE_NEW_DEVICE_V2.name());

        DeviceAuthorizeRequest userDeviceAuthorizeRequest = new DeviceAuthorizeRequest();
        userDeviceAuthorizeRequest.setCode(userId.toString());
        userDeviceAuthorizeRequest.setClientType(baseHelper.getClientType());
        APIResponse<DeviceAuthorizeResponse> authorizeResponse =
                authApi.authorizeDevice(getInstance(userDeviceAuthorizeRequest));
        CommonRet<DeviceAuthorizeForNewProcessRet> finalResult = new CommonRet<>();
        DeviceAuthorizeForNewProcessRet deviceRet = new DeviceAuthorizeForNewProcessRet();
        deviceRet.setUserId(userId);
        // 需要回答问题
        if (authorizeResponse.getData() != null && !authorizeResponse.getData().isValid()
                && authorizeResponse.getData().isNeedAnswerQuestion()) {
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            log.info("redirect user to answer question, uid: {}, flowId: {}", userId,
                    authorizeResponse.getData().getQuestionFlowId());
            BeanUtils.copyProperties(authorizeResponse.getData(), deviceRet);
            finalResult.setData(deviceRet);
            return finalResult;
        }

        if (authorizeResponse.getData() != null && authorizeResponse.getData().isValid()) {
            DeviceAuthorizeResponse deviceAuthorize = authorizeResponse.getData();
            if (deviceAuthorize.getId() != null) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.DEVICE_PK, deviceAuthorize.getId()));
            }
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.TRUE.toString()));
            BeanUtils.copyProperties(authorizeResponse.getData(), deviceRet);
            if (baseHelper.isFromWeb()) {
                deviceRet.setToken(null);
                deviceRet.setCsrfToken(null);
                finalResult.setData(deviceRet);
                String code = authHelper.setAuthCookie(request, response, authorizeResponse.getData().getToken(),
                        authorizeResponse.getData().getCsrfToken());
                deviceRet.setCode(code);
                // } else {
                // 客户端例如mac，pc，返回token
                // data.setToken(qrCodeDto.getToken());
                deviceRet.setRefreshToken(null);
            } else {
                finalResult.setData(deviceRet);
            }
            //绑定三方账户
            String registerToken = baseHelper.isFromWeb()?authHelper.getRegisterTokenCookie():authorizeArg.getRegisterToken();
            if (StringUtils.isNotBlank(registerToken) && StringUtils.isNotBlank(authorizeArg.getThreePartySource()) && BinanceMgsAccountConstant.THREE_PARTY_SOURCE.equals(authorizeArg.getThreePartySource())){
                try {
                    DoLoginBindThreePartyRequest doLoginBindThreePartyRequest = new DoLoginBindThreePartyRequest();
                    doLoginBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
                    doLoginBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, authorizeArg.getEmail()));
                    doLoginBindThreePartyRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
                    doLoginBindThreePartyRequest.setRegisterToken(registerToken);
                    doLoginBindThreePartyRequest.setUserId(Long.valueOf(userId));
                    APIResponse<Boolean> bindThreePartyRes = accountOauthApi.doLoginBindThreeParty(getInstance(doLoginBindThreePartyRequest));
                    log.info("accountOauthApi.doLoginBindThreeParty.userId:{},bindThreePartyRes:{}",userId,JsonUtils.toJsonHasNullKey(bindThreePartyRes));
                }catch (Exception e){
                    log.error("accountOauthApi.doLoginBindThreeParty.userId:{},error:{}",userId,e);
                }
                authHelper.setRegisterTokenCookie(request,response,registerToken,0);
            }
            return finalResult;
        } else {
            log.warn("Authorize failed, the link is invalid or expired, email:{}", authorizeArg.getEmail());
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(authorizeResponse);
        }
        return new CommonRet<>();
    }
}
