package com.binance.mgs.account.security.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.GeetestHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.authcenter.vo.SecurityPreCheckRet;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.security.helper.AntiBotHelper;
import com.binance.mgs.account.security.vo.CaptchaValidateInfo;
import com.binance.mgs.account.security.vo.SecurityAppAttestCheckArg;
import com.binance.mgs.account.security.vo.SecurityAppAttestCheckRet;
import com.binance.mgs.account.security.vo.SecurityCheckArg;
import com.binance.mgs.account.security.vo.SecurityCheckRet;
import com.binance.mgs.account.security.vo.SecurityLoginInfoArg;
import com.binance.mgs.account.security.vo.SecurityLoginInfoRet;
import com.binance.mgs.account.security.vo.SecurityPassChallengeArg;
import com.binance.mgs.account.security.vo.SecurityPassChallengeRet;
import com.binance.mgs.account.security.vo.SecurityPreCheckArg;
import com.binance.mgs.account.security.vo.SecuritySelectChallengeArg;
import com.binance.mgs.account.security.vo.SecuritySelectChallengeRet;
import com.binance.mgs.account.security.vo.UserStatusCacheRet;
import com.binance.mgs.account.service.SecurityAppAttestService;
import com.binance.mgs.account.service.SecurityTokenService;
import com.binance.mgs.account.util.CaptchaCheckUtil;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.account.vo.GtCodeRet;
import com.binance.platform.mgs.constant.Constant;
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
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.binance.mgs.account.constant.BizType.FORGET_PASSWORD;
import static com.binance.mgs.account.constant.BizType.LOGIN;
import static com.binance.mgs.account.constant.BizType.REGISTER;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@RestController
@RequestMapping(value = "/v1/public/account")
@Slf4j
public class AccountSecurityPublicController extends BaseAction {

    private static final int SERIAL_NO_TIMEOUT = 30 * 60;

    @Value("${anti.bot.session.checked.timeout:1810}")
    private long sessionTimeOutAfterCheckSeconds;
    @Value("${ddos.captcha.check.count.limit:10}")
    private int captchaCheckLimit;

    @Value("${get.gtCode.verify.timeout:1000}")
    private Integer gtVerifyTimeOut;

    @Value("${anti.bot.reject.msg.show.switch:false}")
    private boolean rejectMsgShowSwitch;

    @Resource
    private AccountHelper accountHelper;
    @Autowired
    private AntiBotHelper antiBotHelper;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    private GeetestHelper geetestHelper;
    @Autowired
    private SecurityTokenService securityTokenService;
    @Autowired
    private SecurityAppAttestService securityAppAttestService;

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @AccountDefenseResource(name = "AccountSecurityPublicController.securityPreCheck")
    @DDoSPreMonitor(action = "securityPreCheck")
    @PostMapping(value = "/security/precheck")
    public CommonRet<SecurityCheckRet> securityPreCheck(@RequestBody @Validated SecurityPreCheckArg arg) throws Exception {
        SecurityPreCheckRet preCheckRet = antiBotHelper.timeOutSecurityPreCheck(arg);
        String captchaType = preCheckRet.getCaptchaType();
        if (preCheckRet.getCaptchaType().equals("random")) {
            preCheckRet.setNeedCheck(false);
        }

        SecurityCheckRet ret = new SecurityCheckRet();
        ret.setCaptchaType(captchaType);
        ret.setValidationTypes(preCheckRet.getValidationTypes());
        ret.setChallenge(preCheckRet.getChallenge());
        ret.setValidateId(preCheckRet.getValidateId());
        ret.setSessionId(antiBotHelper.createSecuritySessionId(captchaType, arg, preCheckRet));
        ret.setPless(preCheckRet.getIsPless());
        ret.setPlessVerifyTypeList(preCheckRet.getPlessVerifyTypeList());
        ret.setLoginFlowId(preCheckRet.getLoginFlowId());
        log.info("securityPreCheck ret={}, bizType={}", ret, arg.getBizType());
        return new CommonRet<>(ret);
    }

