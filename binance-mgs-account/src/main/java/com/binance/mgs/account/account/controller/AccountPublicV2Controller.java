package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.AccountResetPasswordRequestV2;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.AccountResetPasswordResponseV2;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.user.request.AccountForgotPasswordPreCheckRequest;
import com.binance.account.vo.user.response.AccountForgotPasswordPreCheckResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.vo.ForgotPasswordArgV2;
import com.binance.mgs.account.account.vo.GetCountryAndCityByIpRet;
import com.binance.mgs.account.account.vo.ResetPasswordArgV2;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.account.util.RegexUtils;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.mgs.business.account.vo.LocationInfo;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.binance.mgs.account.constant.BizType.FORGET_PASSWORD;

@RestController
@RequestMapping(value = "/v2/public")
@Slf4j
public class AccountPublicV2Controller extends AccountBaseAction {
    @Resource
    private RiskHelper riskHelper;
    @Resource
    private AccountHelper accountHelper;
    @Resource
    private AuthHelper authHelper;
    @Resource
    private UserSecurityApi userSecurityApi;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheHelper;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Value("${forgot.password.banIp.check.switch:false}")
    private boolean forgetPassBanIpCheck;
    @Value("${reset.password.banIp.check.switch:false}")
    private boolean resetPassBanIpCheck;

    @Value("${DDos.ip.limit.count:10}")
    private int ipLimitCount;

    @Value("${DDos.normal.ip.limit.count:100}")
    private int normalUserIpLimitCount;

    @Value("${DDos.forget.pass.ip.limit.count:10}")
    private int forgetPassIpLimitCount;


    @PostMapping(value = "/account/user/forgot-password-precheck")
    @AntiBotCaptchaValidate(bizType = {FORGET_PASSWORD})
    @AccountDefenseResource(name = "AccountPublicV2Controller.forgotPasswordPreCheck")
    @DDoSPreMonitor(action = "forgotPasswordPreCheck")
    @UserOperation(eventName = "forgotPassword", name = "用户忘记密码", requestKeys = {"email"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {"email"})
    public CommonRet<AccountForgotPasswordPreCheckResponse> forgotPasswordPreCheck(@RequestBody @Validated ForgotPasswordArgV2 forgotPasswordArg)
            throws Exception {

        // 极验验证
        accountHelper.verifyCodeCacheAndBanIp(forgotPasswordArg, BizType.FORGET_PASSWORD);
        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(forgotPasswordArg.getMobile(), forgotPasswordArg.getMobileCode()) &&
                StringUtils.isBlank(forgotPasswordArg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        String ip = WebUtils.getRequestIp();
        if (StringUtils.isNotBlank(forgotPasswordArg.getEmail()) && !timeOutRegexUtils.validateEmail(forgotPasswordArg.getEmail())) {
            if (resetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ipLimitCount, String.format("forgotPasswordPreCheck fake email=%s", forgotPasswordArg.getEmail()));
            }
            throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
        }

        if(StringUtils.isNoneBlank(forgotPasswordArg.getMobile(), forgotPasswordArg.getMobileCode()) && !RegexUtils.validateOnlyChinaMobilePhone(forgotPasswordArg.getMobileCode(), forgotPasswordArg.getMobile())){
            if (resetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ipLimitCount, String.format("forgotPasswordPreCheck fake mobile=%s-%s", forgotPasswordArg.getMobileCode(), forgotPasswordArg.getMobile()));
            }
            throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
        }

        if (StringUtils.isAllBlank(forgotPasswordArg.getMobileVerifyCode(), forgotPasswordArg.getEmailVerifyCode(),
                forgotPasswordArg.getGoogleVerifyCode(), forgotPasswordArg.getYubikeyVerifyCode())) {
            if (forgetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_VERIFY_INFO, ip, forgetPassIpLimitCount, "forgotPasswordPreCheck verify code blank");
            }
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(forgotPasswordArg.getEmail());
        getUserIdReq.setMobileCode(forgotPasswordArg.getMobileCode());
        getUserIdReq.setMobile(forgotPasswordArg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));

