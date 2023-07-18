package com.binance.mgs.account.advice;

import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.PublicMethodCounterMetrics;
import com.binance.master.error.BusinessException;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author rudy.c
 * @date 2022-12-16 18:00
 */
@Slf4j
@Service
@Aspect
@Order(OrderConfig.DDoSPreMonitorAspect_ORDER)
public class QrCodeLoginQueryMonitorAspect {
    private static final String MONITOR_METHOD_NAME = "qrCode.loginQuery";
    @Value("${qrcode.login.query.monitor.switch:true}")
    private boolean qrCodeLoginQueryMonitorSwitch;
    @Value("#{'${qrcode.login.query.monitor.status.list:SCAN,CONFIRM}'.split(',')}")
    private Set<String> monitorStatus; //需要记录的状态值
    @Autowired
    protected MetricsEventPublisher metricsEventPublisher;

    @Around("@annotation(qrCodeLoginQueryMonitor)")
    public Object around(ProceedingJoinPoint pjp, QrCodeLoginQueryMonitor qrCodeLoginQueryMonitor) throws Throwable {
        if (!qrCodeLoginQueryMonitorSwitch) {
            return pjp.proceed();
        }
        Object result;
        try {
            result = pjp.proceed();
            if(!(result instanceof CommonRet) ) {
                return result;
            }
            Object data = ((CommonRet<?>)result).getData();
            if(!(data instanceof QrCodeQueryRet)) {
                return result;
            }
            QrCodeQueryRet qrCodeQueryRet = (QrCodeQueryRet)data;
            if(CollectionUtils.isNotEmpty(monitorStatus) && monitorStatus.contains(qrCodeQueryRet.getStatus())) {
                metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().bizScene(qrCodeQueryRet.getStatus()).methodName(MONITOR_METHOD_NAME).build());
            }
            return result;
        } catch (BusinessException e) {
            String errorCode = e.getErrorCode() == null ? e.getBizCode() : e.getErrorCode().getCode();
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().methodName(MONITOR_METHOD_NAME).callStatus("fail").errorCode(errorCode).build());
            throw e;
        } catch (Exception e) {
            // 非业务异常,没有错误码,直接记异常名
            String errorCode = e.getClass().getSimpleName();
            metricsEventPublisher.publish(PublicMethodCounterMetrics.builder().methodName(MONITOR_METHOD_NAME).callStatus("fail").errorCode(errorCode).build());
            throw e;
        }
    }
}