    @AntiBotCaptchaValidate(bizType = {LOGIN, FORGET_PASSWORD, REGISTER})
    @AccountDefenseResource(name = "AccountSecurityPublicController.securityBizCheck")
    @DDoSPreMonitor(action = "securityBizCheck")
    @PostMapping(value = "/security/bizCheck")
    public CommonRet<SecurityLoginInfoRet> securityBizCheck(@RequestBody @Validated SecurityLoginInfoArg arg) throws Exception {
        String email = arg.getEmail();
        String mobile = arg.getMobile();
        String mobileCode = arg.getMobileCode();
        if (StringUtils.isBlank(email) && StringUtils.isAnyBlank(mobile, mobileCode)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        if (StringUtils.isNotBlank(email) && !timeOutRegexUtils.validateEmail(email)) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        UserStatusCacheRet userStatusCache = antiBotHelper.getUserStatusCache(email, mobile, mobileCode, arg.getBizType());
        log.info("securityBizCheck bizType={}, userStatusCache={}", arg.getBizType(), userStatusCache);
        SecurityLoginInfoRet ret = new SecurityLoginInfoRet();
        ret.setValid(StringUtils.isNotBlank(userStatusCache.getUserIdStr()));
        ret.setDisable(userStatusCache.getIsDisableLogin());
        return new CommonRet<>(ret);
    }

    @AccountDefenseResource(name = "AccountSecurityPublicController.securityCheck")
    @DDoSPreMonitor(action = "securityCheck")
    @PostMapping(value = "/security/check/result")
    public CommonRet<SecurityCheckRet> securityCheck(@RequestBody @Validated SecurityCheckArg arg) throws Exception {
        String sessionId = arg.getSessionId();
        if (!antiBotHelper.checkSessionId(sessionId)) {
            log.warn("sessionId invalid {}", sessionId);
            ddosCacheSeviceHelper.banIp(WebUtils.getRequestIp());
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        CaptchaValidateInfo validateInfo = ddosCacheSeviceHelper.getValidateInfo(sessionId);
        if (validateInfo == null) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        log.info("securityCheck validateInfo={}", JsonUtils.toJsonNotNullKey(validateInfo));
        // 需要做人机
        if (validateInfo.getStatus() == AntiBotHelper.getVALIDATE_STATUS_NEED_CHECK()) {
            if (!CaptchaCheckUtil.checkCaptchaPattern(validateInfo.getCaptchaType(), arg)) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.CAPTCHA_ILLEGAL, WebUtils.getRequestIp(), captchaCheckLimit, String.format("securityCheck illegal arg=%s", JsonUtils.toJsonNotNullKey(arg)));
                throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
            }

            accountHelper.verifyCodeCacheAndBanIpFeedback(arg, validateInfo);
            validateInfo.setStatus(AntiBotHelper.getVALIDATE_STATUS_PASS());

            ddosCacheSeviceHelper.setValidateInfo(sessionId, validateInfo, sessionTimeOutAfterCheckSeconds);
        } else if (validateInfo.getStatus() == AntiBotHelper.getVALIDATE_STATUS_REJECT()) {
            log.warn("sessionId security reject status={}", validateInfo.getStatus());
            if (rejectMsgShowSwitch && StringUtils.isNotBlank(validateInfo.getRejectMsg())) {
                throw new BusinessException(AccountMgsErrorCode.ANTI_BOT_CHECK_REJECT.getCode(), validateInfo.getRejectMsg());
            }
            throw new BusinessException(AccountMgsErrorCode.REGISTER_ANTI_BOT_CHECK_FAILED);
        }
        return new CommonRet<>(new SecurityCheckRet(sessionId));
    }

