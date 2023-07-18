package com.binance.mgs.account.service;

import com.binance.commission.api.UserCommissionApi;
import com.binance.commission.vo.user.request.LongIdRequest;
import com.binance.commission.vo.user.response.SubUserTradingVolumeResponse;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.base.BaseAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author sean w
 * @date 2023/2/27
 **/
@Slf4j
@Service
public class CommissionClient extends BaseAction {

    @Autowired
    private UserCommissionApi userCommissionApi;
    @Value("${subuser.trade.query.commission.timeout:3000}")
    private long queryCommissionTimeOut;

    private static final ExecutorService executorService = new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue(100));

    public SubUserTradingVolumeResponse getSubUserTradeData(Long subUserId) throws Exception {

        LongIdRequest longIdRequest = new LongIdRequest();
        longIdRequest.setId(subUserId);
        APIResponse<SubUserTradingVolumeResponse> apiResponse = new APIResponse<>();
        try {
            String envFlag = EnvUtil.getEnvFlag();
            String traceId = TrackingUtils.getTrace();
            Future<APIResponse<SubUserTradingVolumeResponse>> future = executorService.submit(() -> {
                RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                TrackingUtils.saveTrace(traceId);
                long start = System.currentTimeMillis();
                APIResponse<SubUserTradingVolumeResponse> response = userCommissionApi.subUserTradeNumberV2(APIRequest.instance(longIdRequest));
                log.info("CommissionClient.getSubUserTradeData sendUserId:{} cost:{}ms Resp:{}", start, System.currentTimeMillis() - start, JsonUtils.toJsonNotNullKey(response));
                return response;
            });
            apiResponse = future.get(queryCommissionTimeOut, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("CommissionClient.getSubUserTradeData timeout", e);
        }

        if (apiResponse == null||APIResponse.Status.ERROR.equals(apiResponse.getStatus())) {
            log.error("CommissionClient.getSubUserTradeData error:{}", apiResponse==null?null:apiResponse.getErrorData());
            throw new BusinessException("CommissionClient.getSubUserTradeData error");
        }
        return apiResponse.getData()==null?new SubUserTradingVolumeResponse():apiResponse.getData();
    }
}
