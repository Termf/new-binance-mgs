package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserRegisterApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.user.enums.RegisterationMethodEnum;
import com.binance.account.vo.user.request.OneButtonRegisterRequest;
import com.binance.account.vo.user.request.OneButtonUserAccountActiveRequest;
import com.binance.account.vo.user.response.AccountActiveUserV2Response;
import com.binance.account.vo.user.response.OneButtonRegisterResponse;
import com.binance.account.vo.userRegister.RegisterAndActiveRequest;
import com.binance.account.vo.userRegister.RegisterAndActiveResponse;
import com.binance.account.vo.userRegister.RegisterConfig;
import com.binance.account.vo.userRegister.RegisterPreCheckRequest;
import com.binance.account.vo.userRegister.RegisterPreCheckResponse;
import com.binance.account.vo.userRegister.VerifyActiveCodeRequest;
import com.binance.account.vo.userRegister.VerifyActiveCodeResponse;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.accountdefensecenter.core.annotation.CallAppCheck;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.RegisterCounterMetrics;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.authcenter.api.AuthApi;
import com.binance.authcenter.vo.ActiveUserV2Request;
import com.binance.authcenter.vo.ActiveUserV2Response;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.AccountCountryHelper;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.helper.VersionHelper;
import com.binance.mgs.account.account.vo.ActiveUserV2Ret;
import com.binance.mgs.account.account.vo.OneButtonRegisterArg;
import com.binance.mgs.account.account.vo.OneButtonRegisterConfirmArg;
import com.binance.mgs.account.account.vo.OneButtonRegisterConfirmRet;
import com.binance.mgs.account.account.vo.RegisterConfirmV2Arg;
import com.binance.mgs.account.account.vo.RegisterRet;
import com.binance.mgs.account.account.vo.RegisterV3Arg;
import com.binance.mgs.account.account.vo.RegisterV3Ret;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.helper.GeeTestHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.mgs.account.kafka.CrmKafkaSupport;
import com.binance.mgs.account.kafka.GrowthKafkaSupport;
import com.binance.mgs.account.security.helper.AntiBotHelper;
import com.binance.mgs.account.service.Account2FaService;
import com.binance.mgs.account.util.GrowthRegisterAgentCodePrefixUtil;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.account.util.RegexUtils;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.mgs.account.util.VersionUtil;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.CommonUtil;
import com.binance.platform.mgs.utils.DomainUtils;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.binance.mgs.account.constant.BizType.REGISTER;

@RestController
@Slf4j
public class RegisterV2Controller extends AccountBaseAction {
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private AuthApi authApi;
    @Resource
    private UserSecurityApi userSecurityApi;
    @Resource
    private AuthHelper authHelper;

    @Resource
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;

    @Value("${oneButtonRegister.redPacket.trackSource:RedPacket}")
    private String redPacketTrackSource;

    private static final Long ONEBUTTON_REGISTER_UUID_EXPIRETIME = 1800l;// 30分钟


    @Resource
    private AccountHelper accountHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Resource
    private RiskHelper riskHelper;
    @Resource
    private UserApi userApi;
    @Autowired
    private AntiBotHelper antiBotHelper;

    @Resource
    private SysConfigHelper sysConfigHelper;

    @Resource
    private GeeTestHelper geeTestHelper;
    @Resource
    private RabbitTemplate rabbitTemplate;
    
    @Resource
    private Account2FaService account2FaService;

    @Value("#{'${limit.china.domains:}'.split(',')}")
    private Set<String> limitChinaDomains;

    @Value("${register.gt.app.check:true}")
    private boolean checkAppGt;


    @Value("${ddos.register.confirm.v2.switch:false}")
    private boolean isDdosRegisterConfirmV2Switch;

    @Value("${ddos.one.button.register.switch:false}")
    private boolean isDdosOneButtonRegisterSwitch;

    @Value("${need.force.check.gt.switch:true}")
    private boolean needForceCheckGtSwitch;

    @Value("#{'${kyc.ignore.register.sources:xxx}'.split(',')}")
    private Set<String> kycIgnoreRegisterSources;

    @Resource
    private UserRegisterApi userRegisterApi;

    @Resource
    private GrowthKafkaSupport growthKafkaSupport;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Resource
    private CrmKafkaSupport crmKafkaSupport;
    @Autowired
    private AccountCountryHelper countryHelper;

    @Resource
    private VersionHelper versionHelper;

    @Value("${DDos.normal.ip.limit.count:10}")
    private int normalUserIpLimitCount;

    @Value("${ddos.register.precheck.switch:false}")
    private boolean isDdosRegisterPrecheckSwitch;

    @Value("${enable.register.web.v2.switch:false}")
    private boolean enableRegisterForWebkSwitch;

    @Value("#{'${growth.register.agentcode.prefix:LIMIT_}'.split(',')}")
    private Set<String> growthRegisterAgentCodePrefix;

