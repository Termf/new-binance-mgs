package com.binance.mgs.account.account.controller;

import com.binance.account.api.CountryApi;
import com.binance.account.vo.country.CountryStatusVo;
import com.binance.account.vo.country.CountryVo;
import com.binance.account.vo.country.GetMobileRegisterSupportCountryListeRequest;
import com.binance.account.vo.country.GetMobileRegisterSupportCountryListeResp;
import com.binance.account.vo.country.MobileRegisterSupportCountryListResp;
import com.binance.account.vo.country.RestrictedCountryVo;
import com.binance.account.vo.country.UserInRestrictedCountryRequest;
import com.binance.account.vo.country.UserInRestrictedCountryResp;
import com.binance.compliance.api.UserComplianceApi;
import com.binance.compliance.vo.request.UserComplianceCheckRequest;
import com.binance.compliance.vo.response.UserComplianceCheckResponse;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.AccountCountryHelper;
import com.binance.mgs.account.account.vo.BncLocationRet;
import com.binance.mgs.account.account.vo.RestrictedCountryRet;
import com.binance.mgs.account.account.vo.UserComplianceArg;
import com.binance.mgs.account.service.UserComplianceService;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.business.account.vo.CountryListWithDefaultRet;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.account.vo.CountryRet;
import com.binance.platform.mgs.component.domain.CommonDomainHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.platform.mgs.utils.DomainUtils;
import com.binance.platform.mgs.utils.ListTransformUtil;
import com.binance.platform.mgs.utils.StringUtil;
import com.binance.userbigdata.api.ComplianceApi;
import com.binance.userbigdata.vo.compliance.request.ComplianceCheckRequest;
import com.binance.userbigdata.vo.compliance.request.SelectComplianceLoginRelatedRequest;
import com.binance.userbigdata.vo.compliance.response.ComplianceCheckRes;
import com.binance.userbigdata.vo.compliance.response.SelectComplianceLoginRelatedRes;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Shining.Cai on 2018/11/01.
 **/
@RestController
@Slf4j
public class CountryController extends AccountBaseAction {

    @Value("${enforceKyc.productLine:MAINSITE}")
    private String enforceKycCheckProductLine;

    @Value("${enforceKyc.operation:ENFORCE_KYC}")
    private String enforceKycCheckOperation;

    @Value("${getBncLocation.fromHeader:false}")
    private boolean getBncLocationFromHeader;

    @Value("#{'${support.cn.mobile.regiser.channel:growth}'.split(',')}")
    private List<String> supportCnMobileRegisterChannels;
    @Value("#{'${support.cn.mobile.regiser.terminal:web}'.split(',')}")
    private List<String> supportCnMobileRegisterTerminals;
    @Value("${support.cn.mobile.register.switch:true}")
    private boolean fullOpenCnMobileRegister;


    @Autowired
    private CountryApi countryApi;
    @Autowired
    private ComplianceApi complianceApi;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private CommonDomainHelper commonDomainHelper;
    @Autowired
    private UserComplianceApi userComplianceApi;
    @Autowired
    private AccountCountryHelper countryHelper;
    @Autowired
    private UserComplianceService userComplianceService;

    @GetMapping("/v1/private/country/support/check/userId")
    public CommonRet<Boolean> isUserCountryCodeInBlackList() {
        APIResponse<Boolean> response = countryApi.isUserInBlacklist(getUserId());
        super.checkResponse(response);
        return new CommonRet<>(!response.getData());
    }

    @GetMapping("/v1/public/country/support/check/ip")
    @CacheControl(noStore = true)
    public CommonRet<Boolean> isCountryIpInBlackList(String requestIp) {
        APIResponse<Boolean> response;
        if (StringUtil.isIp(requestIp)) {
            response = countryApi.isIpInBlacklist(requestIp);
        } else {
            response = countryApi.isIpInBlacklist(WebUtils.getRequestIp());
        }
        super.checkResponse(response);
        return new CommonRet<>(!response.getData());
    }

