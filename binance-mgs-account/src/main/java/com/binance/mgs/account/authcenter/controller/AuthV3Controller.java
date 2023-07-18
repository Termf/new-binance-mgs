package com.binance.mgs.account.authcenter.controller;

import static com.binance.mgs.account.constant.BizType.LOGIN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.UserNotActiveCounterMetrics;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.authcenter.vo.LoginResponseV3;
import com.binance.mgs.account.authcenter.dto.LoginContextDto;
import com.binance.mgs.account.authcenter.vo.LoginV3Arg;
import com.binance.mgs.account.authcenter.vo.LoginVerifyMfaArg;
import com.binance.mgs.account.service.RiskService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.error.AccountErrorCode;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.accountdefensecenter.core.annotation.CallAppCheck;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountoauth.api.AccountOauthApi;
import com.binance.accountoauth.vo.oauth.request.DoLoginBindThreePartyRequest;
import com.binance.authcenter.vo.PnkLoginDto;
import com.binance.master.constant.Constant;
import com.binance.master.enums.AuthStatusEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.dto.RiskLoginInfoDto;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.AccountMgsRedisHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.RiskKafkaHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.authcenter.AuthCenterBaseAction;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.helper.GeeTestHelper;
import com.binance.mgs.account.authcenter.helper.SensorHelper;
import com.binance.mgs.account.authcenter.vo.LoginRet;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.mgs.account.security.helper.AntiBotHelper;
import com.binance.mgs.account.service.UserComplianceService;
import com.binance.mgs.account.util.PhoneNumberUtils;
import com.binance.mgs.account.util.RegexUtils;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/v3")
@Slf4j
public class AuthV3Controller extends AuthCenterBaseAction {
    @Resource
    private AuthHelper authHelper;
    @Resource
    private SensorHelper sensorHelper;
    @Resource
    private UserSecurityApi userSecurityApi;
    @Resource
    private GeeTestHelper geeTestHelper;
    @Resource
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;

    @Resource
    private AccountHelper accountHelper;

    @Resource
    private RiskKafkaHelper riskKafkaHelper;

    @Autowired
    private AntiBotHelper antiBotHelper;

    @Autowired
    private AccountOauthApi accountOauthApi;

    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Autowired
    protected MetricsEventPublisher metricsEventPublisher;

    @Value("${ddos.login.check.switch:false}")
    private boolean ddosLoginCheckSwitch;

    @Value("${ddos.login.ip.limit.count:30}")
    private int ddosloginIpLimitCount;


    @Value("${need.plus.gmail.switch:false}")
    private boolean needPlusGmailSwitch;

    @Value("${need.force.check.gt.switch:true}")
    private boolean needForceCheckGtSwitch;

    @Value("${user.notActive.metric.switch:true}")
    private boolean userNotActiveMetricSwitch;

    @Value("${login.mobile.code.check.switch:true}")
    private boolean mobileCodeCheckSwitch;

    @Value("${login.fvideoId.check.switch:false}")
    private boolean loginFvideoIdCheckSwitch;

    /**
     * 只要账号、密码（或passkey）正确即可登录的user id list，不需要做mfa
     */
    @Value("${login.user.id.while.list:}")
    private List<Long> loginUserIdWhiteList;

    @Autowired
    private UserComplianceService userComplianceService;


    private RedisTemplate<String, Object> accountMgsRedisTemplate=AccountMgsRedisHelper.getInstance();

    @Autowired
    private PhoneNumberUtils phoneNumberUtils;
    
    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;

    @Autowired
    private RiskService riskService;

