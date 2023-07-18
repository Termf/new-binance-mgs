package com.binance.mgs.account.Interceptor;

import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.MethodMobileCodeCounterMetrics;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.PublicMethodCounterMetrics;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.platform.openfeign.body.CustomBodyServletRequestWrapper;
import com.binance.platform.openfeign.body.CustomeHeaderServletRequestWrapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ErrorMetricsExceptionResolver implements HandlerExceptionResolver {

    @Autowired
    protected MetricsEventPublisher metricsEventPublisher;

    @Value("${enable.ErrorMetricsExceptionResolver:true}")
    private boolean enableResolver;

    @Value("#{'${mobile.code.action.list:login,sendMobileVerifyCode}'.split(',')}")
    private Set<String> mobileCodeActionList;

    @Value("${old.metrics.switch:true}")
    private boolean oldMetricsSwitch;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!enableResolver) {
            return null;
        }
        try {
            if (Objects.isNull(handler)) {
                log.warn("handler is null", ex);
                return null;
            }
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            if (!method.isAnnotationPresent(DDoSPreMonitor.class)) {
                return null;
            }

            String methodName = method.getName();
            DDoSPreMonitor dDoSPreMonitor = method.getAnnotation(DDoSPreMonitor.class);
            String action = dDoSPreMonitor.action();
            if (StringUtils.isBlank(action)) {
                action = methodName;
            }

            Map<String, Object> params = Maps.newHashMap();

            if (request instanceof CustomBodyServletRequestWrapper) {
                CustomBodyServletRequestWrapper customBodyServletRequestWrapper = (CustomBodyServletRequestWrapper) request;
                String argsJson = new String(customBodyServletRequestWrapper.getBody());
                params.putAll(JsonUtils.toMap(argsJson, String.class, Object.class));
            } else if (request instanceof CustomeHeaderServletRequestWrapper) {
                // 没走body参数的 case
                params.putAll(request.getParameterMap());
            }

            String requestBizScene = getDefaultBizScene(params, method);


            handleExceptionByMethodAndBizScene(ex, action, requestBizScene,params);
        } catch (Exception e) {
            log.warn("resolveException failed", e);
        }
        return null;
    }

    private void handleExceptionByMethodAndBizScene(Exception e, String action, String requestBizScene, Map<String, Object> params) {
        String mobileCode = StringUtils.defaultString(String.valueOf(params.get("mobileCode")), "unknown");
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            String errorCode = businessException.getErrorCode() == null ? businessException.getBizCode() : businessException.getErrorCode().getCode();
            publishMetrics(mobileCode, requestBizScene, action, errorCode);
        } else {
            // 非业务异常,没有错误码,直接记异常名
            publishMetrics(mobileCode, requestBizScene, action, e.getClass().getSimpleName());
        }
    }

    private void publishMetrics(String mobileCode, String requestBizScene, String action, String errorCode) {
        if (mobileCodeActionList.contains(action)) {
            metricsEventPublisher.publish(MethodMobileCodeCounterMetrics.builder().bizScene(requestBizScene).methodName(action).callStatus("fail").errorCode(errorCode).mobileCode(mobileCode).build());
            if (oldMetricsSwitch) {
                metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(requestBizScene).methodName(action).callStatus("fail").errorCode(errorCode).build());
            }
        } else {
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(requestBizScene).methodName(action).callStatus("fail").errorCode(errorCode).build());
        }
    }

    /**
     * 获取参数中的默认场景，无则用方法名称替代
     */
    private String getDefaultBizScene(Map<String, Object> params, Method method) {
        String bizScene = (String) params.get("bizScene");
        if (StringUtils.isBlank(bizScene)) {
            bizScene = (String) params.get("thirdOperatorEnum");
            if (StringUtils.isBlank(bizScene)) {
                bizScene = method.getName();
            }
        }
        return bizScene;
    }
}
