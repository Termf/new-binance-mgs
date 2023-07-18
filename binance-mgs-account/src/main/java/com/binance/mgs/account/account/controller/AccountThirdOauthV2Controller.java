package com.binance.mgs.account.account.controller;


import com.binance.account.vo.user.enums.RegisterationMethodEnum;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.RegisterCounterMetrics;
import com.binance.accountoauth.api.AccountOauthApi;
import com.binance.accountoauth.enums.ThirdLoginResultEnum;
import com.binance.accountoauth.enums.ThirdOperatorEnum;
import com.binance.accountoauth.enums.ThreePartyBindingEnum;
import com.binance.accountoauth.vo.oauth.request.DoBindThreePartyRequest;
import com.binance.accountoauth.vo.oauth.request.DoOauthLoginRequest;
import com.binance.accountoauth.vo.oauth.request.RegisterByThirdRequest;
import com.binance.accountoauth.vo.oauth.request.RegisterConfigRequest;
import com.binance.accountoauth.vo.oauth.request.SelectUserBindThreePartyRequest;
import com.binance.accountoauth.vo.oauth.response.DoOauthLoginResponse;
import com.binance.accountoauth.vo.oauth.response.RegisterByThirdResponse;
import com.binance.accountoauth.vo.oauth.response.SelectUserBindThreePartyResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.Md5Tools;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.dto.RiskLoginInfoDto;
import com.binance.mgs.account.account.helper.AccountCountryHelper;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.AccountMgsRedisHelper;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.helper.RiskKafkaHelper;
import com.binance.mgs.account.account.helper.VersionHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.helper.GeeTestHelper;
import com.binance.mgs.account.authcenter.helper.SensorHelper;
import com.binance.mgs.account.authcenter.vo.DoAppleLoginRet;
import com.binance.mgs.account.authcenter.vo.DoAppleLoginUrlArg;
import com.binance.mgs.account.authcenter.vo.DoBindUnbindThirdPartyArg;
import com.binance.mgs.account.authcenter.vo.DoBindUnbindThreePartyArg;
import com.binance.mgs.account.authcenter.vo.DoGoogleLoginRet;
import com.binance.mgs.account.authcenter.vo.DoGoogleLoginUrlArg;
import com.binance.mgs.account.authcenter.vo.RegisterByThirdRegisterArg;
import com.binance.mgs.account.authcenter.vo.RegisterByThirdRet;
import com.binance.mgs.account.authcenter.vo.SelectBindingThreePartyRelationsRet;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.kafka.CrmKafkaSupport;
import com.binance.mgs.account.kafka.GrowthKafkaSupport;
import com.binance.mgs.account.service.UserComplianceService;
import com.binance.mgs.account.util.GrowthRegisterAgentCodePrefixUtil;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.account.util.MaskUtils;
import com.binance.mgs.account.util.VersionUtil;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.DomainUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Created by Shining.Cai on 2018/11/01.
 **/
@RestController
@Slf4j
public class AccountThirdOauthV2Controller extends AccountBaseAction {



    @Autowired
    private AccountOauthApi accountOauthApi;
    @Value("${google.oauth.login.switch:true}")
    private boolean googleOauthLoginSwitch;
    @Value("${apple.oauth.login.switch:true}")
    private boolean appleOauthLoginSwitch;
    @Value("${third.oauth.register.switch:true}")
    private boolean thirdOauthRegisterSwitch;
    @Value("${google.oauth.bindunbind.three.party.switch:true}")
    private boolean googleOauthBindUnbindThreePartySwitch;
    @Value("${thirdlogin.registercountry.enable.android.version:2.33.1}")
    private String thirdLoginRegisterCountryEnableAndroidVersion;
    @Value("${thirdlogin.registercountry.enable.ios.version:2.33.3}")
    private String thirdLoginRegisterCountryEnableIosVersion;
    @Value("${thirdlogin.registercountry.enable.electron.version:1.21.2}")
    private String thirdLoginRegisterCountryEnableElectronVersion;
    @Value("#{'${limit.china.domains:}'.split(',')}")
    private Set<String> limitChinaDomains;
    @Value("#{'${growth.register.agentcode.prefix:LIMIT_}'.split(',')}")
    private Set<String> growthRegisterAgentCodePrefix;
    @Value("${growth.register.agentcode.switch:true}")
    private Boolean growthRegisterAgentCodeSwitch;
    @Value("${growth.channel.agentcode.switch:true}")
    private Boolean growthChannelAgentCodeSwitch;
    //同account-oauth的jwttoken过期时间
    @Value("${register.token.cookie.maxage:605}")
    private Integer registerTokenCookieMaxAge;
    @Value("#{'${kyc.ignore.register.sources:xxx}'.split(',')}")
    private Set<String> kycIgnoreRegisterSources;

