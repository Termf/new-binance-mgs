package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.CountryApi;
import com.binance.account.vo.country.CountryVo;
import com.binance.account.vo.country.GetCountryByCodeRequest;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account2fa.api.Send2FaApi;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.enums.MsgType;
import com.binance.account2fa.vo.request.SendEmailVerifyCodeRequest;
import com.binance.account2fa.vo.request.SendSmsAuthCodeRequest;
import com.binance.account2fa.vo.response.SendEmailVerifyCodeResponse;
import com.binance.account2fa.vo.response.SendSmsAuthCodeResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.fido2.api.Fido2Api;
import com.binance.fido2.vo.GetAllCredentialsRequest;
import com.binance.fido2.vo.GetAllCredentialsResponse;
import com.binance.fido2.vo.StartAuthenticateRequest;
import com.binance.fido2.vo.StartAuthenticateResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.WebUtils;
import com.binance.messaging.api.msg.MsgApi;
import com.binance.messaging.api.msg.request.QuerySendMethodRequest;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.account.vo.QueryPublicMfaSendChannelArg;
import com.binance.mgs.account.account.vo.QuerySendChannelResp;
import com.binance.mgs.account.account.vo.SendVerifyCodeResponse;
import com.binance.mgs.account.account.vo.mfa.SendPublicMfaEmailVerifyCodeArg;
import com.binance.mgs.account.account.vo.mfa.SendPublicMfaMobileVerifyCodeArg;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.mgs.account.fido.vo.AllCredentialPublicMfaArg;
import com.binance.mgs.account.fido.vo.StartAuthPublicMfaArg;
import com.binance.mgs.account.fido.vo.UserCredentialSimplifyVo;
import com.binance.mgs.account.integration.RiskChallengeServiceApiClient;
import com.binance.mgs.account.integration.UserSecurityApiClient;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.Constant;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.PKGenarator;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Description:
 *
 * @author alven
 * @since 2023/5/3
 */
@Slf4j
@RestController
@RequestMapping(value = "/v2/transient/account/mfa")
public class UserPublicMFaController extends AccountBaseAction {

    @Value("#{'${mfa.public.check.bizScene:REFRESH_ACCESSTOKEN,THIRD_PARTY_LOGIN}'.split(',')}")
    private List<String> checkedBizScenesForPublicMfa;
    
    @Value("#{'${limited.mobile.verify.mobilCode:}'.split(',')}")
    private Set<String> limitedMobileCode;

    @Resource
    private UserDeviceHelper userDeviceHelper;

    @Value("${fido.rpid:www.binance.com}")
    private String rpId;

    @Resource
    private Fido2Api fido2Api;

    @Resource
    private CheckHelper checkHelper;

    @Value("${send.bind.mobile.verify.code.threshold:10}")
    private int sendBindMobileVerifyCodeThreshold;
    
    @Resource
    private RiskChallengeServiceApiClient riskChallengeServiceApiClient;
    
    @Resource
    private UserSecurityApiClient userSecurityApiClient;

    @Value("${send.bind.mobile.verify.code.period:86400}")
    private int sendBindMobileVerifyCodePeriod;

    @Resource
    private Send2FaApi send2FaApi;
    
    @Resource
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;

    @Value("${DDos.ip.limit.count:10}")
    private int ipLimitCount;

    @Value("${general.DDos.check.switch:true}")
    private boolean generalDDosCheckSwitch;
    
    @Resource
    private CountryApi countryApi;
    
    @Resource
    private MsgApi msgApi;

