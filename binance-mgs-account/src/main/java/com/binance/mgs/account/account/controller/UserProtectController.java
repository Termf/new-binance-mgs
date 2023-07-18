package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.CountryApi;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.country.CountryVo;
import com.binance.account.vo.country.GetCountryByCodeRequest;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.enums.MsgType;
import com.binance.account.vo.security.request.*;
import com.binance.account.vo.security.response.*;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.request.SendSmsAuthCodeV2Request;
import com.binance.account.vo.user.request.SendSmsAuthCoderRequest;
import com.binance.account.vo.user.response.SendSmsAuthCodeV2Response;
import com.binance.account.vo.user.response.SendSmsAuthCoderResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.capital.api.AddressApi;
import com.binance.capital.vo.address.request.AddressVerifyRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.messaging.api.msg.MsgApi;
import com.binance.messaging.api.msg.request.QuerySendMethodRequest;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.VersionHelper;
import com.binance.mgs.account.account.vo.*;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.OTPSendLimit;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.constant.Constant;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.PKGenarator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/v1/protect/account")
@Slf4j
public class UserProtectController extends AccountBaseAction {
    @Resource
    private UserSecurityApi userSecurityApi;
    @Resource
    private UserApi userApi;
    @Resource
    private MsgApi msgApi;
    @Resource
    private CountryApi countryApi;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private VersionHelper versionHelper;
    @Autowired
    private CheckHelper checkHelper;
    @Resource
    private ApiHelper apiHelper;
    @Autowired
    private AddressApi addressApi;

    @Value("${send.bind.mobile.verify.code.period:86400}")
    private int sendBindMobileVerifyCodePeriod;

    @Value("${send.bind.mobile.verify.code.threshold:10}")
    private int sendBindMobileVerifyCodeThreshold;


    @Value("${send.bind.mobile.text.limit:3}")
    private int sendBindMobileTextLimit;

    @Value("${send.bind.mobile.voice.limit:3}")
    private int sendBindMobileVoiceLimit;

    @Value("${sendEmail.android.version:}")
    private String sendEmailAndroidVersion;

    @Value("${sendEmail.ios.version:}")
    private String sendEmailIosVersion;

    @Value("${sendEmailVerifyCode.validateWithdrawParam.switch:false}")
    private boolean validateWithdrawParam;
    
    @Resource
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;




