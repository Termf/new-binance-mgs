package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserApi;
import com.binance.account.api.UserRegisterApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.api.CountryApi;
import com.binance.account.vo.country.CountryVo;
import com.binance.account.vo.country.GetCountryByCodeRequest;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.enums.MsgType;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.request.GetVerificationTwoCheckListRequest;
import com.binance.account.vo.security.request.SendEmailVerifyCodeRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.security.response.GetVerificationTwoCheckListResponse;
import com.binance.account.vo.security.response.SendEmailVerifyCodeResponse;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.SendSmsAuthCoderRequest;
import com.binance.account.vo.user.response.SendSmsAuthCoderResponse;
import com.binance.account.vo.userRegister.SendActiveEmailVerifyCodeRequest;
import com.binance.account.vo.userRegister.SendActiveEmailVerifyCodeResponse;
import com.binance.account.vo.userRegister.SendActiveMobileVerifyCodeRequest;
import com.binance.account.vo.userRegister.SendActiveMobileVerifyCodeResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.WebUtils;
import com.binance.messaging.api.msg.MsgApi;
import com.binance.messaging.api.msg.request.QuerySendMethodRequest;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.VersionHelper;
import com.binance.mgs.account.account.vo.GetPubVerificationTwoCheckListArg;
import com.binance.mgs.account.account.vo.GetPubVerificationTwoCheckListRet;
import com.binance.mgs.account.account.vo.QueryPublicSendChannelArg;
import com.binance.mgs.account.account.vo.QuerySendChannelResp;
import com.binance.mgs.account.account.vo.SendPublicEmailVerifyCodeArg;
import com.binance.mgs.account.account.vo.SendPublicMobileVerifyCodeArg;
import com.binance.mgs.account.account.vo.SendVerifyCodeResponse;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.authcenter.helper.TokenHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.mgs.account.util.VersionUtil;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.Constant;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.PKGenarator;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.binance.mgs.account.constant.BizType.FORGET_PASSWORD;

@RestController
@RequestMapping(value = "/v1/public/account")
@Slf4j
public class UserPublicController extends AccountBaseAction {
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
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Resource
    private AccountHelper accountHelper;
    @Resource
    private BaseHelper baseHelper;
    @Autowired
    private VersionHelper versionHelper;
    @Resource
    private UserRegisterApi userRegisterApi;
    @Autowired
    private CheckHelper checkHelper;
    @Autowired
    private TokenHelper tokenHelper;

    @Value("${send.bind.mobile.verify.code.period:86400}")
    private int sendBindMobileVerifyCodePeriod;

    @Value("${send.bind.mobile.verify.code.threshold:10}")
    private int sendBindMobileVerifyCodeThreshold;

    @Value("#{'${2fa.check.biz.scene:ACCOUNT_ACTIVATE,FORGET_PASSWORD,REFRESH_ACCESSTOKEN}'.split(',')}")
    private List<String> checkedBizScenesFor2fa;

    @Value("${mobile.android.version:1.28.1}")
    private String androidVersion;

    @Value("${mobile.ios.version:2.17.4}")
    private String iosVersion;

    @Value("${2fa.DDos.check.switch:false}")
    private boolean twoCheckDDosCheckSwitch;

    @Value("${email.DDos.check.switch:false}")
    private boolean emailDDosCheckSwitch;

    @Value("${mobile.DDos.check.switch:false}")
    private boolean mobileDDosCheckSwitch;

    @Value("${DDos.ip.limit.count:10}")
    private int ipLimitCount;

    @Value("${DDos.normal.ip.limit.count:10}")
    private int normalUserIpLimitCount;

    @Value("${need.check.gt.forgetpwd.switch:true}")
    private boolean needCheckGtForForgetPwdSwitch;

    @Value("${sendEmail.android.version:}")
    private String sendEmailAndroidVersion;

    @Value("${sendEmail.ios.version:}")
    private String sendEmailIosVersion;

    @Value("#{'${limited.mobile.verify.mobilCode:}'.split(',')}")
    private Set<String> limitedMobileCode;

    @Value("${need.route.app.forgetpwd.switch:true}")
    private boolean needRouteAppForForgetPwdSwitch;



    @Value("${forgetpwd.enable.android.version:2.33.1}")
    private String fogetpwdEnableAndroidVersion;

