package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserApi;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountdefensecenter.core.annotation.CallAppCheck;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.AccountMgsRedisHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.vo.AgentInfoArg;
import com.binance.mgs.account.account.vo.AgentInfoRet;
import com.binance.mgs.account.account.vo.PromoteEmailAndTermsFlagRet;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.authcenter.vo.LoginV2Arg;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.util.GrowthRegisterAgentCodePrefixUtil;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.rebate.relation.api.RebateRelationApi;
import com.binance.userbigdata.api.ComplianceApi;
import com.binance.userbigdata.vo.compliance.request.ReopenKycUsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/v1/public")
@Slf4j
public class AccountPublicController extends BaseAction {
    @Resource
    private UserApi userApi;
    @Resource
    private RiskHelper riskHelper;
    @Resource
    private AccountHelper accountHelper;
    @Value("${ddos.login.check.switch:false}")
    private boolean ddosLoginCheckSwitch;
    @Value("${ddos.kyc.us.login.ip.limit.count:3}")
    private int ddosKycUsloginIpLimitCount;
    @Value("${account.email.forbidden.config:@sohu.com}")
    private String accountEmailForbiddenConfig;
    @Value("${account.reopen.login.switch:5}")
    private Long accountReopenLoginSwitch;
    @Value("#{'${growth.register.agentcode.prefix:LIMIT_}'.split(',')}")
    private Set<String> growthRegisterAgentCodePrefix;
    @Value("${mgs-account.redis.incr.limit:1000}")
    private long accountMgsRedisIncrLimit;
    @Value("#{'${show.email.promote.countrys:XXX}'.split(',')}")
    private Set<String> showEmailPromoteCountrys;

    @Value("#{'${show.termsandprivacy.countrys:XXX}'.split(',')}")
    private Set<String> showTermsAndPrivacyCountries;

    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    private ComplianceApi complianceApi;
    private RedisTemplate<String, Object> accountMgsRedisTemplate=AccountMgsRedisHelper.getInstance();

    @Value("${rebate.relation.valid.agentcode.switch:false}")
    private boolean rebateRelationValidAgentCodeSwitch;

    @Autowired
    private RebateRelationApi rebateRelationApi;

