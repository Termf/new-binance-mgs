package com.binance.mgs.account.service;

import com.binance.account.api.entity.UserEntityApi;
import com.binance.account.vo.entity.UserEntityVo;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.compliance.api.UserComplianceApi;
import com.binance.compliance.vo.request.UserComplianceCheckRequest;
import com.binance.compliance.vo.response.UserComplianceCheckResponse;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author rudy.c
 * @date 2022-05-05 17:27
 */
@Service
@Slf4j
public class UserComplianceService {
    @Autowired
    private UserComplianceApi userComplianceApi;
    @Autowired
    private BaseHelper baseHelper;
    @Autowired
    private UserEntityApi userEntityApi;

    @Value("${UserComplianceService.bncLocation.productLine:MAINSITE}")
    private String bncLocationProductLine;
    @Value("${UserComplianceService.bncLocation.operation:UAE_CHECK}")
    private String bncLocationOperation;
    @Value("${UserComplianceService.bncLocation.switch:true}")
    private boolean bncLocationSwitch;

    @Value("${compliance.bncLocation.timeout:200}")
    private Long complianceBncLocationTimeout;

    @Value("${compliance.bncLocation.useAccount:true}")
    private boolean getBncLocationUseAccount;

    @Value("${UserComplianceService.blockLogin.productLine:MAINSITE_SIGN_UP}")
    private String blockLoginProductLine;
    @Value("${UserComplianceService.blockLogin.operation:SANCTION_FREEZE}")
    private String blockLoginOperation;
    @Value("${UserComplianceService.blockLogin.switch:false}")
    private boolean blockLoginSwitch;

    @Value("${compliance.blockLogin.timeout:200}")
    private Long complianceBlockLoginTimeout;

    @Value("${binance.bncLocation.switch:false}")
    private boolean binanceBncLocationSwitch;
    @Value("${binance.bncLocation:BINANCE}")
    private String binanceBncLocation;

    private ThreadFactory namedFactory = new ThreadFactoryBuilder().setNameFormat("compliance-bnc-location-%d").build();
    private ExecutorService executorService = new ThreadPoolExecutor(2, 5, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), namedFactory);

    public String getDefaultBncLocation() {
        if(binanceBncLocationSwitch) {
            return binanceBncLocation;
        }
        return null;
    }

    public String getBncLocation(Long userId) throws Exception{
        if(userId == null) {
            return getDefaultBncLocation();
        }
        if(!bncLocationSwitch) {
            log.info("UserComplianceService.getBncLocation switch is false");
            return getDefaultBncLocation();
        }
        if(getBncLocationUseAccount) {
            return getBncLocationUseAccount(userId);
        }
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(userId);
        request.setProductLine(bncLocationProductLine);
        request.setOperation(bncLocationOperation);
        request.setFront(true);
        APIResponse<UserComplianceCheckResponse> response = userComplianceApi.userComplianceCheck(baseHelper.getInstance(request));
        baseHelper.checkResponse(response);

        UserComplianceCheckResponse userComplianceCheckResponse = response.getData();
        if(userComplianceCheckResponse == null || userComplianceCheckResponse.isPass()) {
            return getDefaultBncLocation();
        }

        return (String) userComplianceCheckResponse.getExtraInfo().get("bncLocation");
    }

    private String getBncLocationUseAccount(Long userId) throws Exception {
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<UserEntityVo> apiResponse = userEntityApi.getUserEntity(baseHelper.getInstance(userIdRequest));
        baseHelper.checkResponse(apiResponse);

        UserEntityVo userEntityVo = apiResponse.getData();
        if(userEntityVo != null && StringUtils.isNotEmpty(userEntityVo.getEntity())) {
            return userEntityVo.getEntity();
        }
        return getDefaultBncLocation();
    }

    public String getBncLocationWithTimeout(Long userId) {
        if(!bncLocationSwitch) {
            log.info("UserComplianceService.getBncLocationWithTimeout switch is false");
            return getDefaultBncLocation();
        }
        if(getBncLocationUseAccount) {
            // 调用account，接口有保障，不在使用异步线程池
            try {
                return getBncLocationUseAccount(userId);
            } catch (Exception e) {
                log.error("UserComplianceService.getBncLocationWithTimeout getBncLocationUseAccount error, userId: {}", userId, e);
                return getDefaultBncLocation();
            }
        }
        String traceId = TrackingUtils.getTrace();
        String envFlag = EnvUtil.getEnvFlag();
        try {
            Future<String> future = executorService.submit(() -> {
                try {
                    TrackingUtils.saveTrace(traceId);
                    RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                    return getBncLocation(userId);
                } finally {
                    TrackingUtils.clearTrace();
                }
            });
            return future.get(complianceBncLocationTimeout, TimeUnit.MILLISECONDS);
        } catch (BusinessException e) {
            log.error("UserComplianceService.getBncLocationWithTimeout business error, userId: {}", userId, e);
        } catch (TimeoutException e) {
            log.error("UserComplianceService.getBncLocationWithTimeout timeout error, userId: {}", userId, e);
        } catch (Exception e) {
            log.error("UserComplianceService.getBncLocationWithTimeout error, userId: {}", userId, e);
        }
        return getDefaultBncLocation();
    }

    /**
     * 合规登录校验
     * @param userId 用户id
     * @throws Exception
     */
    public void complianceBlockLoginWithTimeout(Long userId) throws Exception {
        if(userId == null) {
            return;
        }
        if(!blockLoginSwitch) {
            log.info("UserComplianceService.complianceBlockLogin switch is false");
            return;
        }
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(userId);
        request.setProductLine(blockLoginProductLine);
        request.setOperation(blockLoginOperation);
        request.setUserRequestIp(WebUtils.getRequestIp());
        request.setFront(true);

        APIResponse<UserComplianceCheckResponse> response = null;
        try {
            String traceId = TrackingUtils.getTrace();
            String envFlag = EnvUtil.getEnvFlag();
            Future<APIResponse<UserComplianceCheckResponse>> future = executorService.submit(() -> {
                try {
                    TrackingUtils.saveTrace(traceId);
                    RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                    return userComplianceApi.userComplianceCheck(baseHelper.getInstance(request));
                } finally {
                    TrackingUtils.clearTrace();
                }
            });
            response = future.get(complianceBlockLoginTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("UserComplianceService.complianceBlockLoginWithTimeout timeout error, userId: {}", userId, e);
        } catch (Exception e) {
            log.error("UserComplianceService.complianceBlockLoginWithTimeout error, userId: {}", userId, e);
        }

        if(response == null) {
            return;
        }

        baseHelper.checkResponse(response);

        UserComplianceCheckResponse userComplianceCheckResponse = response.getData();
        if(userComplianceCheckResponse != null && !userComplianceCheckResponse.isPass()) {
            UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, String.valueOf(userId)));
            UserOperationHelper.log(ImmutableMap.of("login", Boolean.FALSE.toString()));
            UserOperationHelper.log("compliance_error_code", userComplianceCheckResponse.getErrorCode());
            throw new BusinessException(userComplianceCheckResponse.getErrorCode(), userComplianceCheckResponse.getErrorMessage());
        }
    }
}
