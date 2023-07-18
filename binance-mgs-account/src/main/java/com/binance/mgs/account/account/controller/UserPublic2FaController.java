package com.binance.mgs.account.account.controller;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.binance.account.vo.user.enums.RegisterationMethodEnum;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.VersionHelper;
import com.binance.mgs.account.account.vo.VerifyCodeWithoutUserIdArg;
import com.binance.mgs.account.service.Account2FaService;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.utils.DomainUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account2fa.api.Send2FaApi;
import com.binance.account2fa.api.Verify2FaApi;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.enums.MsgType;
import com.binance.account2fa.vo.request.SendSmsAuthCodeRequest;
import com.binance.account2fa.vo.request.Verification2FaRequest;
import com.binance.account2fa.vo.response.SendSmsAuthCodeResponse;
import com.binance.account2fa.vo.response.Verify2FaResponse;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.CheckHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.account.vo.GetPubVerificationTwoCheckListRet;
import com.binance.mgs.account.account.vo.SendVerifyCodeResponse;
import com.binance.mgs.account.account.vo.new2fa.GetPubVerificationTwoCheckListArg;
import com.binance.mgs.account.account.vo.new2fa.SendPublicEmailVerifyCodeArg;
import com.binance.mgs.account.account.vo.new2fa.SendPublicMobileVerifyCodeArg;
import com.binance.mgs.account.account.vo.new2fa.Verify2FaPublicArg;
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

import static com.binance.mgs.account.constant.BizType.REGISTER;

@RestController
@RequestMapping(value = "/v2/transient/account")
@Slf4j
public class UserPublic2FaController extends AccountBaseAction {
    @Resource
    private UserSecurityApi userSecurityApi;
    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Resource
    private BaseHelper baseHelper;
    @Autowired
    private CheckHelper checkHelper;
    @Autowired
    private TokenHelper tokenHelper;
    @Autowired
    private Send2FaApi send2FaApi;
    @Autowired
    private Verify2FaApi verify2FaApi;
    @Autowired
    private UserDeviceHelper userDeviceHelper;

    @Resource
    private VersionHelper versionHelper;

    @Resource
    private TimeOutRegexUtils timeOutRegexUtils;
    
    @Resource
    private Account2FaService account2FaService;

    @Value("${send.bind.mobile.verify.code.period:86400}")
    private int sendBindMobileVerifyCodePeriod;

    @Value("${send.bind.mobile.verify.code.threshold:10}")
    private int sendBindMobileVerifyCodeThreshold;

    @Value("#{'${new2fa.public.check.bizScene:REFRESH_ACCESSTOKEN}'.split(',')}")
    private List<String> checkedBizScenesForPublicNew2fa;

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

    @Value("#{'${limited.mobile.verify.mobilCode:}'.split(',')}")
    private Set<String> limitedMobileCode;

    @Value("${forgetpwd.enable.android.version:2.33.1}")
    private String fogetpwdEnableAndroidVersion;

    @Value("${forgetpwd.enable.ios.version:2.33.3}")
    private String fogetpwdEnableIosVersion;

    @Value("${forgetpwd.enable.electron.version:1.21.2}")
    private String fogetpwdEnableElectronVersion;

    @Value("#{'${limit.china.domains:}'.split(',')}")
    private Set<String> limitChinaDomains;

    @Value("${register.mobile.code.check.switch:true}")
    private boolean mobileCodeCheckSwitch;

    @Value("${sendEmail.android.version:}")
    private String sendEmailAndroidVersion;

    @Value("${sendEmail.ios.version:}")
    private String sendEmailIosVersion;

    @Resource
    private AccountHelper accountHelper;