    @Value("${growth.register.agentcode.switch:true}")
    private Boolean growthRegisterAgentCodeSwitch;

    @Value("${growth.channel.agentcode.switch:true}")
    private Boolean growthChannelAgentCodeSwitch;

    @Value("${anti.bot.register.check.switch:true}")
    private Boolean isAntiBotRegisterCheck;

    @Value("${register.mobile.code.check.switch:true}")
    private boolean mobileCodeCheckSwitch;

    @Autowired
    private MetricsEventPublisher eventPublisher;



    @PostMapping(value = "/v2/public/account/user/register")
    public CommonRet<RegisterRet> register()
            throws Exception {
        throw new BusinessException(AccountMgsErrorCode.UPDATE_VERSION);
    }


    @PostMapping(value = "/v3/public/account/user/register")
    @AntiBotCaptchaValidate(bizType = {REGISTER})
    @AccountDefenseResource(name = "RegisterV2Controller.registerV3")
    @DDoSPreMonitor(action = "registerV3")
    @UserOperation(eventName = "registerAndActive", name = "用户注册并且激活", requestKeys = {"email","mobileCode","mobile"}, requestKeyDisplayNames = {"邮箱","手机区号","手机号"},
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"}, forwardedCookies = {"p20t","cr20"})
    public CommonRet<RegisterV3Ret> registerV3(HttpServletRequest request, HttpServletResponse response, @RequestBody @Valid RegisterV3Arg registerArg)
            throws Exception {
        String ip = WebUtils.getRequestIp();
        if (ddosCacheSeviceHelper.isDdosAttach(registerArg.getEmail(),registerArg.getMobile(), ip,DdosOperationEnum.ACTIVE)) {
            String countryCode = Ip2LocationSwitchUtils.getCountryShort(ip);
            log.info("isDdosAttach email={},mobile={},country: {} , ip: {}", registerArg.getEmail(),registerArg.getMobile(), countryCode, ip);
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }
        CommonRet<RegisterV3Ret> ret = new CommonRet<>();
        // 已登录的状态下不允许注册
        if (StringUtils.isNotEmpty(getUserIdStrByToken())) {
            log.warn("illegal register request, requestIp:{}, requestInfo:{}", WebUtils.getRequestIp(),
                    BaseHelper.getRequstParam(request));
            throw new BusinessException(AccountMgsErrorCode.PLEASE_LOG_OUT_BEFORE_REGISTER);
        }

        boolean registerOpen = Boolean.parseBoolean(sysConfigHelper.getCodeByDisplayName("register_open"));
        // 判断注册通道是否已关闭
        if (!registerOpen) {
            throw new BusinessException(MgsErrorCode.REGISTER_CLOSE);
        }
        if(StringUtils.isAnyBlank(registerArg.getPassword(),registerArg.getSafePassword())){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // 校验邮箱格式
        if (RegisterationMethodEnum.EMAIL == registerArg.getRegisterationMethod()) {
            if (StringUtils.isAnyBlank(registerArg.getEmail()) || !timeOutRegexUtils.validateEmailForRegister(registerArg.getEmail())) {
                throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
            }
            UserOperationHelper.log("regType", "email");
        }

        if (RegisterationMethodEnum.MOBILE == registerArg.getRegisterationMethod()) {
            if (StringUtils.isAnyBlank(registerArg.getMobile(), registerArg.getMobileCode())) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);

            }

            if (mobileCodeCheckSwitch && StringUtils.isNumeric(registerArg.getMobileCode())) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
            }
            //需要增加对某些地区的限制，不允许某些地区使用手机号注册
            UserOperationHelper.log("regType", "mobile");
        }
        if(StringUtils.isAllBlank(registerArg.getMobileVerifyCode(),registerArg.getEmailVerifyCode())){
            throw new BusinessException(AccountMgsErrorCode.PLEASE_ENTER_VERIFYCODE);
        }

        // 获取ip所属国家
        String countryCode = Ip2LocationSwitchUtils.getCountryShort(IPUtils.getIpAddress(request));
        UserOperationHelper.log("ip_country", countryCode);
        UserOperationHelper.log("set_language", baseHelper.getLanguage());
        UserOperationHelper.log("client_type", baseHelper.getTerminal());
        if (baseHelper.isIOS()) {
            UserOperationHelper.log("version", versionHelper.getVersion());
            UserOperationHelper.log("app_mode", VersionUtil.getBncAppMode());
        } else if (baseHelper.isAndroid() || baseHelper.isElectron()) {
            UserOperationHelper.log("version", versionHelper.getVersion());
            UserOperationHelper.log("app_mode", VersionUtil.getBncAppMode());
        }

        // 当前域名
        String domain = DomainUtils.getDomain();
        log.info("limitChinaDomains={},isFromWeb = {} , countryCode = {} , domain = {}", JsonUtils.toJsonNotNullKey(limitChinaDomains), baseHelper.isFromWeb(), countryCode, domain);
        if (limitChinaDomains.contains(domain) && StringUtils.equalsIgnoreCase(countryCode, "CN") && baseHelper.isFromWeb()) {
            throw new BusinessException(AccountMgsErrorCode.COUNTRY_NOT_SUPPORT);
        } else {
            log.info("isFromWeb = {} , countryCode = {} , domain = {}", baseHelper.isFromWeb(), countryCode, domain);
        }