    /**
     * 获取推荐人信息
     *
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/account/agent-info/get")
    @CacheControl(maxAge = 30)
    public CommonRet<AgentInfoRet> getAgentInfo(@Valid AgentInfoArg agentInfoArg) throws Exception {
        CommonRet<AgentInfoRet> ret = new CommonRet<>();
        APIResponse<UserStatusEx> apiResponse = null;
        AgentInfoRet data = new AgentInfoRet();
        ret.setData(data);
        if (StringUtils.isNotBlank(agentInfoArg.getAgentId()) && GrowthRegisterAgentCodePrefixUtil.checkGrowthRegisterAgentCodePrefix(growthRegisterAgentCodePrefix, agentInfoArg.getAgentId())) {
            data.setExist(true);
            data.setVerified(true);
            return ret;
        }
        if (rebateRelationValidAgentCodeSwitch) {
            APIResponse<Long> response = rebateRelationApi.verifyAgentCodeValid(getInstance(agentInfoArg.getAgentId()));
            checkResponse(response);
            UserIdRequest request = new UserIdRequest();
            request.setUserId(response.getData());
            apiResponse = userApi.getUserStatusByUserId(getInstance(request));
        } else {
            if (StringUtils.isNumeric(agentInfoArg.getAgentId())) {
                UserIdRequest request = new UserIdRequest();
                request.setUserId(Long.parseLong(agentInfoArg.getAgentId()));
                apiResponse = userApi.getUserStatusByUserId(getInstance(request));
            } else {
                apiResponse = userApi.getUserStatusByAgentCode(getInstance(agentInfoArg.getAgentId()));
            }
        }
        checkResponse(apiResponse);
        UserStatusEx userStatusEx = apiResponse.getData();

        if (userStatusEx != null) {
            data.setExist(true);
            if (userStatusEx.getIsUserActive()) {
                data.setVerified(true);
            } else {
                throw new BusinessException(GeneralCode.USER_NOT_ACTIVE);
            }
            // 子账户禁止使用该功能
            if (userStatusEx.getIsSubUser()) {
                throw new BusinessException(MgsErrorCode.SUBUSER_FEATURE_FORBIDDEN);
            }
        }
        return ret;
    }


    @AccountDefenseResource(name = "AccountPublicController.getCountryShortByIp")
    @GetMapping(value = "/account/ip/country-short")
    public CommonRet<String> getCountryShortByIp(HttpServletRequest request) {
        return new CommonRet<>(Ip2LocationSwitchUtils.getCountryShort(IPUtils.getIpAddress(request)));
    }

    @GetMapping(value = "/account/country/promote/email/show")
    public CommonRet<Boolean> isShowEmailPromote(HttpServletRequest request) {
        String countryShort = Ip2LocationSwitchUtils.getCountryShort(WebUtils.getRequestIp());
        //是不是要换个工具类
        if (StringUtils.isNotBlank(countryShort) &&  showEmailPromoteCountrys.contains(countryShort.toUpperCase())){
            return new CommonRet<>(true);
        }
        return new CommonRet<>(false);
    }

    @GetMapping(value = "/account/country/promote-and-term/show")
    public CommonRet<PromoteEmailAndTermsFlagRet> isShowEmailPromoteAndTerms(HttpServletRequest request) {
        PromoteEmailAndTermsFlagRet result = new PromoteEmailAndTermsFlagRet();
        String countryShort = Ip2LocationSwitchUtils.getCountryShort(WebUtils.getRequestIp());
        if (StringUtils.isNotBlank(countryShort) && showEmailPromoteCountrys.contains(countryShort.toUpperCase())) {
            result.setPromoteEmail(true);
        } else {
            result.setPromoteEmail(false);
        }
        //  如果没有countryshort，需要展示勾选框
        if ((StringUtils.isNotBlank(countryShort) && showTermsAndPrivacyCountries.contains(countryShort.toUpperCase())) || ("-").equals(countryShort)) {
            result.setTermsAndPrivacy(true);
        } else {
            result.setTermsAndPrivacy(false);
        }
        return new CommonRet<>(result);
    }

    /**
     * 禁止注册的邮箱类型
     *
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/account/email/forbidenconfig")
    public CommonRet<String[]> forbidenconfig() throws Exception {
        CommonRet<String[]> ret = new CommonRet<>();
        String[] resultArry = accountEmailForbiddenConfig.split(",");
        ret.setData(resultArry);
        return ret;
    }

    /**
     * 禁止注册的邮箱类型
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/account/login/reopen")
    @CallAppCheck(value = "AccountPublicController.accountLoginReopen")
    public CommonRet<Boolean> accountLoginReopen(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody LoginV2Arg loginArg) throws Exception {
        //kyc us的正常报错，但是会记录到redis-ddos
        Object value = accountMgsRedisTemplate.opsForValue().get(CacheConstant.ACCOUNT_KYC_US_LOGIN + (org.apache.commons.lang3.StringUtils.isNotBlank(loginArg.getEmail()) ? loginArg.getEmail() : loginArg.getMobile() + loginArg.getMobileCode()));
        if (value == null){
            String ip = WebUtils.getRequestIp();
            if (ddosLoginCheckSwitch && ddosCacheSeviceHelper.ipVisitCount(ip, "reopen") > ddosKycUsloginIpLimitCount) {
                log.info("ddos accountLoginReopen banIp,error safepwd email={} ip={}",JsonUtils.toJsonHasNullKey(loginArg), ip);
                ddosCacheSeviceHelper.banIp(ip);
            }
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        //次数限制
        Long total = this.incrAndExpireCache(CacheConstant.ACCOUNT_KYC_US_REOPEN + (org.apache.commons.lang3.StringUtils.isNotBlank(loginArg.getEmail()) ? loginArg.getEmail() : loginArg.getMobile() + loginArg.getMobileCode()));
        if (total == -1 || total > accountReopenLoginSwitch){
            String ip = WebUtils.getRequestIp();
            log.info("ddos accountLoginReopen incrAndExpireCache banIp,error safepwd email={} ip={}",JsonUtils.toJsonHasNullKey(loginArg), ip);
            ddosCacheSeviceHelper.banIp(ip);
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);

        }
        ReopenKycUsRequest reopenKycUsRequest = new ReopenKycUsRequest();
        reopenKycUsRequest.setEmail(loginArg.getEmail());
        reopenKycUsRequest.setMobile(loginArg.getMobile());
        reopenKycUsRequest.setMobileCode(loginArg.getMobileCode());
        reopenKycUsRequest.setOperator("web");
        APIResponse<Boolean> res = complianceApi.reopenKycUs(getInstance(reopenKycUsRequest));
        checkResponse(res);
        CommonRet<Boolean> commonRet = new CommonRet<>();
        commonRet.setData(res.getData());
        if (res.getData() != null && res.getData()){
            accountMgsRedisTemplate.delete(CacheConstant.ACCOUNT_KYC_US_LOGIN + (org.apache.commons.lang3.StringUtils.isNotBlank(loginArg.getEmail()) ? loginArg.getEmail() : loginArg.getMobile() + loginArg.getMobileCode()));
        }
        return commonRet;
    }

    /**
     * 统计gtid使用次数
     *
     */
    public Long incrAndExpireCache(String key) {
        try {
            //超过一定次数不再递增了
            Object oldCount = accountMgsRedisTemplate.opsForValue().get(key);
            if (oldCount != null && Long.parseLong(String.valueOf(oldCount)) > accountMgsRedisIncrLimit) {
                log.warn("incrAndExpireCache incr over limit={} key={} ip={}", accountMgsRedisIncrLimit, key, WebUtils.getRequestIp());
                return Long.parseLong(String.valueOf(oldCount));
            }
            Long count = accountMgsRedisTemplate.opsForValue().increment(key);
            Long expire = accountMgsRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null==expire || -1L==expire) {
                accountMgsRedisTemplate.expire(key, 5, TimeUnit.SECONDS);
                expire = accountMgsRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            }
            log.info("incrAndExpireCache key={},expire={},count={}", key,expire, count);
            return count;
        } catch (Exception e) {
            log.error("incrAndExpireCache error", e);
            return -1L;
        }
    }
}