    @PostMapping(value = "/public/authcenter/login")
    @DDoSPreMonitor(action = "loginV3")
    @AntiBotCaptchaValidate(bizType = LOGIN)
    @AccountDefenseResource(name = "AuthV3Controller.login")
    @UserOperation(eventName = "login", name = "用户登陆", logDeviceOperation = true, deviceOperationNoteField = {"userId"}, requestKeys = {"email"},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {"email", "mobile"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @CallAppCheck(value = "AuthV3Controller.login")
    public CommonRet<LoginRet> login(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody LoginV3Arg loginArg)
            throws Exception {
        // 判断当前版本是否允许登录
        if(authHelper.isLoginBanned()){
            throw new BusinessException(AccountMgsErrorCode.UPDATE_VERSION);
        }
        UserOperationHelper.log("step", "login");
        validateFvideoId(request);
        if (mobileCodeCheckSwitch) {
            if (StringUtils.isNotBlank(loginArg.getMobileCode()) && StringUtils.isNumeric(loginArg.getMobileCode())) {
                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
            }
            if (StringUtils.isNotBlank(loginArg.getMobileCode())) {
                phoneNumberUtils.checkPhoneNumber(loginArg.getMobileCode(), loginArg.getMobile());
            }
        }
        if (StringUtils.isNotBlank(loginArg.getEmail()) && !timeOutRegexUtils.validateEmail(loginArg.getEmail())) {
            String ip = WebUtils.getRequestIp();
            if (ddosLoginCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ddosloginIpLimitCount, String.format("loginV3 fake email=%s", loginArg.getEmail()));
            }
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }
        boolean checkedVerifyCode=false;
        String loginPhoneOrEmail = StringUtils.isAllBlank(loginArg.getMobile(), loginArg.getMobileCode(), loginArg.getEmail())?null:(StringUtils.isNotBlank(loginArg.getEmail())?loginArg.getEmail():loginArg.getMobile()+loginArg.getMobileCode());
        //原来的authtype改成4码验证
//        if (StringUtils.isAllBlank(loginArg.getMobileVerifyCode(), loginArg.getEmailVerifyCode(), loginArg.getGoogleVerifyCode(), loginArg.getYubikeyVerifyCode(),loginArg.getVerifyToken(),loginArg.getFidoVerifyCode(),loginArg.getFidoExternalVerifyCode())) {
            // 首次登录才需要验证极验，authType为空则说明为首次登录 ,关闭极验验证
            ValidateCodeArg validateCodeArg = new ValidateCodeArg();
            BeanUtils.copyProperties(loginArg, validateCodeArg);
            if (geeTestHelper.checkLogin(WebUtils.getRequestIp())&& needForceCheckGtSwitch) {
                log.info("verifying code. email: {}", loginArg.getEmail());
                accountHelper.verifyCodeCacheAndBanIp(validateCodeArg, LOGIN);
            } else {
                log.warn("skip verifying code. email: {}", loginArg.getEmail());
            }
            // 验证通过之后设置标记
            checkedVerifyCode=true;
//        }
        CommonRet<LoginRet> ret = new CommonRet<>();
        String loginSource = RiskLoginInfoDto.MOBILE_LOGIN;
        Long loginUserId;
        if (StringUtils.isNoneBlank(loginArg.getMobile(), loginArg.getMobileCode(), loginArg.getEmail())) {
            // 手机号登录授权
            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
            getUserIdReq.setEmail(null);
            getUserIdReq.setMobileCode(loginArg.getMobileCode());
            getUserIdReq.setMobile(loginArg.getMobile());
            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
            if (!baseHelper.isOk(getUserIdResp)) {
                log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
                //ddos拦截逻辑，不存在的恶意用户刷接口直接封堵掉，以免下游服务被打垮
                if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
                    String ip = WebUtils.getRequestIp();
                    if (ddosLoginCheckSwitch) {
                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosloginIpLimitCount, String.format("loginV3 fake mobile=%s-%s", loginArg.getMobileCode(), loginArg.getMobile()));
                    }
                }
                checkResponseMisleaderUseNotExitsError(getUserIdResp);
            }
            Long userId = getUserIdResp.getData().getUserId();
            loginUserId = userId;
            String userEmail = accountHelper.getUserById(userId).getData().getEmail();
            if (!userEmail.equalsIgnoreCase(loginArg.getEmail())) {
                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
            }
            loginArg.setEmail(userEmail);
            UserOperationHelper.log("loginType", "email");
            loginSource = RiskLoginInfoDto.EMAIL_LOGIN;
        } else if (StringUtils.isNoneBlank(loginArg.getMobile(), loginArg.getMobileCode()) && StringUtils.isBlank(loginArg.getEmail())){
            //手机号登录
            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
            getUserIdReq.setEmail(null);
            getUserIdReq.setMobileCode(loginArg.getMobileCode());
            getUserIdReq.setMobile(loginArg.getMobile());
            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
            if (!baseHelper.isOk(getUserIdResp)) {
                log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
                //ddos拦截逻辑，不存在的恶意用户刷接口直接封堵掉，以免下游服务被打垮
                if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
                    String ip = WebUtils.getRequestIp();
                    if (ddosLoginCheckSwitch) {
                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosloginIpLimitCount, String.format("loginV3 fake mobile=%s-%s", loginArg.getMobileCode(), loginArg.getMobile()));
                    }
                }
                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
            }
            Long userId = getUserIdResp.getData().getUserId();
            loginUserId = userId;
            String userEmail = accountHelper.getUserById(userId).getData().getEmail();
            loginArg.setEmail(userEmail);
            UserOperationHelper.log("loginType", "mobile");
            loginSource = RiskLoginInfoDto.MOBILE_LOGIN;
        }else{
            if (StringUtils.isBlank(loginArg.getEmail())) {
                String ip = WebUtils.getRequestIp();
                if (ddosLoginCheckSwitch) {
                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ddosloginIpLimitCount, String.format("loginV3 fake email=%s", loginArg.getEmail()));
                }
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
            //不允许用fake邮箱登录
            if(loginArg.getEmail().contains("_mobileuser@binance.com")){
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
            //替换逻辑
            if(needPlusGmailSwitch && loginArg.getEmail().contains("@gmail.com")){
                //先把前后的空格去掉，然后替换掉中间的空格
                log.info("oldemail={}",loginArg.getEmail());
                String tempPlusEmail=loginArg.getEmail().trim().replace(" ","+");
                log.info("tempPlusEmail={}",tempPlusEmail);
                loginArg.setEmail(tempPlusEmail);
            }
            //拦截不存在的email登录
            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
            getUserIdReq.setEmail(loginArg.getEmail().trim());
            getUserIdReq.setMobileCode(null);
            getUserIdReq.setMobile(null);
            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
            if (!baseHelper.isOk(getUserIdResp)) {
                log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
                //ddos拦截逻辑，不存在的恶意用户刷接口直接封堵掉，以免下游服务被打垮
                if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
                    String ip = WebUtils.getRequestIp();
                    if (ddosLoginCheckSwitch) {
                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosloginIpLimitCount, String.format("loginV3 fake email=%s", loginArg.getEmail()));
                    }
                }
                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
            }
            loginUserId = getUserIdResp.getData().getUserId();
            UserOperationHelper.log("loginType", "email");
            loginSource = RiskLoginInfoDto.EMAIL_LOGIN;
        }
//        //记录操作日志,现在实际上登录有2fa的话就是手机google 2选一，所以也不需要验证别的了
//        boolean isSecondVerifyFlag=false;
        UserOperationHelper.log("is2faSubmitted", "false");
        UserOperationHelper.log("verifyType", null);
