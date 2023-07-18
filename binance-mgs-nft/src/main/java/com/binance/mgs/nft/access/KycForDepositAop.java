package com.binance.mgs.nft.access;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class KycForDepositAop {
    private final BaseHelper baseHelper;
    private final IUserComplianceApi userComplianceApi;
    //默认开启
    @Value("${nft.asset.deposit.kyc.enabled:true}")
    private Boolean kycEnabled;

    @Around("@annotation(KycForDeposit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        if(!kycEnabled){
            return point.proceed();
        }
        final Long userId = baseHelper.getUserId();
        if (null == userId) {
            log.error("[deposit] KycForDepositAop userId is null");
            return initErrorCommonRet();
        }
        KycForDeposit annotation = ((MethodSignature)point.getSignature()).getMethod().getAnnotation(KycForDeposit.class);

        String kycResult = this.complianceCheckSync(userId, 3, annotation.scene().getCode());
        if (!StringUtil.isEmpty(kycResult)) {
            log.error("[deposit] kyc not passed, userId is {}, kycResult is {}", userId, kycResult);
            return initErrorCommonRet();
        }
        return point.proceed();
    }


    private CommonRet<?> initErrorCommonRet() {
        CommonRet<?> commonRet = new CommonRet<>();
        commonRet.setCode("10000222");
        commonRet.setMessage("Sorry, kyc not passed.");
        return commonRet;
    }

    public String complianceCheckSync(Long userId, Integer type, Integer scene) {
        try {
            type = Optional.ofNullable(type)
                    .orElse(ComplianceTypeEnum.genType(ComplianceTypeEnum.KYC_CHECK, ComplianceTypeEnum.CLEAR_CHECK));
            UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                    .businessScene(scene)
                    .type(type)
                    .userId(userId)
                    .front(false)
                    .build();
            APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(req));
            if (!(response != null && response.getStatus() == APIResponse.Status.OK)) {
                return null;
            }
            if (!Optional.ofNullable(response.getData().getPass()).orElse(Boolean.TRUE)) {
                log.warn("[deposit] complianceCheck hit {} {}", userId, response.getData());
                return response.getData().getErrorCode();
            }
            return null;
        } catch (Exception e) {
            log.error("[deposit] complianceCheckSync error", e);
        }
        return null;
    }
}