        String registerSource = registerArg.getSource();
        boolean frontEndSkipForceKyc = false;
        if (CollectionUtils.isNotEmpty(kycIgnoreRegisterSources) && kycIgnoreRegisterSources.contains(registerSource)) {
            frontEndSkipForceKyc = true;
        }
        String region = countryHelper.getRegionByCountryCode(registerArg.getResidentCountry());
        boolean isEURegister = !frontEndSkipForceKyc && StringUtils.equalsAnyIgnoreCase(region, "EU", "EU_PROCESS");

        UserOperationHelper.log("resident_country", registerArg.getResidentCountry());
        UserOperationHelper.log("eu_register_process", isEURegister);

        log.info("residentCountry={},region={},isEURegister={}", registerArg.getResidentCountry(), region, isEURegister);
        // 人机验证 如果超过1h2次则进行进行校验
        if (geeTestHelper.checkRegister(WebUtils.getRequestIp())||needForceCheckGtSwitch) {
            log.info("checkRegister ip={} clientType={}", WebUtils.getRequestIp(), baseHelper.getClientType());
//            String registerBizType = isEURegister ? BizType.REGISTER_EU : BizType.REGISTER;
            String registerBizType = BizType.REGISTER;
            if (baseHelper.isFromWeb()) {
                accountHelper.verifyCodeCacheAndBanIp(registerArg, registerBizType);
            } else {
                // 非web项目之前没做校验，先临时做个监控，一周之后删掉这段监控，直接验证
                try {
                    accountHelper.verifyCodeCacheAndBanIp(registerArg, registerBizType);
                } catch (Exception e) {
                    log.warn("app gt error , registerArg = {}", logFilter(JSON.toJSONString(registerArg)));
                    if (checkAppGt) {
                        throw e;
                    }
                }
            }
        }
        //开始验证是否存在
        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(registerArg.getEmail().trim());
        getUserIdReq.setMobileCode(registerArg.getMobileCode());
        getUserIdReq.setMobile(registerArg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        if (baseHelper.isOk(getUserIdResp)) {
            GetUserIdByEmailOrMobileResponse getUserIdByEmailOrMobileResponse=getUserIdResp.getData();
            //如果用户存在的话需要增加判断逻辑
            long ipUserIdCount =
                    Long.valueOf(String.valueOf(RedisCacheUtils.get(ip+getUserIdByEmailOrMobileResponse.getUserId(), Long.class, CacheConstant.REGISTER_IP_COUNT_USERID, 0L)));
            log.info("userId={},ip={},ipUserIdCount={}",getUserIdByEmailOrMobileResponse.getUserId(),ip,ipUserIdCount);
            if(ipUserIdCount>2){
                try {
                    log.info("userId={},ip={},overlimit",getUserIdByEmailOrMobileResponse.getUserId(),ip);
                    RedisCacheUtils.increment(ip+getUserIdByEmailOrMobileResponse.getUserId(), CacheConstant.REGISTER_IP_COUNT_USERID, 1L, 24L, TimeUnit.HOURS);// 有效期
                } catch (Exception e) {
                    log.error("注册ip userid限制", e);
                }
                RegisterV3Ret registerRet = new RegisterV3Ret();
                ret.setData(registerRet);
                registerRet.setUserId(CommonUtil.getFakeUserIdByEmail(registerArg.getEmail()));
                // 同ip是否频繁注册
                riskHelper.frequentRegisterSendMq(registerRet.getUserId(), registerArg.getEmail());
                return ret;
            }else{
                try {
                    RedisCacheUtils.increment(ip+getUserIdByEmailOrMobileResponse.getUserId(), CacheConstant.REGISTER_IP_COUNT_USERID, 1L, 24L, TimeUnit.HOURS);// 有效期
                } catch (Exception e) {
                    log.error("注册ip userid限制", e);
                }
                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_HAS_BEEN_REGISTERED);
            }

        }

        if (StringUtils.isNotBlank(registerArg.getVerifyToken())) {
            // 如果token不为空验token, 如果为空继续else老逻辑
            account2FaService.publicTokenVerify(registerArg.getEmail(), registerArg.getMobileCode(),
                    registerArg.getMobile(), registerArg.getVerifyToken(), BizSceneEnum.ACCOUNT_ACTIVATE.name());
        } else {
            //开始进行验证码check
            VerifyActiveCodeRequest verifyActiveCodeRequest=new VerifyActiveCodeRequest();
            verifyActiveCodeRequest.setEmail(registerArg.getEmail());
            verifyActiveCodeRequest.setMobileCode(registerArg.getMobileCode());
            verifyActiveCodeRequest.setMobile(registerArg.getMobile());
            verifyActiveCodeRequest.setEmailVerifyCode(registerArg.getEmailVerifyCode());
            verifyActiveCodeRequest.setMobileVerifyCode(registerArg.getMobileVerifyCode());
            APIResponse<VerifyActiveCodeResponse> verifyActiveCodeResponseAPIResponse=userRegisterApi.verifyActiveCode(getInstance(verifyActiveCodeRequest));
            checkResponse(verifyActiveCodeResponseAPIResponse);
        }

