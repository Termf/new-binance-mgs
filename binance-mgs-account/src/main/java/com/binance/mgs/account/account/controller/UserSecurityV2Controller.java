package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.enums.MsgType;
import com.binance.account.vo.security.request.BindEmailRequest;
import com.binance.account.vo.security.request.BindGoogleVerifyV2Request;
import com.binance.account.vo.security.request.BindMobileV2Request;
import com.binance.account.vo.security.request.ChangeEmailV2Request;
import com.binance.account.vo.security.request.ChangeGoogleVerifyRequest;
import com.binance.account.vo.security.request.ChangeMobileV2Request;
import com.binance.account.vo.security.request.SendEmailVerifyCodeMoreTimeRequest;
import com.binance.account.vo.security.request.UnbindGoogleV2Request;
import com.binance.account.vo.security.request.UnbindMobileV2Request;
import com.binance.account.vo.security.response.BindEmailResponse;
import com.binance.account.vo.security.response.BindGoogleVerifyV2Response;
import com.binance.account.vo.security.response.ChangeGoogleVerifyResponse;
import com.binance.account.vo.security.response.SendEmailVerifyCodeResponse;
import com.binance.account.vo.security.response.UnbindGoogleVerifyV2Response;
import com.binance.account.vo.security.response.UnbindMobileV2Response;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.request.SafePasswordVerifyRequest;
import com.binance.account.vo.user.request.SendSmsAuthCodeMoreTimeRequest;
import com.binance.account.vo.user.request.UpdatePwdUserV2Request;
import com.binance.account.vo.user.request.UpdatePwdUserV3Request;
import com.binance.account.vo.user.response.UpdatePwdUserV2Response;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.helper.UserReqInterceptHelper;
import com.binance.mgs.account.account.vo.BindEmailArgV2;
import com.binance.mgs.account.account.vo.BindGoogleVerifyV2Arg;
import com.binance.mgs.account.account.vo.BindGoogleVerifyV3Arg;
import com.binance.mgs.account.account.vo.BindMobileV2Arg;
import com.binance.mgs.account.account.vo.BindMobileV3Arg;
import com.binance.mgs.account.account.vo.ChangeGoogleVerifyArg;
import com.binance.mgs.account.account.vo.ChangeUserEmailArg;
import com.binance.mgs.account.account.vo.ChangeUserEmailArgV3;
import com.binance.mgs.account.account.vo.ChangeUserMobileArg;
import com.binance.mgs.account.account.vo.ChangeUserMobileArgV3;
import com.binance.mgs.account.account.vo.SafePasswordVerifyArg;
import com.binance.mgs.account.account.vo.SendEmailVerifyCodeForChangeArg;
import com.binance.mgs.account.account.vo.SendMobileVerifyCodeForChangeArg;
import com.binance.mgs.account.account.vo.UnbindGoogleVerifyV2Arg;
import com.binance.mgs.account.account.vo.UnbindGoogleVerifyV3Arg;
import com.binance.mgs.account.account.vo.UnbindMobileV2Arg;
import com.binance.mgs.account.account.vo.UnbindMobileV3Arg;
import com.binance.mgs.account.account.vo.UpdateUserPasswordV2Arg;
import com.binance.mgs.account.account.vo.UpdateUserPasswordV2Ret;
import com.binance.mgs.account.account.vo.UpdateUserPasswordV3Arg;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.OTPSendLimit;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.service.RiskService;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.Constant;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.EnumErrorLogType;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.PKGenarator;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: mingming.sheng
 * @Date: 2020/5/12 5:32 下午
 */
@RestController
@Slf4j
public class UserSecurityV2Controller extends AccountBaseAction {
    @Resource
    private AuthHelper authHelper;
    @Resource
    private UserSecurityApi userSecurityApi;
    @Resource
    private UserReqInterceptHelper reqInterceptHelper;
    @Resource
    private RiskHelper riskHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private CheckHelper checkHelper;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;