    @Autowired
    private VersionHelper versionHelper;

    @Resource
    private AuthHelper authHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Resource
    private SensorHelper sensorHelper;
    @Resource
    private RiskKafkaHelper riskKafkaHelper;
    @Resource
    private SysConfigHelper sysConfigHelper;
    @Resource
    private RiskHelper riskHelper;
    @Resource
    private GeeTestHelper geeTestHelper;
    @Resource
    private GrowthKafkaSupport growthKafkaSupport;
    @Resource
    private CrmKafkaSupport crmKafkaSupport;
    @Resource
    private AccountHelper accountHelper;
    @Resource
    private AccountCountryHelper accountCountryHelper;
    @Autowired
    private UserComplianceService userComplianceService;
    @Autowired
    private MetricsEventPublisher eventPublisher;
    private RedisTemplate<String, Object> accountMgsRedisTemplate=AccountMgsRedisHelper.getInstance();

    @Value("${sharding.redis.migrate.third.oauth.read.switch:false}")
    private Boolean shardingRedisMigrateThirdOauthReadSwitch;
    @Value("${sharding.redis.migrate.third.oauth.write.switch:false}")
    private Boolean shardingRedisMigrateThirdOauthWriteSwitch;



    @PostMapping("/v2/transient/account/oauth/sign/google")
    @AccountDefenseResource(name="AccountThirdOauthController.signWithGoogle")
    @AntiBotCaptchaValidate(bizType = {BizType.THIRD_LOGIN})
    @DDoSPreMonitor(action = "signWithGoogle")
    @UserOperation(eventName = "login", name = "用户登陆", logDeviceOperation = true, deviceOperationNoteField = {"userId"}, requestKeys = {"email"},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {})
    public CommonRet<DoGoogleLoginRet> signWithGoogle(HttpServletRequest request, HttpServletResponse response, @RequestBody @Validated DoGoogleLoginUrlArg arg)throws Exception {
        if (!googleOauthLoginSwitch){
            log.info("AccountThirdOauthController.doBackendGoogleLogin.switch is false");
            throw new BusinessException(AccountMgsErrorCode.GOOGLE_OAUTH_LOGIN_IS_CLOSE);
        }

        DoOauthLoginRequest doBackendGoogleLoginRequest = new DoOauthLoginRequest();
        doBackendGoogleLoginRequest.setIdToken(arg.getIdToken());
        doBackendGoogleLoginRequest.setClientType(BaseHelper.getClientType(request));
        doBackendGoogleLoginRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, null));
        doBackendGoogleLoginRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        doBackendGoogleLoginRequest.setThirdOperatorEnum(arg.getThirdOperatorEnum());
        if (doBackendGoogleLoginRequest.getDeviceInfo() != null) {
            UserOperationHelper.log("device-info", doBackendGoogleLoginRequest.getDeviceInfo());
            String bncUuid = doBackendGoogleLoginRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }

        UserOperationHelper.log("loginType", "google");
        String md5IdToken = Md5Tools.MD5(arg.getIdToken());
        Object value;
        if (shardingRedisMigrateThirdOauthReadSwitch) {
            value = ShardingRedisCacheUtils.get(md5IdToken, String.class, CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        } else {
            value = accountMgsRedisTemplate.opsForValue().get(md5IdToken);
        }
        if (value != null){
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
        APIResponse<DoOauthLoginResponse> apiResponse = accountOauthApi.doOauthLogin(baseHelper.getInstance(doBackendGoogleLoginRequest));
        checkResponse(apiResponse);
        if (shardingRedisMigrateThirdOauthWriteSwitch) {
            ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        } else {
            accountMgsRedisTemplate.opsForValue().set(md5IdToken, md5IdToken, 2, TimeUnit.HOURS);
            ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        }
        DoGoogleLoginRet data = new DoGoogleLoginRet();
        if (ThirdLoginResultEnum.NEED_REGISTER.name().equalsIgnoreCase(apiResponse.getData().getResult()) || ThirdLoginResultEnum.NEED_BINDING.name().equalsIgnoreCase(apiResponse.getData().getResult())){
            data.setResult(apiResponse.getData().getResult());
            data.setRegisterToken(apiResponse.getData().getRegisterToken());
            data.setEmail(apiResponse.getData().getEmail());
            if (baseHelper.isFromWeb()) {
                authHelper.setRegisterTokenCookie(request,response,apiResponse.getData().getRegisterToken(),registerTokenCookieMaxAge);
                data.setRegisterToken(null);
            }else{
                data.setRegisterToken(apiResponse.getData().getRegisterToken());
            }
            return new CommonRet<>(data);
        }

        userComplianceService.complianceBlockLoginWithTimeout(apiResponse.getData().getUserId());

        data.setUserId(String.valueOf(apiResponse.getData().getUserId()));
        BeanUtils.copyProperties(apiResponse.getData(),data);
        if (baseHelper.isFromWeb()) {
            // 网页版设置cookie,半登录态还是顶级域名，兼容yubikey
//            BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, apiResponse.getData().getSerialNo());
            String code = authHelper.setAuthCookie(request,response,apiResponse.getData().getToken(),apiResponse.getData().getCsrfToken());
            data.setCode(code);
            // 根据安全要求，web端不返回token
            data.setToken(null);
            data.setCsrfToken(null);
            data.setRegisterToken(null);
        }
        //成功 //失败可能无法返回 todo
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, data.getUserId()));
        // 登陆成功
        UserOperationHelper.log(ImmutableMap.of("login", Boolean.TRUE.toString()));
        // 数据分析
        sensorHelper.profileSet(Long.valueOf(data.getUserId()));
        // 记录登陆日志
        authHelper.loginWithIpNew(data.getUserId());
        // 发送消息到人机识别系统
        authHelper.sendHumanRecognitionTopic(data.getUserId(), baseHelper.getCookieValue(Constant.COOKIE_CLIENT_ID));

        //发送登录信息给risk
        RiskLoginInfoDto riskLoginInfoDto = new RiskLoginInfoDto();
        riskLoginInfoDto.setMobile(null);
        riskLoginInfoDto.setMobileCode(null);
        riskLoginInfoDto.setEmail(data.getEmail());
        riskLoginInfoDto.setIp(WebUtils.getRequestIp());
        riskLoginInfoDto.setSource("GOOGLE");//todo 通知risk
        riskLoginInfoDto.setUserId(Long.valueOf(data.getUserId()));
        riskKafkaHelper.sendLoginInfoToRiskByDto(riskLoginInfoDto);

        data.setBncLocation(userComplianceService.getBncLocationWithTimeout(apiResponse.getData().getUserId()));
        return new CommonRet<>(data);
    }

    @PostMapping("/v2/private/account/account-connections/{action}")
    @AccountDefenseResource(name="AccountThirdOauthController.doBindUnbindThirdParty")
    @UserOperation(eventName = "threepartybindunbind", name = "绑定解绑三方登陆", logDeviceOperation = true, deviceOperationNoteField = {"userId"}, requestKeys = {"email"},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {})
    public CommonRet<Boolean> doBindUnbindThirdParty(HttpServletRequest request, HttpServletResponse response, @RequestBody @Validated DoBindUnbindThirdPartyArg arg,@PathVariable("action") String action)throws Exception {
        if (!googleOauthBindUnbindThreePartySwitch){
            log.info("AccountThirdOauthController.doBindUnbindThreeParty.switch is false");
            throw new BusinessException(AccountMgsErrorCode.THREE_PARTY_BINDUNBIND_IS_CLOSE);
        }
        if (StringUtils.isBlank(action)){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        ThreePartyBindingEnum threePartyBindingEnum = ThreePartyBindingEnum.valueOf(action);
        String md5IdToken = null;
        if (StringUtils.isNotBlank(arg.getIdToken())){
            md5IdToken = Md5Tools.MD5(arg.getIdToken());
            Object value;
            if (shardingRedisMigrateThirdOauthReadSwitch) {
                value = ShardingRedisCacheUtils.get(md5IdToken, String.class, CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
            } else {
                value = accountMgsRedisTemplate.opsForValue().get(md5IdToken);
            }
            if (value != null){
                throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
            }
        }
        DoBindThreePartyRequest doBindThreePartyRequest = new DoBindThreePartyRequest();
        doBindThreePartyRequest.setUserId(baseHelper.getUserId());
        doBindThreePartyRequest.setThreePartyBindingEnum(threePartyBindingEnum);
        doBindThreePartyRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        doBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, getUserIdStr(), null));
        doBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
        doBindThreePartyRequest.setCode(arg.getCode());
        doBindThreePartyRequest.setIdToken(arg.getIdToken());
        doBindThreePartyRequest.setRedirectUrl(arg.getRedirectUrl());
        APIResponse<Boolean> apiResponse = accountOauthApi.dobindOrUnbindThreeParty(baseHelper.getInstance(doBindThreePartyRequest));
        checkResponse(apiResponse);
        if (StringUtils.isNotBlank(arg.getIdToken())){
            if (shardingRedisMigrateThirdOauthWriteSwitch) {
                ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
            } else {
                accountMgsRedisTemplate.opsForValue().set(md5IdToken, md5IdToken, 2, TimeUnit.HOURS);
                ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
            }
        }
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping("/v2/private/account/oauth/bindunbind/threeparty")
    @AccountDefenseResource(name="AccountThirdOauthController.doBindUnbindThreeParty")
    @UserOperation(eventName = "threepartybindunbind", name = "绑定解绑三方登陆", logDeviceOperation = true, deviceOperationNoteField = {"userId"}, requestKeys = {"email"},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {})
    public CommonRet<Boolean> doBindUnbindThreeParty(HttpServletRequest request, HttpServletResponse response, @RequestBody @Validated DoBindUnbindThreePartyArg arg)throws Exception {
        if (!googleOauthBindUnbindThreePartySwitch){
            log.info("AccountThirdOauthController.doBindUnbindThreeParty.switch is false");
            throw new BusinessException(AccountMgsErrorCode.THREE_PARTY_BINDUNBIND_IS_CLOSE);
        }
        String md5IdToken = null;
        if (StringUtils.isNotBlank(arg.getIdToken())){
            md5IdToken = Md5Tools.MD5(arg.getIdToken());
            Object value;
            if (shardingRedisMigrateThirdOauthReadSwitch) {
                value = ShardingRedisCacheUtils.get(md5IdToken, String.class, CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
            } else {
                value = accountMgsRedisTemplate.opsForValue().get(md5IdToken);
            }
            if (value != null){
                throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
            }
        }

        DoBindThreePartyRequest doBindThreePartyRequest = new DoBindThreePartyRequest();
        doBindThreePartyRequest.setUserId(baseHelper.getUserId());
        doBindThreePartyRequest.setThreePartyBindingEnum(arg.getThreePartyBindingEnum());
        doBindThreePartyRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        doBindThreePartyRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, getUserIdStr(), null));
        doBindThreePartyRequest.setClientType(BaseHelper.getClientType(request));
        doBindThreePartyRequest.setCode(arg.getCode());
        doBindThreePartyRequest.setIdToken(arg.getIdToken());
        doBindThreePartyRequest.setRedirectUrl(arg.getRedirectUrl());
        APIResponse<Boolean> apiResponse = accountOauthApi.dobindOrUnbindThreeParty(baseHelper.getInstance(doBindThreePartyRequest));
        checkResponse(apiResponse);
        if (StringUtils.isNotBlank(arg.getIdToken())){
            if (shardingRedisMigrateThirdOauthWriteSwitch) {
                ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
            } else {
                accountMgsRedisTemplate.opsForValue().set(md5IdToken, md5IdToken, 2, TimeUnit.HOURS);
                ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
            }
        }
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping("/v2/private/account/oauth/threeparty/binding/relation/query")
    public CommonRet<List<SelectBindingThreePartyRelationsRet>> selectBindingThreePartyRelations(HttpServletRequest request, HttpServletResponse response)throws Exception {
        SelectUserBindThreePartyRequest selectUserBindThreePartyRequest = new SelectUserBindThreePartyRequest();
        selectUserBindThreePartyRequest.setUserId(baseHelper.getUserId());
        APIResponse<List<SelectUserBindThreePartyResponse>> relationResponse = accountOauthApi.selectUserBindThreeParty(baseHelper.getInstance(selectUserBindThreePartyRequest));
        checkResponse(relationResponse);
        if (CollectionUtils.isEmpty(relationResponse.getData()) || relationResponse.getData().size() == 0){
            return new CommonRet<>(Lists.newArrayList());
        }
        List<SelectBindingThreePartyRelationsRet> resList = new ArrayList<>(relationResponse.getData().size());
        for (SelectUserBindThreePartyResponse bindRes:relationResponse.getData()){
            SelectBindingThreePartyRelationsRet res = new SelectBindingThreePartyRelationsRet();
            res.setRegisterChannel(bindRes.getRegisterChannel());
            res.setThirdEmail(MaskUtils.maskHalfOpenEmail(bindRes.getThirdEmail()));
            resList.add(res);
        }
        return new CommonRet<>(resList);
    }


    @PostMapping("/v2/transient/account/oauth/sign/apple")
    @AccountDefenseResource(name="AccountThirdOauthController.signWithApple")
    @AntiBotCaptchaValidate(bizType = {BizType.THIRD_LOGIN})
    @DDoSPreMonitor(action = "signWithApple")
    @UserOperation(eventName = "login", name = "用户登陆", logDeviceOperation = true, deviceOperationNoteField = {"userId"}, requestKeys = {"email"},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {})
    public CommonRet<DoAppleLoginRet> signWithApple(HttpServletRequest request, HttpServletResponse response, @RequestBody @Validated DoAppleLoginUrlArg arg)throws Exception {
        if (!appleOauthLoginSwitch){
            log.info("AccountThirdOauthController.doBackendAppleLogin.switch is false");
            throw new BusinessException(AccountMgsErrorCode.APPLE_OAUTH_LOGIN_IS_CLOSE);
        }
        DoOauthLoginRequest doBackendAppleLoginRequest = new DoOauthLoginRequest();
        doBackendAppleLoginRequest.setIdToken(arg.getIdToken());
        doBackendAppleLoginRequest.setCode(arg.getCode());
        doBackendAppleLoginRequest.setRedirectUrl(arg.getRedirectUrl());
        doBackendAppleLoginRequest.setClientType(BaseHelper.getClientType(request));
        doBackendAppleLoginRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, null));
        doBackendAppleLoginRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        doBackendAppleLoginRequest.setThirdOperatorEnum(arg.getThirdOperatorEnum());
        if (doBackendAppleLoginRequest.getDeviceInfo() != null) {
            UserOperationHelper.log("device-info", doBackendAppleLoginRequest.getDeviceInfo());
            String bncUuid = doBackendAppleLoginRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }

        UserOperationHelper.log("loginType", "apple");
        String md5IdToken = Md5Tools.MD5(arg.getIdToken());
        Object value;
        if (shardingRedisMigrateThirdOauthReadSwitch) {
            value = ShardingRedisCacheUtils.get(md5IdToken, String.class, CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        } else {
            value = accountMgsRedisTemplate.opsForValue().get(md5IdToken);
        }
        if (value != null){
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
        APIResponse<DoOauthLoginResponse> apiResponse = accountOauthApi.doOauthLogin(baseHelper.getInstance(doBackendAppleLoginRequest));
        checkResponse(apiResponse);
        if (shardingRedisMigrateThirdOauthWriteSwitch) {
            ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        } else {
            accountMgsRedisTemplate.opsForValue().set(md5IdToken, md5IdToken, 2, TimeUnit.HOURS);
            ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        }
        DoAppleLoginRet data = new DoAppleLoginRet();
        if (ThirdLoginResultEnum.NEED_REGISTER.name().equalsIgnoreCase(apiResponse.getData().getResult()) || ThirdLoginResultEnum.NEED_BINDING.name().equalsIgnoreCase(apiResponse.getData().getResult())){
            data.setResult(apiResponse.getData().getResult());
            data.setEmail(apiResponse.getData().getEmail());
            if (baseHelper.isFromWeb()) {
                authHelper.setRegisterTokenCookie(request,response,apiResponse.getData().getRegisterToken(),registerTokenCookieMaxAge);
                data.setRegisterToken(null);
            }else{
                data.setRegisterToken(apiResponse.getData().getRegisterToken());
            }
            return new CommonRet<>(data);
        }

        userComplianceService.complianceBlockLoginWithTimeout(apiResponse.getData().getUserId());

        data.setUserId(String.valueOf(apiResponse.getData().getUserId()));
        BeanUtils.copyProperties(apiResponse.getData(),data);
        if (baseHelper.isFromWeb()) {
            // 网页版设置cookie,半登录态还是顶级域名，兼容yubikey
//            BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, apiResponse.getData().getSerialNo());
            String code = authHelper.setAuthCookie(request,response,apiResponse.getData().getToken(),apiResponse.getData().getCsrfToken());
            data.setCode(code);
            // 根据安全要求，web端不返回token
            data.setToken(null);
            data.setCsrfToken(null);
            data.setRegisterToken(null);
        }
        //成功
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, data.getUserId()));
        // 登陆成功
        UserOperationHelper.log(ImmutableMap.of("login", Boolean.TRUE.toString()));
        // 数据分析
        sensorHelper.profileSet(Long.valueOf(data.getUserId()));
        // 记录登陆日志
        authHelper.loginWithIpNew(data.getUserId());
        // 发送消息到人机识别系统
        authHelper.sendHumanRecognitionTopic(data.getUserId(), baseHelper.getCookieValue(Constant.COOKIE_CLIENT_ID));
        //发送登录信息给risk
        RiskLoginInfoDto riskLoginInfoDto = new RiskLoginInfoDto();
        riskLoginInfoDto.setMobile(null);
        riskLoginInfoDto.setMobileCode(null);
        riskLoginInfoDto.setEmail(data.getEmail());
        riskLoginInfoDto.setIp(WebUtils.getRequestIp());
        riskLoginInfoDto.setSource("APPLE");//todo 通知risk
        riskLoginInfoDto.setUserId(Long.valueOf(data.getUserId()));
        riskKafkaHelper.sendLoginInfoToRiskByDto(riskLoginInfoDto);

        data.setBncLocation(userComplianceService.getBncLocationWithTimeout(apiResponse.getData().getUserId()));
        return new CommonRet<>(data);
    }

    @PostMapping("/v2/transient/account/oauth/register/third")
    @AccountDefenseResource(name="AccountThirdOauthController.registerByThird")
    @AntiBotCaptchaValidate(bizType = {BizType.THIRD_LOGIN})
    @DDoSPreMonitor(action = "registerByThird")
    @UserOperation(eventName = "register", name = "用户注册", requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<RegisterByThirdRet> registerByThird(HttpServletRequest request, HttpServletResponse response, @RequestBody @Validated RegisterByThirdRegisterArg arg)throws Exception {
        if (!thirdOauthRegisterSwitch){
            log.info("AccountThirdOauthController.registerByThird.switch is false");
            throw new BusinessException(AccountMgsErrorCode.THIRD_OAUTH_LOGIN_IS_CLOSE);
        }
        String registerToken = baseHelper.isFromWeb()?authHelper.getRegisterTokenCookie():arg.getRegisterToken();
        if (StringUtils.isBlank(registerToken)){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        if ((ThirdOperatorEnum.APPLE_LOGIN == arg.getThirdOperatorEnum() || ThirdOperatorEnum.GOOGLE_LOGIN ==  arg.getThirdOperatorEnum()) && StringUtils.isBlank(registerToken)){
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
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
        RegisterByThirdRequest registerByThirdRequest = new RegisterByThirdRequest();
        String agentId = arg.getAgentId();
        Boolean isAgentIdNull = false;
        // TODO gateway 需要处理refer，并在header中传递标识过来
        if (StringUtils.isBlank(agentId) || StringUtils.isNumeric(agentId)) {
            if (StringUtils.isNumeric(agentId) && request.getHeader(Constant.HEADER_AD_BLOCK) != null) {
                // 广告或者子账号推荐的，agentId清空
                registerByThirdRequest.setAgentId(null);
            } else if (StringUtils.isNumeric(agentId)) {
                registerByThirdRequest.setAgentId(Long.parseLong(agentId));
            }
            if (StringUtils.isBlank(agentId) || arg.getAgentId() == null) {
                registerByThirdRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
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
                registerByThirdRequest.setAgentRateCode(null);
                registerByThirdRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
            }else{
                registerByThirdRequest.setAgentRateCode(agentId);
                registerByThirdRequest.setAgentId(NumberUtils.toLong(sysConfigHelper.getCodeByDisplayName("default_agent")));
            }
        }
        registerByThirdRequest.setThirdOperatorEnum(arg.getThirdOperatorEnum());
        registerByThirdRequest.setIsFastCreatFuturesAccountProcess(arg.getIsFastCreatFuturesAccountProcess());
        registerByThirdRequest.setFuturesReferalCode(arg.getFuturesReferalCode());
        registerByThirdRequest.setRegisterToken(registerToken);
        registerByThirdRequest.setIsPersonalAccount(arg.getIsPersonalAccount());
        registerByThirdRequest.setTrackSource(accountHelper.getRegChannel(arg.getRegisterChannel()));
        registerByThirdRequest.setOauthClientId(arg.getOauthClientId());
        String registerSource = arg.getSource();
        boolean frontEndSkipForceKyc = false;
        if (CollectionUtils.isNotEmpty(kycIgnoreRegisterSources) && kycIgnoreRegisterSources.contains(registerSource)) {
            frontEndSkipForceKyc = true;
        }
        String region = accountCountryHelper.getRegionByCountryCode(arg.getResidentCountry());
        boolean isEURegister = !frontEndSkipForceKyc && StringUtils.equalsAnyIgnoreCase(region, "EU", "EU_PROCESS");
        UserOperationHelper.log("resident_country", arg.getResidentCountry());
        UserOperationHelper.log("eu_register_process", isEURegister);
        log.info("residentCountry={},region={},isEURegister={}", arg.getResidentCountry(), region, isEURegister);

        RegisterConfigRequest registerConfig = new RegisterConfigRequest();
        registerConfig.setIsEmailPromote(arg.getIsEmailPromote());
        registerConfig.setFrontEndSkipForceKyc(frontEndSkipForceKyc);
        if (isEURegister) {
            registerConfig.setRegion(region);
            registerConfig.setResidentCountry(arg.getResidentCountry());
            registerByThirdRequest.setRegisterConfigRequest(registerConfig);
        }
        registerByThirdRequest.setRegisterConfigRequest(registerConfig);
        registerByThirdRequest.setClientType(BaseHelper.getClientType(request));
        registerByThirdRequest.setDeviceInfo(userDeviceHelper.buildDeviceInfo(request, null, null));
        registerByThirdRequest.setFvideoId(userDeviceHelper.getFVideoId(request));
        registerByThirdRequest.setIsStatAgentError(arg.getIsStatAgentError());
        if (registerByThirdRequest.getDeviceInfo() != null) {
            UserOperationHelper.log("device-info", registerByThirdRequest.getDeviceInfo());
            String bncUuid = registerByThirdRequest.getDeviceInfo().get(CommonUserDeviceHelper.BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                // log打印前4位
                log.info("login got bnc-uuid: {}", StringUtils.abbreviate(bncUuid, "**", 6));
                UserOperationHelper.log(LocalLogKeys.BNC_UUID, bncUuid);
            } else {
                log.info("login did NOT get bnc-uuid");
            }
        }

        String md5IdToken = Md5Tools.MD5(registerToken);
        Object value;
        if (shardingRedisMigrateThirdOauthReadSwitch) {
            value = ShardingRedisCacheUtils.get(md5IdToken, String.class, CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        } else {
            value = accountMgsRedisTemplate.opsForValue().get(md5IdToken);
        }
        if (value != null){
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
        APIResponse<RegisterByThirdResponse> apiResponse = accountOauthApi.registrByThird(baseHelper.getInstance(registerByThirdRequest));
        checkResponse(apiResponse);
        if (shardingRedisMigrateThirdOauthWriteSwitch) {
            ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        } else {
            accountMgsRedisTemplate.opsForValue().set(md5IdToken, md5IdToken, 2, TimeUnit.HOURS);
            ShardingRedisCacheUtils.set(md5IdToken, md5IdToken, TimeUnit.HOURS.toSeconds(2), CacheConstant.ACCOUNT_MGS_OAUTH_TOKEN_PREFIX);
        }
        RegisterByThirdRet data = new RegisterByThirdRet();
        data.setUserId(String.valueOf(apiResponse.getData().getUserId()));
        BeanUtils.copyProperties(apiResponse.getData(),data);
        if (baseHelper.isFromWeb()) {
            // 网页版设置cookie,半登录态还是顶级域名，兼容yubikey
//            BaseHelper.setCookie(request, response, true, Constant.COOKIE_SERIAL_NO, apiResponse.getData().getSerialNo());
            String code = authHelper.setAuthCookie(request,response,apiResponse.getData().getToken(),apiResponse.getData().getCsrfToken());
            data.setCode(code);
            //完成注册则删除cookie
            authHelper.setRegisterTokenCookie(request,response,arg.getRegisterToken(),0);
            // 根据安全要求，web端不返回token
            data.setToken(null);
            data.setCsrfToken(null);
        }
        UserOperationHelper.log("deviceInfo", registerByThirdRequest.getDeviceInfo());
        UserOperationHelper.log("registerSuccess", Boolean.TRUE);
        UserOperationHelper.log("RegisterationMethodEnum", RegisterationMethodEnum.EMAIL);
        //成功
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, data.getUserId()));
        eventPublisher.publish(RegisterCounterMetrics
                .builder()
                .registerSource(getRegisterSource(arg.getThirdOperatorEnum()))
                .agentIdExist(String.valueOf(StringUtils.isNotBlank(arg.getAgentId())))
                .futuresReferralCodeExist(String.valueOf(StringUtils.isNotBlank(arg.getFuturesReferalCode())))
                .build()
        );
        // 同ip是否频繁注册
        riskHelper.frequentRegisterSendMq(apiResponse.getData().getUserId(), apiResponse.getData().getEmail());
        // 记录redis 判断是否需要极验
        geeTestHelper.increaseRegister(WebUtils.getRequestIp());
        //发送相关注册信息到growthkafka
        if (growthRegisterAgentCodeSwitch && baseHelper.isOk(apiResponse)){
            growthKafkaSupport.sendAgentToGrowthMsg(apiResponse.getData().getUserId(),arg.getAgentId(),System.currentTimeMillis(),apiResponse.getData().getDeviceId(),registerByThirdRequest.getFvideoId(),accountHelper.getRegChannel(arg.getRegisterChannel()));
        }
        //发送相关channel注册信息到crm
        if (growthChannelAgentCodeSwitch && baseHelper.isOk(apiResponse) && isAgentIdNull && request != null && StringUtils.isNotBlank(request.getHeader(CrmKafkaSupport.BNC_APP_CHANNEL))){
            crmKafkaSupport.sendCrmMgsKafkaProducerChannelRegisterTopic(apiResponse.getData().getUserId(),WebUtils.getRequestIp(),request.getHeader(CrmKafkaSupport.BNC_APP_CHANNEL), accountHelper.getBncUuidFromRequest(request), baseHelper.getLanguage());
        }
        return new CommonRet<>(data);
    }

    private String getRegisterSource(ThirdOperatorEnum thirdOperatorEnum) {
        if (Objects.equals(ThirdOperatorEnum.APPLE_LOGIN, thirdOperatorEnum) ||
                Objects.equals(ThirdOperatorEnum.APPLE_REGISTER, thirdOperatorEnum)) {
            return "APPLE";
        }
        if (Objects.equals(ThirdOperatorEnum.GOOGLE_LOGIN, thirdOperatorEnum) ||
                Objects.equals(ThirdOperatorEnum.GOOGLE_REGISTER, thirdOperatorEnum)) {
            return "GOOGLE";
        }
        log.warn("unknown register platform");
        return null;
    }




//    @PostMapping("/v1/private/account/oauth/google/switch")
//    public CommonRet<Boolean> doUpdateThirdLoginSwitch(@RequestBody @Validated UpdateThirdLoginSwitchRequest arg)throws Exception {
//        APIResponse<Boolean> apiResponse = userSecurityApi.updateThirdLoginSwitch(baseHelper.getInstance(arg));
//        checkResponse(apiResponse);
//        return new CommonRet<>(apiResponse.getData());
//    }
}