        if (isAntiBotRegisterCheck && !antiBotHelper.timeoutCheckPassAntiBot(request, REGISTER, null, registerArg.getEmail(), registerArg.getMobile(), registerArg.getMobileCode())) {
            throw new BusinessException(AccountMgsErrorCode.REGISTER_ANTI_BOT_CHECK_FAILED);
        }

        RegisterAndActiveRequest registerUserRequest = new RegisterAndActiveRequest();
        registerUserRequest.setClientType(baseHelper.getClientType());
        String agentId = registerArg.getAgentId();
        Boolean isAgentIdNull = false;
        // TODO gateway 需要处理refer，并在header中传递标识过来
        if (StringUtils.isBlank(agentId) || StringUtils.isNumeric(agentId)) {
            if (StringUtils.isNumeric(agentId) && request.getHeader(Constant.HEADER_AD_BLOCK) != null) {
                // 广告或者子账号推荐的，agentId清空
                registerUserRequest.setAgentId(null);
            } else if (StringUtils.isNumeric(agentId)) {
                registerUserRequest.setAgentId(Long.parseLong(agentId));
            }
            if (StringUtils.isBlank(agentId) || registerUserRequest.getAgentId() == null) {
                registerUserRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
                isAgentIdNull = true;
            }
        } else if (StringUtils.isNotBlank(agentId) && !StringUtils.isNumeric(agentId)) {
            /*if (agentId.startsWith(growthRegisterAgentCodePrefix)){
                registerUserRequest.setAgentRateCode(null);
                registerUserRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
            }else{
                registerUserRequest.setAgentRateCode(agentId);
                registerUserRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
            }*/
            if (GrowthRegisterAgentCodePrefixUtil.checkGrowthRegisterAgentCodePrefix(growthRegisterAgentCodePrefix, agentId)){
                registerUserRequest.setAgentRateCode(null);
                registerUserRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
            }else{
                registerUserRequest.setAgentRateCode(agentId);
                registerUserRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
            }
        }

        if (isEURegister && RegisterationMethodEnum.MOBILE == registerArg.getRegisterationMethod()) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        //手机号注册或者邮箱注册
        if (RegisterationMethodEnum.MOBILE == registerArg.getRegisterationMethod()) {
            boolean isChinaPhoneNumber = registerArg.getMobileCode().equalsIgnoreCase("cn") || registerArg.getMobileCode().equalsIgnoreCase("86");
            if (isChinaPhoneNumber && !RegexUtils.validateMobilePhone(registerArg.getMobile())) {
                new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
            }
            registerUserRequest.setMobileCode(registerArg.getMobileCode());
            registerUserRequest.setMobile(registerArg.getMobile());
            registerUserRequest.setEmail(null);
        } else {
            registerUserRequest.setMobileCode(null);
            registerUserRequest.setMobile(null);
            registerUserRequest.setEmail(registerArg.getEmail());
        }
        registerUserRequest.setRegisterationMethod(registerArg.getRegisterationMethod());
        registerUserRequest.setPassword(registerArg.getPassword());
        registerUserRequest.setConfirmPassword(registerArg.getPassword());
        registerUserRequest.setSafePassword(registerArg.getSafePassword());
        registerUserRequest.setConfirmSafePassword(registerArg.getSafePassword());
        registerUserRequest.setTrackSource(accountHelper.getRegChannel(registerArg.getRegisterChannel()));
        registerUserRequest.setDeviceInfo(
                userDeviceHelper.buildDeviceInfo(request, null, registerArg.getEmail()));
        registerUserRequest.setTerminal(baseHelper.getTerminal());
        registerUserRequest.setNeedCheckIp(registerArg.getCheckIp());
        // 自定义发送到邮箱中的链接
        String customEmailLink =
                String.format("%sgateway-api/v1/public/account/user/register-confirm?userId={userId}&verifyCode={code}",
                        getBaseUrl());
        registerUserRequest.setCustomEmailLink(customEmailLink);
        // operation log device info
        UserOperationHelper.log("deviceInfo", registerUserRequest.getDeviceInfo());
        // 是否走新的注册流程
        registerUserRequest.setIsNewRegistrationProcess(registerArg.getIsNewRegistrationProcess());
        registerUserRequest.setIsFastCreatFuturesAccountProcess(registerArg.getIsFastCreatFuturesAccountProcess());
        registerUserRequest.setFuturesReferalCode(registerArg.getFuturesReferalCode());
        registerUserRequest.setFvideoId(userDeviceHelper.getFVideoId(request));