//        if (StringUtils.isAllBlank(loginArg.getMobileVerifyCode(),loginArg.getEmailVerifyCode(),loginArg.getGoogleVerifyCode(),loginArg.getYubikeyVerifyCode(),loginArg.getFidoVerifyCode(),loginArg.getFidoExternalVerifyCode(),loginArg.getVerifyToken())) {
//            isSecondVerifyFlag=false;
//            UserOperationHelper.log("is2faSubmitted", "false");
//            UserOperationHelper.log("verifyType", null);
//        }else if(StringUtils.isNotBlank(loginArg.getMobileVerifyCode())){
//            UserOperationHelper.log("verifyType", "mobile");
//            UserOperationHelper.log("is2faSubmitted", "true");
//            loginSource = RiskLoginInfoDto.typeTo2FA(loginSource);
//        }else if(StringUtils.isNotBlank(loginArg.getGoogleVerifyCode())){
//            UserOperationHelper.log("verifyType", "google");
//            UserOperationHelper.log("is2faSubmitted", "true");
//            loginSource = RiskLoginInfoDto.typeTo2FA(loginSource);
//        }else if(StringUtils.isNotBlank(loginArg.getYubikeyVerifyCode())){
//            UserOperationHelper.log("verifyType", "yubikey");
//            UserOperationHelper.log("is2faSubmitted", "true");
//        }else if(StringUtils.isNotBlank(loginArg.getFidoVerifyCode())){
//            UserOperationHelper.log("verifyType", "fido");
//            UserOperationHelper.log("is2faSubmitted", "true");
//        }else if(StringUtils.isNotBlank(loginArg.getFidoExternalVerifyCode())){
//            UserOperationHelper.log("verifyType", "fido_external");
//            UserOperationHelper.log("is2faSubmitted", "true");
//        }

        if(StringUtils.isNotBlank(loginArg.getSessionId())){
            UserOperationHelper.log("validate", ddosCacheSeviceHelper.getValidateId(loginArg.getSessionId()));
        }

        //不是二次验证密码又不传肯定是黑客
        if(StringUtils.isAllBlank(loginArg.getPassword(), loginArg.getSafePassword(), loginArg.getFidoVerifyCode())){
            String ip = WebUtils.getRequestIp();
            if (ddosLoginCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_VERIFY_INFO, ip, ddosloginIpLimitCount, String.format("loginV3 blank safepwd email=%s", loginArg.getEmail()));
            }
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }
        //新密码传了，但是算法不对的话就报错然后ddos
        if (StringUtils.isNotBlank(loginArg.getSafePassword()) && !RegexUtils.validateSafePassword(loginArg.getSafePassword())) {
            String ip = WebUtils.getRequestIp();
            if (ddosLoginCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_VERIFY_INFO, ip, ddosloginIpLimitCount, String.format("loginV3 error safepwd email=%s", loginArg.getEmail()));
            }
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }
        if (StringUtils.isBlank(loginArg.getEmail())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        if (!antiBotHelper.timeoutCheckPassAntiBot(request, BizType.LOGIN, loginUserId, loginArg.getEmail(), loginArg.getMobile(), loginArg.getMobileCode())) {
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        userComplianceService.complianceBlockLoginWithTimeout(loginUserId);

        LoginResponseV3 loginResponse = authHelper.loginV3(request, loginArg,checkedVerifyCode);

        PnkLoginDto loginDto = loginResponse.getPnkLoginDto();
        if (loginDto != null) {
            UserOperationHelper.log("authStatus", loginDto.getAuthStatus());
//            if(AuthStatusEnum.NOT_NEED_AUTH.name().equalsIgnoreCase(loginDto.getAuthStatus())){
//                UserOperationHelper.log("verifyStatus", "false");
//                UserOperationHelper.log("loginStatus", "true");
//            }else if(AuthStatusEnum.OK.name().equalsIgnoreCase(loginDto.getAuthStatus())){
//                UserOperationHelper.log("verifyStatus", "true");
//                UserOperationHelper.log("loginStatus", "true");
//            }else if(AuthStatusEnum.NO_AUTH.name().equalsIgnoreCase(loginDto.getAuthStatus())){
                UserOperationHelper.log("verifyStatus", "false");
                UserOperationHelper.log("loginStatus", "false");
                UserOperationHelper.log("loginVerifyType", loginDto.getLoginVerifyType());
                UserOperationHelper.log("newDevice", loginDto.isNewDeviceFLag());
//            }else{
//                UserOperationHelper.log("verifyStatus", "false");
//                UserOperationHelper.log("loginStatus", "false");
//            }
            String userId = loginDto.getUserId();
            if (StringUtils.isNotEmpty(loginDto.getUserId())) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, loginDto.getUserId(), "authStatus",
                        ObjectUtils.defaultIfNull(loginDto.getAuthStatus(), "")));
            } else {
                // 登录失败不返回userId，尝试根据email获取userId。
                GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
                getUserIdReq.setEmail(loginArg.getEmail());
                getUserIdReq.setMobileCode(loginArg.getMobileCode());
                getUserIdReq.setMobile(loginArg.getMobile());
                APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
                if (baseHelper.isOk(getUserIdResp) && null != getUserIdResp.getData().getUserId()) {
                    userId = String.valueOf(getUserIdResp.getData().getUserId());
                    UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
                }
            }
            if (!loginDto.isSuccess() && StringUtils.isNotEmpty(loginDto.getCode())) {
                UserOperationHelper.log(ImmutableMap.of("login", Boolean.FALSE.toString(), "success", Boolean.FALSE.toString()));
                if (TwofaErrorCodes.contains(loginDto.getCode())) {
                    UserOperationHelper.logFailReason("2fa_error");
                    UserOperationHelper.log("2fa_error_code", loginDto.getCode());
                }
                // AccountErrorCode.ACCOUNT_IS_KYC_NONE_US_IN_RISK_HAS_ASSET.getCode() = 001447
                if (StringUtils.equalsAny(loginDto.getCode(), AccountErrorCode.ACCOUNT_IS_KYC_US_HAS_ASSET.getCode(), "001447")) {
                   //kyc us的正常报错，但是会记录到redis
                    accountMgsRedisTemplate.opsForValue().set(CacheConstant.ACCOUNT_KYC_US_LOGIN+loginPhoneOrEmail,1,1, TimeUnit.HOURS);

                }
                if (StringUtils.equals(GeneralCode.USER_NOT_ACTIVE.getCode(), loginDto.getCode())) {
                    // 未激活在新流程里面不需要发送激活邮件
                    ddosCacheSeviceHelper.setRegisterEmailCache(loginArg.getEmail());
                    ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(loginArg.getEmail(),loginArg.getMobile(), DdosOperationEnum.ACTIVE);
                    if (userNotActiveMetricSwitch) {
                        metricsEventPublisher.publish((UserNotActiveCounterMetrics.builder().scene("loginV3").build()));
                    }

                    throw new BusinessException(GeneralCode.USER_NOT_ACTIVE);
                }

                // ios Android若非登陆成功，统统抛异常
                if (StringUtils.equalsAnyIgnoreCase(baseHelper.getClientType(), "ios", "android")) {
                    if (StringUtils.equals(GeneralCode.USER_DISABLED.getCode(), loginDto.getCode())) {
                        // 记录redis 判断是否需要极验
                        geeTestHelper.increaseLogin(WebUtils.getRequestIp());
                        // 为了兼容pnk，authcenter返回的msg=user.disabled，为了给app端展示正确提示，重新定义一个错误码
                        throw new BusinessException(MgsErrorCode.USER_DISABLED);
                    } else {
                        // 记录redis 判断是否需要极验
                        geeTestHelper.increaseLogin(WebUtils.getRequestIp());
                        throw new BusinessException(loginDto.getCode(), loginDto.getMsg(), loginDto.getErrorCodeParam());
                    }
                } else {
                    // 若是用户被禁用，不抛异常，通过disabled字段返回给前端，前端需要特殊处理
                    if (!StringUtils.equals(GeneralCode.USER_DISABLED.getCode(), loginDto.getCode())) {
                        // 记录redis 判断是否需要极验
                        geeTestHelper.increaseLogin(WebUtils.getRequestIp());
                        throw new BusinessException(loginDto.getCode(), loginDto.getMsg(), loginDto.getErrorCodeParam());
                    }
                }
            // loginV3### deviceToken已经没用了
//            } else if (!loginDto.isSuccess() && StringUtils.isNotEmpty(loginDto.getDeviceToken())) {
            } else if (!loginDto.isSuccess() && loginDto.isNewDeviceFLag()) {
                // 用来写入新设备授权的半登陆态
                // 新设备授权需要特殊处理
                if (baseHelper.isFromWeb()) {
                    // 暂不开放
                    // String deviceAuthCode = loginDto.getDeviceToken();
                    // deviceAuthHelper.create(userId, deviceAuthCode);
                    // BaseHelper.setCookie(request, response, true, Constant.COOKIE_DEVICE_AUTH_CODE,
                    // deviceAuthCode);
                    log.info("record half login cookie");
                    // web端 若无2fa 且是新设备才会走到这个逻辑
                    if (StringUtils.isNotBlank(loginDto.getSerialNo())) {
                        BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, loginDto.getSerialNo());
                    }
                } 
                // loginV3### 下面这个逻辑跟正常流程一样，不需要再代码上单独走一个分支了
