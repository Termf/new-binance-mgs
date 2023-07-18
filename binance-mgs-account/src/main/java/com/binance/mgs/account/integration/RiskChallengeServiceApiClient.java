package com.binance.mgs.account.integration;

import com.binance.master.constant.Constant;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.platform.pool.threadpool.DynamicExecutor;
import com.binance.platform.pool.threadpool.DynamicExecutors;
import com.binance.platform.pool.threadpool.ext.VariableLinkedBlockingQueue;
import com.binance.risk.api.RiskChallengeFlowApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Description:
 *
 * @author alven
 * @since 2023/5/3
 */
@Slf4j
@Component
public class RiskChallengeServiceApiClient {
    @Resource
    private RiskChallengeFlowApi riskChallengeFlowApi;

    @Value("${account.mfa.riskservice.timeout:200}")
    private Long riskServiceTimeout;

    private final DynamicExecutor executorService = DynamicExecutors.custom("risk-challenge-executor")
            .transmittable()
            .setCoreSize(5)
            .setMaxSize(10)
            .setKeepAliveTime(60, TimeUnit.SECONDS)
            .setBlockQueue(new VariableLinkedBlockingQueue<>(100))
            .build();
    
    public Long getUserIdByBizNo(String bizNo){
        Future<APIResponse<String>> responseFuture = executorService.submit(() -> {
            APIResponse<String> userIdResponse = riskChallengeFlowApi.getUserIdByBizNoFromRedis(APIRequest.instance(bizNo));
            log.info("getUserIdByBizNo result :{}", userIdResponse);
            return userIdResponse;
        });

        APIResponse<String> response = null;
        Long userId = null;
        try {
            response = responseFuture.get(riskServiceTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("riskChallengeFlowApi getUserIdByBizNoFromRedis timeout, bizNo :{}", bizNo, e);
        } catch (Exception e) {
            log.error("riskChallengeFlowApi getUserIdByBizNoFromRedis fail, bizNo :{}", bizNo, e);
        }

        try {
            // risk返回有可能不是数字, 单独trycatch
            if (response != null && APIResponse.Status.OK == response.getStatus() && StringUtils.isNotBlank(response.getData())) {
                userId = Long.valueOf(response.getData());
            }
        } catch (Exception e) {
            log.error("getUserIdByBizNo risk return error, bizNo :{}", bizNo, e);
        }
        return userId;
    }
}
