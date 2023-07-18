package com.binance.mgs.account.advice;

import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.antibot.AntibotSessionCounterMetrics;
import com.binance.accountmonitorcenter.event.metrics.antibot.SessionIdHistogramMetrics;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.security.helper.AntiBotHelper;
import com.binance.mgs.account.security.vo.CaptchaValidateInfo;
import com.binance.mgs.account.util.VersionUtil;
import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Component
@Aspect
@Order(OrderConfig.AntiBotCaptchaValidate_ORDER)
public class AntiBotCaptchaValidateAspect {
    @Value("#{'${anti.bot.new.captcha.force.check.skip.method:}'.split(',')}")
    private List<String> forceCheckSkipMethod;

    @Value("#{'${login.second.check.params:mobileVerifyCode,googleVerifyCode,emailVerifyCode,yubikeyVerifyCode,verifyToken,fidoVerifyCode,fidoExternalVerifyCode}'.split(',')}")
    private List<String> secondLoginCheckParams;
    @Value("${anti.bot.params.check.switch:true}")
    private boolean needParamsCheck;
    @Value("${anti.bot.new.captcha.sessionid.count.check.switch:false}")
    private boolean sessionIdCountCheckSwitch;
    @Value("${anti.bot.session.checked.limit:10}")
    private int sessionIdCheckLimit;
    @Value("#{${anti.bot.captcha.verify.limit.map:{resetUserPassword:7}}}")
    private Map<String, Integer> captchaVerifyLimitMap;

    @Value("${anti.bot.new.captcha.sessionid.blank.check.switch:true}")
    private boolean sessionIdBlankCheckSwitch;

    @Autowired
    private AntiBotHelper antiBotHelper;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;

    @Autowired
    private MetricsEventPublisher metricsEventPublisher;

    @Around("@annotation(antiBotValidate)")
    public Object preCheck(ProceedingJoinPoint pjp, AntiBotCaptchaValidate antiBotValidate) throws Throwable {
        try {
            Map<String, Object> params = Maps.newHashMap();
            for (Object arg : pjp.getArgs()) {
                if (arg instanceof HttpServletRequest) {
                    continue;
                }
                if (arg instanceof HttpServletResponse) {
                    continue;
                }
                String argsJson = JsonUtils.toJsonNotNullKey(arg);
                params.putAll(JsonUtils.toMap(argsJson, String.class, Object.class));
            }

            String[] bizTypeArr = antiBotValidate.bizType();
            String sessionId = Optional.ofNullable(params.get("sessionId")).orElse("").toString();
            MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
            Method method = methodSignature.getMethod();
            String methodName = method.getName();
            String captchaVerifyLimitKey = StringUtils.defaultIfBlank(antiBotValidate.name(), methodName);

            if (StringUtils.isNotBlank(sessionId)) {
                if (!antiBotHelper.checkSessionId(sessionId)) {
                    log.warn("illegal sessionId = {}, ip = {}", sessionId, WebUtils.getRequestIp());
                    // todo ban ip ?
                    metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("fake").build());
                    throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
                }
                Long sessionIdVerifyCount = ddosCacheSeviceHelper.incrGtForbiddenCache(sessionId);
                SessionIdHistogramMetrics.SessionIdHistogramMetricsBuilder sessionIdHistogramBuilder
                        = SessionIdHistogramMetrics.builder().methodName(captchaVerifyLimitKey).number(sessionIdVerifyCount);
                Integer captchaVerifyLimit = captchaVerifyLimitMap.getOrDefault(captchaVerifyLimitKey, 5);
                if (sessionIdVerifyCount > captchaVerifyLimit) {
                    log.warn("sessionId validate over limit, sessionId = {} verifyCount={}", sessionId, sessionIdVerifyCount);
                    if (sessionIdCountCheckSwitch && sessionIdVerifyCount > sessionIdCheckLimit) {
                        ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.CAPTCHA_ILLEGAL, WebUtils.getRequestIp(), 0, "sessionId over ban limit");
                    }
                    metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("overlimit").build());
                    metricsEventPublisher.publish(sessionIdHistogramBuilder.success(String.valueOf(Boolean.FALSE)).build());
                    throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
                }
                metricsEventPublisher.publish(sessionIdHistogramBuilder.success(String.valueOf(Boolean.TRUE)).build());

