package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserApi;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account2fa.api.Send2FaApi;
import com.binance.account2fa.api.Verify2FaApi;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.enums.MsgType;
import com.binance.account2fa.vo.request.GetVerificationTwoCheckListRequest;
import com.binance.account2fa.vo.request.SendEmailVerifyCodeRequest;
import com.binance.account2fa.vo.request.SendSmsAuthCodeRequest;
import com.binance.account2fa.vo.request.Verification2FaRequest;
import com.binance.account2fa.vo.response.GetVerificationTwoCheckListResponse;
import com.binance.account2fa.vo.response.SendEmailVerifyCodeResponse;
import com.binance.account2fa.vo.response.SendSmsAuthCodeResponse;
import com.binance.account2fa.vo.response.Verify2FaResponse;
import com.binance.capital.api.AddressApi;
import com.binance.capital.vo.address.request.AddressVerifyRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.account.vo.GetVerificationTwoCheckListRet;
import com.binance.mgs.account.account.vo.new2fa.GetVerificationTwoCheckListArg;
import com.binance.mgs.account.account.vo.new2fa.SendEmailVerifyCodeArg;
import com.binance.mgs.account.account.vo.new2fa.SendMobileVerifyCodeArg;
import com.binance.mgs.account.account.vo.new2fa.Verify2FaCodeArg;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.OTPSendLimit;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
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
@RequestMapping(value = "/v2/protect/account")
@Slf4j
public class UserProtect2FaController extends AccountBaseAction {

    @Resource
    private UserApi userApi;
    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Autowired
    private CheckHelper checkHelper;
    @Resource
    private ApiHelper apiHelper;
    @Autowired
    private Send2FaApi send2FaApi;
    @Autowired
    private Verify2FaApi verify2FaApi;
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

    @Value("${sendEmailVerifyCode.validateWithdrawParam.switch:false}")
    private boolean validateWithdrawParam;

    @Autowired
    private UserDeviceHelper userDeviceHelper;

    @PostMapping(value = "/mobile/sendMobileVerifyCode")
    @UserOperation(name = "发送短信验证码", eventName = "sendMobileVerifyCode",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "2faProtect.sendMobileVerifyCode")
    @AccountDefenseResource(name = "UserProtect2FaController.sendMobileVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendMobileVerifyCode(@RequestBody @Validated SendMobileVerifyCodeArg commonArg)
            throws Exception {
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.REGTYPE, commonArg.getMsgType()));

        // 如果是语音短信，检查是否在支持的国家内
        if (MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(commonArg.getMobileCode());
        }