        if (!baseHelper.isOk(getUserIdResp)) {
            if (forgetPassBanIpCheck) {
                String identify = StringUtils.isNotBlank(forgotPasswordArg.getEmail()) ? forgotPasswordArg.getEmail() : forgotPasswordArg.getMobileCode() + "-" + forgotPasswordArg.getMobile();
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("forgotPasswordPreCheck identify=%s", identify));
            }
            log.warn("user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
            checkResponseMisleaderUseNotExitsError(getUserIdResp);
            return new CommonRet<>();
        } else {
            if (forgetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_FORGET_PASS, ip, normalUserIpLimitCount, "forgotPasswordPreCheck");
            }
        }

        // 记录风控信息
        Long userId = getUserIdResp.getData().getUserId();
        String userEmail = accountHelper.getUserById(userId).getData().getEmail();
        // 风控校验
        riskHelper.checkForgetPswLimits(userEmail);
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
        UserOperationHelper.log("device-info", userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getUserEmail()));
        AccountForgotPasswordPreCheckRequest accountForgotPasswordPreCheckRequest = new AccountForgotPasswordPreCheckRequest();
        accountForgotPasswordPreCheckRequest.setEmail(forgotPasswordArg.getEmail());
        accountForgotPasswordPreCheckRequest.setMobileCode(forgotPasswordArg.getMobileCode());
        accountForgotPasswordPreCheckRequest.setMobile(forgotPasswordArg.getMobile());
        accountForgotPasswordPreCheckRequest.setMobileVerifyCode(forgotPasswordArg.getMobileVerifyCode());
        accountForgotPasswordPreCheckRequest.setEmailVerifyCode(forgotPasswordArg.getEmailVerifyCode());
        accountForgotPasswordPreCheckRequest.setGoogleVerifyCode(forgotPasswordArg.getGoogleVerifyCode());
        APIResponse<AccountForgotPasswordPreCheckResponse> accountForgotPasswordPreCheckResponseAPIResponse =
                userSecurityApi.forgotPasswordPreCheck(getInstance(accountForgotPasswordPreCheckRequest));
        if (!baseHelper.isOk(accountForgotPasswordPreCheckResponseAPIResponse)) {
            log.warn("user illegal,userSecurityApi.forgotPasswordPreCheck,response={}", accountForgotPasswordPreCheckResponseAPIResponse);
            checkResponseMisleaderUseNotExitsError(accountForgotPasswordPreCheckResponseAPIResponse);
            return new CommonRet<>();
        }
        CommonRet<AccountForgotPasswordPreCheckResponse> ret = new CommonRet<>();
        ret.setData(accountForgotPasswordPreCheckResponseAPIResponse.getData());
        return ret;
    }

    @PostMapping(value = "/account/user/reset-password")
    @AntiBotCaptchaValidate(bizType = {FORGET_PASSWORD}, name = "resetUserPassword")
    @AccountDefenseResource(name = "AccountPublicV2Controller.resetUserPassword")
    @DDoSPreMonitor(action = "resetUserPassword")
    @UserOperation(eventName = "resetPassword", name = "用户忘记密码邮箱验证", requestKeys = {"email"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeyDisplayNames = {"邮箱"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sensorsRequestKeys = {"email"})
    public CommonRet<String> resetUserPassword(@RequestBody @Validated ResetPasswordArgV2 resetPasswordArg) throws Exception {

        log.info("getTerminal = {},clientType = {}", getTerminal(), baseHelper.getClientType());
       /* if (needRouteAppForForgetPwdSwitch && !forgetPwdEnableVersion()) {
            log.info("route app");
            throw new BusinessException(AccountMgsErrorCode.FOREGT_PWD_NOT_SUPPORT_FOR_APP);
        }

        if (needCheckGtForForgetPwdSwitch && forgetPwdEnableVersion() ) {
            log.info("check gt");
            accountHelper.verifyCodeCacheAndBanIp(resetPasswordArg, BizType.FORGET_PASSWORD);
        }*/

        // 邮箱或手机至少有一个
        if (StringUtils.isAnyBlank(resetPasswordArg.getMobile(), resetPasswordArg.getMobileCode()) &&
                StringUtils.isBlank(resetPasswordArg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        String ip = WebUtils.getRequestIp();
        if (StringUtils.isNotBlank(resetPasswordArg.getEmail()) && !timeOutRegexUtils.validateEmailForReset(resetPasswordArg.getEmail())) {
            if (resetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ipLimitCount, String.format("resetUserPassword fake email=%s", resetPasswordArg.getEmail()));
            }
            throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
        }

        if (StringUtils.isNoneBlank(resetPasswordArg.getMobile(), resetPasswordArg.getMobileCode())
                && !RegexUtils.validateOnlyChinaMobilePhone(resetPasswordArg.getMobileCode(), resetPasswordArg.getMobile())) {
            if (resetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.ILLEGAL_USER_INFO, ip, ipLimitCount, String.format("resetUserPassword fake mobile=%s-%s", resetPasswordArg.getMobileCode(), resetPasswordArg.getMobile()));
            }
            throw new BusinessException(AccountMgsErrorCode.USER_MOBILE_NOT_CORRECT);
        }

        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(resetPasswordArg.getEmail());
        getUserIdReq.setMobile(resetPasswordArg.getMobile());
        getUserIdReq.setMobileCode(resetPasswordArg.getMobileCode());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        if (!baseHelper.isOk(getUserIdResp)) {
            if (resetPassBanIpCheck) {
                String identify = StringUtils.isNotBlank(resetPasswordArg.getEmail()) ? resetPasswordArg.getEmail() : resetPasswordArg.getMobileCode() + "-" + resetPasswordArg.getMobile();
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("resetUserPassword identify=%s", identify));
            }
            log.warn("user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
            checkResponseMisleaderUseNotExitsError(getUserIdResp);
            return new CommonRet<>();
        } else {
            if (resetPassBanIpCheck) {
                ddosCacheHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_FORGET_PASS, ip, normalUserIpLimitCount, "resetUserPassword");
            }
        }
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, getUserIdResp.getData().getUserId()));
        UserOperationHelper.log("device-info", userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getUserEmail()));
        // 重置密码
        AccountResetPasswordRequestV2 request = new AccountResetPasswordRequestV2();
        request.setEmail(resetPasswordArg.getEmail());
        request.setMobileCode(resetPasswordArg.getMobileCode());
        request.setMobile(resetPasswordArg.getMobile());
        request.setPassword(resetPasswordArg.getPassword());
        request.setPasswordConfirm(resetPasswordArg.getPassword());
        request.setSafePassword(resetPasswordArg.getSafePassword());
        request.setConfirmSafePassword(resetPasswordArg.getSafePassword());
        request.setToken(resetPasswordArg.getToken());
        request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getUserEmail()));
        APIResponse<AccountResetPasswordResponseV2> apiResponse = userSecurityApi.resetPasswordV2(getInstance(request));
        checkResponseMisleaderUseNotExitsError(apiResponse);
        if (apiResponse.getData() != null && apiResponse.getData().getUserId() != null) {
            UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, apiResponse.getData().getUserId()));
        }
        // 修改成功后，清理所有token，重新登录
        authHelper.logoutAll(apiResponse.getData().getUserId());
        CommonRet<String> ret = new CommonRet<>();
        ret.setData("");
        ret.setMessage("");
        return ret;
    }


    @AccountDefenseResource(name = "AccountPublicV2Controller.getCountryShortByIp")
    @GetMapping(value = "/account/ip/country-short")
    public CommonRet<String> getCountryShortByIp(HttpServletRequest request) {
        String cloudFrontCountry = WebUtils.getHeader("CloudFront-Viewer-Country");
        if (cloudFrontCountry != null) {
            return new CommonRet<>(cloudFrontCountry);
        }
        return new CommonRet<>(Ip2LocationSwitchUtils.getCountryShort(IPUtils.getIpAddress(request)));
    }

    @AccountDefenseResource(name = "AccountPublicV2Controller.getCountryAndCityShortByIp")
    @GetMapping(value = "/account/ip/country-city-short")
    public CommonRet<GetCountryAndCityByIpRet> getCountryAndCityShortByIp(HttpServletRequest request) {
        String country = WebUtils.getCountry();
        if (StringUtils.isBlank(country)) {
            log.warn("AccountPublicV2Controller.getCountryAndCityShortByIp cannot get country by infra, request is {}", request);
            country = getCountryFromIp(request);
        }
        LocationInfo locationInfo;
        String city = WebUtils.getCity();
        if (StringUtils.isBlank(city)) {
            log.warn("AccountPublicV2Controller.getCountryAndCityShortByIp cannot get city by infra, request is {}", request);
            locationInfo = Ip2LocationSwitchUtils.getDetail(IPUtils.getIpAddress(request));
            city = locationInfo.getCity();
        }
        String subDivision = WebUtils.getState();
        if (StringUtils.isBlank(subDivision)) {
            log.warn("AccountPublicV2Controller.getCountryAndCityShortByIp cannot get subDivision by infra, request is {}", request);
            locationInfo = Ip2LocationSwitchUtils.getDetail(IPUtils.getIpAddress(request));
            subDivision = locationInfo.getRegion();
        }
        return new CommonRet<>(new GetCountryAndCityByIpRet(country, city, subDivision));
    }

    private String getCountryFromIp(HttpServletRequest request){
        String cloudFrontCountry = WebUtils.getHeader("CloudFront-Viewer-Country");
        if (cloudFrontCountry != null) {
            return cloudFrontCountry;
        }
        return Ip2LocationSwitchUtils.getCountryShort(IPUtils.getIpAddress(request));
    }

}