    @PostMapping(value = "/mobile/sendMobileVerifyCode")
    @AntiBotCaptchaValidate(bizType = {BizType.FORGET_PASSWORD, BizType.REGISTER, BizType.REFRESH_ACCESS_TOKEN})
    @AccountDefenseResource(name = "UserPublic2FaController.sendMobileVerifyCode")
    @DDoSPreMonitor(action = "2fa.sendMobileVerifyCode")
    @UserOperation(name = "发送短信验证码对外", eventName = "new2faSendPublicMobileVerifyCode", requestKeys = {"mobileCode", "mobile","email"},
            requestKeyDisplayNames = {"mobileCode", "mobile","email"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<SendVerifyCodeResponse> sendMobileVerifyCode(@RequestBody @Validated SendPublicMobileVerifyCodeArg commonArg) throws Exception {
        // 默认仅支持refreshToken场景
        if (checkedBizScenesForPublicNew2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(commonArg.getBizScene()))) {
            log.warn("Unsupported bizScene:{}", commonArg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
        }
        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());

        if(StringUtils.isNotBlank(commonArg.getRefreshToken())){
            String tempEmail= tokenHelper.checkRefreshTokenAndGetEmail(commonArg.getRefreshToken());
            commonArg.setEmail(tempEmail);
        }

        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(commonArg.getMobile(), commonArg.getMobileCode()) && StringUtils.isBlank(commonArg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }

        // 如果是语音短信，检查是否在支持的国家内
        if (MsgType.VOICE == commonArg.getMsgType()) {
            checkHelper.assetIfSupportVoiceSms(commonArg.getMobileCode());
        }
        UserOperationHelper.log("scene", commonArg.getBizScene());

        String ip = WebUtils.getRequestIp();

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
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_REGISTER, ip, normalUserIpLimitCount, String.format("sendMobileVerifyCode bizScene=%s", commonArg.getBizScene()));
            }
        }
        Long userId = getUserIdResp.getData().getUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));

        UserStatusEx userStatusEx =  this.getUserStatusByUserId(userId);
        // 部分限制地区建议用户使用邮箱验证
        if (BizSceneEnum.FORGET_PASSWORD.name().equalsIgnoreCase(commonArg.getBizScene()) && !userStatusEx.getIsUserNotBindEmail()
                && limitedMobileCode.contains(commonArg.getMobileCode())) {
            throw new BusinessException(AccountMgsErrorCode.EMAIL_VERIFICATION_IS_RECOMMEND);
        }

        // 针对已成功注册手机用户，不实际发送注册验证吗
        if (userStatusEx.getIsUserDelete()
                || (commonArg.getBizScene() != null && BizSceneEnum.ACCOUNT_ACTIVATE.name().equalsIgnoreCase(commonArg.getBizScene()) && userStatusEx.getIsUserActive())) {
            throw new BusinessException(AccountMgsErrorCode.MOBILE_VERIFICATION_CODE_IS_SEND);
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
        ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(commonArg.getEmail(),commonArg.getMobile(), DdosOperationEnum.ACTIVE);


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
    @AntiBotCaptchaValidate(bizType = {BizType.FORGET_PASSWORD, BizType.REGISTER, BizType.REFRESH_ACCESS_TOKEN})
    @AccountDefenseResource(name = "UserPublic2FaController.sendEmailVerifyCode")
    @DDoSPreMonitor(action = "2fa.sendEmailVerifyCode")
    @UserOperation(name = "发送邮件验证码对外", eventName = "new2faSendPublicEmailVerifyCode", requestKeys = {"mobileCode", "mobile","email"},
            requestKeyDisplayNames = {"mobileCode", "mobile","email"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<SendVerifyCodeResponse> sendEmailVerifyCode(@RequestBody @Validated SendPublicEmailVerifyCodeArg commonArg)
            throws Exception {
        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());
        // 默认仅支持refreshToken场景
        if (checkedBizScenesForPublicNew2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(commonArg.getBizScene().name()))) {
            log.warn("Unsupported bizScene:{}", commonArg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
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
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_REGISTER, ip, normalUserIpLimitCount, String.format("sendEmailVerifyCode bizScene=%s", commonArg.getBizScene()));
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

        com.binance.account2fa.vo.request.SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new com.binance.account2fa.vo.request.SendEmailVerifyCodeRequest();
        sendEmailVerifyCodeRequest.setUserId(userId);
        sendEmailVerifyCodeRequest.setResend(commonArg.getResend());
        sendEmailVerifyCodeRequest.setBizScene(commonArg.getBizScene().name());

        APIResponse<com.binance.account2fa.vo.response.SendEmailVerifyCodeResponse> apiResponse = send2FaApi.sendEmailVerifyCode(getInstance(sendEmailVerifyCodeRequest));

        checkResponseWithoutLog(apiResponse);

        SendVerifyCodeResponse sendVerifyCodeResponse = new SendVerifyCodeResponse();
        sendVerifyCodeResponse.setExpireTime(apiResponse.getData().getExpireTime());
        return new CommonRet<>(sendVerifyCodeResponse);
    }

    @PostMapping(value = "/getVerificationTwoCheckList")
    @AntiBotCaptchaValidate(bizType = {BizType.REFRESH_ACCESS_TOKEN})
    @DDoSPreMonitor(action = "2fa.getPubVerificationTwoCheckList")
    @AccountDefenseResource(name = "UserPublic2FaController.getPubVerificationTwoCheckList")
    public CommonRet<GetPubVerificationTwoCheckListRet> getPubVerificationTwoCheckList(@RequestBody @Validated GetPubVerificationTwoCheckListArg arg)
            throws Exception {
        // 默认仅支持refreshToken场景
        if (checkedBizScenesForPublicNew2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(arg.getBizScene()))) {
            log.warn("Unsupported bizScene:{}", arg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
        }
        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());

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

        com.binance.account2fa.vo.request.GetVerificationTwoCheckListRequest getReq = new com.binance.account2fa.vo.request.GetVerificationTwoCheckListRequest();
        getReq.setUserId(userId);
        getReq.setBizScene(arg.getBizScene());
        getReq.setDeviceInfo(commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getUserEmail()));
        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        getReq.setClientId(clientId);
        APIResponse<com.binance.account2fa.vo.response.GetVerificationTwoCheckListResponse> getResp = verify2FaApi.getVerificationTwoCheckList(getInstance(getReq));
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

    @PostMapping(value = "/verify2FaCode")
    @AntiBotCaptchaValidate(bizType = {BizType.REFRESH_ACCESS_TOKEN})
    @DDoSPreMonitor(action = "verify2FaCode")
    @AccountDefenseResource(name="UserPublic2FaController.verify2FaCode")
    public CommonRet<Verify2FaResponse> verify2FaCode(@RequestBody @Validated Verify2FaPublicArg arg)
            throws Exception {
        if (checkedBizScenesForPublicNew2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(arg.getBizScene()))) {
            log.warn("Unsupported bizScene:{}", arg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
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

        Verification2FaRequest request = new Verification2FaRequest();
        request.setUserId(userId);
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
        request.setDeviceInfo(commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), getUserIdStr(), getUserEmail()));
        log.info("public verify2FaCode userId={} izScene={}", request.getUserId(), arg.getBizScene());
        APIResponse<Verify2FaResponse> apiResponse = verify2FaApi.verify2Fa(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }
    
    @PostMapping(value = "/verifyCodeForRegister")
    @AccountDefenseResource(name = "UserPublic2FaController.verifyCodeForRegister")
    @AntiBotCaptchaValidate(bizType = {REGISTER})
    @DDoSPreMonitor(action = "verifyCodeForRegister")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"}, forwardedCookies = {"p20t","cr20"})
    public CommonRet<Verify2FaResponse> verifyCodeForRegister(HttpServletRequest request, HttpServletResponse response, 
                                                                              @RequestBody @Validated VerifyCodeWithoutUserIdArg verifyArg) throws Exception {
        // 校验邮箱格式
        if (RegisterationMethodEnum.EMAIL.name().equalsIgnoreCase(verifyArg.getVerifyType())) {
            if (StringUtils.isAnyBlank(verifyArg.getEmail()) || !timeOutRegexUtils.validateEmailForRegister(verifyArg.getEmail())) {
                throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
            }
        }

        if (RegisterationMethodEnum.MOBILE.name().equalsIgnoreCase(verifyArg.getVerifyType())) {
            if (StringUtils.isAnyBlank(verifyArg.getMobile(), verifyArg.getMobileCode())) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);

            }

            if (mobileCodeCheckSwitch && StringUtils.isNumeric(verifyArg.getMobileCode())) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
            }
        }
        if(StringUtils.isAllBlank(verifyArg.getVerifyCode())){
            throw new BusinessException(AccountMgsErrorCode.PLEASE_ENTER_VERIFYCODE);
        }

        // 获取ip所属国家
        String countryCode = Ip2LocationSwitchUtils.getCountryShort(IPUtils.getIpAddress(request));
        // 当前域名
        String domain = DomainUtils.getDomain();
        log.info("verifyCodeWithoutUserId limitChinaDomains={},isFromWeb = {} , countryCode = {} , domain = {}", JsonUtils.toJsonNotNullKey(limitChinaDomains), baseHelper.isFromWeb(), countryCode, domain);
        if (limitChinaDomains.contains(domain) && StringUtils.equalsIgnoreCase(countryCode, "CN") && baseHelper.isFromWeb()) {
            throw new BusinessException(AccountMgsErrorCode.COUNTRY_NOT_SUPPORT);
        } else {
            log.info("verifyCodeWithoutUserId isFromWeb = {} , countryCode = {} , domain = {}", baseHelper.isFromWeb(), countryCode, domain);
        }

        Verify2FaResponse data = account2FaService.verifyCodeWithoutUserId(verifyArg, BizSceneEnum.ACCOUNT_ACTIVATE);
        return new CommonRet<>(data);
    }
}
