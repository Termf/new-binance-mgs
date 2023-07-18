package com.binance.mgs.account.advice;

import com.binance.master.error.BusinessException;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.platform.mgs.enums.MgsErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Aspect
@Order(OrderConfig.SubAccountForbidden_ORDER)
public class SubAccountForbiddenAspect {

    @Resource
    private AccountHelper accountHelper;

    @Before("@annotation(subAccountForbidden)")
    public void preCheck(SubAccountForbidden subAccountForbidden) throws Throwable {
        if (accountHelper.isSubUserExcludeAssetSub()) {
            throw new BusinessException(MgsErrorCode.SUBUSER_FEATURE_FORBIDDEN);
        }
    }
}
