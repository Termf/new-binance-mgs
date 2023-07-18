package com.binance.mgs.account.security.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileForPrecheckRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.user.enums.ThirdRegisterEnum;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account2fa.api.User2FaApi;
import com.binance.account2fa.enums.AccountVerificationTwoEnum;
import com.binance.account2fa.vo.request.User2FaStatusReq;
import com.binance.account2fa.vo.response.User2FaStatusResp;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.antibot.CaptchaCounterMetrics;
import com.binance.accountoauth.api.AccountOauthApi;
import com.binance.accountoauth.vo.oauth.request.SelectByThirdIdRequest;
import com.binance.accountoauth.vo.oauth.response.ThirdIdUserIdRelationResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.constant.Constant;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.Md5Tools;
import com.binance.master.utils.RandomStringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.security.Base64Util;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.AccountDdosRedisHelper;
import com.binance.mgs.account.account.helper.CaptchaHealthHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.authcenter.vo.AppAttestPrecheckRet;
import com.binance.mgs.account.authcenter.vo.SecurityPreCheckRet;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.integration.SecurityServiceApiClient;
import com.binance.mgs.account.security.vo.CaptchaValidateInfo;
import com.binance.mgs.account.security.vo.SecurityPreCheckArg;
import com.binance.mgs.account.security.vo.UserStatusCacheRet;
import com.binance.mgs.account.service.SecurityAppAttestService;
import com.binance.mgs.account.util.RegexUtils;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.mgs.account.util.VersionUtil;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.security.antibot.api.AntiBotApi;
import com.binance.security.antibot.api.AntiBotApiUtil;
import com.binance.security.antibot.api.SecurityServiceApi;
import com.binance.security.antibot.api.SecurityServiceApiUtil;
import com.binance.security.jantibot.common.enums.RiskLevel;
import com.binance.security.jantibot.common.vo.AntiBotRequest;
import com.binance.security.jantibot.common.vo.AntiBotResponse;
import com.binance.security.jantibot.common.vo.SecurityCheckFeedbackRequest;
import com.binance.security.jantibot.common.vo.SecurityCheckRequest;
import com.binance.security.jantibot.common.vo.SecurityCheckResponse;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/9/18
 */
@Component
@Slf4j
public class AntiBotHelper extends AccountBaseAction {
    private static final int VALIDATION_TYPES_CAPACITY = 2;
    private static final String CAPTCHA_VALIDATION_TYPE = "captcha";

    private static final int SALT_LOCAL_CACHE_CAPACITY = 2;
    private static final int SALT_LOCAL_CACHE_EXPIRATION_SECONDS = 60;

    @Getter
    private static final int VALIDATE_STATUS_REJECT = -1;
    @Getter
    private static final int VALIDATE_STATUS_NEED_CHECK = 0;
    @Getter
    private static final int VALIDATE_STATUS_PASS = 1;
    @Getter
    private static final int VALIDATE_STATUS_BAN_IP = 2;

    @Value("${anti.bot.check.switch:true}")
    private boolean isAntiBotCheck;
    @Value("${anti.bot.new.captcha.check.switch:true}")
    private boolean isAntiBotNewCaptchaCheck;
    @Value("#{'${anti.bot.new.captcha.white.email.list:}'.split(',')}")
    private List<String> emailWhitelist;

    @Value("${anti.bot.session.timeout:1810}")
    private long sessionTimeOutSeconds;
    @Value("${anti.bot.session.salt.lock.timeout:2}")
    private long sessionSaltLockTimeOutSeconds;
    @Value("${anti.bot.session.salt.timeout:1810}")
    private long sessionSaltTimeOutSeconds;
    @Value("${anti.bot.validate.timeout:300}")
    private long antiBotTimeOut;
    @Value("${anti.bot.security.check.timeout:300}")
    private long securityCheckTimeOut;
    @Value("${anti.bot.security.check.userid.timeout:300}")
    private long userIdCacheTimeOut;
    @Value("${anti.bot.query.third.id.timeout:200}")
    private long queryByThirdIdTimeout;

    @Value("${anti.bot.new.captcha.validate.switch:true}")
    private boolean captchaValidateSwitch;
    @Value("${anti.bot.new.captcha.android.version:2.40.0}")
    private String newValidateAndroidVersion;
    @Value("${anti.bot.new.captcha.ios.version:2.40.0}")
    private String newValidateIosVersion;
    @Value("${anti.bot.new.captcha.electron.version:1.29.0}")
    private String newValidateElectronVersion;
    @Value("${ddos.login.ip.limit.count:30}")
    private int ddosLoginIpLimitCount;

