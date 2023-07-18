package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.security.enums.MsgType;
import com.binance.account.vo.security.request.BindEmailRequest;
import com.binance.account.vo.security.request.BindGoogleVerifyRequest;
import com.binance.account.vo.security.request.BindMobileRequest;
import com.binance.account.vo.security.request.DisableFastWithdrawSwitchRequest;
import com.binance.account.vo.security.request.EnableFastWithdrawSwitchRequest;
import com.binance.account.vo.security.request.SendBindEmailVerifyCodeRequest;
import com.binance.account.vo.security.request.SendBindMobileVerifyCodeRequest;
import com.binance.account.vo.security.request.UnbindEmailFrontRequest;
import com.binance.account.vo.security.request.UpdateSecurityKeyApplicationScenarioRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.response.BindEmailResponse;
import com.binance.account.vo.security.response.BindGoogleVerifyResponse;
import com.binance.account.vo.security.response.DisableFastWithdrawSwitchResponse;
import com.binance.account.vo.security.response.EnableFastWithdrawSwitchResponse;
import com.binance.account.vo.security.response.GoogleAuthKeyResp;
import com.binance.account.vo.security.response.SendBindEmailVerifyCodeResponse;
import com.binance.account.vo.security.response.SendBindMobileVerifyCodeResponse;
import com.binance.account2fa.api.Send2FaApi;
import com.binance.account2fa.api.UserVerificationExtraInfoApi;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.vo.UserVerificationExtraInfoVo;
import com.binance.account2fa.vo.request.GetUserVerificationExtraRequest;
import com.binance.account2fa.vo.request.SendNewEmailVerifyCodeRequest;
import com.binance.account2fa.vo.request.SendNewSmsCodeRequest;
import com.binance.account2fa.vo.response.SendEmailVerifyCodeResponse;
import com.binance.account2fa.vo.response.SendSmsAuthCodeResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.vo.BindEmailArg;
import com.binance.mgs.account.account.vo.BindGoogleVerifyArg;
import com.binance.mgs.account.account.vo.BindMobileArg;
import com.binance.mgs.account.account.vo.FastWithdrawSwitchArg;
import com.binance.mgs.account.account.vo.GenerateSecretKeyRet;
import com.binance.mgs.account.account.vo.SendBindEmailVerifyCodeArg;
import com.binance.mgs.account.account.vo.SendBindMobileVerifyCodeArg;
import com.binance.mgs.account.account.vo.UnBindEmailFrontArg;
import com.binance.mgs.account.account.vo.UpdateSecurityKeyApplicationScenarioArg;
import com.binance.mgs.account.account.vo.new2fa.CheckManageMFAArg;
import com.binance.mgs.account.account.vo.new2fa.GetUserVerificationBindRet;
import com.binance.mgs.account.account.vo.new2fa.SendNewEmailVerifyCodeArg;
import com.binance.mgs.account.account.vo.new2fa.SendNewMobileVerifyCodeArg;
import com.binance.mgs.account.advice.OTPSendLimit;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.service.RiskService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.constant.Constant;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.MaskUtil;
import com.binance.platform.mgs.utils.PKGenarator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/v1/private/account")
@Slf4j
public class UserSecurityController extends AccountBaseAction {

    @Resource
    private UserSecurityApi userSecurityApi;

    @Resource
    private AuthHelper authHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private CheckHelper checkHelper;
    @Autowired
    private Send2FaApi send2FaApi;
    @Resource
    private ApiHelper apiHelper;
    @Autowired
    private UserVerificationExtraInfoApi userVerificationExtraInfoApi;
    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Autowired
    private RiskService riskService;


    @Value("${send.bind.mobile.verify.code.period:86400}")
    private int sendBindMobileVerifyCodePeriod;

    @Value("${send.bind.mobile.verify.code.threshold:10}")
    private int sendBindMobileVerifyCodeThreshold;