//                else {
//                    // APP端 若无2fa 且是新设备才会走到这个逻辑
//                    if (StringUtils.isNotBlank(loginDto.getToken())) {
//                        CommonRet<LoginRet> newDeviceRet = new CommonRet<>();
//                        /*
//                         * newDeviceRet.setCode(loginDto.getCode()); newDeviceRet.setMessage(loginDto.getMsg());
//                         */
//                        LoginRet data = new LoginRet();
//                        BeanUtils.copyProperties(loginDto, data);
//                        data.setBncLocation(userComplianceService.getBncLocationWithTimeout(Long.valueOf(userId)));
//                        newDeviceRet.setData(data);
//                        return newDeviceRet;
//                    }
//                }
            }

            LoginRet data = new LoginRet();
            BeanUtils.copyProperties(loginDto, data);
            data.setBncLocation(userComplianceService.getBncLocationWithTimeout(Long.valueOf(userId)));
            ret.setData(data);
            if (baseHelper.isFromWeb()) {
                // 网页版设置cookie,半登录态还是顶级域名，兼容yubikey
                BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, loginDto.getSerialNo());
                // loginV3### 这个逻辑是，第一次login时，这些cookie都设置为空（web端第一次login获取半登录态时，没有token，只有serialNo）；第二次登录成功时，设置真实cookie
                String code = authHelper.setAuthCookie(request,response,loginDto.getToken(),loginDto.getCsrfToken());
                data.setCode(code);
                // 根据安全要求，web端不返回token
                data.setToken(null);
                data.setRefreshToken(null);

            }

            if (loginDto.isMobileSecurity()) {
                UserIdRequest userIdRequest = new UserIdRequest();
                userIdRequest.setUserId(Long.valueOf(loginDto.getUserId()));
                APIResponse<UserSecurityVo> securityResponse = userSecurityApi.getUserSecurityByUserId(getInstance(userIdRequest));
                if (securityResponse.getData() != null) {
                    data.setMobile(securityResponse.getData().getMobile());
                    data.setMobileCode(securityResponse.getData().getMobileCode());

                }
            }
            // Logic for pendingReview
            boolean isSuccess = loginDto.isSuccess();
            boolean isDisabled = loginDto.isDisable();
            if (!isSuccess && isDisabled && StringUtils.isNotBlank(userId)) {
                data.setPendingReview(authHelper.securityResetIsPending(Long.valueOf(userId)));
            }
            
            // loginV3### 缓存一些登录信息（新设备、登录验证方式等），缓存时间长于半登录态的有效期（半个小时）
            LoginContextDto loginContext = LoginContextDto.builder()
                    .userId(Long.valueOf(userId))
                    .fvideoId(userDeviceHelper.getFVideoId(request))
                    .newDeviceFlag(loginDto.isNewDeviceFLag())
                    .loginVerifyType(loginDto.getLoginVerifyType())
                    .build();
            
            String deviceFlag = userDeviceHelper.getFVideoId(request);
            if (StringUtils.isBlank(deviceFlag)) {
                deviceFlag = request.getHeader(CommonUserDeviceHelper.BNC_UUID);    
            }
            String loginContextKey = CacheConstant.ACCOUNT_MGS_LOGIN_CONTEXT_CACHE_KEY+userId+":"+deviceFlag;
            ShardingRedisCacheUtils.set(loginContextKey, loginContext, TimeUnit.HOURS.toSeconds(1));

//            // 登陆成功
//            if (isSuccess && StringUtils.isNotEmpty(loginDto.getToken())) {
//                UserOperationHelper.log(ImmutableMap.of("login", Boolean.TRUE.toString()));
//                // 数据分析
//                sensorHelper.profileSet(Long.valueOf(userId));
//                // 记录登陆日志
//                authHelper.loginWithIpNew(loginDto,WebUtils.getRequestIp());
//                // 发送消息到人机识别系统
//                authHelper.sendHumanRecognitionTopic(userId, baseHelper.getCookieValue(Constant.COOKIE_CLIENT_ID));
//
//                //发送登录信息给risk
//                RiskLoginInfoDto riskLoginInfoDto = new RiskLoginInfoDto();
//                riskLoginInfoDto.setMobile(loginArg.getMobile());
//                riskLoginInfoDto.setMobileCode(loginArg.getMobileCode());
//                riskLoginInfoDto.setEmail(loginArg.getEmail());
//                riskLoginInfoDto.setIp(WebUtils.getRequestIp());
//                riskLoginInfoDto.setSource(loginSource);
//                riskLoginInfoDto.setUserId(Long.valueOf(userId));
//                riskKafkaHelper.sendLoginInfoToRiskByDto(riskLoginInfoDto);
//
//                //绑定三方账户
//                String registerToken = baseHelper.isFromWeb()?authHelper.getRegisterTokenCookie():loginArg.getRegisterToken();
//                log.info("loginV2.coockie.registerToken:{}",registerToken);
//                if (StringUtils.isNotBlank(registerToken) && StringUtils.isNotBlank(loginArg.getThreePartySource()) && BinanceMgsAccountConstant.THREE_PARTY_SOURCE.equals(loginArg.getThreePartySource())){
//                    try {
//                        DoLoginBindThreePartyRequest doLoginBindThreePartyRequest = new DoLoginBindThreePartyRequest();
//                        doLoginBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
//                        doLoginBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, loginArg.getEmail()));
//                        doLoginBindThreePartyRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
//                        doLoginBindThreePartyRequest.setRegisterToken(registerToken);
//                        doLoginBindThreePartyRequest.setUserId(Long.valueOf(userId));
//                        APIResponse<Boolean> bindThreePartyRes = accountOauthApi.doLoginBindThreeParty(getInstance(doLoginBindThreePartyRequest));
//                        log.info("accountOauthApi.doLoginBindThreeParty.userId:{},bindThreePartyRes:{}",userId,JsonUtils.toJsonHasNullKey(bindThreePartyRes));
//                    }catch (Exception e){
//                        log.error("accountOauthApi.doLoginBindThreeParty.userId:{},error:{}",userId,e);
//                    }
//                    authHelper.setRegisterTokenCookie(request,response,registerToken,0);
//                }
//            } else {
            UserOperationHelper.log(ImmutableMap.of("login", Boolean.FALSE.toString()));
            // 记录redis 判断是否需要极验
            geeTestHelper.increaseLogin(WebUtils.getRequestIp());