    @PostMapping("/fido2/start-auth")
    @DDoSPreMonitor(action = "mfaStartAuthenticate")
    @AccountDefenseResource(name = "UserPublicMFaController.startAuthenticate")
    public CommonRet<StartAuthenticateResponse> startAuthenticate(@RequestBody @Valid StartAuthPublicMfaArg arg) throws Exception {
        if (checkedBizScenesForPublicMfa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(arg.getBizScene()))) {
            log.warn("UserPublicMFaController startAuthenticate Unsupported bizScene:{}", arg.getBizScene());
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        Long userId = riskChallengeServiceApiClient.getUserIdByBizNo(arg.getBizNo());
        String ip = WebUtils.getRequestIp();
        if (userId == null) {
            if (generalDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, 
                        String.format("startAuthenticate bizScene=%s identify=%s", arg.getBizScene(), arg.getBizNo()));
            }
            log.warn("startAuthenticate public mfa : userId is illegal");
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        
        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        StartAuthenticateRequest request = new StartAuthenticateRequest();
        request.setClientId(clientId);
        request.setClientType(this.baseHelper.getClientType());
        request.setRpId(this.rpId);
        request.setUserId(userId.toString());
        log.info("fido public mfa start Authenticate request:{}", JSON.toJSONString(request));
        APIResponse<StartAuthenticateResponse> startAuth = this.fido2Api.startAuth(APIRequest.instance(request));
        log.info("fido public mfa start Authenticate response:{}", JSON.toJSONString(startAuth));
        this.baseHelper.checkResponse(startAuth);
        return new CommonRet<>(startAuth.getData());
    }

    @PostMapping("/fido2/all-credentials")
    @DDoSPreMonitor(action = "allCredentialPublicMfa")
    @AccountDefenseResource(name = "UserPublicMFaController.allCredentialPublic")
    public CommonRet<List<UserCredentialSimplifyVo>> allCredentialPublic(@RequestBody @Valid AllCredentialPublicMfaArg arg) throws Exception {
        if (checkedBizScenesForPublicMfa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(arg.getBizScene()))) {
            log.warn("UserPublicMFaController allCredentialPublic Unsupported bizScene:{}", arg.getBizScene());
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        
        Long userId = riskChallengeServiceApiClient.getUserIdByBizNo(arg.getBizNo());
        
        String ip = WebUtils.getRequestIp();
        if (userId == null) {
            if (generalDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, 
                        String.format("allCredentialPublic bizScene=%s identify=%s", arg.getBizScene(), arg.getBizNo()));
            }
            log.warn("startAuthenticate public mfa : userId is illegal");
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }

        GetAllCredentialsRequest request = new GetAllCredentialsRequest();
        request.setRpId(this.rpId);
        request.setUserId(String.valueOf(userId));
        log.info("fido mfa allCredentialPublic request:{}", JSON.toJSONString(request));
        APIResponse<List<GetAllCredentialsResponse>> allCredential = this.fido2Api.allCredential(APIRequest.instance(request));
        log.info("fido mfa allCredentialPublic response:{}", JSON.toJSONString(allCredential));
        this.baseHelper.checkResponse(allCredential);

        List<UserCredentialSimplifyVo> resultList = allCredential.getData().stream().map(x -> {
            UserCredentialSimplifyVo userCredentialSimplifyVo = CopyBeanUtils.fastCopy(x, UserCredentialSimplifyVo.class);
            userCredentialSimplifyVo.setTransportType(getTransportType(x.getTransTypes()));
            return userCredentialSimplifyVo;
        }).collect(Collectors.toList());