                CaptchaValidateInfo validateInfo = ddosCacheSeviceHelper.getValidateInfo(sessionId);
                if (validateInfo == null) {
                    log.warn("sessionId expired sessionId = {}", sessionId);
                    metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("expired").build());
                    throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
                }
                if (validateInfo.getStatus() != 1) {
                    log.warn("sessionId status error sessionId = {}, status = {}", sessionId, validateInfo.getStatus());
                    metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result(String.valueOf(validateInfo.getStatus())).build());
                    throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
                }
                if (bizTypeArr.length > 0) {
                    boolean isBizTypeValid = false;
                    for (String biz : antiBotValidate.bizType()) {
                        if (validateInfo.getBizType().equalsIgnoreCase(biz)) {
                            isBizTypeValid = true;
                            break;
                        }
                    }
                    if (!isBizTypeValid) {
                        log.warn("sessionId bizType not match, bizType={} sessionId = {}", validateInfo.getBizType(), sessionId);
                        metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("bizNotMatch").build());
                        throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
                    }
                }
                if (needParamsCheck && !validateUserInfo(params, validateInfo, (MethodSignature) pjp.getSignature())) {
                    log.warn("user params not match");
                    metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("paramNotMatch").build());
                    throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
                }

                AntiBotHelper.setThreadLocalSessionId(sessionId);
            } else {

                // 没有传sessionId，判断是否老版本，走老的流程
                if (!antiBotHelper.getNewCaptchaValidateSwitch()) {
                    metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("success_blank").build());
                    return pjp.proceed();
                }

                // 新版本没传sessionId，直接报错
                if (CollectionUtils.isEmpty(forceCheckSkipMethod) || !forceCheckSkipMethod.contains(methodName)) {
                    sessionIdBlankForceCheck(params, methodName);
                }
            }
            metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("success").build());

            return pjp.proceed();
        } finally {
            AntiBotHelper.getAndClearThreadLocalSessionId();
        }
    }

    private void sessionIdBlankForceCheck(Map<String, Object> params, String methodName) {
        TerminalEnum terminal = WebUtils.getTerminal();
        // 登录接口第二次不需要做人机，但是2fa等验证参数必然不会为空
        if (methodName.equals("login")) {
            boolean isAllBlank = true;
            for (String paramName : secondLoginCheckParams) {
                if (StringUtils.isNotBlank(String.valueOf(params.getOrDefault(paramName, "")))) {
                    isAllBlank = false;
                    break;
                }
            }
            if (isAllBlank) {
                log.warn("sessionId blank, methodName={}, terminal={}, version={}", methodName, terminal, VersionUtil.getVersion(terminal));
                if (sessionIdBlankCheckSwitch) {
                    ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.SESSION_ID_ILLEGAL, WebUtils.getRequestIp(), 0, "sessionId blank");
                }
                metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("blank_2login").build());
                throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
            }
        } else {
            log.warn("sessionId blank, methodName={}, terminal={}, version={}", methodName, terminal, VersionUtil.getVersion(terminal));
            if (sessionIdBlankCheckSwitch) {
                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.SESSION_ID_ILLEGAL, WebUtils.getRequestIp(), 0, "sessionId blank");
            }
            metricsEventPublisher.publish(AntibotSessionCounterMetrics.builder().result("blank").build());
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
    }

    /**
     * 这里校验各个业务的参数绑定
     */
    private boolean validateUserInfo(Map<String, Object> params, CaptchaValidateInfo validateInfo, MethodSignature methodSignature) {
        String bizType = StringUtils.defaultString(validateInfo.getBizType(), "");
        log.info("validateUserInfo bizType={}", bizType);
        switch (bizType.toLowerCase()) {
            case BizType.LOGIN:
            case BizType.REGISTER:
            case BizType.FORGET_PASSWORD:
                return validateMobileOrEmail(validateInfo, params);
            case BizType.REFRESH_ACCESS_TOKEN:
                return validateRefreshToken(validateInfo, params);
            case BizType.CREATE_APIKEY:
                return validateCreateSubApiKey(validateInfo, params);
            case BizType.THIRD_LOGIN:
                //只校验google，apple登陆，三方注册忽略
                if (methodSignature != null && !methodSignature.getMethod().getName().equalsIgnoreCase("registerByThird")) {
                    return validateThirdLogin(validateInfo, params);
                }
                return true;
            default:
                log.info("invalid bizType {}", validateInfo.getBizType());
                return false;
        }
    }

    private boolean validateMobileOrEmail(CaptchaValidateInfo validateInfo, Map<String, Object> params) {
        if (StringUtils.isAllBlank(validateInfo.getEmail(), validateInfo.getMobile(), validateInfo.getMobileCode())) {
            log.info("email or mobile empty bizType={}", validateInfo.getBizType());
            return false;
        }
        if (StringUtils.isBlank(validateInfo.getEmail()) && StringUtils.isAnyBlank(validateInfo.getMobile(), validateInfo.getMobileCode())) {
            log.info("mobile or mobileCode empty bizType={}", validateInfo.getBizType());
            return false;
        }

        if (StringUtils.isNotBlank(validateInfo.getEmail()) && !StringUtils.equals(String.valueOf(params.get("email")), validateInfo.getEmail())) {
            log.info("email not match {} {} bizType={}", validateInfo.getEmail(), params.get("email"), validateInfo.getBizType());
            return false;
        }
        if (StringUtils.isNotBlank(validateInfo.getMobile()) && !StringUtils.equals(String.valueOf(params.get("mobile")), validateInfo.getMobile())) {
            log.info("mobile not match {} {} bizType={}", validateInfo.getMobile(), params.get("mobile"), validateInfo.getBizType());
            return false;
        }
        if (StringUtils.isNotBlank(validateInfo.getMobileCode()) && !StringUtils.equals(String.valueOf(params.get("mobileCode")), validateInfo.getMobileCode())) {
            log.info("mobileCode not match {} {} bizType={}", validateInfo.getMobileCode(), params.get("mobileCode"), validateInfo.getBizType());
            return false;
        }
        return true;
    }

    private boolean validateCreateSubApiKey(CaptchaValidateInfo validateInfo, Map<String, Object> params) {
        // 校验子账户邮箱
        if (StringUtils.isNotBlank(validateInfo.getSubUserEmail()) && !StringUtils.equals(validateInfo.getSubUserEmail(), String.valueOf(params.get("subUserEmail")))) {
            log.info("subUserEmail not match {} {}", validateInfo.getSubUserEmail(), params.get("subUserEmail"));
            return false;
        }
        return true;
    }

    private boolean validateRefreshToken(CaptchaValidateInfo validateInfo, Map<String, Object> params) {
        if (StringUtils.isBlank(validateInfo.getRefreshToken()) || !StringUtils.equals(String.valueOf(params.get("refreshToken")), validateInfo.getRefreshToken())) {
            log.info("refreshToken not match {} {}", validateInfo.getRefreshToken(), params.get("refreshToken"));
            return false;
        }
        return true;
    }

    private boolean validateThirdLogin(CaptchaValidateInfo validateInfo, Map<String, Object> params) {
        if (StringUtils.isBlank(validateInfo.getIdToken()) || !StringUtils.equals(String.valueOf(params.get("idToken")), validateInfo.getIdToken())) {
            log.info("idToken not match {} {}", validateInfo.getIdToken(), params.get("idToken"));
            return false;
        }
        return true;
    }
}