    @Value("${send.bind.mobile.text.limit:3}")
    private int sendBindMobileTextLimit;
    @Value("${send.bind.mobile.voice.limit:3}")
    private int sendBindMobileVoiceLimit;
    @Value("${verificationBindInfo.useSecurity.updateTime:true}")
    private boolean verificationBindInfoUseSecurityUpdateTime;
    @Value("#{'${checkManageMFA.bizScene:BIND_GOOGLE,CHANGE_GOOGLE,UNBIND_GOOGLE,BIND_EMAIL,CHANGE_EMAIL_V2,BIND_MOBILE,UNBIND_MOBILE,CHANGE_MOBILE_V2,MODIFY_PASSWORD}'.split(',')}")
    private List<String> checkManageMFABizSceneList;


    /**
     * 生成Google密钥
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/user/generate-secret-key")
    public CommonRet<GenerateSecretKeyRet> generateSecretKey() throws Exception {
        UserIdRequest request = new UserIdRequest();
        request.setUserId(getUserId());
        APIResponse<GoogleAuthKeyResp> apiResponse = userSecurityApi.generateAuthKeyAndQrCode(getInstance(request));
        checkResponse(apiResponse);
        CommonRet<GenerateSecretKeyRet> ret = new CommonRet<>();
        GenerateSecretKeyRet data = new GenerateSecretKeyRet();
        GoogleAuthKeyResp googleAuthenticatorKey = apiResponse.getData();
        data.setSecretKey(googleAuthenticatorKey.getAuthKey());
        data.setQrcode(googleAuthenticatorKey.getQrCode());
        ret.setData(data);
        return ret;
    }


    /**
     * 绑定谷歌验证
     *
     * @param bindGoogleVerifyArg
     * @return
     */
    @PostMapping(value = "/user/bind-google-verify")
    @UserOperation(eventName = "bindGoogleVerify", name = "绑定谷歌验证",
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> bindGoogleVerify(
            @RequestBody @Validated BindGoogleVerifyArg bindGoogleVerifyArg) throws Exception {
        BindGoogleVerifyRequest bindRequest = new BindGoogleVerifyRequest();
        BeanUtils.copyProperties(bindGoogleVerifyArg, bindRequest);
        bindRequest.setUserId(getUserId());
        APIResponse<BindGoogleVerifyResponse> bindResponse = userSecurityApi.bindGoogleVerify(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 绑定手机
     *
     * @return
     */
    @PostMapping(value = "/user/bind-mobile")
    @UserOperation(name = "绑定手机", eventName = "bindMobile",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    public CommonRet<Integer> bindMobile(@RequestBody @Validated BindMobileArg bindMobileArg) throws Exception {
        BindMobileRequest bindRequest = new BindMobileRequest();
        BeanUtils.copyProperties(bindMobileArg, bindRequest);
        bindRequest.setUserId(getUserId());

        APIResponse<Integer> bindResponse = userSecurityApi.bindMobile(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 发送绑定手机验证码
     *
     * @return
     */
    @PostMapping(value = "/user/send-bind-mobile-verify-code")
    @DDoSPreMonitor(action = "security.sendBindMobileVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendBindMobileVerifyCode(
            @RequestBody @Validated SendBindMobileVerifyCodeArg sendVerCodeArg) throws Exception {

        if (MsgType.VOICE == sendVerCodeArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(sendVerCodeArg.getMobileCode());

            Long increment = RedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
            if (increment == 1l) {
                RedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment, sendVerCodeArg.getMobileCode(), sendVerCodeArg.getMobile());
            if (increment >= sendBindMobileVerifyCodeThreshold) {
                log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment, sendVerCodeArg.getMobileCode(), sendVerCodeArg.getMobile());
                throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
            }
        }

        SendBindMobileVerifyCodeRequest sendVerCodeRequest = new SendBindMobileVerifyCodeRequest();
        sendVerCodeRequest.setUserId(getUserId());
        sendVerCodeRequest.setVerifyCodeId(PKGenarator.getId());
        sendVerCodeRequest.setMobile(sendVerCodeArg.getMobile());
        sendVerCodeRequest.setMobileCode(sendVerCodeArg.getMobileCode());
        sendVerCodeRequest.setMsgType(sendVerCodeArg.getMsgType());
        sendVerCodeRequest.setResend(sendVerCodeArg.getResend());
        sendVerCodeRequest.setUserChannel(sendVerCodeArg.getUserChannel());
        APIResponse<SendBindMobileVerifyCodeResponse> sendVerCodeResponse =
                userSecurityApi.sendBindMobileVerifyCode(getInstance(sendVerCodeRequest));
        checkResponse(sendVerCodeResponse);
        RedisCacheUtils.set(CacheKey.getUserMobileCode(getUserIdStr()), sendVerCodeArg.getMobileCode(),
                Constant.MOBILE_CODE_TIMEOUT);
        return new CommonRet<>();
    }

    /**
     * 用户自己一键禁用账户
     *
     * @return
     */
    @PostMapping(value = "/user/forbidden-by-oneself")
    @DDoSPreMonitor(action = "security.forbiddenByOneself")
    @UserOperation(eventName = "forbiddenByOneSelf", name = "forbiddenByOneSelf", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> forbiddenByOneself(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(getUserId());
        APIResponse<Integer> apiResponse = userSecurityApi.forbiddenUserTotal(getInstance(userIdRequest));
        checkResponse(apiResponse);
        // 修改成功后，清理所有token，重新登录
        authHelper.logoutAll(getUserId());
        return new CommonRet<>();
    }

    @UserOperation(eventName = "更新Security Key应用场景", name = "更新Security Key应用场景",
            requestKeys = {"scenarios"}, requestKeyDisplayNames = {"scenarios"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/user/security-key/scenario/update")
    @DDoSPreMonitor(action = "security.updateSecurityKeyApplicationScenarios")
    public CommonRet<Void> updateSecurityKeyApplicationScenarios(@RequestBody @Validated UpdateSecurityKeyApplicationScenarioArg updateSecurityKeyApplicationScenarioArg) throws Exception {
        UpdateSecurityKeyApplicationScenarioRequest request = new UpdateSecurityKeyApplicationScenarioRequest();
        request.setScenarios(updateSecurityKeyApplicationScenarioArg.getScenarios());
        request.setCode(updateSecurityKeyApplicationScenarioArg.getCode());
        request.setUserId(getUserId());
        APIResponse<Void> apiResponse = userSecurityApi.updateSecurityKeyApplicationScenarios(APIRequest.instance(request));
        checkResponseAndLog2fa(apiResponse);
        return new CommonRet<>();
    }


    /**
     * 发送绑定邮箱验证码
     *
     * @return
     */
    @PostMapping(value = "/user/sendBindEmailVerifyCode")
    @DDoSPreMonitor(action = "security.sendBindEmailVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<String> sendBindEmailVerifyCode(
            @RequestBody @Validated SendBindEmailVerifyCodeArg sendVerCodeArg) throws Exception {

        SendBindEmailVerifyCodeRequest sendVerCodeRequest = new SendBindEmailVerifyCodeRequest();
        sendVerCodeRequest.setUserId(getUserId());
        sendVerCodeRequest.setEmail(sendVerCodeArg.getEmail());
        sendVerCodeRequest.setResend(sendVerCodeArg.getResend());
        APIResponse<SendBindEmailVerifyCodeResponse> sendVerCodeResponse = userSecurityApi.sendBindEmailVerifyCode(getInstance(sendVerCodeRequest));
        checkResponse(sendVerCodeResponse);
        return new CommonRet<>();
    }


    /**
     * 绑定邮箱
     *
     * @return
     */
    @PostMapping(value = "/user/bind-email")
    @UserOperation(name = "绑定邮箱", eventName = "bindEmail",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "security.bindEmail")
    public CommonRet<Integer> bindEmail(@RequestBody @Validated BindEmailArg bindEmailArg) throws Exception {
        BindEmailRequest bindRequest = new BindEmailRequest();
        BeanUtils.copyProperties(bindEmailArg, bindRequest);
        bindRequest.setUserId(getUserId());
        APIResponse<BindEmailResponse> bindResponse = userSecurityApi.bindEmail(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 绑定邮箱
     *
     * @return
     */
    @PostMapping(value = "/user/unbind-email")
    @UserOperation(name = "解绑邮箱", eventName = "unbindEmail",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "security.unbindEmail")
    public CommonRet<Boolean> unbindEmail(@RequestBody @Validated UnBindEmailFrontArg unBindEmailFrontArg) throws Exception {
        UnbindEmailFrontRequest unbindRequest = new UnbindEmailFrontRequest();
        BeanUtils.copyProperties(unBindEmailFrontArg, unbindRequest);
        unbindRequest.setUserId(getUserId());
        unbindRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<Boolean> bindResponse = userSecurityApi.unbindEmailFront(getInstance(unbindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 开启关闭站内转账
     *
     * @return
     */
    @PostMapping(value = "/user/enableFastWithdraw")
    @UserOperation(eventName = "enableFastWithdraw", name = "开启关闭站内转账", sendToSensorData = true, sensorsRequestKeys = {"dummy"},
            requestKeys = {"enableFastWithdraw"}, requestKeyDisplayNames = {"是否开启站内转账"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> enableFastWithdraw(@RequestBody @Validated FastWithdrawSwitchArg fastWithdrawSwitchArg)
            throws Exception {

        if (fastWithdrawSwitchArg.getEnableFastWithdraw()) {
            EnableFastWithdrawSwitchRequest fastWithdrawSwitchRequest = new EnableFastWithdrawSwitchRequest();
            fastWithdrawSwitchRequest.setUserId(getUserId());
            APIResponse<EnableFastWithdrawSwitchResponse> enableResponse = userSecurityApi.enableFastWithdrawSwitch(getInstance(fastWithdrawSwitchRequest));
            checkResponseAndLog2fa(enableResponse);
        } else {
            DisableFastWithdrawSwitchRequest fastWithdrawSwitchRequest = new DisableFastWithdrawSwitchRequest();
            fastWithdrawSwitchRequest.setUserId(getUserId());
            APIResponse<DisableFastWithdrawSwitchResponse> disableResponse = userSecurityApi.disableFastWithdrawSwitch(getInstance(fastWithdrawSwitchRequest));
            checkResponseAndLog2fa(disableResponse);
        }
        return new CommonRet<>();
    }

    @PostMapping(value = "/mobile/sendNewMobileVerifyCode")
    @UserOperation(name = "发送短信验证码", eventName = "sendMobileVerifyCode",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "sendNewMobileVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendNewMobileVerifyCode(@RequestBody @Validated SendNewMobileVerifyCodeArg commonArg)
            throws Exception {
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.REGTYPE, commonArg.getMsgType()));

        // 如果是语音短信，检查是否在支持的国家内
        if (com.binance.account2fa.enums.MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(commonArg.getMobileCode());
        }

        boolean isVnMobile = "vn".equalsIgnoreCase
                (commonArg.getMobileCode());
        if (!isVnMobile && com.binance.account2fa.enums.MsgType.VOICE == commonArg.getMsgType() && (BizSceneEnum.BIND_MOBILE.name().equalsIgnoreCase(commonArg.getBizScene()) || BizSceneEnum.BIND_MOBILE_EU.name().equalsIgnoreCase(commonArg.getBizScene()) || BizSceneEnum.ACCOUNT_ACTIVATE.name().equalsIgnoreCase(commonArg.getBizScene()))) {
            Long increment = RedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
            if (increment == 1l) {
                RedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment, commonArg.getMobileCode(), commonArg.getMobile());
            if (increment >= sendBindMobileVerifyCodeThreshold) {
                log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment, commonArg.getMobileCode(), commonArg.getMobile());
                throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
            }
        }
        Long loginUserId = getUserId();
        boolean checkMobileExist = false;
        if (BizSceneEnum.BIND_MOBILE.name().equalsIgnoreCase(commonArg.getBizScene()) || BizSceneEnum.BIND_MOBILE_EU.name().equalsIgnoreCase(commonArg.getBizScene())
                || BizSceneEnum.CHANGE_MOBILE_V2.name().equalsIgnoreCase(commonArg.getBizScene())) {
            String bindMobileCacheKey = Constant.BIND_MOBILE_KEY + commonArg.getMsgType() + loginUserId;
            Long increment = RedisCacheUtils.increment(bindMobileCacheKey, 1);
            if (increment == 1l) {
                //有效期24小时
                RedisCacheUtils.expire(bindMobileCacheKey, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("bind mobile ，loginUserId={},msgtype={},send num{},mobileCode:{},mobile:{}", loginUserId, commonArg.getMsgType(), increment, commonArg.getMobileCode(), commonArg.getMobile());
            if (com.binance.account2fa.enums.MsgType.VOICE == commonArg.getMsgType() && increment >= sendBindMobileVoiceLimit) {
                log.info("overlimit bind mobile ，loginUserId={},msgtype={},send num{},mobileCode:{},mobile:{}", loginUserId, commonArg.getMsgType(), increment, commonArg.getMobileCode(), commonArg.getMobile());
                throw new BusinessException(AccountMgsErrorCode.BIND_MOBILE_SEND_MOBILE_CODE_OVERLIMIT);
            }
            if (com.binance.account2fa.enums.MsgType.TEXT == commonArg.getMsgType() && increment >= sendBindMobileTextLimit) {
                log.info("overlimit bind mobile ，loginUserId={},msgtype={},send num{},mobileCode:{},mobile:{}", loginUserId, commonArg.getMsgType(), increment, commonArg.getMobileCode(), commonArg.getMobile());
                throw new BusinessException(AccountMgsErrorCode.BIND_MOBILE_SEND_MOBILE_CODE_OVERLIMIT);
            }
            checkMobileExist = true;
        }


        CommonRet<String> ret = new CommonRet<>();
        String verifyCodeId = PKGenarator.getId();
        SendNewSmsCodeRequest sendNewSmsCodeRequest = new SendNewSmsCodeRequest();
        sendNewSmsCodeRequest.setUserId(loginUserId);
        sendNewSmsCodeRequest.setMobileCode(commonArg.getMobileCode());
        sendNewSmsCodeRequest.setMobile(commonArg.getMobile());
        sendNewSmsCodeRequest.setVerifyCodeId(verifyCodeId);
        sendNewSmsCodeRequest.setMsgType(commonArg.getMsgType());
        sendNewSmsCodeRequest.setResend(commonArg.getResend());
        sendNewSmsCodeRequest.setBizScene(commonArg.getBizScene());
        sendNewSmsCodeRequest.setUserChannel(commonArg.getUserChannel());
        sendNewSmsCodeRequest.setParams(commonArg.getParams());
        sendNewSmsCodeRequest.setCheckMobileExist(checkMobileExist);
        APIResponse<SendSmsAuthCodeResponse> apiResponse =
                send2FaApi.sendNewSmsCode(getInstance(sendNewSmsCodeRequest));
        checkResponseWithoutLog(apiResponse);
        return ret;
    }

    @PostMapping(value = "/email/sendNewEmailVerifyCode")
    @DDoSPreMonitor(action = "sendNewEmailVerifyCode")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<SendEmailVerifyCodeResponse> sendNewEmailVerifyCode(@RequestBody @Validated SendNewEmailVerifyCodeArg commonArg)
            throws Exception {

        boolean checkEmailExist = false;
        if (BizSceneEnum.BIND_EMAIL.name().equalsIgnoreCase(commonArg.getBizScene())
                || BizSceneEnum.CHANGE_EMAIL_V2.name().equalsIgnoreCase(commonArg.getBizScene())) {
            checkEmailExist = true;
        }

        Long userId = getUserId();
        SendNewEmailVerifyCodeRequest sendNewEmailVerifyCodeRequest = new SendNewEmailVerifyCodeRequest();
        sendNewEmailVerifyCodeRequest.setUserId(userId);
        sendNewEmailVerifyCodeRequest.setEmail(commonArg.getEmail());
        sendNewEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendNewEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());
        sendNewEmailVerifyCodeRequest.setParams(commonArg.getParams());
        Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
        sendNewEmailVerifyCodeRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : deviceInfo);
        sendNewEmailVerifyCodeRequest.setCheckEmailExist(checkEmailExist);
        APIResponse<SendEmailVerifyCodeResponse> apiResponse = send2FaApi.sendNewEmailVerifyCode(getInstance(sendNewEmailVerifyCodeRequest));

        checkResponseWithoutLog(apiResponse);
        CommonRet<SendEmailVerifyCodeResponse> ret = new CommonRet<>();
        ret.setData(apiResponse.getData());
        return ret;
    }

    @GetMapping(value = "/security/getUserVerificationBindInfo")
    public CommonRet<GetUserVerificationBindRet> getUserVerificationBindInfo() throws Exception {
        Long userId = getUserId();
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<UserSecurityVo> userSecurityApiResponse = userSecurityApi.getUserSecurityByUserId(APIRequest.instance(userIdRequest));
        checkResponseWithoutLog(userSecurityApiResponse);
        UserSecurityVo userSecurityVo = userSecurityApiResponse.getData();

        GetUserVerificationExtraRequest verificationExtraRequest = new GetUserVerificationExtraRequest();
        verificationExtraRequest.setUserId(userId);
        APIResponse<UserVerificationExtraInfoVo> verificationExtraInfoAPIResponse = userVerificationExtraInfoApi.getUserVerificationExtra(APIRequest.instance(verificationExtraRequest));
        checkResponseWithoutLog(verificationExtraInfoAPIResponse);
        UserVerificationExtraInfoVo userVerificationExtraInfo = verificationExtraInfoAPIResponse.getData();

        GetUserVerificationBindRet userVerificationBindRet = new GetUserVerificationBindRet();
        if (StringUtils.isNotBlank(userSecurityVo.getEmail())) {
            userVerificationBindRet.setIsBindEmail(true);
            userVerificationBindRet.setEmail(userSecurityVo.getEmail());
            if (userVerificationExtraInfo != null) {
                userVerificationBindRet.setBindEmailTime(userVerificationExtraInfo.getBindEmailTime());
            }
        } else {
            userVerificationBindRet.setIsBindEmail(false);
        }
        if (StringUtils.isNotBlank(userSecurityVo.getMobile())) {
            userVerificationBindRet.setIsBindMobile(true);
            userVerificationBindRet.setMobileCode(userSecurityVo.getMobileCode());
            if (baseHelper.isFromWeb()) {
                userVerificationBindRet.setMobile(MaskUtil.maskMobileNo(userSecurityVo.getMobile()));
            } else {
                userVerificationBindRet.setMobile(userSecurityVo.getMobile());
            }
            if (userVerificationExtraInfo != null) {
                userVerificationBindRet.setBindMobileTime(userVerificationExtraInfo.getBindMobileTime());
            }
        } else {
            userVerificationBindRet.setIsBindMobile(false);
        }
        if (StringUtils.isNotBlank(userSecurityVo.getAuthKey())) {
            userVerificationBindRet.setIsBindGoogle(true);
            if (userVerificationExtraInfo != null) {
                userVerificationBindRet.setBindGoogleTime(userVerificationExtraInfo.getBindGoogleTime());
            }
        } else {
            userVerificationBindRet.setIsBindGoogle(false);
        }

        if (verificationBindInfoUseSecurityUpdateTime) {
            if (userVerificationBindRet.getIsBindEmail() && userVerificationBindRet.getBindEmailTime() == null) {
                userVerificationBindRet.setBindEmailTime(userSecurityVo.getUpdateTime());
            }
            if (userVerificationBindRet.getIsBindMobile() && userVerificationBindRet.getBindMobileTime() == null) {
                userVerificationBindRet.setBindMobileTime(userSecurityVo.getUpdateTime());
            }
            if (userVerificationBindRet.getIsBindGoogle() && userVerificationBindRet.getBindGoogleTime() == null) {
                userVerificationBindRet.setBindGoogleTime(userSecurityVo.getUpdateTime());
            }
        }
        CommonRet<GetUserVerificationBindRet> ret = new CommonRet<>();
        ret.setData(userVerificationBindRet);
        return ret;
    }

    @PostMapping("/user/checkManageMFA")
    @DDoSPreMonitor(action = "checkManageMFA")
    public CommonRet<Boolean> checkManageMFA(@RequestBody @Valid CheckManageMFAArg arg) throws Exception {
        if (!checkManageMFABizSceneList.contains(StringUtils.upperCase(arg.getBizScene()))) {
            log.warn("unsupported bizScene: {}", arg.getBizScene());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, arg.getBizScene());
        return new CommonRet<>(true);
    }
}