        return new CommonRet<>(resultList);
    }

    @PostMapping(value = "/mobile/sendMobileVerifyCode")
    @AccountDefenseResource(name = "UserPublicMFaController.sendMobileVerifyCode")
    @DDoSPreMonitor(action = "mfa.publicSendMobileVerifyCode")
    @UserOperation(name = "发送短信验证码对外", eventName = "new2faSendPublicMobileVerifyCode", requestKeys = {"mobileCode", "mobile","email"},
            requestKeyDisplayNames = {"mobileCode", "mobile","email"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<SendVerifyCodeResponse> sendMobileVerifyCode(@RequestBody @Validated SendPublicMfaMobileVerifyCodeArg commonArg) throws Exception {
        // 默认仅支持refreshToken等场景
        if (checkedBizScenesForPublicMfa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(commonArg.getBizScene()))) {
            log.warn("UserPublicMFaController sendMobileVerifyCode Unsupported bizScene:{}", commonArg.getBizScene());
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());

        Long userId = riskChallengeServiceApiClient.getUserIdByBizNo(commonArg.getBizNo());
        String ip = WebUtils.getRequestIp();
        if (userId == null) {
            if (generalDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("sendMobileVerifyCode bizScene=%s identify=%s", commonArg.getBizScene(), commonArg.getBizNo()));
            }
            log.warn("sendMobileVerifyCode public mfa : userId is illegal");
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));

        UserSecurityVo userSecurityVo = userSecurityApiClient.getUserSecurityByUserId(userId);
        // 获取mobile和mobilecode
        // 如果是语音短信，检查是否在支持的国家内
        if (MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(userSecurityVo.getMobileCode());
        }
        UserOperationHelper.log("scene", commonArg.getBizScene());

        UserStatusEx userStatusEx =  this.getUserStatusByUserId(userId);
        // 部分限制地区建议用户使用邮箱验证
        if (BizSceneEnum.FORGET_PASSWORD.name().equalsIgnoreCase(commonArg.getBizScene()) && !userStatusEx.getIsUserNotBindEmail()
                && limitedMobileCode.contains(userSecurityVo.getMobileCode())) {
            throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_IS_RECOMMEND);
        }

        boolean isVnMobile = StringUtils.isNotBlank(userSecurityVo.getMobileCode()) && "vn".equalsIgnoreCase(userSecurityVo.getMobileCode());
        if (!isVnMobile && MsgType.VOICE == commonArg.getMsgType()) {
            Long increment = ShardingRedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
            if (increment == 1L) {
                ShardingRedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment, userSecurityVo.getMobileCode(), userSecurityVo.getMobile());
            if (increment >= sendBindMobileVerifyCodeThreshold) {
                log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment, userSecurityVo.getMobileCode(), userSecurityVo.getMobile());
                throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
            }
        }

        ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(userSecurityVo.getEmail(), userSecurityVo.getMobile(), DdosOperationEnum.ACTIVE);
        
        String verifyCodeId = PKGenarator.getId();
        SendSmsAuthCodeRequest sendSmsAuthCoderRequest = new SendSmsAuthCodeRequest();
        sendSmsAuthCoderRequest.setUserId(userId);
        sendSmsAuthCoderRequest.setVerifyCodeId(verifyCodeId);
        sendSmsAuthCoderRequest.setMsgType(commonArg.getMsgType());
        sendSmsAuthCoderRequest.setResend(commonArg.getResend());
        sendSmsAuthCoderRequest.setBizScene(commonArg.getBizScene());
        sendSmsAuthCoderRequest.setUserChannel(commonArg.getUserChannel());
        APIResponse<SendSmsAuthCodeResponse> apiResponse = send2FaApi.sendSmsAuthCode(getInstance(sendSmsAuthCoderRequest));
        checkResponseWithoutLog(apiResponse);
        return new CommonRet<>();
    }

    @PostMapping(value = "/email/sendEmailVerifyCode")
    @AccountDefenseResource(name = "UserPublicMFaController.sendEmailVerifyCode")
    @DDoSPreMonitor(action = "mfa.sendEmailVerifyCodePublic")
    @UserOperation(name = "发送邮件验证码对外", eventName = "new2faSendPublicEmailVerifyCode", requestKeys = {"mobileCode", "mobile","email"},
            requestKeyDisplayNames = {"mobileCode", "mobile","email"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<SendVerifyCodeResponse> sendEmailVerifyCode(@RequestBody @Validated SendPublicMfaEmailVerifyCodeArg commonArg)
            throws Exception {
        log.info("UserPublicMFaController sendEmailVerifyCode getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());
        // 默认仅支持refreshToken等场景
        if (checkedBizScenesForPublicMfa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(commonArg.getBizScene()))) {
            log.warn("UserPublicMFaController sendEmailVerifyCode Unsupported bizScene:{}", commonArg.getBizScene());
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }

        Long userId = riskChallengeServiceApiClient.getUserIdByBizNo(commonArg.getBizNo());
        String ip = WebUtils.getRequestIp();
        if (userId == null) {
            if (generalDDosCheckSwitch) {
                String identify = commonArg.getBizNo();
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("sendEmailVerifyCode bizScene=%s identify=%s", commonArg.getBizScene(), identify));
            }
            log.warn("sendEmailVerifyCode public mfa : userId is illegal");
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));

        UserStatusEx userStatusEx = this.getUserStatusByUserId(userId);
        if (userStatusEx.getIsUserDelete()) {
            throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_CODE_IS_SEND);
        }
        UserSecurityVo userSecurityVo = userSecurityApiClient.getUserSecurityByUserId(userId);
        ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(userSecurityVo.getEmail(), userSecurityVo.getMobile(), DdosOperationEnum.ACTIVE);
        
        SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeRequest();
        sendEmailVerifyCodeRequest.setUserId(userId);
        sendEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());
        APIResponse<SendEmailVerifyCodeResponse> apiResponse = send2FaApi.sendEmailVerifyCode(getInstance(sendEmailVerifyCodeRequest));
        checkResponseWithoutLog(apiResponse);
        SendVerifyCodeResponse sendVerifyCodeResponse = new SendVerifyCodeResponse();
        sendVerifyCodeResponse.setExpireTime(apiResponse.getData().getExpireTime());
        return new CommonRet<>(sendVerifyCodeResponse);
    }

    @PostMapping(value = "/mobile/querySendChannel")
    @AccountDefenseResource(name = "UserPublicMFaController.querySendChannel")
    @DDoSPreMonitor(action = "mfa.querySendChannel")
    public CommonRet<QuerySendChannelResp> querySendChannel(@RequestBody @Validated QueryPublicMfaSendChannelArg mfaSendChannelArg) throws Exception {
        // 默认仅支持refreshToken等场景
        if (checkedBizScenesForPublicMfa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(mfaSendChannelArg.getBizScene()))) {
            log.warn("UserPublicMFaController querySendChannel Unsupported bizScene:{}", mfaSendChannelArg.getBizScene());
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
        
        Long userId = riskChallengeServiceApiClient.getUserIdByBizNo(mfaSendChannelArg.getBizNo());
        String ip = WebUtils.getRequestIp();
        if (userId == null) {
            if (generalDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, 
                        String.format("startAuthenticate bizScene=%s identify=%s", mfaSendChannelArg.getBizScene(), mfaSendChannelArg.getBizNo()));
            }
            log.warn("querySendChannel public mfa : userId is illegal");
            //返回模糊异常
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }

        QuerySendMethodRequest querySendMethodRequest = new QuerySendMethodRequest();
        UserSecurityVo userSecurityVo = userSecurityApiClient.getUserSecurityByUserId(userId);
        querySendMethodRequest.setRecipient(userSecurityVo.getMobile());
        querySendMethodRequest.setMobileCode(userSecurityVo.getMobileCode());
        if (StringUtils.isAnyBlank(querySendMethodRequest.getRecipient(),querySendMethodRequest.getMobileCode())) {
            //未绑定手机
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        if (!StringUtils.isNumeric(querySendMethodRequest.getMobileCode())) {
            //转换mobileCode
            GetCountryByCodeRequest getCountryByCodeRequest = new GetCountryByCodeRequest();
            getCountryByCodeRequest.setCode(querySendMethodRequest.getMobileCode());
            APIResponse<CountryVo> countryVoAPIResponse = countryApi.getCountryByCode(getInstance(getCountryByCodeRequest));
            checkResponseWithoutLog(countryVoAPIResponse);
            querySendMethodRequest.setMobileCode(countryVoAPIResponse.getData().getMobileCode());
        }
        querySendMethodRequest.setResend(mfaSendChannelArg.getResend());
        querySendMethodRequest.setMsgType(com.binance.messaging.api.msg.request.MsgType.valueOf(mfaSendChannelArg.getMsgType().name()));
        APIResponse<String> apiResponse = msgApi.querySendOtpMethod(getInstance(querySendMethodRequest));
        checkResponseWithoutLog(apiResponse);
        QuerySendChannelResp resp = new QuerySendChannelResp();
        resp.setUserChannel(apiResponse.getData());
        return new CommonRet<>(resp);
    }
    
    private String getTransportType(List<String> transports) {
        if (CollectionUtils.isEmpty(transports)) {
            return "internal";
        }

        boolean internal = false;
        boolean passkeys = false;
        // 此处未使用List.contains是为了不区分大小写
        for (String transport : transports) {
            if (transport.equalsIgnoreCase("internal")) {
                internal = true;
            }
            if(transport.equalsIgnoreCase("passkeys")) {
                passkeys = true;
            }
        }
        if(passkeys) {
            return "passkeys";
        } else if (internal) {
            return "internal";
        }
        return "external";
    }
}