    @AccountDefenseResource(name = "AccountSecurityPublicController.securityAppAttestCheck")
    @DDoSPreMonitor(action = "securityAppAttestCheck")
    @PostMapping(value = "/security/attest/check/result")
    public CommonRet<SecurityAppAttestCheckRet> securityAppAttestCheck(@RequestBody @Validated SecurityAppAttestCheckArg arg) {
        String sessionId = arg.getSessionId();
        if (!antiBotHelper.checkSessionId(sessionId)) {
            log.warn("sessionId invalid {}", sessionId);
            ddosCacheSeviceHelper.banIp(WebUtils.getRequestIp());
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        CaptchaValidateInfo validateInfo = ddosCacheSeviceHelper.getValidateInfo(sessionId);
        if (validateInfo == null) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        final SecurityAppAttestCheckRet response = new SecurityAppAttestCheckRet();
        SecurityAppAttestService.CheckAssertionResult result = securityAppAttestService.checkAndGetAssertionResult(arg);
        if (result.isAssertionAction() && result.isAssertionValid()) {
            validateInfo.setStatus(AntiBotHelper.getVALIDATE_STATUS_PASS());
            ddosCacheSeviceHelper.setValidateInfo(sessionId, validateInfo, sessionTimeOutAfterCheckSeconds);
        }
        response.setSessionId(arg.getSessionId());
        return new CommonRet<>(response);
    }

    /**
     * 获取极验验证码
     */
    @DDoSPreMonitor(action = "getGtCode")
    @AccountDefenseResource(name = "AccountSecurityPublicController.getGtCode")
    @GetMapping(value = "/security/gt-code")
    @CacheControl(noStore = true)
    public CommonRet<GtCodeRet> getGtCode(HttpServletRequest request, HttpServletResponse response) {
        // 获取用户真实IP地址
        String ip = WebUtils.getRequestIp();
        String serialNo = UUID.randomUUID().toString();
        try {
            CommonRet<GtCodeRet> ret = geetestHelper.getGtCodeAsync(ip, serialNo, request).get(gtVerifyTimeOut, TimeUnit.MILLISECONDS);
            BaseHelper.setCookie(request, response, true, Constant.COOKIE_GT_ID, serialNo, SERIAL_NO_TIMEOUT, true);
            return ret;
        } catch (TimeoutException e) {
            log.warn("getGtCode timeout", e);
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        } catch (Exception e) {
            log.warn("getGtCode exception:", e);
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
    }

    @AccountDefenseResource(name = "AccountSecurityPublicController.securitySelectChallenge")
    @DDoSPreMonitor(action = "securitySelectChallenge")
    @PostMapping(value = "/security/challenge/select")
    public CommonRet<SecuritySelectChallengeRet> securitySelectChallenge(@RequestBody @Validated SecuritySelectChallengeArg arg) {
        log.trace("securitySelectChallenge request [{}]", arg);

        SecuritySelectChallengeRet result = securityTokenService.selectChallenge(arg);

        log.trace("selectChallenge result [{}]", result);
        return new CommonRet<>(result);
    }

    @AccountDefenseResource(name = "AccountSecurityPublicController.securityPassChallenge")
    @DDoSPreMonitor(action = "securityPassChallenge")
    @PostMapping(value = "/security/challenge/pass")
    public CommonRet<SecurityPassChallengeRet> securityPassChallenge(@RequestBody @Validated SecurityPassChallengeArg arg) {
        log.trace("securityPassChallenge request [{}]", arg);

        if (!securityTokenService.isChallengeValid(arg)) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        if (!securityTokenService.isChallengePassed(arg)) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        String token = securityTokenService.issueToken();
        if (StringUtils.isBlank(token)) {
            log.error("Error while issuing token");
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
        SecurityPassChallengeRet result = new SecurityPassChallengeRet();
        result.setToken(token);

        log.trace("securityPassChallenge result [{}]", result);
        return new CommonRet<>(result);
    }
}