        boolean isVnMobile="vn".equalsIgnoreCase
                (commonArg.getMobileCode());
        if (!isVnMobile && MsgType.VOICE == commonArg.getMsgType() && (BizSceneEnum.BIND_MOBILE.name().equalsIgnoreCase(commonArg.getBizScene())  || BizSceneEnum.BIND_MOBILE_EU.name().equalsIgnoreCase(commonArg.getBizScene()) || BizSceneEnum.ACCOUNT_ACTIVATE.name().equalsIgnoreCase(commonArg.getBizScene()))) {
            Long increment = RedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
            if (increment == 1l) {
                RedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
            if (increment >= sendBindMobileVerifyCodeThreshold) {
                log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
                throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
            }
        }
        Long loginUserId = getLoginUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, loginUserId));

        if(BizSceneEnum.BIND_MOBILE.name().equalsIgnoreCase(commonArg.getBizScene()) || BizSceneEnum.BIND_MOBILE_EU.name().equalsIgnoreCase(commonArg.getBizScene())){
            String bindMobileCacheKey=Constant.BIND_MOBILE_KEY+commonArg.getMsgType()+loginUserId;
            Long increment = RedisCacheUtils.increment(bindMobileCacheKey, 1);
            if (increment == 1l) {
                //有效期24小时
                RedisCacheUtils.expire(bindMobileCacheKey, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
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
        SendSmsAuthCodeRequest sendSmsAuthCoderRequest = new SendSmsAuthCodeRequest();
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
        APIResponse<SendSmsAuthCodeResponse> apiResponse =
                send2FaApi.sendSmsAuthCode(getInstance(sendSmsAuthCoderRequest));
        checkResponseWithoutLog(apiResponse);
        return ret;
    }

    @PostMapping(value = "/email/sendEmailVerifyCode")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "2faProtect.sendEmailVerifyCode")
    @AccountDefenseResource(name = "UserProtect2FaController.sendEmailVerifyCode")
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
        com.binance.account2fa.vo.request.SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeRequest();
        sendEmailVerifyCodeRequest.setUserId(userId);
        sendEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());
        sendEmailVerifyCodeRequest.setParams(commonArg.getParams());
        sendEmailVerifyCodeRequest.setFlowId(commonArg.getFlowId());
        Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
        sendEmailVerifyCodeRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : deviceInfo);
        APIResponse<SendEmailVerifyCodeResponse> apiResponse = send2FaApi.sendEmailVerifyCode(getInstance(sendEmailVerifyCodeRequest));

        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    private void validateParams(SendEmailVerifyCodeArg commonArg) throws Exception {
        // 提币需要校验addressTag（MEMO）和address
        if (validateWithdrawParam && commonArg.getBizScene() != null && ( BizSceneEnum.CRYPTO_WITHDRAW.name().equalsIgnoreCase(commonArg.getBizScene())
            || BizSceneEnum.CAPITAL_WITHDRAW.name().equalsIgnoreCase(commonArg.getBizScene()) || BizSceneEnum.P2P_PASS_WITHDRAW.name().equalsIgnoreCase(commonArg.getBizScene()))) {
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

    @PostMapping(value = "/getVerificationTwoCheckList")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "2faProtect.getVerificationTwoCheckList")
    public CommonRet<GetVerificationTwoCheckListRet> getVerificationTwoCheckList(@RequestBody @Validated GetVerificationTwoCheckListArg arg)
            throws Exception {
        GetVerificationTwoCheckListRequest getReq = new GetVerificationTwoCheckListRequest();
        getReq.setUserId(getLoginUserId());
        getReq.setBizScene(arg.getBizScene());
        getReq.setFlowId(arg.getFlowId());
        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        getReq.setClientId(clientId);
        getReq.setDeviceInfo(commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getTrueUserEmail()));
        getReq.setPlaceMap(arg.getPlaceMap());
        APIResponse<GetVerificationTwoCheckListResponse> getResp = verify2FaApi.getVerificationTwoCheckList(getInstanceByAccountVersion(getReq));
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
            ret.setRequestId(getResp.getData().getRequestId());
            ret.setLast2fa(getResp.getData().getLast2fa());
        }
        return new CommonRet<>(ret);
    }

    @PostMapping(value = "/verify2FaCode")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "2faProtect.verify2FaCode")
    public CommonRet<Verify2FaResponse> verify2FaCode(@RequestBody @Validated Verify2FaCodeArg arg)
            throws Exception {
        Verification2FaRequest request = new Verification2FaRequest();
        request.setUserId(getLoginUserId());
        request.setBizScene(arg.getBizScene());
        request.setClientId(userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest()));
        request.setRequestId(arg.getRequestId());
        request.setEmailVerifyCode(arg.getEmailVerifyCode());
        request.setMobileVerifyCode(arg.getMobileVerifyCode());
        request.setGoogleVerifyCode(arg.getGoogleVerifyCode());
        request.setYubikeyVerifyCode(arg.getYubikeyVerifyCode());
        request.setFidoVerifyCode(arg.getFidoVerifyCode());
        request.setFidoExternalVerifyCode(arg.getFidoExternalVerifyCode());
        request.setPasskeysVerifyCode(arg.getPasskeysVerifyCode());
        request.setRoamingVerifyCode(arg.getRoamingVerifyCode());
        request.setDeviceInfo(commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        log.info("protect verify2FaCode userId={} izScene={}", request.getUserId(), arg.getBizScene());
        APIResponse<Verify2FaResponse> apiResponse = verify2FaApi.verify2Fa(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }
}