    @GetMapping("/v1/public/country/list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE)
    @CacheControl(maxAge = 30)
    public CommonRet<List<CountryRet>> getCountryList(String requestIp) {
        APIResponse<List<CountryVo>> response = countryApi.getCountryList();
        checkResponseWithoutLog(response);
        CommonRet<List<CountryRet>> ret = new CommonRet<>();
        if (!CollectionUtils.isEmpty(response.getData())) {
            ret.setData(ListTransformUtil.transform(response.getData(), CountryRet.class));
        }
        return ret;
    }

    @GetMapping("/v1/public/country/default/list")
    public CommonRet<CountryListWithDefaultRet> getCountryListWithDefault() {
        // 这里通过容器调用，触发缓存
        CommonRet<List<CountryRet>> listCommonRet = applicationContext.getBean(this.getClass()).getCountryList("");
        List<CountryRet> countryVoList = listCommonRet.getData();
        if (commonDomainHelper.isChinaCDN(DomainUtils.getDomain())){
            countryVoList.forEach(countryRet -> countryRet.setCountryImageUrl(countryRet.getCountryImageUrl().replace("https://bin.bnbstatic.com", commonDomainHelper.getCDNUrlByRegion())));
        }
        Map<String, CountryRet> countryVoMap = Maps.uniqueIndex(countryVoList, CountryRet::getCode);
        String code = Ip2LocationSwitchUtils.getCountryShort(WebUtils.getRequestIp());
        CountryListWithDefaultRet ret = new CountryListWithDefaultRet();
        ret.setSupportCountryList(countryVoList);
        if (StringUtils.isNotBlank(code) && countryVoMap.containsKey(code)) {
            CountryRet defaultCountry = new CountryRet();
            BeanUtils.copyProperties(countryVoMap.get(code), defaultCountry);
            ret.setDefaultCountry(defaultCountry);
        }
        return new CommonRet<>(ret);
    }

    @GetMapping("/v1/public/account/all/countries")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE)
    @CacheControl(maxAge = 30)
    public CommonRet<List<CountryRet>> getAllCountries() {
        APIResponse<List<CountryVo>> response = countryApi.getAllCountryList();
        checkResponseWithoutLog(response);
        CommonRet<List<CountryRet>> ret = new CommonRet<>();
        if (!CollectionUtils.isEmpty(response.getData())) {
            ret.setData(ListTransformUtil.transform(response.getData(), CountryRet.class));
        }
        return ret;
    }

    @GetMapping("/v1/public/country/ip/restricted")
    @CacheControl(noStore = true)
    public CommonRet<RestrictedCountryRet> isCountryIpRestricted() {
        APIResponse<RestrictedCountryVo> response = countryApi.isIpInRestrictedCountry(WebUtils.getRequestIp());
        checkResponseWithoutLog(response);
        RestrictedCountryRet ret = new RestrictedCountryRet();
        if (response.getData() != null) {
            BeanUtils.copyProperties(response.getData(), ret);
        }
        return new CommonRet<>(ret);
    }


