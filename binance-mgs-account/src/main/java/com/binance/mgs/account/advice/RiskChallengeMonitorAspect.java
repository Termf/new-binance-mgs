package com.binance.mgs.account.advice;

import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.PublicMethodCounterMetrics;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * @author rudy.c
 * @date 2022-12-23 15:07
 */
@Slf4j
@Service
@Aspect
@Order(OrderConfig.DDoSPreMonitorAspect_ORDER)
public class RiskChallengeMonitorAspect {
    private static final String MONITOR_METHOD_NAME_PREFIX = "riskChallenge";
    private static final String FIRST_REQUEST_MONITOR_NAME = MONITOR_METHOD_NAME_PREFIX + ".first";
    private static final String SECOND_REQUEST_MONITOR_NAME = MONITOR_METHOD_NAME_PREFIX + ".second";
    @Value("${risk.challenge.monitor.switch:true}")
    private boolean riskChallengeMonitorSwitch;
    @Autowired
    protected MetricsEventPublisher metricsEventPublisher;

    @Around("@annotation(riskChallengeMonitor)")
    public Object around(ProceedingJoinPoint pjp, RiskChallengeMonitor riskChallengeMonitor) throws Throwable {
        if (!riskChallengeMonitorSwitch) {
            return pjp.proceed();
        }

        Object[] args = pjp.getArgs();
        String bizScene = (args != null && args.length >= 3) ? (String)args[2] : null;

        String riskChallengeBizNo = WebUtils.getHeader("risk_challenge_biz_no");
        boolean isFirstRequest = StringUtils.isBlank(riskChallengeBizNo);
        String methodName = isFirstRequest ? FIRST_REQUEST_MONITOR_NAME : SECOND_REQUEST_MONITOR_NAME;
        try {
            Object result = pjp.proceed();
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(bizScene).methodName(methodName).build());
            return result;
        } catch (BusinessException e) {
            String errorCode = e.getErrorCode() == null ? e.getBizCode() : e.getErrorCode().getCode();
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(bizScene).methodName(methodName).callStatus("fail").errorCode(errorCode).build());
            throw e;
        } catch (Exception e) {
            // 非业务异常,没有错误码,直接记异常名
            String errorCode = e.getClass().getSimpleName();
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(bizScene).methodName(methodName).callStatus("fail").errorCode(errorCode).build());
            throw e;
        }
    }
}
