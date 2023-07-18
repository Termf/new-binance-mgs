package com.binance.mgs.account.advice;

import com.binance.account.api.UserInfoApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.account.vo.user.request.UserIdRequest;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.OTPSendLimitMetrics;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.compliance.api.UserComplianceApi;
import com.binance.compliance.vo.request.UserComplianceCheckRequest;
import com.binance.compliance.vo.response.UserComplianceCheckResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.advice.config.OTPSendLimitManager;
import com.binance.mgs.account.advice.config.OTPSendLimitRule;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.google.api.client.util.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author rudy.c
 * @date 2023-03-31 10:33
 */
@Slf4j
@Service
@Aspect
@Order(OrderConfig.OTPSendLimit_ORDER)
public class OTPSendLimitAspect {
    @Value("${otp.send.limit.switch:false}")
    private boolean otpSendLimitSwitch;
    @Value("${otp.send.limit.allBizScene:ALL_BIZ_SCENE}")
    private String otpSendLimitAllBizScene;
    @Value("#{'${otp.send.limit.allBizScene.excludes:CAPITAL_WITHDRAW,CRYPTO_WITHDRAW}'.split(',')}")
    private Set<String> otpSendLimitAllBizSceneExcludes;
    @Value("#{'${otp.send.limit.whiteUsers:}'.split(',')}")
    private Set<Long> otpSendLimitWhiteUsers;

    @Value("${otp.send.limit.forceKyc.switch:false}")
    private Boolean otpSendLimitForceKycSwitch;
    @Value("${otp.send.limit.productLine:ACCOUNT}")
    private String otpSendLimitProductLine;
    @Value("${otp.send.limit.operation:ENFORCE_KYC}")
    private String otpSendLimitOperation;

    @Autowired
    private OTPSendLimitManager otpSendLimitManager;
    @Autowired
    private BaseHelper baseHelper;
    @Autowired
    private MetricsEventPublisher metricsEventPublisher;
    @Autowired
    private UserComplianceApi userComplianceApi;
    @Autowired
    private UserSecurityApi userSecurityApi;
    @Autowired
    private UserInfoApi userInfoApi;

    @Around("@annotation(otpSendLimit)")
    public Object around(ProceedingJoinPoint pjp, OTPSendLimit otpSendLimit) throws Throwable {
        if(!otpSendLimitSwitch) {
            return pjp.proceed();
        }

        Long userId = getLoginUserId();
        if(userId == null) {
            // 如果没有登录态或者半登录态，则直接放过
            return pjp.proceed();
        }
        if(CollectionUtils.isNotEmpty(otpSendLimitWhiteUsers) && otpSendLimitWhiteUsers.contains(userId)) {
            // 用户白名单，直接放过
            log.info("user is in white list, userId: {}", userId);
            return pjp.proceed();
        }

        String otpType = otpSendLimit.otpType();
        String bizScene = getBizScene(pjp, userId);
        if(StringUtils.isAnyBlank(otpType, bizScene)) {
            return pjp.proceed();
        }

        UserInfoVo userInfoVo = getUserInfoByUserId(userId);
        Integer tradeLevel = userInfoVo.getTradeLevel();
        if (tradeLevel != null && tradeLevel > 0) {
            // 用户VIP>0，不做限制
            return pjp.proceed();    
        }
        Long tradingAccount = userInfoVo.getTradingAccount();
        if (tradingAccount != null || ifPassKyc(userId)) {
            // 有tradingAccount或完成了kyc，不做限制
            return pjp.proceed();    
        }
        // 限制短信发送频率
        log.info("OTPSendLimit send request otpType: {}, bizScene: {} userId: {}", otpType, bizScene, userId);
        OTPSendLimitRule sendLimitRule = otpSendLimitManager.getOTPSendLimitRule(otpType, bizScene);
        if (sendLimitRule != null) {
            requestLimit(userId, sendLimitRule, bizScene);
        }

        if (CollectionUtils.isEmpty(otpSendLimitAllBizSceneExcludes) || (!otpSendLimitAllBizSceneExcludes.contains(StringUtils.upperCase(bizScene)))) {
            OTPSendLimitRule allBizSceneSendLimitRule = otpSendLimitManager.getOTPSendLimitRule(otpType, otpSendLimitAllBizScene);
            if (allBizSceneSendLimitRule != null) {
                requestLimit(userId, allBizSceneSendLimitRule, bizScene);
            }
        }

        return pjp.proceed();
    }

