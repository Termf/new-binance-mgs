package com.binance.mgs.account.advice;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.userbigdata.vo.kyc.response.KycBriefInfoResp;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Log4j2
@Component
@Aspect
@Order(OrderConfig.ApiKeyKycCheck_ORDER)
public class ApiKeyKycCheckAspect extends BaseAction {

    @Value("${user.api.not.check:true}")
    private Boolean notCheckKyc;

    @Autowired
    private ApiHelper apiHelper;

    @Pointcut("@annotation(com.binance.mgs.account.advice.ApiKeyKycCheck)")
    public void validatePointCut() {

    }

    @Before("validatePointCut()")
    public void before(JoinPoint joinPoint) throws Exception {
        if (!notCheckKyc) {
            return;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        ApiKeyKycCheck apiKeyKycCheck = method.getAnnotation(ApiKeyKycCheck.class);
        if (null == apiKeyKycCheck) {
            log.warn("ApiUserKycCheckAspect before, annotation is null, return.");
            return;
        }
        Long userId = getUserId();
        if (null == userId) {
            log.warn("ApiUserKycCheckAspect before, userId is null, return.");
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        KycBriefInfoResp resp = apiHelper.checkPassAboveIntermediateKyc(userId);
        if (!resp.isPass()) {
            log.info("user kyc validate failed, userId : {}.", userId);
            throw new BusinessException(AccountMgsErrorCode.API_UPDATE_NEED_KYC_COMPLETE);
        }
    }
}