    @Value("${reset2fa.next.request.count:50}")
    private int reset2faNextReqCount;
    @Value("${reset2fa.next.request.expireTime:3600}")
    private int reset2faNextReqExpireTime;
    @Value("${reset2fa.next.request.lockTime:86400}")
    private int retReset2faNextReqLockTime;
    @Value("${send.bind.mobile.verify.code.period:86400}")
    private int sendBindMobileVerifyCodePeriod;
    @Value("${send.bind.mobile.verify.code.threshold:10}")
    private int sendBindMobileVerifyCodeThreshold;
    @Value("${send.bind.mobile.text.limit:3}")
    private int sendBindMobileTextLimit;
    @Value("${send.bind.mobile.voice.limit:3}")
    private int sendBindMobileVoiceLimit;

    @Resource
    private RiskService riskService;


    @PostMapping(value = "/v2/private/account/user/updatePassword")
    @UserOperation(name = "用户修改密码", eventName = "updatePassword", logDeviceOperation = true,
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.updateUserPasswordV2")
    @AccountDefenseResource(name = "UserSecurityV2Controller.updateUserPasswordV2")
    public CommonRet<UpdateUserPasswordV2Ret> updateUserPasswordV2(@RequestBody @Validated UpdateUserPasswordV2Arg updateUserPasswordArg,
                                                                   HttpServletRequest request) throws Exception {
        if (StringUtils.equals(updateUserPasswordArg.getNewPassword(), updateUserPasswordArg.getOldPassword())) {
            throw new BusinessException(GeneralCode.GW_NEWPWD_EQUAL_OLDPWD);
        }

        if (StringUtils.equals(updateUserPasswordArg.getNewSafePassword(), updateUserPasswordArg.getOldSafePassword())) {
            throw new BusinessException(GeneralCode.GW_NEWPWD_EQUAL_OLDPWD);
        }

        UpdatePwdUserV2Request updatePwdUserRequest = new UpdatePwdUserV2Request();
        BeanUtils.copyProperties(updateUserPasswordArg, updatePwdUserRequest);
        updatePwdUserRequest.setUserId(getUserId());
        updatePwdUserRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<UpdatePwdUserV2Response> apiResponse = userApi.updatePwdV2(getInstance(updatePwdUserRequest));
        if (StringUtils.equals(GeneralCode.USER_UPDATE_PASSWORD_ERROR.getCode(), apiResponse.getCode())) {
            log.info("密码错误次数太多，强制退出，userId={}", getUserId());
            // 失败3次退出
            authHelper.logout(request);
            checkResponse(apiResponse);
        } else {
            checkResponseAndLog2fa(apiResponse);
            // 修改成功后，清理所有token，重新登录
            authHelper.logoutAll(updatePwdUserRequest.getUserId());
        }
        return new CommonRet<>();
    }

    @PostMapping(value = "/v3/private/account/user/updatePassword")
    @UserOperation(name = "用户修改密码", eventName = "updatePassword", logDeviceOperation = true,
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.updateUserPasswordV3")
    @AccountDefenseResource(name = "UserSecurityV2Controller.updateUserPasswordV2")
    public CommonRet<UpdateUserPasswordV2Ret> updateUserPasswordV3(@RequestBody @Validated UpdateUserPasswordV3Arg updateUserPasswordArg,
                                                                   HttpServletRequest request) throws Exception {

        Map<String, String> deviceMap = userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceMap, BizSceneEnum.MODIFY_PASSWORD.name());
        
        UpdatePwdUserV3Request updatePwdUserRequest = new UpdatePwdUserV3Request();
        BeanUtils.copyProperties(updateUserPasswordArg, updatePwdUserRequest);
        updatePwdUserRequest.setUserId(getUserId());
        updatePwdUserRequest.setDeviceInfo(deviceMap);
        APIResponse<UpdatePwdUserV2Response> apiResponse = userApi.updatePwdV3(getInstance(updatePwdUserRequest));
        checkResponse(apiResponse);
        // 修改成功后，清理所有token，重新登录
        authHelper.logoutAll(updatePwdUserRequest.getUserId());
        return new CommonRet<>();
    }

    /**
     * 绑定手机
     *
     * @return
     */
    @PostMapping(value = "/v2/private/account/user/bindMobile")
    @UserOperation(name = "绑定手机", eventName = "bindMobile",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "securityV2.bindMobileV2")
    @AccountDefenseResource(name = "UserSecurityV2Controller.bindMobileV2")
    public CommonRet<Integer> bindMobileV2(@RequestBody @Validated BindMobileV2Arg bindMobileArg) throws Exception {
        BindMobileV2Request bindRequest = new BindMobileV2Request();
        BeanUtils.copyProperties(bindMobileArg, bindRequest);
        bindRequest.setUserId(getUserId());
        APIResponse<Integer> bindResponse = userSecurityApi.bindMobileV2(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 解绑手机
     *
     * @return
     */
    @PostMapping(value = "/v2/private/account/user/unbindMobile")
    @UserOperation(eventName = "unbindMobile", name = "解绑手机", sendToSensorData = true, sensorsRequestKeys = {"dummy"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.unbindMobileV2")
    @AccountDefenseResource(name = "UserSecurityV2Controller.unbindMobileV2")
    public CommonRet<String> unbindMobileV2(@RequestBody @Validated UnbindMobileV2Arg unbindMobileArg)
            throws Exception {
        // 发送数据到风控
        riskHelper.frequentUnfaSendMq(getUserId(), getTrueUserEmail(), EnumErrorLogType.FUFA__MOBILE);

        UnbindMobileV2Request unbindRequest = new UnbindMobileV2Request();
        BeanUtils.copyProperties(unbindMobileArg, unbindRequest);
        unbindRequest.setUserId(getUserId());
        unbindRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<UnbindMobileV2Response> unbindResponse = userSecurityApi.unbindMobileV2(getInstance(unbindRequest));

        checkResponseAndLog2fa(unbindResponse);
        return new CommonRet<>();
    }

    /**
     * 绑定谷歌验证
     *
     * @param bindGoogleVerifyArg
     * @return
     */
    @PostMapping(value = "/v2/private/account/user/bindGoogleVerify")
    @UserOperation(eventName = "bindGoogleVerify", name = "绑定谷歌验证",
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.bindGoogleVerify")
    @AccountDefenseResource(name = "UserSecurityV2Controller.bindGoogleVerify")
    public CommonRet<String> bindGoogleVerify(
            @RequestBody @Validated BindGoogleVerifyV2Arg bindGoogleVerifyArg) throws Exception {
        BindGoogleVerifyV2Request bindRequest = new BindGoogleVerifyV2Request();
        BeanUtils.copyProperties(bindGoogleVerifyArg, bindRequest);
        bindRequest.setUserId(getUserId());
        APIResponse<BindGoogleVerifyV2Response> bindResponse = userSecurityApi.bindGoogleVerifyV2(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    @PostMapping(value = "/v3/private/account/user/bindGoogleVerify")
    @UserOperation(eventName = "bindGoogleVerifyV3", name = "绑定谷歌验证",
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.bindGoogleVerifyV3")
    @AccountDefenseResource(name = "UserSecurityV2Controller.bindGoogleVerify")
    public CommonRet<String> bindGoogleVerifyV3(
            @RequestBody @Validated BindGoogleVerifyV3Arg bindGoogleVerifyArg) throws Exception {

        HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.BIND_GOOGLE.name());

        BindGoogleVerifyV2Request bindRequest = new BindGoogleVerifyV2Request();
        BeanUtils.copyProperties(bindGoogleVerifyArg, bindRequest);
        bindRequest.setUserId(getUserId());
        bindRequest.setAlreadyCheckMFA(true);
        APIResponse<BindGoogleVerifyV2Response> bindResponse = userSecurityApi.bindGoogleVerifyV2(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 解绑谷歌验证
     *
     * @return
     */
    @PostMapping(value = "/v2/private/account/user/unbindGoogleVerify")
    @UserOperation(eventName = "unbindGoogleVerify", name = "解绑谷歌验证",
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.unbindGoogleVerify")
    @AccountDefenseResource(name = "UserSecurityV2Controller.unbindGoogleVerify")
    public CommonRet<String> unbindGoogleVerify(
            @RequestBody @Validated UnbindGoogleVerifyV2Arg unbindGoogleVerifyArg) throws Exception {

        // 检查多次调用问题
        reqInterceptHelper.reqIntercept("userunbindgoogleverify", String.valueOf(getUserId()),
                reset2faNextReqCount, reset2faNextReqExpireTime, retReset2faNextReqLockTime);

        // 发送数据到风控
        riskHelper.frequentUnfaSendMq(getUserId(), getTrueUserEmail(), EnumErrorLogType.FUFA_GOOGLE);

        UnbindGoogleV2Request unbindRequest = new UnbindGoogleV2Request();
        BeanUtils.copyProperties(unbindGoogleVerifyArg, unbindRequest);
        unbindRequest.setUserId(getUserId());
        unbindRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));

        APIResponse<UnbindGoogleVerifyV2Response> unbindResponse =
                userSecurityApi.unbindGoogleVerifyV2(getInstance(unbindRequest));
        checkResponseAndLog2fa(unbindResponse);

        return new CommonRet<>();
    }

    @PostMapping(value = "/v3/private/account/user/unbindGoogleVerify")
    @UserOperation(eventName = "unbindGoogleVerify", name = "解绑谷歌验证",
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.unbindGoogleVerifyV3")
    @AccountDefenseResource(name = "UserSecurityV2Controller.unbindGoogleVerify")
    public CommonRet<String> unbindGoogleVerifyV3(
            @RequestBody @Validated UnbindGoogleVerifyV3Arg unbindGoogleVerifyArg) throws Exception {

        // 检查多次调用问题
        reqInterceptHelper.reqIntercept("userunbindgoogleverify", String.valueOf(getUserId()),
                reset2faNextReqCount, reset2faNextReqExpireTime, retReset2faNextReqLockTime);

        // 发送数据到风控
        riskHelper.frequentUnfaSendMq(getUserId(), getTrueUserEmail(), EnumErrorLogType.FUFA_GOOGLE);

        HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.UNBIND_GOOGLE.name());

        UnbindGoogleV2Request unbindRequest = new UnbindGoogleV2Request();
        BeanUtils.copyProperties(unbindGoogleVerifyArg, unbindRequest);
        unbindRequest.setUserId(getUserId());
        unbindRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        unbindRequest.setAlreadyCheckMFA(true);

        APIResponse<UnbindGoogleVerifyV2Response> unbindResponse =
                userSecurityApi.unbindGoogleVerifyV2(getInstance(unbindRequest));
        checkResponseAndLog2fa(unbindResponse);

        return new CommonRet<>();
    }

    @PostMapping(value = "/v3/private/account/user/changeGoogleVerify")
    @UserOperation(eventName = "changeGoogleVerify", name = "换绑谷歌验证",
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.changeGoogleVerify")
    @AccountDefenseResource(name = "UserSecurityV2Controller.changeGoogleVerify")
    public CommonRet<String> changeGoogleVerify(
            @RequestBody @Validated ChangeGoogleVerifyArg changeGoogleVerifyArg) throws Exception {

        HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.CHANGE_GOOGLE.name());

        ChangeGoogleVerifyRequest request = new ChangeGoogleVerifyRequest();
        BeanUtils.copyProperties(changeGoogleVerifyArg, request);
        request.setUserId(getUserId());
        request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<ChangeGoogleVerifyResponse> bindResponse = userSecurityApi.changeGoogleVerifyWithoutMFA(getInstance(request));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    @PostMapping(value = "/v2/private/account/mobile/sendMobileVerifyCodeForChange")
    @UserOperation(name = "发送短信验证码", eventName = "sendMobileVerifyCode",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "securityV2.sendMobileVerifyCodeForChange")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<Boolean> sendMobileVerifyCodeForChange(@RequestBody @Validated SendMobileVerifyCodeForChangeArg commonArg)
            throws Exception {
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.REGTYPE, commonArg.getMsgType()));

        // 如果是语音短信，检查是否在支持的国家内
        if (MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(commonArg.getMobileCode());
        }

        boolean isVnMobile="vn".equalsIgnoreCase(commonArg.getMobileCode());
        if (!isVnMobile && MsgType.VOICE == commonArg.getMsgType() && (BizSceneEnum.BIND_MOBILE == commonArg.getBizScene() || BizSceneEnum.ACCOUNT_ACTIVATE == commonArg.getBizScene())) {
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
        Long loginUserId=getUserId();
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

        CommonRet<Boolean> ret = new CommonRet<>();
        String verifyCodeId = PKGenarator.getId();
        SendSmsAuthCodeMoreTimeRequest sendSmsAuthCoderRequest = new SendSmsAuthCodeMoreTimeRequest();
        sendSmsAuthCoderRequest.setUserId(getUserId());
        sendSmsAuthCoderRequest.setMobileCode(commonArg.getMobileCode());
        sendSmsAuthCoderRequest.setMobile(commonArg.getMobile());
        sendSmsAuthCoderRequest.setVerifyCodeId(verifyCodeId);
        sendSmsAuthCoderRequest.setMsgType(commonArg.getMsgType());
        sendSmsAuthCoderRequest.setResend(commonArg.getResend());
        sendSmsAuthCoderRequest.setBizScene(commonArg.getBizScene());
        sendSmsAuthCoderRequest.setParams(commonArg.getParams());
        sendSmsAuthCoderRequest.setIsNewMobile(commonArg.getIsNewMobile());
        sendSmsAuthCoderRequest.setUserChannel(commonArg.getUserChannel());
        APIResponse<Boolean> apiResponse =
                userApi.sendSmsAuthCodeMoreTime(getInstance(sendSmsAuthCoderRequest));
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }


    @PostMapping(value = "/v2/private/account/email/sendEmailVerifyCodeForChange")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "securityV2.sendEmailVerifyCodeForChange")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<SendEmailVerifyCodeResponse> sendEmailVerifyCodeForChange(@RequestBody @Validated SendEmailVerifyCodeForChangeArg commonArg)
            throws Exception {
        if(StringUtils.isNotBlank(commonArg.getEmail()) && !timeOutRegexUtils.validateEmail(commonArg.getEmail())){
            throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
        }
        CommonRet<SendEmailVerifyCodeResponse> ret = new CommonRet<>();
        String email = getTrueUserEmail();
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!baseHelper.isOk(emailApiResponse)) {
            log.warn("sendEmailVerifyCodeForChange:userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        SendEmailVerifyCodeMoreTimeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeMoreTimeRequest();
        sendEmailVerifyCodeRequest.setUserId(userId);
        sendEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());
        sendEmailVerifyCodeRequest.setParams(commonArg.getParams());
        sendEmailVerifyCodeRequest.setIsNewEmail(commonArg.getIsNewEmail());
        sendEmailVerifyCodeRequest.setEmail(commonArg.getEmail());
        APIResponse<SendEmailVerifyCodeResponse> apiResponse = userSecurityApi.sendEmailVerifyCodeMoreTime(getInstance(sendEmailVerifyCodeRequest));
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }


    @PostMapping(value = "/v2/private/account/email/change")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @UserOperation(name = "更改邮箱", eventName = "changeEmail",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"newEmail"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "securityV2.changeUserEmail")
    public CommonRet<Boolean> changeUserEmail(@RequestBody @Validated ChangeUserEmailArg commonArg)
            throws Exception {
        CommonRet<Boolean> ret = new CommonRet<>();
        Long userId = getUserId();
        if(!timeOutRegexUtils.validateEmailForChangeEmail(commonArg.getNewEmail())){
            throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
        }
        ChangeEmailV2Request changeEmailV2Request = new ChangeEmailV2Request();
        changeEmailV2Request.setUserId(userId);
        changeEmailV2Request.setNewEmail(commonArg.getNewEmail());
        changeEmailV2Request.setNewEmailVerifyCode(commonArg.getNewEmailVerifyCode());
        changeEmailV2Request.setEmailVerifyCode(commonArg.getEmailVerifyCode());
        changeEmailV2Request.setGoogleVerifyCode(commonArg.getGoogleVerifyCode());
        changeEmailV2Request.setMobileVerifyCode(commonArg.getMobileVerifyCode());
        changeEmailV2Request.setYubikeyVerifyCode(commonArg.getYubikeyVerifyCode());
        changeEmailV2Request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<Boolean> apiResponse = userSecurityApi.changeEmailV2(getInstance(changeEmailV2Request));
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    @PostMapping(value = "/v3/private/account/email/change")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @UserOperation(name = "更改邮箱", eventName = "changeEmail",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"newEmail"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "securityV2.changeUserEmailV3")
    public CommonRet<Boolean> changeUserEmailV3(@RequestBody @Validated ChangeUserEmailArgV3 commonArg)
            throws Exception {
        HashMap<String, String> deviceInfo =
                commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.CHANGE_EMAIL_V2.name());

        CommonRet<Boolean> ret = new CommonRet<>();
        Long userId = getUserId();
        if(!timeOutRegexUtils.validateEmailForChangeEmail(commonArg.getNewEmail())){
            throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
        }
        ChangeEmailV2Request changeEmailV2Request = new ChangeEmailV2Request();
        changeEmailV2Request.setUserId(userId);
        changeEmailV2Request.setNewEmail(commonArg.getNewEmail());
        changeEmailV2Request.setNewEmailVerifyCode(commonArg.getNewEmailVerifyCode());
        changeEmailV2Request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        changeEmailV2Request.setAlreadyCheckMFA(true);
        APIResponse<Boolean> apiResponse = userSecurityApi.changeEmailV2(getInstance(changeEmailV2Request));
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }


    @PostMapping(value = "/v2/private/account/mobile/change")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @UserOperation(name = "更改手机", eventName = "changeMobile",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"newMobile"}, requestKeyDisplayNames = {"手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "securityV2.changeUserMobile")
    public CommonRet<Boolean> changeUserMobile(@RequestBody @Validated ChangeUserMobileArg commonArg)
            throws Exception {
        CommonRet<Boolean> ret = new CommonRet<>();
        Long userId = getUserId();
        ChangeMobileV2Request changeMobileV2Request = new ChangeMobileV2Request();
        changeMobileV2Request.setUserId(userId);
        changeMobileV2Request.setNewMobile(commonArg.getNewMobile());
        changeMobileV2Request.setNewMobileCode(commonArg.getNewMobileCode());
        changeMobileV2Request.setNewMobileVerifyCode(commonArg.getNewMobileVerifyCode());
        changeMobileV2Request.setEmailVerifyCode(commonArg.getEmailVerifyCode());
        changeMobileV2Request.setGoogleVerifyCode(commonArg.getGoogleVerifyCode());
        changeMobileV2Request.setMobileVerifyCode(commonArg.getMobileVerifyCode());
        changeMobileV2Request.setYubikeyVerifyCode(commonArg.getYubikeyVerifyCode());
        changeMobileV2Request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<Boolean> apiResponse = userSecurityApi.changeMobileV2(getInstance(changeMobileV2Request));
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    @PostMapping(value = "/v2/private/account/user/safe-password-check")
    public CommonRet<Boolean> safePasswordCheck(@Validated @RequestBody SafePasswordVerifyArg arg) throws Exception {
        SafePasswordVerifyRequest request = new SafePasswordVerifyRequest();
        Long userId = getUserId();
        request.setUserId(userId);
        request.setSafePassword(arg.getSafePassword());
        APIResponse<Boolean> apiResponse = userApi.safepasswordCheck(APIRequest.instance(request));
        baseHelper.checkResponse(apiResponse);
        String bizNo = WebUtils.getHeader(BinanceMgsAccountConstant.RISK_CHALLENGE_BIZ_NO_HEADER_KEY);
        if (com.binance.master.utils.StringUtils.isNotBlank(bizNo)) {
            riskService.noticeRiskChallengeResult(userId, bizNo, BinanceMgsAccountConstant.MFA_PASSWORD_VERIFY_TYPE);
        }
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping(value = "/v2/private/account/user/bind-email")
    @UserOperation(name = "绑定邮箱", eventName = "bindEmail",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "security.bindEmailV2")
    public CommonRet<Integer> bindEmailV2(@RequestBody @Validated BindEmailArgV2 bindEmailArg) throws Exception {
        HashMap<String, String> deviceInfo =
                commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.BIND_EMAIL.name());

        BindEmailRequest bindRequest = new BindEmailRequest();
        bindRequest.setAlreadyCheckMFA(true);
        BeanUtils.copyProperties(bindEmailArg, bindRequest);
        bindRequest.setUserId(getUserId());
        APIResponse<BindEmailResponse> bindResponse = userSecurityApi.bindEmail(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 绑定手机V3
     *
     * @return
     */
    @PostMapping(value = "/v3/private/account/user/bindMobile")
    @UserOperation(name = "绑定手机", eventName = "bindMobile",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "securityV2.bindMobileV3")
    @AccountDefenseResource(name = "UserSecurityV2Controller.bindMobileV2")
    public CommonRet<Integer> bindMobileV3(@RequestBody @Validated BindMobileV3Arg bindMobileArg) throws Exception {
        HashMap<String, String> deviceInfo =
                commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.BIND_MOBILE.name());
        BindMobileV2Request bindRequest = new BindMobileV2Request();
        BeanUtils.copyProperties(bindMobileArg, bindRequest);
        bindRequest.setUserId(getUserId());
        bindRequest.setAlreadyCheckMFA(true);
        APIResponse<Integer> bindResponse = userSecurityApi.bindMobileV2(getInstance(bindRequest));
        checkResponseAndLog2fa(bindResponse);
        return new CommonRet<>();
    }

    /**
     * 更改手机V3
     * @param commonArg
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/v3/private/account/mobile/change")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @UserOperation(name = "更改手机", eventName = "changeMobile",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"newMobile"}, requestKeyDisplayNames = {"手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "securityV2.changeUserMobileV3")
    public CommonRet<Boolean> changeUserMobileV3(@RequestBody @Validated ChangeUserMobileArgV3 commonArg)
            throws Exception {
        HashMap<String, String> deviceInfo =
                commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.CHANGE_MOBILE_V2.name());
        CommonRet<Boolean> ret = new CommonRet<>();
        Long userId = getUserId();
        ChangeMobileV2Request changeMobileV2Request = new ChangeMobileV2Request();
        changeMobileV2Request.setUserId(userId);
        changeMobileV2Request.setNewMobile(commonArg.getNewMobile());
        changeMobileV2Request.setNewMobileCode(commonArg.getNewMobileCode());
        changeMobileV2Request.setNewMobileVerifyCode(commonArg.getNewMobileVerifyCode());
        changeMobileV2Request.setAlreadyCheckMFA(true);
        changeMobileV2Request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<Boolean> apiResponse = userSecurityApi.changeMobileV2(getInstance(changeMobileV2Request));
        checkResponseWithoutLog(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    /**
     * 解绑手机V3
     *
     * @return
     */
    @PostMapping(value = "/v3/private/account/user/unbindMobile")
    @UserOperation(eventName = "unbindMobile", name = "解绑手机", sendToSensorData = true, sensorsRequestKeys = {"dummy"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "securityV2.unbindMobileV3")
    @AccountDefenseResource(name = "UserSecurityV2Controller.unbindMobileV2")
    public CommonRet<String> unbindMobileV3(@RequestBody @Validated UnbindMobileV3Arg unbindMobileArg)
            throws Exception {
        HashMap<String, String> deviceInfo =
                commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, com.binance.account2fa.enums.BizSceneEnum.UNBIND_MOBILE.name());

        // 发送数据到风控
        riskHelper.frequentUnfaSendMq(getUserId(), getTrueUserEmail(), EnumErrorLogType.FUFA__MOBILE);

        UnbindMobileV2Request unbindRequest = new UnbindMobileV2Request();
        BeanUtils.copyProperties(unbindMobileArg, unbindRequest);
        unbindRequest.setUserId(getUserId());
        unbindRequest.setAlreadyCheckMFA(true);
        unbindRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<UnbindMobileV2Response> unbindResponse = userSecurityApi.unbindMobileV2(getInstance(unbindRequest));

        checkResponseAndLog2fa(unbindResponse);
        return new CommonRet<>();
    }

}