    private void requestLimit(Long userId, OTPSendLimitRule sendLimitRule, String requestBizScene) {
        String redisKey = StringUtils.upperCase(sendLimitRule.getOtpType()) + ":" + StringUtils.upperCase(sendLimitRule.getBizScene()) + ":" + userId;
        Long currentRequestCount = ShardingRedisCacheUtils.increment(redisKey, CacheConstant.OTP_SEND_LIMIT_PREFIX, 1L, sendLimitRule.getDuration(), TimeUnit.SECONDS);
        if(currentRequestCount != null && currentRequestCount > sendLimitRule.getLimit()) {
            metricsEventPublisher.publish(OTPSendLimitMetrics.builder().otpType(sendLimitRule.getOtpType()).bizScene(requestBizScene).build());
            log.error("OTPSendLimit otpType: {}, bizScene: {} userId: {} , currentRequestCount: {}", sendLimitRule.getOtpType(), requestBizScene, userId, currentRequestCount);
            throw new BusinessException(GeneralCode.GW_TOO_MANY_REQUESTS);
        }
    }

    private String getBizScene(ProceedingJoinPoint pjp, Long userId) {
        try {
            Map<String, Object> params = Maps.newHashMap();
            Object[] args = pjp.getArgs();
            if(args != null) {
                for(Object arg : args) {
                    params.putAll(JsonUtils.toMap(JsonUtils.toJsonNotNullKey(arg), String.class, Object.class));
                }
            }
            String bizScene = (String) params.get("bizScene");
            if(StringUtils.isBlank(bizScene)) {
                MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
                String className = methodSignature.getMethod().getDeclaringClass().getSimpleName();
                bizScene = className + "." + methodSignature.getName();
            }
            return bizScene;
        } catch (Exception e) {
            log.error("getBizScene error, userId: {}", userId);
        }
        return null;
    }

    private Long getLoginUserId() {
        Long userId = this.baseHelper.getUserId();
        if (userId != null) {
            return userId;
        }
        String email = this.baseHelper.getUserEmail();
        if(StringUtils.isBlank(email)) {
            return null;
        }
        try {
            GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
            getUserIdReq.setEmail(email);
            APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(this.baseHelper.getInstance(getUserIdReq));
            if (!baseHelper.isOk(getUserIdResp)) {
                log.warn("getUserIdByMobileOrEmail error,request={}, response={}", getUserIdReq, getUserIdResp);
                throw new BusinessException(getUserIdResp.getCode(), baseHelper.getErrorMsg(getUserIdResp), getUserIdResp.getParams());
            }
            return getUserIdResp.getData().getUserId();
        } catch (Exception e) {
            log.warn("OTPSendLimit get loginUserId error", e);
            return null;
        }
    }

    private UserInfoVo getUserInfoByUserId(Long userId)throws Exception{
        UserIdRequest request = new UserIdRequest();
        request.setUserId(userId.toString());
        APIResponse<UserInfoVo> apiResponse = userInfoApi.getUserInfoByUserId(APIRequest.instance(request));
        if (APIResponse.Status.ERROR == apiResponse.getStatus()|| null==apiResponse.getData()) {
            log.error("userInfoApi.getUserInfoByUserId :userId=" + userId + "  error" + apiResponse.getErrorData());
            throw new BusinessException("getUserInfoByUserId failed");
        }
        return apiResponse.getData();
    }
    
    private Boolean ifPassKyc(Long userId) throws Exception {
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(userId);
        request.setProductLine(otpSendLimitProductLine);
        request.setOperation(otpSendLimitOperation);
        request.setFront(true);
        APIResponse<UserComplianceCheckResponse> apiResponse = userComplianceApi.userComplianceCheck(baseHelper.getInstance(request));
        baseHelper.checkResponse(apiResponse);
        UserComplianceCheckResponse userComplianceCheckResponse = apiResponse.getData();
        if (userComplianceCheckResponse != null && userComplianceCheckResponse.isPass()) {
            // 用户通过了kyc
            return true;
        }
        return false;
    }
}