        RegisterConfig registerConfig = new RegisterConfig();
        registerConfig.setIsEmailPromote(registerArg.getIsEmailPromote());
        registerConfig.setFrontEndSkipForceKyc(frontEndSkipForceKyc);
        if (isEURegister) {
            registerConfig.setRegion(region);
            registerConfig.setResidentCountry(registerArg.getResidentCountry());
            registerUserRequest.setRegisterConfig(registerConfig);
        }
        log.info("registerConfig={}", registerConfig);
        registerUserRequest.setRegisterConfig(registerConfig);
        registerUserRequest.setIsPersonalAccount(registerArg.getIsPersonalAccount());
        registerUserRequest.setIsStatAgentError(registerArg.getIsStatAgentError());
        registerUserRequest.setOauthClientId(registerArg.getOauthClientId());
        APIResponse<RegisterAndActiveResponse> apiResponse = authApi.registerAndActive(getInstance(registerUserRequest));
        RegisterV3Ret registerRet = new RegisterV3Ret();
        ret.setData(registerRet);
        if (!baseHelper.isOk(apiResponse)) {
            if (StringUtils.equals(GeneralCode.USER_EMAIL_USE.getCode(), apiResponse.getCode())) {
                registerRet.setUserId(CommonUtil.getFakeUserIdByEmail(registerArg.getEmail()));
            } else {
                checkResponse(apiResponse);
            }
        } else {
            //需要设置登录态
            UserOperationHelper.log("isFastCreatFuturesAccountProcess", registerUserRequest.getIsFastCreatFuturesAccountProcess());
            UserOperationHelper.log("registerSuccess", Boolean.TRUE);
            UserOperationHelper.log("validate", ddosCacheSeviceHelper.getValidateId(registerArg.getSessionId()));
            registerRet.setUserId(apiResponse.getData().getUserId());
            if (apiResponse.getData().getUserId() != null) {
                authHelper.sendHumanRecognitionTopic(String.valueOf(apiResponse.getData().getUserId()),
                        baseHelper.getCookieValue(Constant.COOKIE_CLIENT_ID));
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, apiResponse.getData().getUserId()));
            }
            UserOperationHelper.log("RegisterationMethodEnum", registerArg.getRegisterationMethod().getRegisterType());
            if (baseHelper.isFromWeb()) {
                String code = authHelper.setAuthCookie(request, response, apiResponse.getData().getToken(), apiResponse.getData().getCsrfToken());
                registerRet.setCode(code);
            } else {
                ret.setData(CopyBeanUtils.copy(apiResponse.getData(),RegisterV3Ret.class));
            }

            String identify = CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_KEY_PREFIX + ":" + (StringUtils.isNotBlank(registerArg.getEmail()) ? registerArg.getEmail() : registerArg.getMobileCode() + "-" + registerArg.getMobile());
            ShardingRedisCacheUtils.del(identify);
        }


        eventPublisher.publish(RegisterCounterMetrics
                .builder()
                .registerChannel(registerArg.getRegisterChannel())
                .registerSource("REGISTER_V3")
                .agentIdExist(String.valueOf(StringUtils.isNotBlank(registerArg.getAgentId())))
                .futuresReferralCodeExist(String.valueOf(StringUtils.isNotBlank(registerArg.getFuturesReferalCode())))
                .registerMethod(registerArg.getRegisterationMethod().getRegisterType())
                .country(registerArg.getMobileCode())
                .build()
        );
        // 同ip是否频繁注册
        riskHelper.frequentRegisterSendMq(registerRet.getUserId(), registerArg.getEmail());
        // 记录redis 判断是否需要极验
        geeTestHelper.increaseRegister(WebUtils.getRequestIp());
        //发送相关注册信息到growthkafka
        if (growthRegisterAgentCodeSwitch && baseHelper.isOk(apiResponse)){
            growthKafkaSupport.sendAgentToGrowthMsg(registerRet.getUserId(),registerArg.getAgentId(),System.currentTimeMillis(),apiResponse.getData().getDeviceId(),userDeviceHelper.getFVideoId(request), accountHelper.getRegChannel(registerArg.getRegisterChannel()));
        }
        //发送相关channel注册信息到crm
        if (growthChannelAgentCodeSwitch && baseHelper.isOk(apiResponse) &&  isAgentIdNull && request != null && StringUtils.isNotBlank(request.getHeader(CrmKafkaSupport.BNC_APP_CHANNEL))){
            crmKafkaSupport.sendCrmMgsKafkaProducerChannelRegisterTopic(registerRet.getUserId(),WebUtils.getRequestIp(),request.getHeader(CrmKafkaSupport.BNC_APP_CHANNEL), accountHelper.getBncUuidFromRequest(request), baseHelper.getLanguage());
        }
        return ret;
    }



    @PostMapping(value = "/v2/public/account/user/registerConfirm")
    @AccountDefenseResource(name = "RegisterV2Controller.registerConfirm")
    @DDoSPreMonitor(action = "registerConfirm")
    @UserOperation(eventName = "registerConfirm", name = "用户注册验证", sendToSensorData = false, sendToBigData = false,
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @CacheControl(noStore = true)
    @CallAppCheck(value="RegisterV2Controller.registerConfirm")
    public CommonRet<ActiveUserV2Ret> registerConfirm(HttpServletRequest request, HttpServletResponse response,
                                                           @Validated @RequestBody RegisterConfirmV2Arg registerConfirmArg) throws Exception {
        log.info("registerConfirmV2 request={}", JSON.toJSONString(registerConfirmArg));
        String ip = WebUtils.getRequestIp();
        if (ddosCacheSeviceHelper.isDdosAttach(registerConfirmArg.getEmail(),registerConfirmArg.getMobile(), ip,DdosOperationEnum.ACTIVE)) {
            String countryCode = Ip2LocationSwitchUtils.getCountryShort(ip);
            log.info("isDdosAttach email={},mobile={},country: {} , ip: {}", registerConfirmArg.getEmail(),registerConfirmArg.getMobile(), countryCode, ip);
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }

        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(registerConfirmArg.getMobile(), registerConfirmArg.getMobileCode()) &&
                StringUtils.isBlank(registerConfirmArg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(registerConfirmArg.getEmail());
        getUserIdReq.setMobileCode(registerConfirmArg.getMobileCode());
        getUserIdReq.setMobile(registerConfirmArg.getMobile());

        Map<String, String> relatedInfo = new HashMap<>();
        relatedInfo.put("oldRegistration", "true");
        if (!antiBotHelper.timeoutCheckPassAntiBot(request, BizType.REGISTER, null, registerConfirmArg.getEmail(), registerConfirmArg.getMobile(), registerConfirmArg.getMobileCode(), relatedInfo)) {
            throw new BusinessException(AccountMgsErrorCode.REGISTER_VALIDATE_FAILED);
        }


        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        if (!baseHelper.isOk(getUserIdResp)) {
            log.warn("user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
            checkResponseMaskUseNotExits(getUserIdResp);
            return new CommonRet<>();
        }
        if(StringUtils.isNotBlank(registerConfirmArg.getEmail())){
            UserOperationHelper.log("regType", "email");
        }else{
            UserOperationHelper.log("regType", "mobile");
        }
        Long userId = getUserIdResp.getData().getUserId();
        CommonRet<ActiveUserV2Ret> ret = new CommonRet<>();
        ActiveUserV2Request accountActiveUserRequest = new ActiveUserV2Request();
        accountActiveUserRequest.setUserId(userId);
        accountActiveUserRequest.setMobileVerifyCode(registerConfirmArg.getMobileVerifyCode());
        accountActiveUserRequest.setEmailVerifyCode(registerConfirmArg.getEmailVerifyCode());
        accountActiveUserRequest.setGoogleVerifyCode(registerConfirmArg.getGoogleVerifyCode());
        accountActiveUserRequest.setClientType(baseHelper.getClientType());
        APIResponse<ActiveUserV2Response> apiResponse = authApi.accountActiveV2(getInstance(accountActiveUserRequest));
        if (baseHelper.isOk(apiResponse)) {
            UserOperationHelper.log(ImmutableMap.of("userId", apiResponse.getData().getUserId()));
            if (baseHelper.isFromWeb()) {
                String code = authHelper.setAuthCookie(request, response, apiResponse.getData().getToken(), apiResponse.getData().getCsrfToken());
                ActiveUserV2Ret activeUserV2Ret = new ActiveUserV2Ret();
                activeUserV2Ret.setCode(code);
                ret.setData(activeUserV2Ret);
            } else {
                ret.setData(CopyBeanUtils.copy(apiResponse.getData(),ActiveUserV2Ret.class));
            }
        } else {
            checkResponse(apiResponse);
        }
        return ret;
    }


    @PostMapping(value = "/v2/public/account/user/oneButtonRegister")
    @AccountDefenseResource(name = "RegisterV2Controller.oneButtonRegister")
    @DDoSPreMonitor(action = "oneButtonRegister")
    @UserOperation(eventName = "oneButtonRegister", name = "用户一键注册", requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"}, forwardedCookies = {"p20t","cr20"})
    public CommonRet<RegisterRet> oneButtonRegister(HttpServletRequest request, @RequestBody @Valid OneButtonRegisterArg registerArg)
            throws Exception {
        // 极验验证
        accountHelper.verifyCodeCacheAndBanIp(registerArg, REGISTER);
        CommonRet<RegisterRet> ret = new CommonRet<>();
        // 已登录的状态下不允许注册
        if (StringUtils.isNotEmpty(getUserIdStrByToken())) {
            log.warn("illegal register request, requestIp:{}, requestInfo:{}", WebUtils.getRequestIp(),
                    BaseHelper.getRequstParam(request));
            throw new BusinessException(AccountMgsErrorCode.ONEBUTTON_OPEN_TO_NEW_USER);
        }

        boolean registerOpen = Boolean.parseBoolean(sysConfigHelper.getCodeByDisplayName("register_open"));
        // 判断注册通道是否已关闭
        if (!registerOpen) {
            throw new BusinessException(MgsErrorCode.REGISTER_CLOSE);
        }

        // 校验邮箱格式
        if (RegisterationMethodEnum.EMAIL == registerArg.getRegisterationMethod()) {
            if (StringUtils.isBlank(registerArg.getEmail()) && !timeOutRegexUtils.validateEmailForRegister(registerArg.getEmail())) {
                throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
            }
            UserOperationHelper.log("regType", "email");
        }

        if (RegisterationMethodEnum.MOBILE == registerArg.getRegisterationMethod()) {
            if (StringUtils.isAnyBlank(registerArg.getMobile(), registerArg.getMobileCode())) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);

            }
            //需要增加对某些地区的限制，不允许某些地区使用手机号注册
            UserOperationHelper.log("regType", "mobile");
        }

        // 获取ip所属国家
        String countryCode = Ip2LocationSwitchUtils.getCountryShort(IPUtils.getIpAddress(request));
        UserOperationHelper.log("ip_country", countryCode);
        UserOperationHelper.log("set_language", baseHelper.getLanguage());
        UserOperationHelper.log("client_type", baseHelper.getTerminal());
        if (baseHelper.isIOS()) {
            UserOperationHelper.log("version", versionHelper.getVersion());
            UserOperationHelper.log("app_mode", VersionUtil.getBncAppMode());
        } else if (baseHelper.isAndroid() || baseHelper.isElectron()) {
            UserOperationHelper.log("version", VersionUtil.getVersionName());
            UserOperationHelper.log("app_mode", VersionUtil.getBncAppMode());
        }

        UserOperationHelper.log("deviceInfo", userDeviceHelper.buildDeviceInfo(request, null, registerArg.getEmail()));


        //手机号注册或者邮箱注册
        if (RegisterationMethodEnum.MOBILE == registerArg.getRegisterationMethod()) {
            boolean isChinaPhoneNumber = registerArg.getMobileCode().equalsIgnoreCase("cn") || registerArg.getMobileCode().equalsIgnoreCase("86");
            if (isChinaPhoneNumber && !RegexUtils.validateMobilePhone(registerArg.getMobile())) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
            }
            registerArg.setMobileCode(registerArg.getMobileCode());
            registerArg.setMobile(registerArg.getMobile());
            registerArg.setEmail(null);
        } else {
            registerArg.setMobileCode(null);
            registerArg.setMobile(null);
            registerArg.setEmail(registerArg.getEmail());
        }

        OneButtonRegisterRequest apiRequest = new OneButtonRegisterRequest();
        BeanUtils.copyProperties(registerArg, apiRequest);
        APIResponse<OneButtonRegisterResponse> apiResponse = userApi.oneButtonRegister(getInstance(apiRequest));
        RegisterRet registerRet = new RegisterRet();
        ret.setData(registerRet);
        if (!baseHelper.isOk(apiResponse)) {
            if (StringUtils.equals(GeneralCode.USER_EMAIL_USE.getCode(), apiResponse.getCode())) {
                registerRet.setUserId(CommonUtil.getFakeUserIdByEmail(registerArg.getEmail()));
            } else {
                checkResponse(apiResponse);
            }
        } else {
            // 防ddos攻击
            ddosCacheSeviceHelper.setEmailOrMobileCacheWithOperationEnum(registerArg.getEmail(),registerArg.getMobile(), DdosOperationEnum.ONE_BUTTON_REGISTER);
            UserOperationHelper.log("registerSuccess", Boolean.TRUE);
            UserOperationHelper.log("RegisterationMethodEnum", registerArg.getRegisterationMethod().getRegisterType());
            registerRet.setUserId(apiResponse.getData().getUserId());
            if (apiResponse.getData().getUserId() != null) {
                authHelper.sendHumanRecognitionTopic(String.valueOf(apiResponse.getData().getUserId()),
                        baseHelper.getCookieValue(Constant.COOKIE_CLIENT_ID));
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, apiResponse.getData().getUserId()));
            }
        }

        eventPublisher.publish(RegisterCounterMetrics
                .builder()
                .registerChannel(registerArg.getTrackSource())
                .registerSource("ONE_BUTTON")
                .agentIdExist(String.valueOf(!Objects.isNull(registerArg.getAgentId())))
                .registerMethod(registerArg.getRegisterationMethod().getRegisterType())
                .country(registerArg.getMobileCode())
                .build()
        );
        // 同ip是否频繁注册
        riskHelper.frequentRegisterSendMq(registerRet.getUserId(), registerArg.getEmail());
        return ret;
    }

    @PostMapping(value = "/v2/public/account/user/oneButtonRegisterConfirm")
    @AccountDefenseResource(name = "RegisterV2Controller.oneButtonRegisterConfirm")
    @DDoSPreMonitor(action = "oneButtonRegisterConfirm")
    @UserOperation(eventName = "oneButtonRegisterConfirm", name = "一键注册用户注册验证", sendToSensorData = false, sendToBigData = false,
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<OneButtonRegisterConfirmRet> oneButtonRegisterConfirm(HttpServletRequest request, HttpServletResponse response,
                                                                           @Validated @RequestBody OneButtonRegisterConfirmArg registerConfirmArg) throws Exception {
        log.info("oneButtonRegisterConfirm request={}", JSON.toJSONString(registerConfirmArg));
        String ip = WebUtils.getRequestIp();
        if (ddosCacheSeviceHelper.isDdosAttach(registerConfirmArg.getEmail(),registerConfirmArg.getMobile(), ip,DdosOperationEnum.ONE_BUTTON_REGISTER)) {
            String countryCode = Ip2LocationSwitchUtils.getCountryShort(ip);
            log.info("isDdosAttach email={},mobile={},country: {} , ip: {}", registerConfirmArg.getEmail(),registerConfirmArg.getMobile(), countryCode, ip);
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }

        CommonRet<OneButtonRegisterConfirmRet> ret = new CommonRet<>();
        OneButtonUserAccountActiveRequest activeRequest = new OneButtonUserAccountActiveRequest();
        activeRequest.setEmail(registerConfirmArg.getEmail());
        activeRequest.setMobileCode(registerConfirmArg.getMobileCode());
        activeRequest.setMobile(registerConfirmArg.getMobile());
        activeRequest.setMobileVerifyCode(registerConfirmArg.getMobileVerifyCode());
        activeRequest.setEmailVerifyCode(registerConfirmArg.getEmailVerifyCode());
        activeRequest.setGoogleVerifyCode(registerConfirmArg.getGoogleVerifyCode());
        APIResponse<AccountActiveUserV2Response> apiResponse = userApi.oneButtonUserAccountActive(getInstance(activeRequest));

        if (baseHelper.isOk(apiResponse)) {
            Long userId = apiResponse.getData().getUserId();
            UserOperationHelper.log(ImmutableMap.of("userId", userId));

            // 安全方面考虑，不直接返回userId，而且返回uuid
            OneButtonRegisterConfirmRet confirmRet = new OneButtonRegisterConfirmRet();
            String uuid = com.binance.master.utils.StringUtils.uuid();
            RedisCacheUtils.set("ONEBUTTON_REGISTER:" + uuid, userId, ONEBUTTON_REGISTER_UUID_EXPIRETIME);
            confirmRet.setUuid(uuid);
            ret.setData(confirmRet);
            
            if (redPacketTrackSource.equals(registerConfirmArg.getTrackSource())) {
                Map<String, Object> dataMsg = new HashMap<>();
                try {
                    // 发送mq通知
                    dataMsg.put("optType", "REDPACKET_ONE_BUTTON_REGISTER");
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", userId);
                    data.put("email", registerConfirmArg.getEmail());
                    data.put("mobile", registerConfirmArg.getMobile());
                    data.put("mobileCode", registerConfirmArg.getMobileCode());
                    data.put("trackSource", registerConfirmArg.getTrackSource());
                    data.put("token", uuid);
                    data.put("fvideoId", CommonUserDeviceHelper.getFVideoId(request));
                    if (registerConfirmArg.getExternalData() != null) {
                        data.putAll(registerConfirmArg.getExternalData());
                    }
                    dataMsg.put("data", data);
                    log.info("oneButtonRegisterConfirm sendMq request: {}", JSON.toJSONString(dataMsg));
                    rabbitTemplate.convertAndSend("account.user.oneButtonRegister", "redpacket.user.register", dataMsg);
                } catch (Exception e) {
                    log.error("oneButtonRegisterConfirm sendMq error, mq:" + JSON.toJSONString(dataMsg), e);
                }
            }
        } else {
            checkResponse(apiResponse);
        }
        return ret;
    }


    @PostMapping(value = "/v1/public/account/user/register/precheck")
    public CommonRet<RegisterPreCheckResponse> registerPreCheck(HttpServletRequest request, HttpServletResponse response,
                                                                @Validated @RequestBody RegisterPreCheckRequest registerPreCheckRequest) throws Exception {
        CommonRet<RegisterPreCheckResponse> ret = new CommonRet<>();
        APIResponse<RegisterPreCheckResponse> apiResponse = userRegisterApi.registerPreCheck(getInstance(registerPreCheckRequest));
        checkResponse(apiResponse);
        ret.setData(apiResponse.getData());
        String ip = WebUtils.getRequestIp();
        if (isDdosRegisterPrecheckSwitch) {
            ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_REGISTER, ip, normalUserIpLimitCount, "registerPreCheck");
        }
        return ret;
    }
}