//            }
        } else {
            // authcenter不应该返回null
            throw new BusinessException(GeneralCode.ERROR);
        }
        return ret;
    }

    /**
     * 校验fvideoId不能为空
     */
    private void validateFvideoId(HttpServletRequest request) {
        String fvideoId = userDeviceHelper.getFVideoId(request);
        if (StringUtils.isBlank(fvideoId)) {
            log.error("loginV3 check fvideoId null, loginFvideoIdCheckSwitch = {}", loginFvideoIdCheckSwitch);
            
            if (loginFvideoIdCheckSwitch) {
                if (baseHelper.isFromWeb()) {
                    throw new BusinessException(AccountMgsErrorCode.PLEASE_CONTACT_CUSTOMER_SERVICE);
                } else {
                    throw new BusinessException(AccountMgsErrorCode.UPDATE_VERSION);
                }
            }
        }
    }

    /**
     * 校验风控MFA，获取完整登录态
     * @param request
     * @param response
     * @param loginArg
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/protect/authcenter/login/verifyMfa")
    @DDoSPreMonitor(action = "loginVerifyMfa")
    @AccountDefenseResource(name = "AuthV3Controller.loginVerifyMfa")
    @UserOperation(eventName = "login", name = "用户登陆", logDeviceOperation = true, deviceOperationNoteField = {"userId"}, requestKeys = {"email"},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {"email", "mobile"})
    @CallAppCheck(value = "AuthV3Controller.loginVerifyMfa")
    public CommonRet<LoginRet> loginVerifyMfa(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody LoginVerifyMfaArg loginArg)
            throws Exception {
        // 判断当前版本是否允许登录
        if(authHelper.isLoginBanned()){
            throw new BusinessException(AccountMgsErrorCode.UPDATE_VERSION);
        }
        UserOperationHelper.log("step", "mfa");
        validateFvideoId(request);
        Long userId = getLoginUserId();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
        log.info("loginVerifyMfa start {}", userId);

        // loginV3### 从缓存中查询登录信息。理论上缓存不可能为空，如果真为空了，就按照最严格的方式做次mfa吧
        String deviceFlag = userDeviceHelper.getFVideoId(request);
        if (StringUtils.isBlank(deviceFlag)) {
            deviceFlag = request.getHeader(CommonUserDeviceHelper.BNC_UUID);
        }
        String loginContextKey = CacheConstant.ACCOUNT_MGS_LOGIN_CONTEXT_CACHE_KEY+userId+":"+deviceFlag;
        Boolean newDeviceFlag = true;
        String loginVerifyType = "PASSWORD";
        LoginContextDto loginContext = ShardingRedisCacheUtils.get(loginContextKey, LoginContextDto.class);
        if (loginContext != null) {
            newDeviceFlag = loginContext.getNewDeviceFlag();
            loginVerifyType = loginContext.getLoginVerifyType();    
        } else {
            log.error("loginVerifyMfa userId {} loginContext cache null", userId);
        }
        UserOperationHelper.log("loginVerifyType", loginVerifyType);
        UserOperationHelper.log("newDevice", newDeviceFlag);
        
        if (loginUserIdWhiteList.contains(userId)) {
            log.info("loginVerifyMfa userId {} in whitelist skip mfa", userId);
            UserOperationHelper.log("skipMfa", "true");
        } else {
            Map<String, String> extraParams = new HashMap<>();
            extraParams.put("deviceType", newDeviceFlag ? "NEW_DEVICE" : "EXIST_DEVICE");
            extraParams.put("preValidationItem", loginVerifyType);
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            riskService.getRiskChallengeTimeOut(userId, deviceInfo, BizSceneEnum.LOGIN.name(), extraParams);
        }
//        if (mobileCodeCheckSwitch) {
//            if (StringUtils.isNotBlank(loginArg.getMobileCode()) && StringUtils.isNumeric(loginArg.getMobileCode())) {
//                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//            }
//            if (StringUtils.isNotBlank(loginArg.getMobileCode())) {
//                phoneNumberUtils.checkPhoneNumber(loginArg.getMobileCode(), loginArg.getMobile());
//            }
//        }
//        if (StringUtils.isNotBlank(loginArg.getEmail()) && !timeOutRegexUtils.validateEmail(loginArg.getEmail())) {
//            String ip = WebUtils.getRequestIp();
//            if (ddosLoginCheckSwitch) {
//                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ddosloginIpLimitCount, String.format("loginV2 fake email=%s", loginArg.getEmail()));
//            }
//            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//        }
//        boolean checkedVerifyCode=false;
        String loginPhoneOrEmail = StringUtils.isAllBlank(loginArg.getMobile(), loginArg.getMobileCode(), loginArg.getEmail())?null:(StringUtils.isNotBlank(loginArg.getEmail())?loginArg.getEmail():loginArg.getMobile()+loginArg.getMobileCode());
//        //原来的authtype改成4码验证
//        if (StringUtils.isAllBlank(loginArg.getMobileVerifyCode(), loginArg.getEmailVerifyCode(), loginArg.getGoogleVerifyCode(), loginArg.getYubikeyVerifyCode(),loginArg.getVerifyToken(),loginArg.getFidoVerifyCode(),loginArg.getFidoExternalVerifyCode())) {
//            // 首次登录才需要验证极验，authType为空则说明为首次登录 ,关闭极验验证
//            ValidateCodeArg validateCodeArg = new ValidateCodeArg();
//            BeanUtils.copyProperties(loginArg, validateCodeArg);
//            if (geeTestHelper.checkLogin(WebUtils.getRequestIp())&& needForceCheckGtSwitch) {
//                log.info("verifying code. email: {}", loginArg.getEmail());
//                accountHelper.verifyCodeCacheAndBanIp(validateCodeArg, LOGIN);
//            } else {
//                log.warn("skip verifying code. email: {}", loginArg.getEmail());
//            }
//            // 验证通过之后设置标记
//            checkedVerifyCode=true;
//        }
        CommonRet<LoginRet> ret = new CommonRet<>();
        // loginV3### 下面这段代码最大的作用是 1.做email、mobile的参数校验；2.当有传mobile时，根据mobile查用户并重新setEmail；3.只有email时，做些校验和转换。但在该接口中已经不需要了，此时用户已经获取半登录态了
//        String loginSource = RiskLoginInfoDto.MOBILE_LOGIN;
//        Long loginUserId;
//        if (StringUtils.isNoneBlank(loginArg.getMobile(), loginArg.getMobileCode(), loginArg.getEmail())) {
//            // 手机号登录授权
//            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
//            getUserIdReq.setEmail(null);
//            getUserIdReq.setMobileCode(loginArg.getMobileCode());
//            getUserIdReq.setMobile(loginArg.getMobile());
//            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
//            if (!baseHelper.isOk(getUserIdResp)) {
//                log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
//                //ddos拦截逻辑，不存在的恶意用户刷接口直接封堵掉，以免下游服务被打垮
//                if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
//                    String ip = WebUtils.getRequestIp();
//                    if (ddosLoginCheckSwitch) {
//                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosloginIpLimitCount, String.format("loginV2 fake mobile=%s-%s", loginArg.getMobileCode(), loginArg.getMobile()));
//                    }
//                }
//                checkResponseMisleaderUseNotExitsError(getUserIdResp);
//            }
//            Long userId = getUserIdResp.getData().getUserId();
//            loginUserId = userId;
//            String userEmail = accountHelper.getUserById(userId).getData().getEmail();
//            if (!userEmail.equalsIgnoreCase(loginArg.getEmail())) {
//                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//            }
//            loginArg.setEmail(userEmail);
//            UserOperationHelper.log("loginType", "email");
//            loginSource = RiskLoginInfoDto.EMAIL_LOGIN;
//        } else if (StringUtils.isNoneBlank(loginArg.getMobile(), loginArg.getMobileCode()) && StringUtils.isBlank(loginArg.getEmail())){
//            //手机号登录
//            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
//            getUserIdReq.setEmail(null);
//            getUserIdReq.setMobileCode(loginArg.getMobileCode());
//            getUserIdReq.setMobile(loginArg.getMobile());
//            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
//            if (!baseHelper.isOk(getUserIdResp)) {
//                log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
//                //ddos拦截逻辑，不存在的恶意用户刷接口直接封堵掉，以免下游服务被打垮
//                if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
//                    String ip = WebUtils.getRequestIp();
//                    if (ddosLoginCheckSwitch) {
//                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosloginIpLimitCount, String.format("loginV2 fake mobile=%s-%s", loginArg.getMobileCode(), loginArg.getMobile()));
//                    }
//                }
//                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//            }
//            Long userId = getUserIdResp.getData().getUserId();
//            loginUserId = userId;
//            String userEmail = accountHelper.getUserById(userId).getData().getEmail();
//            loginArg.setEmail(userEmail);
//            UserOperationHelper.log("loginType", "mobile");
//            loginSource = RiskLoginInfoDto.MOBILE_LOGIN;
//        }else{
//            if (StringUtils.isBlank(loginArg.getEmail())) {
//                String ip = WebUtils.getRequestIp();
//                if (ddosLoginCheckSwitch) {
//                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ddosloginIpLimitCount, String.format("loginV2 fake email=%s", loginArg.getEmail()));
//                }
//                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
//            }
//            //不允许用fake邮箱登录
//            if(loginArg.getEmail().contains("_mobileuser@binance.com")){
//                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
//            }
//            //替换逻辑
//            if(needPlusGmailSwitch && loginArg.getEmail().contains("@gmail.com")){
//                //先把前后的空格去掉，然后替换掉中间的空格
//                log.info("oldemail={}",loginArg.getEmail());
//                String tempPlusEmail=loginArg.getEmail().trim().replace(" ","+");
//                log.info("tempPlusEmail={}",tempPlusEmail);
//                loginArg.setEmail(tempPlusEmail);
//            }
//            //拦截不存在的email登录
//            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
//            getUserIdReq.setEmail(loginArg.getEmail().trim());
//            getUserIdReq.setMobileCode(null);
//            getUserIdReq.setMobile(null);
//            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
//            if (!baseHelper.isOk(getUserIdResp)) {
//                log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
//                //ddos拦截逻辑，不存在的恶意用户刷接口直接封堵掉，以免下游服务被打垮
//                if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
//                    String ip = WebUtils.getRequestIp();
//                    if (ddosLoginCheckSwitch) {
//                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosloginIpLimitCount, String.format("loginV2 fake email=%s", loginArg.getEmail()));
//                    }
//                }
//                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//            }
//            loginUserId = getUserIdResp.getData().getUserId();
//            UserOperationHelper.log("loginType", "email");
//            loginSource = RiskLoginInfoDto.EMAIL_LOGIN;
//        }
        
        // loginV3### 这段代码是从上面简化来的
        String loginSource = RiskLoginInfoDto.MOBILE_LOGIN;
        if (StringUtils.isNoneBlank(loginArg.getMobile(), loginArg.getMobileCode(), loginArg.getEmail())) {
            UserOperationHelper.log("loginType", "email");
            loginSource = RiskLoginInfoDto.EMAIL_LOGIN;    
        } else if (StringUtils.isNoneBlank(loginArg.getMobile(), loginArg.getMobileCode()) && StringUtils.isBlank(loginArg.getEmail())){
            UserOperationHelper.log("loginType", "mobile");
            loginSource = RiskLoginInfoDto.MOBILE_LOGIN;    
        } else if (StringUtils.isBlank(loginArg.getEmail())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);    
        } else {
            UserOperationHelper.log("loginType", "email");
            loginSource = RiskLoginInfoDto.EMAIL_LOGIN;    
        }
        String userEmail = accountHelper.getUserById(userId).getData().getEmail();
        loginArg.setEmail(userEmail);
        
        //记录操作日志,现在实际上登录有2fa的话就是手机google 2选一，所以也不需要验证别的了
//        boolean isSecondVerifyFlag=true;
//        if (StringUtils.isAllBlank(loginArg.getMobileVerifyCode(),loginArg.getEmailVerifyCode(),loginArg.getGoogleVerifyCode(),loginArg.getYubikeyVerifyCode(),loginArg.getFidoVerifyCode(),loginArg.getFidoExternalVerifyCode(),loginArg.getVerifyToken())) {
//            isSecondVerifyFlag=false;
//            UserOperationHelper.log("is2faSubmitted", "false");
//            UserOperationHelper.log("verifyType", null);
//        }else if(StringUtils.isNotBlank(loginArg.getMobileVerifyCode())){
//            UserOperationHelper.log("verifyType", "mobile");
//            UserOperationHelper.log("is2faSubmitted", "true");
            loginSource = RiskLoginInfoDto.typeTo2FA(loginSource);
//        }else if(StringUtils.isNotBlank(loginArg.getGoogleVerifyCode())){
//            UserOperationHelper.log("verifyType", "google");
//            UserOperationHelper.log("is2faSubmitted", "true");
//            loginSource = RiskLoginInfoDto.typeTo2FA(loginSource);
//        }else if(StringUtils.isNotBlank(loginArg.getYubikeyVerifyCode())){
//            UserOperationHelper.log("verifyType", "yubikey");
//            UserOperationHelper.log("is2faSubmitted", "true");
//        }else if(StringUtils.isNotBlank(loginArg.getFidoVerifyCode())){
//            UserOperationHelper.log("verifyType", "fido");
//            UserOperationHelper.log("is2faSubmitted", "true");
//        }else if(StringUtils.isNotBlank(loginArg.getFidoExternalVerifyCode())){
//            UserOperationHelper.log("verifyType", "fido_external");
//            UserOperationHelper.log("is2faSubmitted", "true");
//        }

//        if(StringUtils.isNotBlank(loginArg.getSessionId())){
//            UserOperationHelper.log("validate", ddosCacheSeviceHelper.getValidateId(loginArg.getSessionId()));
//        }

//        //不是二次验证密码又不传肯定是黑客
//        if(!isSecondVerifyFlag && StringUtils.isBlank(loginArg.getPassword())){
//            String ip = WebUtils.getRequestIp();
//            if (ddosLoginCheckSwitch) {
//                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_VERIFY_INFO, ip, ddosloginIpLimitCount, String.format("loginV2 blank safepwd email=%s", loginArg.getEmail()));
//            }
//            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//        }
//        //新密码传了，但是算法不对的话就报错然后ddos
//        if (StringUtils.isNotBlank(loginArg.getSafePassword()) && !RegexUtils.validateSafePassword(loginArg.getSafePassword())) {
//            String ip = WebUtils.getRequestIp();
//            if (ddosLoginCheckSwitch) {
//                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_VERIFY_INFO, ip, ddosloginIpLimitCount, String.format("loginV2 error safepwd email=%s", loginArg.getEmail()));
//            }
//            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//        }
//        if (StringUtils.isBlank(loginArg.getEmail())) {
//            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
//        }

//        if (!antiBotHelper.timeoutCheckPassAntiBot(request, BizType.LOGIN, loginUserId, loginArg.getEmail(), loginArg.getMobile(), loginArg.getMobileCode())) {
//            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
//        }

        userComplianceService.complianceBlockLoginWithTimeout(userId);

        LoginResponseV3 loginResponse = authHelper.loginVerifyMfa(request, loginArg, userId, newDeviceFlag);

        PnkLoginDto loginDto = loginResponse.getPnkLoginDto();
        if (loginDto != null) {
            UserOperationHelper.log("authStatus", loginDto.getAuthStatus());
//            if(AuthStatusEnum.NOT_NEED_AUTH.name().equalsIgnoreCase(loginDto.getAuthStatus())){
//                UserOperationHelper.log("verifyStatus", "false");
//                UserOperationHelper.log("loginStatus", "true");
//            }else 
            if(AuthStatusEnum.OK.name().equalsIgnoreCase(loginDto.getAuthStatus())){
                UserOperationHelper.log("verifyStatus", "true");
                UserOperationHelper.log("loginStatus", "true");
            }
//            else if(AuthStatusEnum.NO_AUTH.name().equalsIgnoreCase(loginDto.getAuthStatus())){
//                UserOperationHelper.log("verifyStatus", "false");
//                UserOperationHelper.log("loginStatus", "false");
//            }
            else{
                UserOperationHelper.log("verifyStatus", "false");
                UserOperationHelper.log("loginStatus", "false");
            }
//            String userId = loginDto.getUserId();
//            if (StringUtils.isNotEmpty(loginDto.getUserId())) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId, "authStatus",
                        ObjectUtils.defaultIfNull(loginDto.getAuthStatus(), "")));
//            } 
//            else {
//                // 登录失败不返回userId，尝试根据email获取userId。
//                GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
//                getUserIdReq.setEmail(loginArg.getEmail());
//                getUserIdReq.setMobileCode(loginArg.getMobileCode());
//                getUserIdReq.setMobile(loginArg.getMobile());
//                APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
//                if (baseHelper.isOk(getUserIdResp) && null != getUserIdResp.getData().getUserId()) {
//                    userId = String.valueOf(getUserIdResp.getData().getUserId());
//                    UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
//                }
//            }
            if (!loginDto.isSuccess() && StringUtils.isNotEmpty(loginDto.getCode())) {
                UserOperationHelper.log(ImmutableMap.of("login", Boolean.FALSE.toString(), "success", Boolean.FALSE.toString()));
//                if (TwofaErrorCodes.contains(loginDto.getCode())) {
//                    UserOperationHelper.logFailReason("2fa_error");
//                    UserOperationHelper.log("2fa_error_code", loginDto.getCode());
//                }
                // AccountErrorCode.ACCOUNT_IS_KYC_NONE_US_IN_RISK_HAS_ASSET.getCode() = 001447
                if (StringUtils.equalsAny(loginDto.getCode(), AccountErrorCode.ACCOUNT_IS_KYC_US_HAS_ASSET.getCode(), "001447")) {
                    //kyc us的正常报错，但是会记录到redis
                    accountMgsRedisTemplate.opsForValue().set(CacheConstant.ACCOUNT_KYC_US_LOGIN+loginPhoneOrEmail,1,1, TimeUnit.HOURS);

                }
                if (StringUtils.equals(GeneralCode.USER_NOT_ACTIVE.getCode(), loginDto.getCode())) {
                    // 未激活在新流程里面不需要发送激活邮件
                    ddosCacheSeviceHelper.setRegisterEmailCache(loginArg.getEmail());
                    ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(loginArg.getEmail(),loginArg.getMobile(), DdosOperationEnum.ACTIVE);
                    if (userNotActiveMetricSwitch) {
                        metricsEventPublisher.publish((UserNotActiveCounterMetrics.builder().scene("loginV3").build()));
                    }

                    throw new BusinessException(GeneralCode.USER_NOT_ACTIVE);
                }

                // ios Android若非登陆成功，统统抛异常
                if (StringUtils.equalsAnyIgnoreCase(baseHelper.getClientType(), "ios", "android")) {
                    if (StringUtils.equals(GeneralCode.USER_DISABLED.getCode(), loginDto.getCode())) {
                        // 记录redis 判断是否需要极验
                        geeTestHelper.increaseLogin(WebUtils.getRequestIp());
                        // 为了兼容pnk，authcenter返回的msg=user.disabled，为了给app端展示正确提示，重新定义一个错误码
                        throw new BusinessException(MgsErrorCode.USER_DISABLED);
                    } else {
                        // 记录redis 判断是否需要极验
                        geeTestHelper.increaseLogin(WebUtils.getRequestIp());
                        throw new BusinessException(loginDto.getCode(), loginDto.getMsg(), loginDto.getErrorCodeParam());
                    }
                } else {
                    // 若是用户被禁用，不抛异常，通过disabled字段返回给前端，前端需要特殊处理
                    if (!StringUtils.equals(GeneralCode.USER_DISABLED.getCode(), loginDto.getCode())) {
                        // 记录redis 判断是否需要极验
                        geeTestHelper.increaseLogin(WebUtils.getRequestIp());
                        throw new BusinessException(loginDto.getCode(), loginDto.getMsg(), loginDto.getErrorCodeParam());
                    }
                }
            }
            // loginV3### 此处已经没有新设备逻辑了。下面代码web端的逻辑是给半登录态，app端是直接返回，可以废弃
//            else if (!loginDto.isSuccess() && StringUtils.isNotEmpty(loginDto.getDeviceToken())) {
//                // 用来写入新设备授权的半登陆态
//                // 新设备授权需要特殊处理
//                if (baseHelper.isFromWeb()) {
//                    // 暂不开放
//                    // String deviceAuthCode = loginDto.getDeviceToken();
//                    // deviceAuthHelper.create(userId, deviceAuthCode);
//                    // BaseHelper.setCookie(request, response, true, Constant.COOKIE_DEVICE_AUTH_CODE,
//                    // deviceAuthCode);
//                    log.info("record half login cookie");
//                    // web端 若无2fa 且是新设备才会走到这个逻辑
//                    if (StringUtils.isNotBlank(loginDto.getSerialNo())) {
//                        BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, loginDto.getSerialNo());
//                    }
//                } else {
//                    // APP端 若无2fa 且是新设备才会走到这个逻辑
//                    if (StringUtils.isNotBlank(loginDto.getToken())) {
//                        CommonRet<LoginRet> newDeviceRet = new CommonRet<>();
//                        /*
//                         * newDeviceRet.setCode(loginDto.getCode()); newDeviceRet.setMessage(loginDto.getMsg());
//                         */
//                        LoginRet data = new LoginRet();
//                        BeanUtils.copyProperties(loginDto, data);
//                        data.setBncLocation(userComplianceService.getBncLocationWithTimeout(Long.valueOf(userId)));
//                        newDeviceRet.setData(data);
//                        return newDeviceRet;
//                    }
//                }
//            }

            LoginRet data = new LoginRet();
            BeanUtils.copyProperties(loginDto, data);
            data.setBncLocation(userComplianceService.getBncLocationWithTimeout(Long.valueOf(userId)));
            ret.setData(data);
            if (baseHelper.isFromWeb()) {
                // 网页版设置cookie,半登录态还是顶级域名，兼容yubikey
                BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, loginDto.getSerialNo());
                // loginV3### 这个逻辑是，第一次login时，这些cookie都设置为空（web端第一次login获取半登录态时，没有token，只有serialNo）；第二次登录成功时，设置真实cookie
                String code = authHelper.setAuthCookie(request,response,loginDto.getToken(),loginDto.getCsrfToken());
                data.setCode(code);
                // 根据安全要求，web端不返回token
                data.setToken(null);
                data.setRefreshToken(null);

            }

            if (loginDto.isMobileSecurity()) {
                UserIdRequest userIdRequest = new UserIdRequest();
                userIdRequest.setUserId(Long.valueOf(loginDto.getUserId()));
                APIResponse<UserSecurityVo> securityResponse = userSecurityApi.getUserSecurityByUserId(getInstance(userIdRequest));
                if (securityResponse.getData() != null) {
                    data.setMobile(securityResponse.getData().getMobile());
                    data.setMobileCode(securityResponse.getData().getMobileCode());

                }
            }
            // Logic for pendingReview
            boolean isSuccess = loginDto.isSuccess();
            boolean isDisabled = loginDto.isDisable();