    @Value("${forgetpwd.enable.ios.version:2.33.3}")
    private String fogetpwdEnableIosVersion;

    @Value("${forgetpwd.enable.electron.version:1.21.2}")
    private String fogetpwdEnableElectronVersion;

    @Value("#{'${mobile.code.reset.pwd.force.email:RU}'.split(',')}")
    private Set<String> mobileCodeListResetPwdForceEmail;

    @Value("${mobile.code.reset.pwd.force.email.switch:false}")
    private boolean mobileCodeResetForceEmailSwitch;


    @PostMapping(value = "/mobile/sendMobileVerifyCode")
    @AntiBotCaptchaValidate(bizType = {BizType.FORGET_PASSWORD, BizType.REGISTER, BizType.REFRESH_ACCESS_TOKEN})
    @AccountDefenseResource(name = "UserPublicController.sendMobileVerifyCode")
    @DDoSPreMonitor(action = "sendMobileVerifyCode")
    @UserOperation(name = "发送短信验证码对外", eventName = "sendPublicMobileVerifyCode", requestKeys = {"mobileCode", "mobile","email"},
            requestKeyDisplayNames = {"mobileCode", "mobile","email"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<SendVerifyCodeResponse> sendMobileVerifyCode(@RequestBody @Validated SendPublicMobileVerifyCodeArg commonArg) throws Exception {

        // 默认仅支持账户激活和忘记密码两个场景
        if (checkedBizScenesFor2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(commonArg.getBizScene().name()))) {
            log.warn("Unsupported bizScene:{}", commonArg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
        }
        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());

        if (needRouteAppForForgetPwdSwitch && !forgetPwdEnableVersion()&& BizSceneEnum.FORGET_PASSWORD == commonArg.getBizScene()) {
            log.info("route app");
            throw new BusinessException(AccountMgsErrorCode.FOREGT_PWD_NOT_SUPPORT_FOR_APP);
        }


        if (needCheckGtForForgetPwdSwitch && forgetPwdEnableVersion() && BizSceneEnum.FORGET_PASSWORD == commonArg.getBizScene()) {
            log.info("check gt");
            accountHelper.verifyCodeCacheAndBanIp(commonArg, FORGET_PASSWORD);
        }
        if(StringUtils.isNotBlank(commonArg.getRefreshToken())){
           String tempEmail= tokenHelper.checkRefreshTokenAndGetEmail(commonArg.getRefreshToken());
            commonArg.setEmail(tempEmail);
        }

        if (mobileCodeResetForceEmailSwitch && BizSceneEnum.FORGET_PASSWORD == commonArg.getBizScene() && StringUtils.isNotBlank(commonArg.getMobileCode())
                && mobileCodeListResetPwdForceEmail.contains(commonArg.getMobileCode().toUpperCase())) {
            log.warn("this mobileCode force email to reset pwd:{}", commonArg.getMobileCode());
            throw new BusinessException(AccountMgsErrorCode.EMAIL_IS_ESSENTIAL_CONDITION_TO_RESET_PWD);
        }
        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(commonArg.getMobile(), commonArg.getMobileCode()) && StringUtils.isBlank(commonArg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }

        // 如果是语音短信，检查是否在支持的国家内
        if (MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(commonArg.getMobileCode());
        }
        UserOperationHelper.log("scene", commonArg.getBizScene().name());

        String ip = WebUtils.getRequestIp();

        if (commonArg.getNewRegisterVersion() && (TerminalEnum.WEB == getTerminal() || TerminalEnum.ELECTRON == getTerminal() || TerminalEnum.ANDROID == getTerminal() || TerminalEnum.IOS == getTerminal()) && BizSceneEnum.ACCOUNT_ACTIVATE == commonArg.getBizScene()) {
            accountHelper.verifyCodeCacheAndBanIp(commonArg, BizType.REGISTER);
            SendActiveMobileVerifyCodeRequest sendActiveMobileVerifyCodeRequest=new SendActiveMobileVerifyCodeRequest();
            sendActiveMobileVerifyCodeRequest.setBizScene(commonArg.getBizScene());
            sendActiveMobileVerifyCodeRequest.setMobileCode(commonArg.getMobileCode());
            sendActiveMobileVerifyCodeRequest.setMobile(commonArg.getMobile());
            sendActiveMobileVerifyCodeRequest.setMsgType(commonArg.getMsgType());
            sendActiveMobileVerifyCodeRequest.setResend(commonArg.getResend());
            sendActiveMobileVerifyCodeRequest.setUserChannel(commonArg.getUserChannel());
            APIResponse<SendActiveMobileVerifyCodeResponse> sendActiveEmailVerifyCodeResponseAPIResponse =  userRegisterApi.sendActiveMobileVerifyCode(getInstance(sendActiveMobileVerifyCodeRequest));
            if (!baseHelper.isOk(sendActiveEmailVerifyCodeResponseAPIResponse)){
                if (mobileDDosCheckSwitch) {
                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("sendMobileVerifyCode bizScene=%s mobile=%s-%s", commonArg.getBizScene(), commonArg.getMobileCode(), commonArg.getMobile()));
                }
                checkResponseMisleaderUseNotExitsErrorForPublicInterface(sendActiveEmailVerifyCodeResponseAPIResponse);
            }else{
                if (mobileDDosCheckSwitch) {
                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_REGISTER, ip, normalUserIpLimitCount, String.format("sendMobileVerifyCode bizScene=%s", commonArg.getBizScene(), commonArg.getMobileCode()));
                }
            }
            boolean isVnMobile = StringUtils.isNotBlank(commonArg.getMobileCode()) && "vn".equalsIgnoreCase(commonArg.getMobileCode());

            if (!isVnMobile && MsgType.VOICE == commonArg.getMsgType()) {
                Long increment = ShardingRedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
                if (increment == 1L) {
                    ShardingRedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
                }
                log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
                if (increment >= sendBindMobileVerifyCodeThreshold) {
                    log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
                    throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
                }
            }
            ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(null,commonArg.getMobile(), DdosOperationEnum.ACTIVE);
            return new CommonRet<>();
        }


        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(commonArg.getEmail());
        getUserIdReq.setMobileCode(commonArg.getMobileCode());
        getUserIdReq.setMobile(commonArg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));

        if (!baseHelper.isOk(getUserIdResp)) {
            if (mobileDDosCheckSwitch) {
                String identify = StringUtils.isNotBlank(commonArg.getEmail()) ? commonArg.getEmail() : commonArg.getMobileCode() + "-" + commonArg.getMobile();
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("sendMobileVerifyCode bizScene=%s identify=%s", commonArg.getBizScene(), identify));
            }
            log.warn("user illegal,userSecurityApi.sendMobileVerifyCode,response={}", getUserIdResp);
            if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode()) && isCheckByClientType()) {
                throw new BusinessException(AccountMgsErrorCode.MOBILE_VERIFICATION_CODE_IS_SEND);
            }
            checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
        } else {
            if (mobileDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_FORGET_PASS, ip, normalUserIpLimitCount, String.format("sendMobileVerifyCode bizScene=%s", commonArg.getBizScene()));
            }
        }
        Long userId = getUserIdResp.getData().getUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));

        UserStatusEx userStatusEx =  this.getUserStatusByUserId(userId);
        // 部分限制地区建议用户使用邮箱验证
        if (commonArg.getBizScene() == BizSceneEnum.FORGET_PASSWORD && !userStatusEx.getIsUserNotBindEmail()
                && limitedMobileCode.contains(commonArg.getMobileCode())) {
            throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_IS_RECOMMEND);
        }

        // 针对已成功注册手机用户，不实际发送注册验证吗
        if (userStatusEx.getIsUserDelete()
                || (commonArg.getBizScene() != null && commonArg.getBizScene() == BizSceneEnum.ACCOUNT_ACTIVATE && userStatusEx.getIsUserActive())) {
            throw new BusinessException(AccountMgsErrorCode.MOBILE_VERIFICATION_CODE_IS_SEND);
        }

        boolean isVnMobile = StringUtils.isNotBlank(commonArg.getMobileCode()) && "vn".equalsIgnoreCase(commonArg.getMobileCode());

        if (!isVnMobile && MsgType.VOICE == commonArg.getMsgType()) {
            Long increment = RedisCacheUtils.increment(Constant.BIND_MOBILE_KEY, 1);
            if (increment == 1L) {
                RedisCacheUtils.expire(Constant.BIND_MOBILE_KEY, sendBindMobileVerifyCodePeriod, TimeUnit.SECONDS);
            }
            log.info("语音绑定手机号，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
            if (increment >= sendBindMobileVerifyCodeThreshold) {
                log.info("达到阈值，已经发送了{}次,mobileCode:{},mobile:{}", increment,commonArg.getMobileCode(),commonArg.getMobile());
                throw new BusinessException(MgsErrorCode.VOICE_NOT_SUPPORT);
            }
        }
        ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(commonArg.getEmail(),commonArg.getMobile(), DdosOperationEnum.ACTIVE);


        String verifyCodeId = PKGenarator.getId();
        SendSmsAuthCoderRequest sendSmsAuthCoderRequest = new SendSmsAuthCoderRequest();
        sendSmsAuthCoderRequest.setUserId(userId);
        sendSmsAuthCoderRequest.setVerifyCodeId(verifyCodeId);
        sendSmsAuthCoderRequest.setMsgType(commonArg.getMsgType());
        sendSmsAuthCoderRequest.setResend(commonArg.getResend());
        sendSmsAuthCoderRequest.setBizScene(commonArg.getBizScene());
        sendSmsAuthCoderRequest.setUserChannel(commonArg.getUserChannel());
        APIResponse<SendSmsAuthCoderResponse> apiResponse = userApi.sendSmsAuthCode(getInstance(sendSmsAuthCoderRequest));
        checkResponseWithoutLog(apiResponse);
        return new CommonRet<>();
    }

    @PostMapping(value = "/mobile/querySendChannel")
    @AntiBotCaptchaValidate(bizType = {BizType.REGISTER, BizType.FORGET_PASSWORD, BizType.REFRESH_ACCESS_TOKEN})
    @AccountDefenseResource(name = "UserPublicController.querySendChannel")
    @DDoSPreMonitor(action = "public.querySendChannel")
    public CommonRet<QuerySendChannelResp> querySendChannel(@RequestBody @Validated QueryPublicSendChannelArg queryPublicSendChannelArg) throws Exception {
        QuerySendMethodRequest querySendMethodRequest = new QuerySendMethodRequest();
        querySendMethodRequest.setRecipient(queryPublicSendChannelArg.getMobile());
        querySendMethodRequest.setMobileCode(queryPublicSendChannelArg.getMobileCode());
        if(StringUtils.isNotBlank(queryPublicSendChannelArg.getRefreshToken())){
            Long userId = tokenHelper.checkRefreshTokenAndGetUserId(queryPublicSendChannelArg.getRefreshToken());
            UserIdRequest userIdRequest = new UserIdRequest();
            userIdRequest.setUserId(userId);
            APIResponse<UserSecurityVo> userSecurityVoAPIResponse = userSecurityApi.getUserSecurityByUserId(getInstance(userIdRequest));
            checkResponseWithoutLog(userSecurityVoAPIResponse);
            querySendMethodRequest.setRecipient(userSecurityVoAPIResponse.getData().getMobile());
            querySendMethodRequest.setMobileCode(userSecurityVoAPIResponse.getData().getMobileCode());
        }
        if (StringUtils.isAnyBlank(querySendMethodRequest.getRecipient(),querySendMethodRequest.getMobileCode())) {
            //未绑定手机
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        if (!org.apache.commons.lang.StringUtils.isNumeric(querySendMethodRequest.getMobileCode())) {
            //转换mobileCode
            GetCountryByCodeRequest getCountryByCodeRequest = new GetCountryByCodeRequest();
            getCountryByCodeRequest.setCode(querySendMethodRequest.getMobileCode());
            APIResponse<CountryVo> countryVoAPIResponse = countryApi.getCountryByCode(getInstance(getCountryByCodeRequest));
            checkResponseWithoutLog(countryVoAPIResponse);
            querySendMethodRequest.setMobileCode(countryVoAPIResponse.getData().getMobileCode());
        }
        querySendMethodRequest.setResend(queryPublicSendChannelArg.getResend());
        querySendMethodRequest.setMsgType(com.binance.messaging.api.msg.request.MsgType.valueOf(queryPublicSendChannelArg.getMsgType().name()));
        APIResponse<String> apiResponse = msgApi.querySendOtpMethod(getInstance(querySendMethodRequest));
        checkResponseWithoutLog(apiResponse);
        QuerySendChannelResp resp = new QuerySendChannelResp();
        resp.setUserChannel(apiResponse.getData());
        return new CommonRet<>(resp);
    }

    @PostMapping(value = "/email/sendEmailVerifyCode")
    @AntiBotCaptchaValidate(bizType = {BizType.FORGET_PASSWORD, BizType.REGISTER, BizType.REFRESH_ACCESS_TOKEN})
    @AccountDefenseResource(name = "UserPublicController.sendEmailVerifyCode")
    @DDoSPreMonitor(action = "sendEmailVerifyCode")
    @UserOperation(name = "发送邮件验证码对外", eventName = "sendPublicEmailVerifyCode", requestKeys = {"mobileCode", "mobile", "email"},
            requestKeyDisplayNames = {"mobileCode", "mobile", "email"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<SendVerifyCodeResponse> sendEmailVerifyCode(@RequestBody @Validated SendPublicEmailVerifyCodeArg commonArg)
            throws Exception {
        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());
        // 默认仅支持账户激活和忘记密码两个场景
        if (checkedBizScenesFor2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(commonArg.getBizScene().name()))) {
            log.warn("Unsupported bizScene:{}", commonArg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
        }

        if (needRouteAppForForgetPwdSwitch && !forgetPwdEnableVersion()&& BizSceneEnum.FORGET_PASSWORD == commonArg.getBizScene()) {
            log.info("route app");
            throw new BusinessException(AccountMgsErrorCode.FOREGT_PWD_NOT_SUPPORT_FOR_APP);
        }



        if (needCheckGtForForgetPwdSwitch && forgetPwdEnableVersion() && BizSceneEnum.FORGET_PASSWORD == commonArg.getBizScene()) {
            log.info("check gt");
            accountHelper.verifyCodeCacheAndBanIp(commonArg, FORGET_PASSWORD);
        }
        if(StringUtils.isNotBlank(commonArg.getRefreshToken())){
            String tempEmail= tokenHelper.checkRefreshTokenAndGetEmail(commonArg.getRefreshToken());
            commonArg.setEmail(tempEmail);
        }



        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(commonArg.getMobile(), commonArg.getMobileCode()) &&
                StringUtils.isBlank(commonArg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        UserOperationHelper.log("scene", commonArg.getBizScene().name());

        String ip = WebUtils.getRequestIp();

        if (commonArg.getNewRegisterVersion() && (TerminalEnum.WEB == getTerminal() || TerminalEnum.ELECTRON == getTerminal() || TerminalEnum.ANDROID == getTerminal() || TerminalEnum.IOS == getTerminal()) && BizSceneEnum.ACCOUNT_ACTIVATE == commonArg.getBizScene()) {
            accountHelper.verifyCodeCacheAndBanIp(commonArg, BizType.REGISTER);
            SendActiveEmailVerifyCodeRequest sendActiveEmailVerifyCodeRequest=new SendActiveEmailVerifyCodeRequest();
            sendActiveEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());
            sendActiveEmailVerifyCodeRequest.setEmail(commonArg.getEmail());
            sendActiveEmailVerifyCodeRequest.setResend(commonArg.getResend());
            APIResponse<SendActiveEmailVerifyCodeResponse> sendActiveEmailVerifyCodeResponseAPIResponse =  userRegisterApi.sendActiveEmailVerifyCode(getInstance(sendActiveEmailVerifyCodeRequest));
            if (!baseHelper.isOk(sendActiveEmailVerifyCodeResponseAPIResponse)){
                if (emailDDosCheckSwitch) {
                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("sendEmailVerifyCode bizScene=%s email=%s", commonArg.getBizScene(), commonArg.getEmail()));
                }
                checkResponseMisleaderUseNotExitsErrorForPublicInterface(sendActiveEmailVerifyCodeResponseAPIResponse);
            }else{
                if (emailDDosCheckSwitch) {
                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_REGISTER, ip, normalUserIpLimitCount, String.format("sendEmailVerifyCode bizScene=%s", commonArg.getBizScene()));
                }
            }
            ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(commonArg.getEmail(),null, DdosOperationEnum.ACTIVE);
            SendVerifyCodeResponse sendVerifyCodeResponse = new SendVerifyCodeResponse();
            sendVerifyCodeResponse.setExpireTime(sendActiveEmailVerifyCodeResponseAPIResponse.getData().getExpireTime());
            return new CommonRet<>(sendVerifyCodeResponse);
        }

        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(commonArg.getEmail());
        getUserIdReq.setMobileCode(commonArg.getMobileCode());
        getUserIdReq.setMobile(commonArg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));

        if (!baseHelper.isOk(getUserIdResp)) {

            if (emailDDosCheckSwitch) {
                String identify = StringUtils.isNotBlank(commonArg.getEmail()) ? commonArg.getEmail() : commonArg.getMobileCode() + "-" + commonArg.getMobile();
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("sendEmailVerifyCode bizScene=%s identify=%s", commonArg.getBizScene(), identify));
            }

            log.warn("sendEmailVerifyCode:userId is illegal,getUserIdByMobileOrEmail={}", getUserIdResp);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode()) && isCheckByClientType()) {
                throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_CODE_IS_SEND);
            }
            checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
        } else {
            if (emailDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_FORGET_PASS, ip, normalUserIpLimitCount, String.format("sendEmailVerifyCode bizScene=%s", commonArg.getBizScene()));
            }
        }
        Long userId = getUserIdResp.getData().getUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));

        UserStatusEx userStatusEx = this.getUserStatusByUserId(userId);
        if (userStatusEx.getIsUserDelete()) {
            throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_CODE_IS_SEND);
        }
        if (commonArg.getBizScene() == BizSceneEnum.ACCOUNT_ACTIVATE
                && userStatusEx.getIsUserActive()) {
            throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_CODE_IS_SEND);
        }
        ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(commonArg.getEmail(),commonArg.getMobile(), DdosOperationEnum.ACTIVE);

        SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeRequest();
        sendEmailVerifyCodeRequest.setUserId(userId);
        sendEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene());

        APIResponse<SendEmailVerifyCodeResponse> apiResponse;
        if (versionHelper.checkAppVersion(sendEmailAndroidVersion, sendEmailIosVersion)) {
            apiResponse = userSecurityApi.sendEmailVerifyCodeV2(getInstance(sendEmailVerifyCodeRequest));
        } else {
            apiResponse = userSecurityApi.sendEmailVerifyCode(getInstance(sendEmailVerifyCodeRequest));
        }
        checkResponseWithoutLog(apiResponse);
        SendVerifyCodeResponse sendVerifyCodeResponse = new SendVerifyCodeResponse();
        sendVerifyCodeResponse.setExpireTime(apiResponse.getData().getExpireTime());
        return new CommonRet<>(sendVerifyCodeResponse);
    }

    @PostMapping(value = "/getVerificationTwoCheckList")
    @AccountDefenseResource(name = "UserPublicController.getPubVerificationTwoCheckList")
    @AntiBotCaptchaValidate(bizType = {BizType.FORGET_PASSWORD, BizType.REFRESH_ACCESS_TOKEN})
    @DDoSPreMonitor(action = "getPubVerificationTwoCheckList")
    public CommonRet<GetPubVerificationTwoCheckListRet> getPubVerificationTwoCheckList(@RequestBody @Validated GetPubVerificationTwoCheckListArg arg)
            throws Exception {
        // 默认仅支持账户激活和忘记密码两个场景
        if (checkedBizScenesFor2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(arg.getBizScene().name()))) {
            log.warn("Unsupported bizScene:{}", arg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
        }




        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());
        // 默认仅支持账户激活和忘记密码两个场景
        if (needRouteAppForForgetPwdSwitch && !forgetPwdEnableVersion()&& BizSceneEnum.FORGET_PASSWORD == arg.getBizScene()) {
            log.info("route app");
            throw new BusinessException(AccountMgsErrorCode.FOREGT_PWD_NOT_SUPPORT_FOR_APP);
        }



        if (needCheckGtForForgetPwdSwitch && forgetPwdEnableVersion() && BizSceneEnum.FORGET_PASSWORD == arg.getBizScene()) {
            log.info("check gt");
            accountHelper.verifyCodeCacheAndBanIp(arg, FORGET_PASSWORD);
        }


        if(StringUtils.isNotBlank(arg.getRefreshToken())){
            String tempEmail= tokenHelper.checkRefreshTokenAndGetEmail(arg.getRefreshToken());
            arg.setEmail(tempEmail);
        }

        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(arg.getMobile(), arg.getMobileCode()) && StringUtils.isBlank(arg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }


        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(arg.getEmail());
        getUserIdReq.setMobileCode(arg.getMobileCode());
        getUserIdReq.setMobile(arg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));

        String ip = WebUtils.getRequestIp();
        if (!baseHelper.isOk(getUserIdResp)) {
            if (twoCheckDDosCheckSwitch) {
                String identify = Objects.nonNull(arg.getEmail()) ? String.valueOf(arg.getEmail()) : arg.getMobileCode() + "-" + arg.getMobile();
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("getPubVerificationTwoCheckList identify=%s", identify));
            }
            log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
            checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
        } else {
            if (twoCheckDDosCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_GET_2FA_LIST, ip, normalUserIpLimitCount, "getPubVerificationTwoCheckList");
            }
        }
        Long userId = getUserIdResp.getData().getUserId();

        GetVerificationTwoCheckListRequest getReq = new GetVerificationTwoCheckListRequest();
        getReq.setUserId(userId);
        getReq.setBizScene(arg.getBizScene());
        getReq.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getUserEmail()));
        APIResponse<GetVerificationTwoCheckListResponse> getResp = userSecurityApi.getVerificationTwoCheckList(getInstance(getReq));
        checkResponseWithoutLog(getResp);
        GetPubVerificationTwoCheckListRet ret = new GetPubVerificationTwoCheckListRet();
        if (getResp.getData() != null) {
            // 绑定项
            if (!CollectionUtils.isEmpty(getResp.getData().getNeedBindVerifyList())) {
                getResp.getData().getNeedBindVerifyList().forEach(x -> {
                    GetPubVerificationTwoCheckListRet.VerificationTwoBind bind = new GetPubVerificationTwoCheckListRet.VerificationTwoBind();
                    bind.setVerifyType(x.getVerifyType().getCode());
                    bind.setBindOption(x.getOption());
                    ret.getNeedBindVerifyList().add(bind);
                });
            }

            // 校验项
            if (!CollectionUtils.isEmpty(getResp.getData().getNeedCheckVerifyList())) {
                getResp.getData().getNeedCheckVerifyList().forEach(x -> {
                    GetPubVerificationTwoCheckListRet.VerificationTwoCheck check = new GetPubVerificationTwoCheckListRet.VerificationTwoCheck();
                    check.setVerifyType(x.getVerifyType().getCode());
                    check.setVerifyTargetMask("******");
                    check.setVerifyOption(x.getOption());
                    ret.getNeedCheckVerifyList().add(check);
                });
            }
        }
        return new CommonRet<>(ret);
    }

    /**
     * userId不存在时，判断是否走新报错逻辑
     */
    private boolean isCheckByClientType() {
        String currentVersion = versionHelper.getVersion();
        TerminalEnum terminal = this.getTerminal();
        log.info("Current terminal is {}", terminal.getCode());
        switch (terminal) {
            case ANDROID:
                // 当前版本大于指定版本时走新报错逻辑
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higherOrEqual(currentVersion, androidVersion);
            case IOS:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higherOrEqual(currentVersion, iosVersion);
            case WEB:
            case ELECTRON:
                return true;
            case PC:
            default:
                return false;
        }
    }



    /**
     * 可以做忘记密码的版本
     */
    private boolean forgetPwdEnableVersion() {
        TerminalEnum terminal = this.getTerminal();
        String currentVersion = VersionUtil.getVersion(terminal);
        log.info("Current terminal is {},currentVersion={}", terminal.getCode(),currentVersion);
        switch (terminal) {
            case ANDROID:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higher(currentVersion, fogetpwdEnableAndroidVersion);
            case IOS:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higher(currentVersion, fogetpwdEnableIosVersion);
            case WEB:
                return true;
            case ELECTRON:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higherOrEqual(currentVersion, fogetpwdEnableElectronVersion);
            default:
                return false;
        }
    }
}
