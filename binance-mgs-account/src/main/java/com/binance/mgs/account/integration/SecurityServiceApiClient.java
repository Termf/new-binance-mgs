package com.binance.mgs.account.integration;

import com.binance.accountanalyze.api.LogApi;
import com.binance.accountanalyze.vo.SecurityCheckResultRequest;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.pool.threadpool.DynamicExecutor;
import com.binance.security.antibot.api.SecurityServiceApi;
import com.binance.security.jantibot.common.enums.RiskLevel;
import com.binance.security.jantibot.common.vo.SecurityCheckRequest;
import com.binance.security.jantibot.common.vo.SecurityCheckResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SecurityServiceApiClient {

    private static final SecurityCheckResultRequest SECURITY_CHECK_RESULT_REQUEST =
            new SecurityCheckResultRequest();

    @Resource
    private LogApi logApi;

    @Resource
    @Qualifier("methodAnalyzeExecutor")
    private DynamicExecutor executor;

    @Resource
    private SecurityServiceApi securityServiceApi;

    public SecurityCheckResponse securityCheck(SecurityCheckRequest request) throws Exception {

        APIResponse<SecurityCheckResponse> response = securityServiceApi.securityCheck(APIRequest.instance(request));
        SecurityCheckResponse data = response.getData();
        RiskLevel riskLevel = data.getRiskLevel();
        if (riskLevel.equals(RiskLevel.PASS) ||
                riskLevel.equals(RiskLevel.SUSPECT) ||
                riskLevel.equals(RiskLevel.STRONG_SUSPECT)) {
            executor.submit(() -> {
                logApi.logSecurityCheckResult(APIRequest.instance(SECURITY_CHECK_RESULT_REQUEST));
            });
        }

        return data;
    }
}