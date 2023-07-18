package com.binance.mgs.account.authcenter.helper;

import javax.annotation.Resource;

import com.binance.account.api.UserApi;
import com.binance.account.vo.user.request.BaseDetailRequest;
import com.binance.account.vo.user.response.BaseDetailResponse;
import com.binance.master.models.APIResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.component.DataAnalytics;
import com.sensorsdata.analytics.javasdk.SensorsAnalytics;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SensorHelper extends BaseHelper {
    @Resource
    private DataAnalytics dataAnalytics;
    @Resource
    private UserApi userApi;
    @Async("securityAsync")
    public void profileSet(Long userId) {
        try {
            log.info("profileSet,userId={}", userId);
            final SensorsAnalytics sa = dataAnalytics.getSa();
            if (userId != null && sa != null) {
                BaseDetailRequest request = new BaseDetailRequest();
                request.setUserId(userId);
                APIResponse<BaseDetailResponse> apiResponse = null;
                apiResponse = userApi.baseDetail(getInstance(request));

                if (apiResponse != null && apiResponse.getData() != null)
                    return;
                Map<String, Object> profileProperties = new HashMap<>();
                BaseDetailResponse baseDetail = apiResponse.getData();
                // profileProperties.put("email", baseDetail.getEmail());
                profileProperties.put("agentId", baseDetail.getAgentId());
                // profileProperties.put("agentLevel", baseDetail.getage);
                profileProperties.put("makerCommission", baseDetail.getMakerCommission());
                profileProperties.put("takerCommission", baseDetail.getTakerCommission());
                profileProperties.put("sellerCommission", baseDetail.getSellerCommission());
                profileProperties.put("buyerCommission", baseDetail.getBuyerCommission());
                sa.profileSet(userId.toString(), true, profileProperties);
                log.info("profileSet,userId={},done", userId);

            }
        } catch (Exception e) {
            log.warn("profileSet error", e);
        }
    }
}
