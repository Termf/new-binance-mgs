package com.binance.mgs.account.account.helper;

import com.binance.account.api.UserApi;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.antibot.CaptchaCounterMetrics;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.dto.SubAccountStatusDto;
import com.binance.mgs.account.account.enums.CaptchaType;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.security.helper.AntiBotHelper;
import com.binance.mgs.account.security.vo.CaptchaValidateInfo;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import com.binance.platform.mgs.constant.Constant;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AccountHelper extends BaseHelper {
    @Resource
    private UserApi userApi;
    @Resource
    private BCaptchaHelper bCaptchaHelper;

    @Value("${check.android.gt.flag:false}")
    private boolean checkAndroidGtFlag;

    @Value("${verify.captcha.non-prod:false}")
    private boolean verifyCaptchaNonProd; // 为了直接测试方便
    @Value("${verify.captcha.old.switch:true}")
    private boolean verifyCaptchaOldSwitch;

    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    private MetricsEventPublisher metricsEventPublisher;
    @Autowired
    private GeetestHelper geetestHelper;
    @Autowired
    private GoogleRecaptchaHelper googleRecaptchaHelper;

    @Autowired
    private AntiBotHelper antiBotHelper;

    @Value("${bCaptcha.verify.timeout:500}")
    private Integer bCaptchaVerifyTimeOut;
    @Value("${captcha.verify.limit:5}")
    private int captchaVerifyLimit;
    @Value("${verifyCode.ip.limit.count:10}")
    private int ipLimitCount;

    @Value("${remove.gt.switch:true}")
    private boolean removeGt;

    @Value("${anti.bot.header.preCheck.switch:false}")
    private boolean headerPreCheckSwitch;


    public APIResponse<UserVo> getUserById(Long userId) throws Exception {
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<UserVo> apiResponse = userApi.getUserByRootUserId(getInstance(userIdRequest));
        checkResponse(apiResponse);
        return apiResponse;
    }

    /**
     * 判断当前登录用户是否为子账户
     *
     * @return
     */
    public boolean isSubUser() {
        Long userId = getUserId();
        if (userId == null) {
            // 调用该方法必然意味用户已登录，若userId为空，则说明参数异常或者不该调用此方法，故统一提示参数异常
            log.warn("isSubUser userId is null");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        SubAccountStatusDto dto = getSubAccountStatusByUserId(userId);
        return dto.isSubUser();
    }

    /**
     * 判断当前登录用户是否为子账户,排除资管子账户
     *
     * @return
     */
    public boolean isSubUserExcludeAssetSub() {
        Long userId = getUserId();
        if (userId == null) {
            // 调用该方法必然意味用户已登录，若userId为空，则说明参数异常或者不该调用此方法，故统一提示参数异常
            log.warn("isSubUser userId is null");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        SubAccountStatusDto dto = getSubAccountStatusByUserId(userId);
        if (dto.isAssetSubUser()) {
            return false;
        }
        return dto.isSubUser();
    }


    /**
     * 获取子母账户相关状态
     *
     * @param userId
     * @return
     */
    public SubAccountStatusDto getSubAccountStatusByUserId(Long userId) {
        if (userId == null) {
            log.warn("getSubAccountStatusByUserId userId is null");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        UserIdRequest request = new UserIdRequest();
        request.setUserId(userId);
        APIResponse<UserStatusEx> apiResponse = null;
        try {
            apiResponse = userApi.getUserStatusByUserId(getInstance(request));
        } catch (Exception e) {
            log.warn("get base detail fail,userId={}", userId, e);
        }
        checkResponse(apiResponse);
        UserStatusEx userStatusEx = apiResponse.getData();
        if (userStatusEx == null) {
            throw new BusinessException(GeneralCode.USER_NOT_EXIST);
        }
        SubAccountStatusDto dto = new SubAccountStatusDto();
        dto.setSubUserFunctionEnabled(userStatusEx.getIsSubUserFunctionEnabled());
        dto.setSubUser(userStatusEx.getIsSubUser());
        dto.setSubUserEnabled(userStatusEx.getIsSubUserEnabled());
        dto.setAssetSubUser(userStatusEx.getIsAssetSubUser());
        return dto;
    }

    public String getRegChannel(String registerChannel) {
        if (registerChannel == null) {
            return null;
        } else if (registerChannel.length() > 32) {
            registerChannel = StringUtils.substring(registerChannel, 0, 32);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < registerChannel.length(); i++) {
            char c = registerChannel.charAt(i);
            if (Character.isDigit(c) || Character.isLetter(c) || c == '_' || c == '.' || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * reCAPTCHA验证，5条命
     */
    private void validateReCaptchaCache(ValidateCodeArg validateCodeArg, String bizId) {
        String switchStr = sysConfigHelper.getCodeByDisplayName("re_captcha_switch");
        if ("off".equalsIgnoreCase(switchStr)) {
            log.info("validateCodeByGeetest reCAPTCHA closed");
            throw new BusinessException("reCAPTCHA closed.");
        }

        String recaptchaResponse = validateCodeArg.getRecaptchaResponse();
        String siteKey = validateCodeArg.getSiteKey();
        if (StringUtils.isBlank(recaptchaResponse)) {
            log.error("reCAPTCHA验证失败，recaptchaResponse为空");
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
        Long verifyCount = ddosCacheSeviceHelper.incrGtForbiddenCache(recaptchaResponse);
        boolean verifySuccess = false;
        if (verifyCount > 1 && verifyCount <= captchaVerifyLimit) {
            verifySuccess = ddosCacheSeviceHelper.getVerifyResult(recaptchaResponse);
        }

        if (verifySuccess && verifyCount < captchaVerifyLimit) {
            return;
        }

        if (verifyCount < captchaVerifyLimit) {
            boolean passVerify = googleRecaptchaHelper.timeOutRecaptchaAssessment(recaptchaResponse, siteKey, bizId);
            if (!passVerify) {
                ddosCacheSeviceHelper.setVerifyResult(recaptchaResponse, false);
                throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
            } else {
                ddosCacheSeviceHelper.setVerifyResult(recaptchaResponse, true);
            }
        } else {
            log.info("reCAPTCHA over limit, token={} verifyCount={}", recaptchaResponse, verifyCount);
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
    }

    private void validateGtCaptchaCache(ValidateCodeArg validateCodeArg) {
        if (removeGt) {
            throw new BusinessException(MgsErrorCode.NEED_CHECK_ROBOT);
        }
        // 极验验证
        String gtId = StringUtils.defaultIfBlank(getCookieValue(WebUtils.getHttpServletRequest(), Constant.COOKIE_GT_ID),
                validateCodeArg.getGtId());
        if (StringUtils.isEmpty(gtId)) {
            log.info("cookie gtid is null,userId={}", getUserIdStr());
            throwValidateFailureError();
        }
        Long gtVerifyCount = ddosCacheSeviceHelper.incrGtForbiddenCache(gtId);
        boolean verifySuccess = false;
        if (gtVerifyCount > 1) {
            verifySuccess = ddosCacheSeviceHelper.getVerifyResult(gtId);
        }

        if (verifySuccess && gtVerifyCount < captchaVerifyLimit) {
            return;
        }

        if (gtVerifyCount < captchaVerifyLimit) {
            if (!geetestHelper.validateCodeByGeetest(validateCodeArg)) {
                throwValidateFailureError();
            }
        } else {
            log.info("gt over limit, gtId={} verifyCount={}", gtId, gtVerifyCount);
            throwValidateFailureError();
        }
    }

    /**
     * 验证码校验,带缓存的逻辑，有5条命，一个验证码
     */
    public void verifyCodeCache(ValidateCodeArg validateCodeArg, String bizId, boolean bindSessionId) {
        String validateCodeType = validateCodeArg.getValidateCodeType();
        log.info("start verify, validateCodeType={},bizId={}", validateCodeType, bizId);
        metricsEventPublisher.publish(CaptchaCounterMetrics.builder().bizType(bizId).captchaType(validateCodeType).captchaStatus("start").bindSessionId(bindSessionId).build());

        if (headerPreCheckSwitch) {
            String clientType = WebUtils.getClientType();
            if (StringUtils.isBlank(clientType)) {
                throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
            }
        }

        if (verifyByEnv()) {
            if (StringUtils.equals(CaptchaType.gt.name(), validateCodeType)) {
                validateGtCaptchaCache(validateCodeArg);
            } else if (StringUtils.equals(CaptchaType.reCAPTCHA.name(), validateCodeType)) {
                validateReCaptchaCache(validateCodeArg, bizId);
            } else if (StringUtils.equalsAny(validateCodeType, CaptchaType.bCAPTCHA.name(), CaptchaType.bCAPTCHA2.name())) {
                validateBCaptchaCache(validateCodeArg, bizId);
            } else {
                throw new BusinessException(MgsErrorCode.NEED_CHECK_ROBOT);
            }
        }
        log.info("end verify, validateCodeType={},bizId={}", validateCodeType, bizId);
        metricsEventPublisher.publish(CaptchaCounterMetrics.builder().bizType(bizId).captchaType(validateCodeType).captchaStatus("success").bindSessionId(bindSessionId).build());
    }

    private boolean verifyByEnv() {
        if (verifyCaptchaOldSwitch) {
            return !StringUtils.equalsAnyIgnoreCase(System.getProperty("env"), "local", "dev", "qa")
                    || StringUtils.equals("ON", sysConfigHelper.getCodeByDisplayName("test_to_verify"));
        }

        if (EnvUtil.isDev() || EnvUtil.isQa()) {
            return verifyCaptchaNonProd;
        }

        return EnvUtil.isProd() && StringUtils.equals("ON", sysConfigHelper.getCodeByDisplayName("test_to_verify"));
    }

    /**
     * 验证码校验,带缓存的逻辑，有5条命，一个验证码
     */
    public void verifyCodeCacheAndBanIp(ValidateCodeArg validateCodeArg, String bizId, boolean bindSessionId) {
        String sessionId = AntiBotHelper.getThreadLocalSessionId();
        if (StringUtils.isNotBlank(sessionId)) {
            // sessionId不为空表示走新流程，并且aop已经验过了
            return;
        }

        String ip = WebUtils.getRequestIp();
        try {
            verifyCodeCache(validateCodeArg, bizId, bindSessionId);
        } catch (Exception e) {
            ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.CAPTCHA_VERIFY_ERROR, ip, ipLimitCount, String.format("captcha verify cache terminal=%s bizId=%s arg=%s ", WebUtils.getTerminal(), bizId, JsonUtils.toJsonNotNullKey(validateCodeArg)));
            throw e;
        }
    }

    public void verifyCodeCacheAndBanIpFeedback(ValidateCodeArg validateCodeArg, CaptchaValidateInfo validateInfo) {
        try {
            verifyCodeCache(validateCodeArg, validateInfo.getBizType(), true);
            antiBotHelper.securityPreCheckFeedback(validateInfo, true);
        } catch (Exception e) {
            antiBotHelper.securityPreCheckFeedback(validateInfo, false);
            ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.CAPTCHA_VERIFY_ERROR, WebUtils.getRequestIp(), ipLimitCount, String.format("captcha verify cache terminal=%s bizId=%s arg=%s ", WebUtils.getTerminal(), validateInfo.getBizType(), JsonUtils.toJsonNotNullKey(validateCodeArg)));
            throw e;
        }
    }

    public void verifyCodeCacheAndBanIp(ValidateCodeArg validateCodeArg, String bizId) {
        // 人机 旧流程调用
        verifyCodeCacheAndBanIp(validateCodeArg, bizId, false);
    }

    private void throwValidateFailureError() {
        if (checkAndroidGtFlag && isAndroid()) {
            throw new BusinessException(MgsErrorCode.NEED_CHECK_ROBOT);
        } else {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
    }

    private void validateBCaptchaCache(ValidateCodeArg validateCode, String bizId) {
        String captchaToken = validateCode.getBCaptchaToken();
        if (StringUtils.isBlank(captchaToken)) {
            log.info("bCaptchaToken is null,userId={}", getUserIdStr());
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }

        Long captchaVerifyCount = ddosCacheSeviceHelper.incrGtForbiddenCache(captchaToken);
        boolean verifySuccess = false;
        if (captchaVerifyCount > 1 && captchaVerifyCount <= captchaVerifyLimit) {
            verifySuccess = ddosCacheSeviceHelper.getVerifyResult(captchaToken);
        }

        if (verifySuccess && captchaVerifyCount < captchaVerifyLimit) {
            return;
        }

        if (captchaVerifyCount < captchaVerifyLimit) {
            boolean result = bCaptchaHelper.validate(validateCode.getBCaptchaToken(), bizId, bCaptchaVerifyTimeOut, TimeUnit.MILLISECONDS);
            if (!result) {
                ddosCacheSeviceHelper.setVerifyResult(captchaToken, false);
                throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
            }
            ddosCacheSeviceHelper.setVerifyResult(captchaToken, true);
        } else {
            log.info("bCaptcha over limit, captchaToken={} verifyCount={}", captchaToken, captchaVerifyCount);
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
    }

    public String getBncUuidFromRequest(HttpServletRequest request){
        if (request == null){
            return null;
        }
        String bncuuid = request.getHeader(LocalLogKeys.BNCUUID);
        if (StringUtils.isBlank(bncuuid)) {
            bncuuid = request.getHeader(LocalLogKeys.BNC_UUID);
        }
        return bncuuid;
    }

}