    @PostMapping(value = "/mobile/send-mobile-verify-code")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "protect.sendMobileVerifyCode")
    @AccountDefenseResource(name = "UserProtectController.sendMobileVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendMobileVerifyCode(@RequestBody @Validated SendMobileVerifyCodeArg commonArg)
            throws Exception {
        CommonRet<String> ret = new CommonRet<>();
        String verifyCodeId = PKGenarator.getId();
        SendSmsAuthCoderRequest sendSmsAuthCoderRequest = new SendSmsAuthCoderRequest();
        sendSmsAuthCoderRequest.setEmail(getTrueUserEmail());
        sendSmsAuthCoderRequest.setUserId(getUserId());
        sendSmsAuthCoderRequest.setVerifyCodeId(verifyCodeId);
        sendSmsAuthCoderRequest.setMsgType(commonArg.getMsgType());
        sendSmsAuthCoderRequest.setResend(commonArg.getResend());
        sendSmsAuthCoderRequest.setUserChannel(commonArg.getUserChannel());
        APIResponse<SendSmsAuthCoderResponse> apiResponse =
                userApi.sendSmsAuthCode(getInstance(sendSmsAuthCoderRequest));
        checkResponseWithoutLog(apiResponse);
        return ret;
    }

    @PostMapping(value = "/mobile/sendMobileVerifyCode")
    @UserOperation(name = "发送短信验证码", eventName = "sendMobileVerifyCode",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "protect.sendMobileVerifyCodeV2")
    @AccountDefenseResource(name = "UserProtectController.sendMobileVerifyCodeV2")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendMobileVerifyCodeV2(@RequestBody @Validated SendMobileVerifyCodeV2Arg commonArg)
            throws Exception {
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.REGTYPE, commonArg.getMsgType()));

        // 如果是语音短信，检查是否在支持的国家内
        if (MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(commonArg.getMobileCode());
        }
        
        boolean isVnMobile="vn".equalsIgnoreCase(commonArg.getMobileCode());
        if (!isVnMobile && MsgType.VOICE == commonArg.getMsgType() && (BizSceneEnum.BIND_MOBILE == commonArg.getBizScene() || BizSceneEnum.BIND_MOBILE_EU == commonArg.getBizScene()  ||BizSceneEnum.ACCOUNT_ACTIVATE == commonArg.getBizScene())) {
            Long increment = ShardingRedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
            if (increment == 1l) {
                ShardingRedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
            if (increment >= sendBindMobileVerifyCodeThreshold) {
                log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
                throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
            }
        }
        Long loginUserId = getLoginUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, loginUserId));

        if(BizSceneEnum.BIND_MOBILE == commonArg.getBizScene() || BizSceneEnum.BIND_MOBILE_EU == commonArg.getBizScene()){
            String bindMobileCacheKey=Constant.BIND_MOBILE_KEY+commonArg.getMsgType()+loginUserId;
            Long increment = ShardingRedisCacheUtils.increment(bindMobileCacheKey, 1);
            if (increment == 1l) {
                //有效期24小时
                ShardingRedisCacheUtils.expire(bindMobileCacheKey, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("bind mobile ，loginUserId={},msgtype={},send num{},mobileCode:{},mobile:{}",loginUserId, commonArg.getMsgType(),increment,commonArg.getMobileCode(),commonArg.getMobile());
            if(MsgType.VOICE == commonArg.getMsgType() && increment >= sendBindMobileVoiceLimit){
                log.info("overlimit bind mobile ，loginUserId={},msgtype={},send num{},mobileCode:{},mobile:{}",loginUserId, commonArg.getMsgType(),increment,commonArg.getMobileCode(),commonArg.getMobile());
                throw new BusinessException(AccountMgsErrorCode.BIND_MOBILE_SEND_MOBILE_CODE_OVERLIMIT);
            }
            if(MsgType.TEXT == commonArg.getMsgType() && increment >= sendBindMobileTextLimit){
                log.info("overlimit bind mobile ，loginUserId={},msgtype={},send num{},mobileCode:{},mobile:{}",loginUserId, commonArg.getMsgType(),increment,commonArg.getMobileCode(),commonArg.getMobile());
                throw new BusinessException(AccountMgsErrorCode.BIND_MOBILE_SEND_MOBILE_CODE_OVERLIMIT);
            }
        }

        CommonRet<String> ret = new CommonRet<>();
        String verifyCodeId = PKGenarator.getId();
        SendSmsAuthCodeV2Request sendSmsAuthCoderRequest = new SendSmsAuthCodeV2Request();
        sendSmsAuthCoderRequest.setEmail(getTrueUserEmail());
        sendSmsAuthCoderRequest.setMobileCode(commonArg.getMobileCode());
        sendSmsAuthCoderRequest.setMobile(commonArg.getMobile());
        sendSmsAuthCoderRequest.setUserId(loginUserId);
        sendSmsAuthCoderRequest.setVerifyCodeId(verifyCodeId);
        sendSmsAuthCoderRequest.setMsgType(commonArg.getMsgType());
        sendSmsAuthCoderRequest.setResend(commonArg.getResend());
        sendSmsAuthCoderRequest.setBizScene(commonArg.getBizScene());
        sendSmsAuthCoderRequest.setParams(commonArg.getParams());
        sendSmsAuthCoderRequest.setUserChannel(commonArg.getUserChannel());
        APIResponse<SendSmsAuthCodeV2Response> apiResponse =
                userApi.sendSmsAuthCodeV2(getInstance(sendSmsAuthCoderRequest));
        checkResponseWithoutLog(apiResponse);
        return ret;
    }

    @PostMapping(value = "/mobile/querySendChannel")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @AccountDefenseResource(name = "UserProtectController.querySendChannel")
    @DDoSPreMonitor(action = "protect.querySendChannel")
    public CommonRet<QuerySendChannelResp> querySendChannel(@RequestBody @Validated QueryProtectSendChannelArg queryProtectSendChannelArg)
            throws Exception {
        if (StringUtils.isBlank(queryProtectSendChannelArg.getMobile()) || StringUtils.isBlank(queryProtectSendChannelArg.getMobileCode())) {
            UserIdRequest request = new UserIdRequest();
            request.setUserId(getLoginUserId());
            APIResponse<UserSecurityVo> userSecurityResponse = userSecurityApi.getUserSecurityByUserId(getInstance(request));
            checkResponseWithoutLog(userSecurityResponse);
            queryProtectSendChannelArg.setMobile(userSecurityResponse.getData().getMobile());
            queryProtectSendChannelArg.setMobileCode(userSecurityResponse.getData().getMobileCode());
        }
        if (StringUtils.isBlank(queryProtectSendChannelArg.getMobile()) || StringUtils.isBlank(queryProtectSendChannelArg.getMobileCode())) {
            //未绑定手机
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        QuerySendMethodRequest querySendMethodRequest = new QuerySendMethodRequest();
        querySendMethodRequest.setMobileCode(queryProtectSendChannelArg.getMobileCode());
        if (!org.apache.commons.lang.StringUtils.isNumeric(queryProtectSendChannelArg.getMobileCode())) {
            //转换mobileCode
            GetCountryByCodeRequest getCountryByCodeRequest = new GetCountryByCodeRequest();
            getCountryByCodeRequest.setCode(queryProtectSendChannelArg.getMobileCode());
            APIResponse<CountryVo> countryVoAPIResponse = countryApi.getCountryByCode(getInstance(getCountryByCodeRequest));
            checkResponseWithoutLog(countryVoAPIResponse);
            querySendMethodRequest.setMobileCode(countryVoAPIResponse.getData().getMobileCode());
        }
        querySendMethodRequest.setRecipient(queryProtectSendChannelArg.getMobile());
        querySendMethodRequest.setResend(queryProtectSendChannelArg.getResend());
        querySendMethodRequest.setMsgType(com.binance.messaging.api.msg.request.MsgType.valueOf(queryProtectSendChannelArg.getMsgType().name()));
        APIResponse<String> apiResponse = msgApi.querySendOtpMethod(getInstance(querySendMethodRequest));
        checkResponseWithoutLog(apiResponse);
        QuerySendChannelResp resp = new QuerySendChannelResp();
        resp.setUserChannel(apiResponse.getData());
        return new CommonRet<>(resp);
    }

    @PostMapping(value = "/email/sendEmailVerifyCode")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "protect.sendEmailVerifyCode")
    @AccountDefenseResource(name = "UserProtectController.sendEmailVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<SendEmailVerifyCodeResponse> sendEmailVerifyCode(@RequestBody @Validated SendEmailVerifyCodeArg commonArg)
            throws Exception {
        validateParams(commonArg);
        CommonRet<SendEmailVerifyCodeResponse> ret = new CommonRet<>();
        String email = getTrueUserEmail();
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!baseHelper.isOk(emailApiResponse)) {
            log.warn("sendMobileVerifyCode:userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeRequest();
        sendEmailVerifyCodeRequest.setUserId(userId);
        sendEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());
        sendEmailVerifyCodeRequest.setParams(commonArg.getParams());
        sendEmailVerifyCodeRequest.setFlowId(commonArg.getFlowId());
        Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
        sendEmailVerifyCodeRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : deviceInfo);
        APIResponse<SendEmailVerifyCodeResponse> apiResponse;
        //CHANGE_EMAIL_V2 发送老邮箱用老接口，适配前端足见
        if(commonArg.getBizScene() != null && commonArg.getBizScene() == BizSceneEnum.CHANGE_EMAIL_V2){
            SendEmailVerifyCodeMoreTimeRequest sendEmailVerifyCodeMoreTimeRequest = new SendEmailVerifyCodeMoreTimeRequest();
            sendEmailVerifyCodeMoreTimeRequest.setUserId(userId);
            sendEmailVerifyCodeMoreTimeRequest.setResend(commonArg.getResend());
            sendEmailVerifyCodeMoreTimeRequest.setBizScene(commonArg.getBizScene());
            sendEmailVerifyCodeMoreTimeRequest.setParams(commonArg.getParams());
            sendEmailVerifyCodeMoreTimeRequest.setIsNewEmail(false);
            apiResponse = userSecurityApi.sendEmailVerifyCodeMoreTime(getInstance(sendEmailVerifyCodeMoreTimeRequest));
        } else if (versionHelper.checkAppVersion(sendEmailAndroidVersion, sendEmailIosVersion)) {
            apiResponse = userSecurityApi.sendEmailVerifyCodeV2(getInstance(sendEmailVerifyCodeRequest));
        } else {
            apiResponse = userSecurityApi.sendEmailVerifyCode(getInstance(sendEmailVerifyCodeRequest));
        }
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    private void validateParams(SendEmailVerifyCodeArg commonArg) throws Exception {
        // 提币需要校验addressTag（MEMO）和address
        if (validateWithdrawParam && commonArg.getBizScene() != null && commonArg.getBizScene() == BizSceneEnum.CRYPTO_WITHDRAW) {
            String asset = commonArg.getParams().getOrDefault("asset", "").toString();
            String address = commonArg.getParams().getOrDefault("address", "").toString();
            String addressTag = commonArg.getParams().getOrDefault("addressTag", "").toString();
            if (StringUtils.isBlank(asset)) {
                throw new BusinessException(AccountMgsErrorCode.WITHDRAW_ASSET_IS_EMPTY);
            }
            
            AddressVerifyRequest verifyRequest = new AddressVerifyRequest();
            verifyRequest.setCoin(asset);
            verifyRequest.setAddress(address);
            verifyRequest.setMemo(addressTag);
            log.info("validateWithdrawParam request {}", JSONObject.toJSONString(verifyRequest));
            APIResponse<Boolean> apiResponse = addressApi.verify(getInstance(verifyRequest));
            if (!this.baseHelper.isOk(apiResponse) || apiResponse.getData() == null || !apiResponse.getData()) {
                log.warn("validateWithdrawParam fail response={}", JSONObject.toJSONString(apiResponse));
                throw new BusinessException(AccountMgsErrorCode.WITHDRAW_ADDRESS_MEMO_FORMAT_INVALID);
            }
        }
    }

    /**
     * 发送绑定手机验证码, 半登录态下用新手机发送, 底层校验新手机是否已经被占用, 但忽略用户是否已经绑定过手机
     *
     * @return
     */
    @UserOperation(name = "发送绑定短信验证码", eventName = "sendBindMobileVerifyCode",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"mobile"})
    @PostMapping(value = "/mobile/sendBindMobileVerifyCode/byNewMobile")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "protect.sendBindMobileVerifyCodeByNew")
    @AccountDefenseResource(name = "UserProtectController.sendBindMobileVerifyCodeByNew")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendBindMobileVerifyCodeByNew(
            @RequestBody @Validated SendBindMobileVerifyCodeArg sendVerCodeArg) throws Exception {
        if (MsgType.VOICE == sendVerCodeArg.getMsgType()) {
            throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
        }
        Long loginUserId = getLoginUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, loginUserId));
        SendBindMobileVerifyCodeRequest sendVerCodeRequest = new SendBindMobileVerifyCodeRequest();
        sendVerCodeRequest.setUserId(loginUserId);
        sendVerCodeRequest.setVerifyCodeId(PKGenarator.getId());
        sendVerCodeRequest.setMobile(sendVerCodeArg.getMobile());
        sendVerCodeRequest.setMobileCode(sendVerCodeArg.getMobileCode());
        sendVerCodeRequest.setMsgType(sendVerCodeArg.getMsgType());
        sendVerCodeRequest.setResend(sendVerCodeArg.getResend());
        sendVerCodeRequest.setResetMobile(true);
        sendVerCodeRequest.setUserChannel(sendVerCodeArg.getUserChannel());
        APIResponse<SendBindMobileVerifyCodeResponse> sendVerCodeResponse =
                userSecurityApi.sendBindMobileVerifyCode(getInstance(sendVerCodeRequest));
        checkResponse(sendVerCodeResponse);
        RedisCacheUtils.set(CacheKey.getUserMobileCode(loginUserId.toString()), sendVerCodeArg.getMobileCode(),
                Constant.MOBILE_CODE_TIMEOUT);
        return new CommonRet<>();
    }

    /**
     * 发送绑定邮箱验证码, 半登录态下用新邮箱发送, 底层校验新邮箱是否已经被占用, 但忽略用户是否已经绑定过邮箱
     *
     * @return
     */
    @UserOperation(name = "发送绑定邮箱验证码", eventName = "sendBindEmailVerifyCode",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"email"})
    @PostMapping(value = "/email/sendBindEmailVerifyCode/byNewEmail")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "protect.sendBindEmailVerifyCodeByNewEmail")
    @AccountDefenseResource(name = "UserProtectController.sendBindEmailVerifyCodeByNewEmail")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<String> sendBindEmailVerifyCodeByNewEmail(
            @RequestBody @Validated SendBindEmailVerifyCodeArg sendVerCodeArg) throws Exception {
        Long loginUserId = getLoginUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, loginUserId));
        SendBindEmailVerifyCodeRequest sendVerCodeRequest = new SendBindEmailVerifyCodeRequest();
        sendVerCodeRequest.setUserId(loginUserId);
        sendVerCodeRequest.setEmail(sendVerCodeArg.getEmail());
        sendVerCodeRequest.setResend(sendVerCodeArg.getResend());
        sendVerCodeRequest.setResetEmail(true);
        APIResponse<SendBindEmailVerifyCodeResponse> sendVerCodeResponse = userSecurityApi.sendBindEmailVerifyCode(getInstance(sendVerCodeRequest));
        checkResponse(sendVerCodeResponse);

        ddosCacheSeviceHelper.setEmailCacheWithOperationEnum(loginUserId.toString(), DdosOperationEnum.DEVICE_AUTH);

        return new CommonRet<>();
    }

    @PostMapping(value = "/getVerificationTwoCheckList")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "protect.getVerificationTwoCheckList")
    public CommonRet<GetVerificationTwoCheckListRet> getVerificationTwoCheckList(@RequestBody @Validated GetVerificationTwoCheckListArg arg)
            throws Exception {
        GetVerificationTwoCheckListRequest getReq = new GetVerificationTwoCheckListRequest();
        getReq.setUserId(getLoginUserId());
        getReq.setBizScene(arg.getBizScene());
        getReq.setFlowId(arg.getFlowId());
        getReq.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getTrueUserEmail()));
        APIResponse<GetVerificationTwoCheckListResponse> getResp = userSecurityApi.getVerificationTwoCheckList(getInstanceByAccountVersion(getReq));
        checkResponseWithoutLog(getResp);

        GetVerificationTwoCheckListRet ret = new GetVerificationTwoCheckListRet();
        if (getResp.getData() != null) {
            // 绑定项
            if (!CollectionUtils.isEmpty(getResp.getData().getNeedBindVerifyList())) {
                getResp.getData().getNeedBindVerifyList().forEach(x -> {
                    GetVerificationTwoCheckListRet.VerificationTwoBind bind = new GetVerificationTwoCheckListRet.VerificationTwoBind();
                    bind.setVerifyType(x.getVerifyType().getCode());
                    bind.setBindOption(x.getOption());
                    ret.getNeedBindVerifyList().add(bind);
                });
            }

            // 校验项
            if (!CollectionUtils.isEmpty(getResp.getData().getNeedCheckVerifyList())) {
                getResp.getData().getNeedCheckVerifyList().forEach(x -> {
                    GetVerificationTwoCheckListRet.VerificationTwoCheck check = new GetVerificationTwoCheckListRet.VerificationTwoCheck();
                    check.setVerifyType(x.getVerifyType().getCode());
                    check.setVerifyTargetMask(x.getVerifyTargetMask());
                    check.setVerifyOption(x.getOption());
                    ret.getNeedCheckVerifyList().add(check);
                });
            }
        }
        return new CommonRet<>(ret);
    }
    
}
