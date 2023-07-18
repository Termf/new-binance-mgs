package com.binance.mgs.account.advice;


import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.MethodMobileCodeCounterMetrics;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.PublicMethodCounterMetrics;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.account.vo.SendVerifyCodeResponse;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/08/26
 */
@Slf4j
@Service
@Aspect
@Order(OrderConfig.DDoSPreMonitorAspect_ORDER)
public class DDoSPreMonitorAspect {

    @Value("${ddos.precheck.switch:false}")
    private boolean ddosPreCheckSwitch;

    @Value("#{'${ddos.unwrap.errorcode.list:}'.split(',')}")
    private Set<String> unWrapErrorCodes; //特定需要直接抛出的errorCode
    @Autowired
    protected MetricsEventPublisher metricsEventPublisher;

    @Value("${enable.ErrorMetricsExceptionResolver:true}")
    private boolean enableResolver;

    // 需要同时记录手机的 action
    @Value("#{'${mobile.code.action.list:login,sendMobileVerifyCode}'.split(',')}")
    private Set<String> mobileCodeActionList;

    @Value("${old.metrics.switch:true}")
    private boolean oldMetricsSwitch;


    @Around("@annotation(dDoSPreMonitor)")
    public Object preCheck(ProceedingJoinPoint pjp, DDoSPreMonitor dDoSPreMonitor) throws Throwable {
        if (!ddosPreCheckSwitch) {
            return pjp.proceed();
        }

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String methodName = method.getName();
        Map<String, Object> params = Maps.newHashMap();
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof HttpServletRequest) {
                continue;
            }
            if (arg instanceof HttpServletResponse) {
                continue;
            }

            String argsJson = JsonUtils.toJsonNotNullKey(arg);
            params.putAll(JsonUtils.toMap(argsJson, String.class, Object.class)); // 拿到request里的请求参数
        }

        String action = dDoSPreMonitor.action();
        if (StringUtils.isBlank(action)) {
            action = methodName;
        }
        String requestBizScene = getDefaultBizScene(params, method);

        try {
            return handleResultByMethodAndBizScene(pjp.proceed(), methodSignature, action, requestBizScene, params);
        } catch (BusinessException e) {
            return handleExceptionByMethodAndBizScene(e, methodSignature, action, requestBizScene);
        }
    }

    private Object handleResultByMethodAndBizScene(Object result, MethodSignature method, String action, String requestBizScene, Map<String, Object> params) {
        if (mobileCodeActionList.contains(action)) {
            String mobileCode = StringUtils.defaultString(String.valueOf(params.get("mobileCode")), "unknown");
            metricsEventPublisher.publish(MethodMobileCodeCounterMetrics.builder().bizScene(requestBizScene).methodName(action).mobileCode(mobileCode).build());
            if (oldMetricsSwitch) {
                metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(requestBizScene).methodName(action).build());
            }
        } else {
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(requestBizScene).methodName(action).build());
        }
        if (requestBizScene.equalsIgnoreCase(BizSceneEnum.FORGET_PASSWORD.name())
                && StringUtils.equalsAnyIgnoreCase(method.getName(), "sendMobileVerifyCode", "sendEmailVerifyCode")) {
            CommonRet<SendVerifyCodeResponse> commonRet = (CommonRet<SendVerifyCodeResponse>) result;
            commonRet.setMessage(AccountMgsErrorCode.VERIFY_CODE_IS_SEND.getMessage());
            return commonRet;
        }
        // todo 注册、登录
        return result;
    }

    private Object handleExceptionByMethodAndBizScene(BusinessException e, MethodSignature method, String action, String requestBizScene) {
        String errorCode = e.getErrorCode() == null ? e.getBizCode() : e.getErrorCode().getCode();
        if (!enableResolver) {
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(requestBizScene).methodName(action).callStatus("fail").errorCode(errorCode).build());
        }
        if ((e.getErrorCode() != null && unWrapErrorCodes.contains(e.getErrorCode().getCode()))
                || (StringUtils.isNotBlank(e.getBizCode()) && unWrapErrorCodes.contains(e.getBizCode()))) {
            throw e;
        }

        if (StringUtils.equalsAnyIgnoreCase(requestBizScene, BizSceneEnum.FORGET_PASSWORD.name())
                && StringUtils.equalsAnyIgnoreCase(method.getName(), "sendMobileVerifyCode", "sendEmailVerifyCode")) {
            CommonRet<SendVerifyCodeResponse> commonRet = new CommonRet<>();
            if (method.getName().equals("sendEmailVerifyCode")) {
                SendVerifyCodeResponse response = new SendVerifyCodeResponse();
                response.setExpireTime(60L);
                commonRet.setData(response);
            }
            commonRet.setMessage(AccountMgsErrorCode.VERIFY_CODE_IS_SEND.getMessage());
            return commonRet;
        }
        // todo 注册、登录
        throw e;
    }

    /**
     * 获取参数中的默认场景，无则用方法名称替代
     */
    private String getDefaultBizScene(Map<String, Object> params, Method method) {
        String bizScene = (String) params.get("bizScene");
        if (StringUtils.isBlank(bizScene)) {
            bizScene = (String) params.get("bizId");
        }
        if (StringUtils.isBlank(bizScene)) {
            bizScene = (String) params.get("thirdOperatorEnum");
            if (StringUtils.isBlank(bizScene)) {
                bizScene = method.getName();
            }
        }
        return bizScene;
    }
}