//            if (!isSuccess && isDisabled && StringUtils.isNotBlank(userId)) {
            if (!isSuccess && isDisabled) {
                data.setPendingReview(authHelper.securityResetIsPending(Long.valueOf(userId)));
            }

            // 登陆成功
            if (isSuccess && StringUtils.isNotEmpty(loginDto.getToken())) {
                UserOperationHelper.log(ImmutableMap.of("login", Boolean.TRUE.toString()));
                // 数据分析
                sensorHelper.profileSet(Long.valueOf(userId));
                // 记录登陆日志
                authHelper.loginWithIpNew(loginDto,WebUtils.getRequestIp());
                // 发送消息到人机识别系统
                authHelper.sendHumanRecognitionTopic(userId.toString(), baseHelper.getCookieValue(Constant.COOKIE_CLIENT_ID));
                // loginV3### 删除登录上下文cache；登录flowId cache
                ShardingRedisCacheUtils.del(loginContextKey);
                ShardingRedisCacheUtils.del(loginArg.getLoginFlowId(), CacheConstant.ACCOUNT_MGS_LOGIN_FLOWID_CACHE_KEY);

                //发送登录信息给risk
                RiskLoginInfoDto riskLoginInfoDto = new RiskLoginInfoDto();
                riskLoginInfoDto.setMobile(loginArg.getMobile());
                riskLoginInfoDto.setMobileCode(loginArg.getMobileCode());
                riskLoginInfoDto.setEmail(loginArg.getEmail());
                riskLoginInfoDto.setIp(WebUtils.getRequestIp());
                riskLoginInfoDto.setSource(loginSource);
                riskLoginInfoDto.setUserId(Long.valueOf(userId));
                riskKafkaHelper.sendLoginInfoToRiskByDto(riskLoginInfoDto);

                //绑定三方账户
                String registerToken = baseHelper.isFromWeb()?authHelper.getRegisterTokenCookie():loginArg.getRegisterToken();
                log.info("loginV3.coockie.registerToken:{}",registerToken);
                if (StringUtils.isNotBlank(registerToken) && StringUtils.isNotBlank(loginArg.getThreePartySource()) && BinanceMgsAccountConstant.THREE_PARTY_SOURCE.equals(loginArg.getThreePartySource())){
                    try {
                        DoLoginBindThreePartyRequest doLoginBindThreePartyRequest = new DoLoginBindThreePartyRequest();
                        doLoginBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
                        doLoginBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, getUserEmail()));
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
            } else {
                UserOperationHelper.log(ImmutableMap.of("login", Boolean.FALSE.toString()));
                // 记录redis 判断是否需要极验
                geeTestHelper.increaseLogin(WebUtils.getRequestIp());
            }
        } else {
            // authcenter不应该返回null
            throw new BusinessException(GeneralCode.ERROR);
        }
        return ret;
    }
    
}