    @Value("${anti.bot.skip.refresh.token.switch:false}")
    private boolean skipCheckRefreshToken;
    @Value("${anti.bot.third.id.query.switch:false}")
    private boolean queryByThirdId;
    @Value("${anti.bot.param.preCheck.switch:false}")
    private boolean paramPreCheckSwitch;

    @Value("${anti.bot.enable.passwordless.login.switch:true}")
    private boolean enablePasswordlessLogin;

    @Value("${ddos.sharding.redis.migrate.write.switch:false}")
    private boolean ddosShardingRedisMigrateSwitch;

    @Value("${anti.bot.header.preCheck.switch:false}")
    private boolean headerPreCheckSwitch;

    @Value("${anti.bot.preCheck.feedback.switch:false}")
    private boolean securityPreCheckFeedbackSwitch;

    @Value("${login.pless.supportAllFido:false}")
    private boolean loginPlessSupportAllFido;

    @Value("${anti.bot.reject.msg.write.switch:false}")
    private boolean rejectMsgWriteSwitch;

    @Value("${anti.bot.sesion.salt.local.cache.enabled:false}")
    private boolean saltLocalCacheEnabled;

    @Value("${anti.bot.breaker.switch.enable:false}")
    private boolean breakerSwitchEnable;

    @Autowired
    private UserSecurityApi userSecurityApi;
    @Autowired
    private AntiBotApi antiBotApi;
    @Autowired
    private BaseHelper baseHelper;
    @Autowired
    private SecurityServiceApi securityServiceApi;
    @Resource
    private SecurityServiceApiClient securityServiceApiClient;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    private AccountOauthApi accountOauthApi;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;
    @Autowired
    private CaptchaHealthHelper captchaHealthHelper;
    @Autowired
    private User2FaApi user2FaApi;
    @Autowired
    private UserDeviceHelper userDeviceHelper;
    @Autowired
    private SecurityAppAttestService securityAppAttestService;
    @Autowired
    private MetricsEventPublisher metricsEventPublisher;

    private RedisTemplate<String, Object> accountDdosRedisTemplate = AccountDdosRedisHelper.getInstance();

    // 防止传过来的大小写不一致
    private static final Map<String, String> CAPTCHA_MAP = ImmutableMap.of("gt", "gt", "bcaptcha", "bCAPTCHA", "recaptcha", "reCAPTCHA", "bcaptcha2", "bCAPTCHA2");
    private static final String THIRD_LOGIN_GOOGLE_ISS = "https://accounts.google.com";
    private static final String THIRD_LOGIN_APPLE_ISS = "https://appleid.apple.com";
    private static final ThreadLocal<String> localSessionId = new ThreadLocal<>();