    @GetMapping("/v1/public/country/support/mobile/register")
    @CacheControl(maxAge = 30)
    public CommonRet<GetMobileRegisterSupportCountryListeResp> getMobileRegisterSupportCountryList(@RequestParam(value = "ip", required = false) String ip) {
        GetMobileRegisterSupportCountryListeRequest getMobileRegisterSupportCountryListeRequest = new GetMobileRegisterSupportCountryListeRequest();
        getMobileRegisterSupportCountryListeRequest.setIp(ip);
        if (StringUtils.isBlank(ip)) {
            ip = WebUtils.getRequestIp();
            getMobileRegisterSupportCountryListeRequest.setIp(ip);
        }

        log.info("getMobileRegisterSupportCountryList, request ip = {} ", ip);
        GetMobileRegisterSupportCountryListeResp resp;
        try {
            TerminalEnum terminalEnum = WebUtils.getTerminal();
            String terminal = terminalEnum == null ? "" : terminalEnum.getCode();
            resp = countryHelper.getMobileRegisterCountryListCache().get(terminal);
        } catch (Exception e) {
            log.error("error while get mobileRegisterCountryListCache", e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }

        String defaultCode = Ip2LocationSwitchUtils.getCountryShort(ip);
        List<CountryVo> supportCountryList = resp.getSupportCountryList();
        Map<String, CountryVo> countryVoMap = Maps.uniqueIndex(supportCountryList, CountryVo::getCode);

        CountryVo defaultCountry = countryVoMap.get(defaultCode);
        resp.setDefaultCountry(defaultCountry);
        return new CommonRet<>(resp);
    }

    @GetMapping("/v2/public/country/support/mobile/register")
    @CacheControl(maxAge = 30)
    public CommonRet<MobileRegisterSupportCountryListResp> getMobileRegisterSupportCountryListV2(@RequestParam(value = "ip", required = false) String ip) {
        MobileRegisterSupportCountryListResp resp;
        TerminalEnum terminalEnum = WebUtils.getTerminal();
        String terminal = terminalEnum == null ? "" : terminalEnum.getCode();
        try {
            resp = countryHelper.getMobileRegisterCountryListCacheV2().get(terminal);
        } catch (Exception e) {
            log.error("error while get mobileRegisterCountryListCache", e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }

        String defaultCode = Ip2LocationSwitchUtils.getCountryShort(ip);
        List<CountryStatusVo> supportCountryList = resp.getCountryList();
        Map<String, CountryStatusVo> countryVoMap = supportCountryList.stream().collect(Collectors.toMap(CountryStatusVo::getCode, Function.identity()));
        String registerChannelHeader = WebUtils.getHeader("registerchannel");
        log.info("registerChannelHeader = {}", registerChannelHeader);
        if (supportCnMobileRegisterTerminals.contains(terminal) && supportCnMobileRegisterChannels.contains(registerChannelHeader)) {
            countryVoMap.computeIfPresent("CN", (code, countryStatusVo) -> {
                countryStatusVo.setSupport(true);
                countryStatusVo.setCompliance(true);
                return countryStatusVo;
            });
        } else {
            countryVoMap.computeIfPresent("CN", (code, countryStatusVo) -> {
                countryStatusVo.setSupport(fullOpenCnMobileRegister);
                countryStatusVo.setCompliance(fullOpenCnMobileRegister);
                return countryStatusVo;
            });
        }
        supportCountryList = Lists.newArrayList(countryVoMap.values());
        CountryStatusVo defaultCountry = countryVoMap.get(defaultCode);
        resp.setCountryList(supportCountryList);
        resp.setDefaultCountry(defaultCountry);
        return new CommonRet<>(resp);
    }


    @GetMapping("/v1/private/country/user/restricted")
    public CommonRet<UserInRestrictedCountryResp> isUserInRestrictedCountry() {
        UserInRestrictedCountryRequest userInRestrictedCountryRequest = new UserInRestrictedCountryRequest();
        userInRestrictedCountryRequest.setUserId(getUserId());
        userInRestrictedCountryRequest.setCheckBlackUser(true);
        userInRestrictedCountryRequest.setIp(WebUtils.getRequestIp());
        userInRestrictedCountryRequest.setNeedReturnCountryCategory(true);
        userInRestrictedCountryRequest.setFront(true);
        APIResponse<UserInRestrictedCountryResp> response = countryApi.isUserInRestrictedCountry(getInstance(userInRestrictedCountryRequest));
        checkResponse(response);
        UserInRestrictedCountryResp resp = response.getData();
        if (resp.isBlackIpCountry() || resp.isBlackCountry()){
            log.info("CountryController.isUserInRestrictedCountry.userId:{},kyc:{},ip:{}",getUserId(),resp.isBlackCountry(),resp.isBlackIpCountry());
        }
        return new CommonRet<>(resp);
    }

    @GetMapping("/v2/private/country/user/restricted")
    public CommonRet<ComplianceCheckRes> complianceCheck()throws Exception {
        ComplianceCheckRequest request = new ComplianceCheckRequest();
        request.setUserId(getUserId());
        request.setIp(WebUtils.getRequestIp());
        request.setNeedReturnCountryCategory(true);
        request.setFront(true);
        APIResponse<ComplianceCheckRes> response = complianceApi.complianceCheck(getInstance(request));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/private/country/disable/timeleft")
    public CommonRet<String> selectloginTimeLeft()throws Exception {
        APIResponse<String> response = complianceApi.selectCloseTimeByUserId(getInstance(getUserId()));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v2/private/account/compliance/login")
    public CommonRet<SelectComplianceLoginRelatedRes> selectComplianceLoginRelated()throws Exception {
        SelectComplianceLoginRelatedRequest request = new SelectComplianceLoginRelatedRequest();
        request.setUserId(getUserId());
        APIResponse<SelectComplianceLoginRelatedRes> response = complianceApi.selectComplianceLoginRelated(getInstance(request));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * 新合规接口，给web端使用，支持传定制参数
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping("/v1/private/account/compliance/userComplianceCheck")
    public CommonRet<UserComplianceCheckResponse> userComplianceCheck(@RequestBody @Validated UserComplianceArg arg)throws Exception {
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(getUserId());
        request.setUserRequestIp(WebUtils.getRequestIp());
        request.setProductLine(arg.getProductLine());
        request.setOperation(arg.getOperation());
        request.setFront(true);
        APIResponse<UserComplianceCheckResponse> response = userComplianceApi.userComplianceCheck(getInstance(request));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * 强制kyc独立接口，无入参，提供给app端使用
     * @return
     * @throws Exception
     */
    @PostMapping("/v1/private/account/compliance/enforceKycCheck")
    public CommonRet<UserComplianceCheckResponse> enforceKycCheck()throws Exception {
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(getUserId());
        request.setProductLine(enforceKycCheckProductLine);
        request.setOperation(enforceKycCheckOperation);
        request.setFront(true);
        APIResponse<UserComplianceCheckResponse> response = userComplianceApi.userComplianceCheck(getInstance(request));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * 新合规接口，给web端使用，支持传定制参数
     * 如果合规校验失败，抛出异常
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping("/v2/private/account/compliance/userComplianceCheck")
    public CommonRet<UserComplianceCheckResponse> userComplianceCheckV2(@RequestBody @Validated UserComplianceArg arg)throws Exception {
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(getUserId());
        request.setUserRequestIp(WebUtils.getRequestIp());
        request.setProductLine(arg.getProductLine());
        request.setOperation(arg.getOperation());
        request.setFront(true);
        APIResponse<UserComplianceCheckResponse> response = userComplianceApi.userComplianceCheck(getInstance(request));
        checkResponse(response);

        UserComplianceCheckResponse userComplianceCheckResponse = response.getData();
        if(userComplianceCheckResponse != null && !userComplianceCheckResponse.isPass()) {
            throw new BusinessException(userComplianceCheckResponse.getErrorCode(), userComplianceCheckResponse.getErrorMessage());
        }
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/private/account/compliance/getBncLocation")
    public CommonRet<BncLocationRet> getBncLocation() throws Exception {
        BncLocationRet bncLocationRet = new BncLocationRet();
        if(getBncLocationFromHeader) {
            // 用户登录后，保存entity至session，架构组在网关层从session中获取entity后通过header向下传递
            bncLocationRet.setBncLocation(WebUtils.getHeader("entity"));
            if(StringUtils.isNotBlank(bncLocationRet.getBncLocation())) {
                log.info("getBncLocation from header, userId: {}, bncLocation: {}", getUserId(), bncLocationRet.getBncLocation());
            } else {
                bncLocationRet.setBncLocation(userComplianceService.getDefaultBncLocation());
            }
        } else {
            bncLocationRet.setBncLocation(userComplianceService.getBncLocation(getUserId()));
        }
        return new CommonRet<>(bncLocationRet);
    }
}
