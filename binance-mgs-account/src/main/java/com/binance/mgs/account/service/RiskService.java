package com.binance.mgs.account.service;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.Interceptor.FeignAsyncHelper;
import com.binance.mgs.account.account.vo.RiskResultPack;
import com.binance.mgs.account.account.vo.UserDeviceRet;
import com.binance.mgs.account.advice.RiskChallengeMonitor;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.mgs.account.constant.FidoType;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.risk.api.RiskChallengeFlowApi;
import com.binance.risk.vo.challenge.ChallengeFlowItemEnum;
import com.binance.risk.vo.challenge.request.ChallengeFlowItemUpdReq;
import com.binance.rule.api.CommonRiskApi;
import com.binance.rule.request.DecisionCommonRequest;
import com.binance.rule.response.DecisionCommonResponse;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author alven
 * @since 2022/6/1
 */
@Slf4j
@Service
public class RiskService extends BaseHelper {

    private static final String EVENTCODE_CHALLENGE = "common_challenge_valid";
    private static final String RISK_LOGIN_EVENT_RESULT_KEY = "userReject";
    public static final String RISK_LOGIN_EVENT_CONTENT_KEY = "content";

    private static final ExecutorService EXECUTORSERVICE= new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue(100));

    private static final ThreadFactory RISK_LOGIN_NAMED_FACTORY = new ThreadFactoryBuilder().setNameFormat("risk-login-rule-%d").build();
    private static final ExecutorService RISK_LOGIN_EXECUTOR_SERVICE = new ThreadPoolExecutor(5, 10,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), RISK_LOGIN_NAMED_FACTORY);

    @Value("${risk.challenge.rule.timeout:300}")
    private int riskChallengeRuleTimeOut;

    @Value("#{'${risk.header.key.list:risk_challenge_token,risk_challenge_biz_no}'.split(',')}")
    private List<String> riskHeaderKeyList;

    @Value("${risk.challenge.newResponse:false}")
    private boolean riskChallengeNewResponse;
    @Value("#{'${risk.challenge.passResult.list:risk_challenge_pass,risk_2fa_free_verification}'.split(',')}")
    private List<String> riskChallengePassResultList;
    @Value("#{'${risk.challenge.rejectResult.list:risk_challenge_reject}'.split(',')}")
    private List<String> riskChallengeRejectResultList;
    @Value("${risk.challenge.compatibleOldLogic:true}")
    private boolean riskChallengeCompatibleOldLogic;
    @Value("${risk.challenge.newResponse.nullExtendThrowException.switch:true}")
    private boolean riskChallengeNullExtendThrowException;

    @Value("${risk.login.rule.mock.switch:false}")
    private boolean riskLoginRuleMockSwitch;
    @Value("${risk.login.rule.timeout:200}")
    private int riskLoginRuleTimeOut;
    @Value("#{'${risk.login.rule.deviceInfoKey.list:system_lang,timezone,screen_resolution,platform,device_name,system_version}'.split(',')}")
    private List<String> deviceInfoKeys;
    @Value("${risk.login.content.parse.switch:false}")
    private boolean riskLoginContentSwitch;

    @Autowired
    private CommonRiskApi commonRiskApi;

    @Autowired
    private RiskChallengeFlowApi riskChallengeFlowApi;

    public DecisionCommonResponse getRiskChallenge(Map<String, Object> params) {
        // 拼装请求风控的参数
        DecisionCommonRequest request = new DecisionCommonRequest();
        request.setEventCode(EVENTCODE_CHALLENGE);
        request.setContext(params);
        APIResponse<DecisionCommonResponse> decisionResponse = commonRiskApi.commonRule(APIRequest.instance(request));
        log.info("risk getRiskChallenge commonRule response={}", decisionResponse);
        checkResponse(decisionResponse);
        return decisionResponse.getData();
    }
    
    @RiskChallengeMonitor
    public void getRiskChallengeTimeOut(Long userId, Map<String, String> deviceInfo, String bizCode){
        getRiskChallengeTimeOut(userId, deviceInfo, bizCode, null);    
    }

    @RiskChallengeMonitor
    public void getRiskChallengeTimeOut(Long userId, Map<String, String> deviceInfo, String bizCode, Map<String, String> extraParams){
        log.info("getRiskChallenge from risk...");

        boolean result = true;
        boolean riskFail = false;
        Map<String, String> headerMap = buildHeaderMap();
        String traceId = StringUtils.isBlank(TrackingUtils.getTrace()) ? TrackingUtils.generateUUID() : TrackingUtils.getTrace();
        Map<String, Object> params = buildChallengeParams(userId, deviceInfo, bizCode, extraParams);
        Future<DecisionCommonResponse> future = EXECUTORSERVICE.submit(() -> {
            //异步调用，这里需要set header
            FeignAsyncHelper.addHead(headerMap);
            TrackingUtils.saveTrace(traceId);
            return getRiskChallenge(params);
        });
        DecisionCommonResponse response = null;
        try{
            //500ms超时拿不到就返回
            response = future.get(riskChallengeRuleTimeOut, TimeUnit.MILLISECONDS);
            copyResHeader(response);
            result = response == null || response.getIsHit();
        }catch (TimeoutException e){
            riskFail = true;
            log.error("risk getRiskChallenge timeout ",e);
        }catch (Exception e){
            riskFail = true;
            log.error("risk getRiskChallenge error ",e);
        }

        if(riskChallengeNewResponse) {
            if(riskFail) {
                throw new BusinessException(AccountMgsErrorCode.RISK_COMMON_RULE_FAIL);
            }
            if(response == null) {
                log.info("getRiskChallenge response is null, userId: {}", userId);
                return;
            }
            if(riskChallengeCompatibleOldLogic && !response.getIsHit()) {
                log.info("getRiskChallenge compatibleOldLogic, isHit : {}", response.getIsHit());
                return;
            }
            if(MapUtils.isNotEmpty(response.getExtend())) {
                // 参考：https://confluence.toolsfdg.net/pages/viewpage.action?pageId=171945485
                Map<String,Object> riskExtendMap = response.getExtend();
                String enableChallengeFlow = (String) riskExtendMap.get(BinanceMgsAccountConstant.RISK_CHALLENGE_FLOW_ENABLE);
                String riskChangeResult = (String) response.getExtend().get(BinanceMgsAccountConstant.RISK_CHALLENGE_RESULT);
                log.info("getRiskChallenge, enableChallengeFlow: {}, riskChangeResult: {}", enableChallengeFlow, riskChangeResult);
                if(BooleanUtils.toBoolean(enableChallengeFlow)) {
                    throw new BusinessException(AccountMgsErrorCode.USER_EXIST_RISK);
                }
                if(riskChallengePassResultList.contains(riskChangeResult)) {
                    return;
                }
                if(riskChallengeRejectResultList.contains(riskChangeResult)) {
                    throw new BusinessException(AccountMgsErrorCode.RISK_COMMON_RULE_FAIL);
                }
            } else {
                // extend为空，代表着risk内部处理失败
                if(riskChallengeNullExtendThrowException) {
                    throw new BusinessException(AccountMgsErrorCode.RISK_COMMON_RULE_FAIL);
                }
            }
            return;
        }


        String headerValue = (String) RpcContext.getServerContext().getAttachment(BinanceMgsAccountConstant.RISK_CHALLENGE_FLOW_ENABLE);
        if (result && !Boolean.parseBoolean(headerValue)) {
            throw new BusinessException(AccountMgsErrorCode.RISK_COMMON_RULE_FAIL);
        }

        if (riskFail) {
            throw new BusinessException(AccountMgsErrorCode.RISK_COMMON_RULE_FAIL);
        }
        if (result) {
            throw new BusinessException(AccountMgsErrorCode.USER_EXIST_RISK);
        }
    }

    /**
     * 架构封装的response header透传不支持异步线程，risk约定header里数据会在extend里再放入一遍，所以这里需要拿到extend里的数据在复制一遍到header
     * 同时极端情况下风控返回可能与header种状态不一致，会阻塞用户流程，这里需要处理下
     * @param response
     */
    private void copyResHeader(DecisionCommonResponse response) {
        if (response != null && !CollectionUtils.isEmpty(response.getExtend())) {
            Map<String, Object> extend = response.getExtend();
            extend.forEach((k, v) -> RpcContext.getServerContext().setAttachment(k, v.toString()));
        }
    }

    private Map<String, String> buildHeaderMap() {
        Map<String, String> headerMap = new HashMap<>();
        riskHeaderKeyList.forEach(k -> headerMap.put(k, WebUtils.getHeader(k)));
        return headerMap;
    }

    private Map<String, Object> buildChallengeParams(Long userId, Map<String, String> deviceInfo, String bizCode, Map<String, String> extraParams) {
        Map<String, Object> params = new HashMap<>();
        params.put(BinanceMgsAccountConstant.COMMON_RULE_UID, userId.toString());
        params.put(BinanceMgsAccountConstant.COMMON_RULE_IP, WebUtils.getRequestIp());
        params.put(BinanceMgsAccountConstant.COMMON_RULE_SENSE_CODE,bizCode );
        if (deviceInfo != null && !deviceInfo.isEmpty()) {
            params.put(UserDeviceRet.SYS_VERSION, deviceInfo.get(UserDeviceRet.SYS_VERSION));
            params.put(UserDeviceRet.DEVICE_NAME, deviceInfo.get(UserDeviceRet.DEVICE_NAME));
            params.put(UserDeviceRet.SCREEN_RESOLUTION, deviceInfo.get(UserDeviceRet.SCREEN_RESOLUTION));
            params.put(UserDeviceRet.SYS_LANG, deviceInfo.get(UserDeviceRet.SYS_LANG));
            params.put(BinanceMgsAccountConstant.COMMON_RULE_PLATFORM, deviceInfo.get(
                    BinanceMgsAccountConstant.COMMON_RULE_PLATFORM));
            params.put(BinanceMgsAccountConstant.COMMON_RULE_FVIDEOID,
                    deviceInfo.getOrDefault("fvideo-id", ""));
        }
        if (extraParams != null) {
            params.putAll(extraParams);    
        }
        return params;
    }

    public void noticeRiskChallengeResult(Long userId, String bizNo, String verifyType){
        ChallengeFlowItemUpdReq req = new ChallengeFlowItemUpdReq();
        try {
            req.setUserId(userId);
            req.setBizNo(bizNo);
            if (verifyType.equalsIgnoreCase(FidoType.FIDO.name())) {
                req.setFlowStep(ChallengeFlowItemEnum.FIDO);
            } else if (verifyType.equalsIgnoreCase(FidoType.FIDO_EXTERNAL.name())) {
                req.setFlowStep(ChallengeFlowItemEnum.FIDO_EXTERNAL);
            } else if (verifyType.equalsIgnoreCase(BinanceMgsAccountConstant.MFA_PASSWORD_VERIFY_TYPE)) {
                req.setFlowStep(ChallengeFlowItemEnum.PASSWORD);
            } else if (verifyType.equalsIgnoreCase(FidoType.PASSKEYS.name())) {
                req.setFlowStep(ChallengeFlowItemEnum.PASSKEY);
            }
            APIResponse<Boolean> response = riskChallengeFlowApi.finishTheFlowStep(APIRequest.instance(req));
            log.info("riskChallengeFlowApi.finishTheFlowStep response,request:{}, result :{}", req, response);
            if (response == null || response.getStatus() == APIResponse.Status.ERROR) {
                log.error("riskChallengeFlowApi.finishTheFlowStep invoke fail, request :{}", req);
            }
        } catch (Exception e) {
            log.error("riskChallengeFlowApi.finishTheFlowStep invoke error, req :{}", req, e);
            throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
        }
    }

    public boolean loginRiskRuleTimeOut(Long userId, Map<String, String> deviceInfo,String fvideoId,String requestIp, String clientType) {
        RiskResultPack resultPack = RiskResultPack.builder()
                .hit(false)
                .build();
        if(riskLoginRuleMockSwitch){
            return resultPack.isHit();
        }

        String traceId = StringUtils.isBlank(TrackingUtils.getTrace()) ? TrackingUtils.generateUUID() : TrackingUtils.getTrace();
        Map<String, String> headerMap = buildHeaderMap();
        Future<RiskResultPack> futureresponse = RISK_LOGIN_EXECUTOR_SERVICE.submit(() -> {
            TrackingUtils.saveTrace(traceId);
            //set header，risk解析多语言需要
            FeignAsyncHelper.addHead(headerMap);
            return loginRiskRule(userId, deviceInfo,fvideoId,requestIp, clientType);
        });
        try {
            //200ms超时拿不到就返回
            resultPack = futureresponse.get(riskLoginRuleTimeOut, TimeUnit.MILLISECONDS);
        }catch (TimeoutException e){
            log.error("CommonRiskApiClient timeout",e);
        }catch (Exception e){
            log.error("CommonRiskApiClient.loginRiskRuleTimeOut error",e);
        }

        // EX-24033 解析风控extend具体文案，将文案抛出去，覆盖GeneralCode.USER_DISABLED_LOGIN
        if (riskLoginContentSwitch && resultPack.isHit() && StringUtils.isNotEmpty(resultPack.getContent())) {
            log.warn("loginRiskRuleTimeOut risk error, userId: {}", userId);
            throw new BusinessException(AccountMgsErrorCode.USER_HIT_RISK_AND_CUSTOMIZE_MSG.getCode(), resultPack.getContent());
        }
        return resultPack.isHit();
    }

    public RiskResultPack loginRiskRule(Long userId, Map<String, String> deviceInfo,String fvideoId,String requestIp, String clientType){
        RiskResultPack result = RiskResultPack.builder()
                .hit(false)
                .build();
        try {
            Map<String, Object> context = Maps.newHashMap();
            context.put("user_id", userId);
            context.put("ip", requestIp);
            context.put("fvideoid", fvideoId);
            //设备相关的
            for(String deviceInfoKey : deviceInfoKeys) {
                context.put(deviceInfoKey, deviceInfo.getOrDefault(deviceInfoKey,null));
            }
            context.put("type", "login");
            context.put("call_method", "sync");
            context.put("client_type", clientType);
            context.put("login_type", "scan_code");
            DecisionCommonResponse decisionCommonResponse = this.commonRule(context, "user_login_event");
            result = handleDecisionResult(decisionCommonResponse);
        }catch (Exception e){
            log.warn("CommonRiskApiClient.loginRiskRule error",e);
        }
        return result;
    }

    public DecisionCommonResponse commonRule(Map<String, Object> context,String eventCode){
        DecisionCommonRequest request = new DecisionCommonRequest();
        request.setEventCode(eventCode);
        request.setContext(context);
        log.info("CommonRiskApiClient.commonRule.eventCode:{}, context:{}", eventCode, JsonUtils.toJsonNotNullKey(context));
        APIResponse<DecisionCommonResponse> apiResponse = commonRiskApi.commonRule(APIRequest.instance(request));
        log.info("CommonRiskApiClient.commonRule.reault:{}",JsonUtils.toJsonNotNullKey(apiResponse));
        if (APIResponse.Status.ERROR == apiResponse.getStatus()) {
            log.error("CommonRiskApiClient.commonRule :"+ "  error" + apiResponse.getErrorData());
            throw new BusinessException("call commonRule failed");
        }
        return apiResponse.getData();
    }

    private RiskResultPack handleDecisionResult(DecisionCommonResponse decisionCommonResponse) {
        RiskResultPack resultPack = RiskResultPack.builder()
                .hit(false)
                .build();
        if (decisionCommonResponse != null && BooleanUtils.isTrue(decisionCommonResponse.getIsHit()) && MapUtils.isNotEmpty(decisionCommonResponse.getExtend())) {
            Map<String, Object> map = decisionCommonResponse.getExtend();
            resultPack.setHit(Boolean.parseBoolean((String) map.get(RISK_LOGIN_EVENT_RESULT_KEY)));
            resultPack.setContent((String) map.get(RISK_LOGIN_EVENT_CONTENT_KEY));
        }
        return resultPack;
    }
}