    private static final ExecutorService executorService = new ThreadPoolExecutor(5, 5, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

    private final LoadingCache<String, String> saltLocalCache =
            CacheBuilder.newBuilder()
                    .maximumSize(SALT_LOCAL_CACHE_CAPACITY)
                    .expireAfterWrite(SALT_LOCAL_CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS)
                    .build(new CacheLoader<String, String>() {
                        @Override
                        public String load(String key) {
                            return ShardingRedisCacheUtils.get(key);
                        }
                    });

    private final Supplier<Boolean> breakerSwitchCache = Suppliers.memoizeWithExpiration(() -> {
        Integer result = ShardingRedisCacheUtils.get(com.binance.accountanalyze.vo.Constant.ACCOUNT_DDOS_SECURITY_CHECK_SWITCH,Integer.class);
        log.debug("breakerSwitchCache query from redis");
        return Objects.equals(1, result);
    }, 5, TimeUnit.SECONDS);


    /**
     * @param request    HttpServletRequest
     * @param bizId      登陆：login，注册：register，找回密码：forget_password, 重置2fa：reset2fa
     * @param userId     用户存在必须，不存在为空。
     * @param email      页面用户输入参数email，没有则不填写
     * @param mobile     页面用户输入的mobile，没有则不填写
     * @param mobileCode 页面用户输入的mobile country code，没有则不填写
     */
    public boolean timeoutCheckPassAntiBot(HttpServletRequest request, String bizId, Long userId, String email, String mobile, String mobileCode) {
        return this.timeoutCheckPassAntiBot(request, bizId, userId, email, mobile, mobileCode, null);
    }


    public boolean timeoutCheckPassAntiBot(HttpServletRequest request, String bizId, Long userId, String email, String mobile, String mobileCode, Map<String, String> relatedInfo) {
        if (!isAntiBotCheck) {
            return true;
        }

        try {
            AntiBotRequest antibotRequest = AntiBotApiUtil.composeAntiBotRequest(request, bizId, String.valueOf(userId), email, mobile, mobileCode);
            String envFlag = EnvUtil.getEnvFlag();
            String traceId = TrackingUtils.getTrace();

            Map<String, String> relatedInfoFromAntiBotRequest = antibotRequest.getRelatedInfo();
            if (relatedInfoFromAntiBotRequest == null) {
                relatedInfoFromAntiBotRequest = new HashMap<>();
            }
            relatedInfoFromAntiBotRequest.put("call", "validate");
            if (relatedInfo != null) {
                relatedInfoFromAntiBotRequest.putAll(relatedInfo);
            }
            antibotRequest.setRelatedInfo(relatedInfoFromAntiBotRequest);

            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                    TrackingUtils.saveTrace(traceId);
                    APIResponse<AntiBotResponse> antiBotApiResponse = antiBotApi.validate(APIRequest.instance(antibotRequest));
                    AntiBotResponse response = antiBotApiResponse.getData();
                    if (response.getRiskLevel() == RiskLevel.REJECT) {
                        log.info("anti bot reject,bizId={}", bizId);
                        return false;
                    }
                    return true;
                }
            });
            return future.get(antiBotTimeOut, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("checkPassAntiBot timeout", e);
            return true;
        } catch (Exception e) {
            log.warn("anti bot exception", e);
        }
        return true;
    }

    /**
     * 人机验证安全预检
     */
    public SecurityPreCheckRet timeOutSecurityPreCheck(SecurityPreCheckArg arg) throws Exception {
        String bizId = arg.getBizType();
        String email = arg.getEmail();
        String subUserEmail = arg.getSubUserEmail();
        String mobile = arg.getMobile();
        String mobileCode = arg.getMobileCode();
        String refreshToken = arg.getRefreshToken();
        String thirdPartIdToken = arg.getIdToken();

        SecurityPreCheckRet response = new SecurityPreCheckRet();
        response.setReject(false);
        response.setNeedCheck(true);
        response.setCaptchaType(captchaHealthHelper.backUpCaptcha()); // 默认bCAPTCHA
        addValidationType(response, CAPTCHA_VALIDATION_TYPE);

        if (paramPreCheckSwitch) {
            // check params
            if (!BizType.isMatch(bizId)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
            if (StringUtils.isNotBlank(email) && !timeOutRegexUtils.validateEmail(email)) {
                throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
            }
            if (StringUtils.isNotBlank(subUserEmail) && !timeOutRegexUtils.validateEmailForSub(subUserEmail)) {
                throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
            }
            if (StringUtils.isNoneBlank(mobile, mobileCode) && !RegexUtils.validateOnlyChinaMobilePhone(mobileCode, mobile)) {
                throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
            }
        }

        if (emailWhitelist.contains(email)) {
            response.setNeedCheck(false);
            response.setCaptchaType("random");
            return response;
        }

        String userIdStr = null;
        if (queryByThirdId && bizId.equalsIgnoreCase(BizType.THIRD_LOGIN)) {
            try {
                Pair<String, String> pair = parseFromThirdIdToken(thirdPartIdToken);
                String thirdId = pair.getLeft();
                String channel = pair.getRight();
                if (StringUtils.isBlank(thirdId)) {
                    throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
                }
                userIdStr = getUserIdFromThirdBindingCache(thirdId, channel);
                log.info("securityPreCheck {} userId={}", bizId, StringUtils.isBlank(userIdStr) ? "empty" : userIdStr);
            } catch (Exception e) {
                log.error("securityPreCheck ", e);
            }
        }

        if (StringUtils.equalsAnyIgnoreCase(bizId, BizType.LOGIN, BizType.REGISTER, BizType.FORGET_PASSWORD)) {
            if (StringUtils.isBlank(email) && StringUtils.isAnyBlank(mobile, mobileCode)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
            if (StringUtils.isNotBlank(email) && !timeOutRegexUtils.validateEmail(email)) {
                throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
            }

            UserStatusCacheRet ret = getUserStatusCache(email, mobile, mobileCode, arg.getBizType());
            userIdStr = ret.getUserIdStr();
            log.info("securityPreCheck {} userId={}", bizId, StringUtils.isBlank(userIdStr) ? "empty" : userIdStr);
        }

        if (bizId.equalsIgnoreCase(BizType.REFRESH_ACCESS_TOKEN)) {
            Long userId = ShardingRedisCacheUtils.get(refreshToken, Long.class, CacheConstant.ACCOUNT_REFRESH_TOKEN_KEY);
            userIdStr = String.valueOf(userId);
            log.info("securityPreCheck refreshToken userId={}", userIdStr);
            if (skipCheckRefreshToken) {
                response.setNeedCheck(false);
                response.setCaptchaType("random");
                return response;
            }
        }

        fillAdditionalValidationDetails(arg.getAttestKeyId(), response);

        if (isAntiBotNewCaptchaCheck) {
            try {
                SecurityCheckRequest securityCheckRequest = SecurityServiceApiUtil.composeSecurityCheckRequest(WebUtils.getHttpServletRequest(), bizId, userIdStr, email, mobile, mobileCode);
                securityCheckRequest.setToken(arg.getSecCheckToken());
                //非必须
                Map<String, String> relatedInfo = securityCheckRequest.getRelatedInfo();
                relatedInfo.put("captchaStatus", String.valueOf(arg.getCs()));
                securityCheckRequest.setRelatedInfo(relatedInfo);
                String envFlag = EnvUtil.getEnvFlag();
                ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                String traceId = TrackingUtils.getTrace();
                Future<SecurityCheckResponse> future = executorService.submit(() -> {
                    TrackingUtils.saveTrace(traceId);
                    RequestContextHolder.setRequestAttributes(sra, true);
                    RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                    log.info("anti bot securityCheck start");
                    SecurityCheckResponse securityCheckResponse = securityServiceApiClient.securityCheck(securityCheckRequest);
                    log.info("anti bot securityCheck securityCheckResponse={}", JsonUtils.toJsonNotNullKey(securityCheckResponse));
                    TrackingUtils.clearTrace();
                    return securityCheckResponse;
                });
                SecurityCheckResponse securityCheckResponse = future.get(securityCheckTimeOut, TimeUnit.MILLISECONDS);
                wrapSecurityPreCheckRet(response, securityCheckResponse);
            } catch (TimeoutException e) {
                log.warn("securityPreCheck timeout", e);
                response.setCaptchaType(captchaHealthHelper.backUpCaptcha()); //安全接口报错，其他验证兜底
            } catch (Exception e) {
                log.warn("securityPreCheck exception", e);
                response.setCaptchaType(captchaHealthHelper.backUpCaptcha()); //安全接口报错，其他验证兜底
            }
        }

        if (bizId.equalsIgnoreCase(BizType.LOGIN) && !response.isReject()) {
            if (StringUtils.isNotBlank(userIdStr) && !"null".equals(userIdStr) && enablePasswordlessLogin) {
                // 生成loginFlowId
                String loginFlowId = Md5Tools.MD5(UUID.randomUUID().toString());
                ShardingRedisCacheUtils.set(loginFlowId, Long.valueOf(userIdStr), TimeUnit.HOURS.toSeconds(1), CacheConstant.ACCOUNT_MGS_LOGIN_FLOWID_CACHE_KEY);
                response.setLoginFlowId(loginFlowId);
                
                User2FaStatusReq user2FaStatusReq = new User2FaStatusReq();
                user2FaStatusReq.setUserId(Long.valueOf(userIdStr));
                String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
                user2FaStatusReq.setClientId(clientId);
                APIResponse<User2FaStatusResp> apiResponse = user2FaApi.user2FaStatus(getInstance(user2FaStatusReq));
                if (baseHelper.isOk(apiResponse)) {
                    User2FaStatusResp user2FaStatusResp = apiResponse.getData();
                    if(null != user2FaStatusResp) {
                        boolean pless = false;
                        List<AccountVerificationTwoEnum> plessVerifyTypeList = new ArrayList<>(10);
                        if (user2FaStatusResp.getIsCurrentDeviceBindFido()){
                            pless = true;
                            plessVerifyTypeList.add(AccountVerificationTwoEnum.FIDO);

                        }
                        if(loginPlessSupportAllFido && BooleanUtils.isTrue(user2FaStatusResp.getIsBindExternalFido())) {
                            pless = true;
                            plessVerifyTypeList.add(AccountVerificationTwoEnum.FIDO_EXTERNAL);
                        }

                        if(loginPlessSupportAllFido && BooleanUtils.isTrue(user2FaStatusResp.getIsBindPasskeys())) {
                            pless = true;
                            plessVerifyTypeList.add(AccountVerificationTwoEnum.PASSKEYS);
                        }
                        if (pless) {
                            response.setNeedCheck(false);
                            response.setCaptchaType("random");
                            response.setIsPless(true);
                            response.setPlessVerifyTypeList(plessVerifyTypeList);
                            return response;
                        }
                    }
                }
   
            } else {
                // 生成假loginFlowId
                String loginFlowId = Md5Tools.MD5(UUID.randomUUID().toString());
                response.setLoginFlowId(loginFlowId);    
            }
        }

        return response;
    }

    /**
     * @return Pair<thirdId, channel>
     */
    private Pair<String, String> parseFromThirdIdToken(String thirdPartIdToken) {
        String[] chunks = thirdPartIdToken.split("\\.");
//        String header = Base64Util.decode(chunks[0]);
        String payload = Base64Util.decode(chunks[1]);
        JSONObject jsonObject = JSON.parseObject(payload);
        String iss = jsonObject.getString("iss");
        String thirdId = jsonObject.getString("sub");
        String channel = StringUtils.equals(THIRD_LOGIN_GOOGLE_ISS, iss) ? ThirdRegisterEnum.GOOGLE.name() :
                StringUtils.equals(THIRD_LOGIN_APPLE_ISS, iss) ? ThirdRegisterEnum.APPLE.name() : StringUtils.EMPTY;
        return Pair.of(thirdId, channel.toLowerCase());
    }

    private void wrapSecurityPreCheckRet(SecurityPreCheckRet response, SecurityCheckResponse securityCheckResponse) {
        if (breakerSwitchEnable && breakerSwitchCache.get()) {
            log.warn("security check not reliable");
            response.setCaptchaType(captchaHealthHelper.backUpCaptcha());
            return;
        }

        if (securityCheckResponse.getRiskLevel() == RiskLevel.REJECT) {
            response.setReject(true);
            if (rejectMsgWriteSwitch) {
                response.setRejectMsg(securityCheckResponse.getToastMessage());
            }
        } else if (securityCheckResponse.getRiskLevel() == RiskLevel.SUSPECT || securityCheckResponse.getRiskLevel() == RiskLevel.STRONG_SUSPECT) {
            String detail = securityCheckResponse.getDetail();
            String validateId = securityCheckResponse.getValidateId();//标示一次验证
            JSONObject jsonObject = (JSONObject) JSON.parse(detail);
            String captchaType = (String) jsonObject.get("captchaType");//验证类型
            log.info("anti bot securityCheck validateId={},captchaType={}", validateId, captchaType);
            response.setValidateId(validateId);
            String localCaptchaType = CAPTCHA_MAP.get(captchaType);
            if (StringUtils.isBlank(localCaptchaType)) {
                localCaptchaType = captchaHealthHelper.backUpCaptcha();
            }
            response.setCaptchaType(captchaHealthHelper.getCaptcha(localCaptchaType));
        } else if (securityCheckResponse.getRiskLevel() == RiskLevel.PASS) {
            response.setCaptchaType("random");
            response.setNeedCheck(false);
            response.setValidateId(securityCheckResponse.getValidateId());
        }
    }

    public void securityPreCheckFeedback(CaptchaValidateInfo validateInfo, boolean result) {
        if (!securityPreCheckFeedbackSwitch) {
            return;
        }
        try {
            String traceId = TrackingUtils.getTrace();
            String envFlag = EnvUtil.getEnvFlag();
            executorService.execute(() -> {
                TrackingUtils.saveTrace(traceId);
                RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                SecurityCheckFeedbackRequest securityCheckFeedbackRequest = new SecurityCheckFeedbackRequest();
                securityCheckFeedbackRequest.setBizId(validateInfo.getBizType());
                securityCheckFeedbackRequest.setSecurityCheckValidateId(validateInfo.getValidateId());
                Map<String, Object> param = Maps.newHashMap();
                param.put("email", validateInfo.getEmail());
                param.put("mobileCode", validateInfo.getMobileCode());
                param.put("mobile", validateInfo.getMobile());
                param.put("success", result);
                param.put("captchaType", validateInfo.getCaptchaType());
                securityCheckFeedbackRequest.setFeedback(param);
                securityServiceApi.sendSecurityCheckFeedback(APIRequest.instance(securityCheckFeedbackRequest));
            });
        } catch (Exception e) {
            log.error("securityPreCheckFeedback error", e);
        }
    }

    private String getUserIdFromThirdBindingCache(String thirdId, String channel) throws Exception {
        String identify = CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_BY_THIRD_KEY_PREFIX + ":" + thirdId;
        String userIdCache = ShardingRedisCacheUtils.get(identify);
        String envFlag = EnvUtil.getEnvFlag();
        String traceId = TrackingUtils.getTrace();
        if (StringUtils.isBlank(userIdCache)) {
            SelectByThirdIdRequest emailRequest = new SelectByThirdIdRequest();
            emailRequest.setThirdId(thirdId);
            emailRequest.setRegisterChannel(channel);
            Future<String> future = executorService.submit(() -> {
                TrackingUtils.saveTrace(traceId);
                RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                APIResponse<ThirdIdUserIdRelationResponse> apiResponse = accountOauthApi.selectThirdBindingByThirdId(APIRequest.instance(emailRequest));
                if (baseHelper.isOk(apiResponse)) {
                    ThirdIdUserIdRelationResponse response = apiResponse.getData();
                    return String.valueOf(response.getUserId());
                } else {
                    log.warn("selectThirdBindingByThirdId error response = {} ", this.logFilter(apiResponse));
                    return StringUtils.EMPTY;
                }
            });
            userIdCache = future.get(queryByThirdIdTimeout, TimeUnit.MILLISECONDS);
            ShardingRedisCacheUtils.set(identify, userIdCache, userIdCacheTimeOut);
        }
        return userIdCache;
    }

    public UserStatusCacheRet getUserStatusCache(String email, String mobile, String mobileCode, String bizType) throws Exception {
        String userIdStr = null;
        String identify = CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_KEY_PREFIX + ":" + (StringUtils.isNotBlank(email) ? email : mobileCode + "-" + mobile);
        String userIdCache = ShardingRedisCacheUtils.get(identify);
        Boolean disableLogin = null;
        if (userIdCache != null) {
            userIdStr = userIdCache;
            disableLogin = ShardingRedisCacheUtils.get(userIdStr, Boolean.class, CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_DISABLE_LOGIN_STATUS_PREFIX + ":");
        } else {
            GetUserIdByEmailOrMobileForPrecheckRequest getUserIdReq = new GetUserIdByEmailOrMobileForPrecheckRequest();
            try {
                getUserIdReq.setEmail(email);
                getUserIdReq.setMobileCode(mobileCode);
                getUserIdReq.setMobile(mobile);
                getUserIdReq.setBizType(bizType);
                APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmailForPrecheck(getInstance(getUserIdReq));
                if (!baseHelper.isOk(getUserIdResp)) {
                    if (StringUtils.equals(getUserIdResp.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
                        String ip = WebUtils.getRequestIp();
                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ddosLoginIpLimitCount, "securityPreCheck ban user not exist");
                    }
                    userIdStr = "";
                    ShardingRedisCacheUtils.set(identify, "", userIdCacheTimeOut);
                } else {
                    userIdStr = String.valueOf(getUserIdResp.getData().getUserId());
                    ShardingRedisCacheUtils.set(identify, userIdStr, userIdCacheTimeOut);
                }
            } catch (Exception e) {
                log.warn("securityPreCheck getUserIdReq request={}", getUserIdReq, e);
            }
        }
        if (Objects.isNull(disableLogin) && StringUtils.isNotBlank(userIdStr)) {
            UserIdRequest request = new UserIdRequest();
            request.setUserId(Long.valueOf(userIdStr));
            APIResponse<UserStatusEx> apiResponse = userApi.getUserStatusByUserId(APIRequest.instance(request));
            if (baseHelper.isOk(apiResponse)) {
                UserStatusEx userStatusEx = apiResponse.getData();
                disableLogin = userStatusEx.getIsUserDisabledLogin();
                ShardingRedisCacheUtils.set(userIdStr, disableLogin, userIdCacheTimeOut, CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_DISABLE_LOGIN_STATUS_PREFIX + ":");
            } else {
                log.warn("getUserStatusByUserId error userId={} {}", userIdStr, apiResponse.getErrorData());
            }
        }

        UserStatusCacheRet ret = new UserStatusCacheRet();
        ret.setUserIdStr(userIdStr);
        ret.setIsDisableLogin(disableLogin);
        return ret;
    }

    public boolean getNewCaptchaValidateSwitch() {
        return captchaValidateSwitch && checkVersion();
    }

    private boolean checkVersion() {
        TerminalEnum terminal = WebUtils.getTerminal();
        if (terminal == null) {
            log.warn("Terminal is null, requestIp={}", WebUtils.getRequestIp());
            ddosCacheSeviceHelper.banIp(WebUtils.getRequestIp());
            if (headerPreCheckSwitch) {
                throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
            } else {
                return true;
            }
        }

        String currentVersion = VersionUtil.getVersion(terminal);
        log.info("Current terminal is {},currentVersion={}", terminal.getCode(), currentVersion);
        switch (terminal) {
            case ANDROID:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higher(currentVersion, newValidateAndroidVersion);
            case IOS:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higher(currentVersion, newValidateIosVersion);
            case WEB:
                return true;
            case ELECTRON:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.higher(currentVersion, newValidateElectronVersion);
            default:
                return false;
        }
    }

    public static void setThreadLocalSessionId(String sessionId) {
        if (sessionId == null) {
            sessionId = "";
        }
        localSessionId.set(sessionId);
    }

    public static String getThreadLocalSessionId() {
        return localSessionId.get();
    }

    public static String getAndClearThreadLocalSessionId() {
        String sessionId = localSessionId.get();
        localSessionId.set(null);
        return sessionId;
    }

    public String createSecuritySessionId(String captchaType, SecurityPreCheckArg arg, SecurityPreCheckRet checkRet) {
        boolean reject = checkRet.isReject();
        boolean needCheck = checkRet.isNeedCheck();
        String validateId = checkRet.getValidateId();
        String rejectMsg = checkRet.getRejectMsg();

        // 保证 sessionId 唯一性
        String code;
        String fvideoid = WebUtils.getHeader(LocalLogKeys.FVIDEO_ID);
        if (StringUtils.isBlank(fvideoid)) {
            fvideoid = WebUtils.getHeader(LocalLogKeys.FVIDEO_ID_APP);
        }
        code = WebUtils.getRequestIp() + fvideoid + Strings.nullToEmpty(baseHelper.getUserIdStr());
        arg.setCode(code);
        String sessionId;
        sessionId = Md5Tools.MD5(Base64Util.encode(getSaltV2())).substring(0, 8).concat(Md5Tools.MD5(arg.toString()));

        CaptchaValidateInfo validateInfo = new CaptchaValidateInfo();
        validateInfo.setCaptchaType(captchaType);
        validateInfo.setBizType(arg.getBizType());
        validateInfo.setEmail(arg.getEmail());
        validateInfo.setMobile(arg.getMobile());
        validateInfo.setMobileCode(arg.getMobileCode());
        validateInfo.setSubUserEmail(arg.getSubUserEmail());
        validateInfo.setRefreshToken(arg.getRefreshToken());
        validateInfo.setIdToken(arg.getIdToken());
        validateInfo.setUserId(baseHelper.getUserIdStr());
        validateInfo.setValidateId(validateId);

        if (reject) {
            validateInfo.setStatus(AntiBotHelper.getVALIDATE_STATUS_REJECT());
            validateInfo.setRejectMsg(rejectMsg);
        } else {
            if (needCheck) {
                validateInfo.setStatus(AntiBotHelper.getVALIDATE_STATUS_NEED_CHECK());
            } else {
                validateInfo.setStatus(AntiBotHelper.getVALIDATE_STATUS_PASS());
            }
        }

        ddosCacheSeviceHelper.setValidateInfo(sessionId, validateInfo, sessionTimeOutSeconds);

        // 创建新的 sessionId，之前的计数清0
        if (ddosShardingRedisMigrateSwitch) {
            ShardingRedisCacheUtils.del(CacheConstant.ACCOUNT_DDOS_CAPTCHA_TOKEN_COUNT_PREFIX + ":" + sessionId);
        } else {
            accountDdosRedisTemplate.delete(DdosCacheSeviceHelper.getGT_FORBIDDEN_CACHE_PREFIX() + sessionId);
            ShardingRedisCacheUtils.del(CacheConstant.ACCOUNT_DDOS_CAPTCHA_TOKEN_COUNT_PREFIX + ":" + sessionId);
        }
        metricsEventPublisher.publish(CaptchaCounterMetrics.builder().bizType(arg.getBizType()).captchaType(reject ? "reject" : captchaType).captchaStatus("init").build());
        return sessionId;
    }

    public boolean checkSessionId(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return false;
        }

        return sessionId.startsWith(Md5Tools.MD5(Base64Util.encode(getSaltV2())).substring(0, 8))
                || sessionId.startsWith(Md5Tools.MD5(Base64Util.encode(getLastSaltV2())).substring(0, 8));
    }

    public String getSaltV2() {
        String salt = loadSalt(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_KEY);
        if (salt == null) {
            salt = RandomStringUtils.getRandomString(5);
            try {
                boolean success = ShardingRedisCacheUtils.setNX(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_LOCK_KEY, salt, sessionSaltLockTimeOutSeconds);
                if(success){
                    ShardingRedisCacheUtils.set(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_KEY, salt, sessionSaltTimeOutSeconds);
                }
                if (BooleanUtils.isNotTrue(success)) {
                    // we expect that another thread (or another server) has already generated a new salt (if not, we take the previous one)
                    String latestValue =  loadSalt(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_KEY);
                    if (StringUtils.isNotBlank(latestValue)) {
                        return latestValue;
                    }
                    String currentValue = getCurrentSaltV2();
                    if (StringUtils.isNotBlank(currentValue)) {
                        return currentValue;
                    }
                    // we try load from Redis the latest salt as the last resort
                    return loadSalt(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_KEY);
                } else {
                    String newSalt = salt;

                    String[] valueArr = null;
                    String oldSaltValue = getOldSaltValue();
                    if (StringUtils.isNotBlank(oldSaltValue) && StringUtils.contains(oldSaltValue, ":")) {
                        valueArr = oldSaltValue.split(":");
                    }

                    String v = generateLastSaltValue(newSalt, valueArr);
                    log.info("new salt value={}", v);
                    ShardingRedisCacheUtils.set(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_OLD_SALT_KEY, v, 3600);
                }
            } finally {
                ShardingRedisCacheUtils.del(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_LOCK_KEY);
            }
        }
        return salt;
    }

    // 格式为 oldSalt:newSalt
    private String generateLastSaltValue(String newSalt, String[] value) {
        if (value == null) {
            return newSalt.concat(":").concat(newSalt);
        }
        String v1;
        String v2 = value[1];

        v1 = newSalt;
        return v2.concat(":").concat(v1);
    }

    public String getLastSaltV2() {
        String value = getOldSaltValue();
        if (StringUtils.isNotBlank(value)) {
            return getOldSaltValue().split(":")[0];
        }
        return "";
    }

    private String getCurrentSaltV2() {
        String value = getOldSaltValue();
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }
        String[] tokens = value.split(":");
        if (tokens == null || tokens.length < 2) {
            return StringUtils.EMPTY;
        }
        return tokens[1];
    }

    private String getOldSaltValue() {
        String lastSalt = ShardingRedisCacheUtils.get(CacheConstant.ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_OLD_SALT_KEY);
        if (lastSalt != null) {
            return lastSalt;
        }
        return "";
    }

    /**
     * Loads salt value from Redis trying to fetch initial value from local cache.
     * Should be used only for hot keys (we should keep cache capacity small)
     */
    private String loadSalt(String key) {
        if (!saltLocalCacheEnabled) {
            return ShardingRedisCacheUtils.get(key);
        }

        final String localCacheSalt = loadSaltLocalCache(key);
        log.debug("Loaded salt for key [{}] from local cache [{}]", key, localCacheSalt);
        if (StringUtils.isNotBlank(localCacheSalt)) {
            return localCacheSalt;
        }
        return ShardingRedisCacheUtils.get(key);
    }

    private String loadSaltLocalCache(String key) {
        try {
            return saltLocalCache.get(key);
        } catch (ExecutionException | RuntimeException e) {
            log.error("Error while loading salt for key [{}]", key, e);
        }
        return null;
    }

    /**
     * Adds AppAttest details (validationTypes + challenge)
     */
    private void fillAdditionalValidationDetails(String attestKeyId ,SecurityPreCheckRet ret) {
        try {
            final AppAttestPrecheckRet response = securityAppAttestService.securityPreCheck(attestKeyId);
            if (response == null || StringUtils.isBlank(response.getValidationType()) || StringUtils.isBlank(response.getChallenge())) {
                return;
            }
            addValidationType(ret, response.getValidationType());
            ret.setChallenge(response.getChallenge());
        } catch (RuntimeException e) {
            log.error("Error while filling additional validation details for input params [{}]", attestKeyId, e);
        }
    }

    private static void addValidationType(SecurityPreCheckRet response, String validationType) {
        if (response.getValidationTypes() == null) {
            response.setValidationTypes(new ArrayList<>(VALIDATION_TYPES_CAPACITY));
        }
        response.getValidationTypes().add(validationType);
    }
}
