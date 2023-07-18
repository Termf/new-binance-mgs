package com.binance.mgs.account.service;

import com.binance.accountapimanage.api.AccountApiManageDeleteApi;
import com.binance.accountapimanage.api.AccountApiManageUpdateApi;
import com.binance.accountapimanage.vo.apimanage.request.DeleteApiKeyForSubRequest;
import com.binance.accountapimanage.vo.apimanage.request.ResetApiModeRequest;
import com.binance.accountapimanage.vo.apimanage.request.ResetTradeTimeRequest;
import com.binance.accountapimanage.vo.apimanage.response.DeleteAllApiKeyResponse;
import com.binance.accountapimanage.vo.apimanage.response.DeleteApiKeyForSubResponse;
import com.binance.accountapimanage.vo.apimanage.response.DeleteApiKeyResponse;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.Interceptor.FeignAsyncHelper;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.api.vo.ResetEnableTradeTimeRet;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author sean w
 * @date 2022/8/23
 **/
@Slf4j
@Service
public class AccountApiManageClient extends BaseAction {

    @Autowired
    private ApiHelper apiHelper;

    @Autowired
    private AccountApiManageUpdateApi accountApiManageUpdateApi;

    @Autowired
    private AccountApiManageDeleteApi accountApiManageDeleteApi;

    @Value("${check.api.key.reset.enable.trade.days:80}")
    private int checkResetEnableTradeDays;
    @Value("${reset.sub.api.key.timeout:3000}")
    private long resetSubApiKeyTimeout;

    @Value("${delete.apiKey.split.switch:false}")
    private boolean splitDeleteApiKeySwitch;

    private static final String IP = "0.0.0.0";

    private static final ExecutorService executorService = new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue(100));

    public boolean checkCanResetEnableTradeTime(Date enableTradeTime, String tradeIp) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -checkResetEnableTradeDays);
        Date endDate = cal.getTime();
        if (enableTradeTime!=null && enableTradeTime.before(endDate)) {
            List<String> ipList = Arrays.asList(tradeIp.split(","));
            if (CollectionUtils.isEmpty(ipList) || (ipList.size() == 1 && ipList.contains(IP))) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public ResetEnableTradeTimeRet resetEnableTradeTimeRet(HttpServletRequest httpServletRequest, Long parentUserId, List<ResetApiModeRequest> timeRequests) throws Exception {
        ResetEnableTradeTimeRet resetEnableTradeTimeRet = new ResetEnableTradeTimeRet();
        Date enableTradeEndDate = setCountResetEnableTradeDate(checkResetEnableTradeDays);
        ResetTradeTimeRequest request = new ResetTradeTimeRequest();
        Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
        request.setParentUserId(parentUserId);
        request.setApiModeRequests(timeRequests);
        request.setCanResetTradeEndDate(enableTradeEndDate);
        request.setFvideoId(CommonUserDeviceHelper.getFVideoId(httpServletRequest));
        request.setDeviceInfo(org.springframework.util.CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);

        APIResponse<Long> apiResponse = new APIResponse<>();
        try {

            String envFlag = EnvUtil.getEnvFlag();
            String traceId = TrackingUtils.getTrace();
            String clientType = WebUtils.getClientType();
            Future<APIResponse<Long>> future = executorService.submit(new Callable<APIResponse<Long>>() {
                @Override
                public APIResponse<Long> call() throws Exception {
                    TrackingUtils.saveTrace(traceId);
                    RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                    FeignAsyncHelper.addHead("clienttype", clientType);
                    try {
                        long start = System.currentTimeMillis();
                        APIResponse<Long> result =  accountApiManageUpdateApi.resetEnableTradeTime(APIRequest.instance(request));
                        log.info("accountApiManage.resetEnableTradeTime request:{} cost:{}ms Resp:{}", JsonUtils.toJsonHasNullKey(request), System.currentTimeMillis() - start, result==null?null:JsonUtils.toJsonHasNullKey(result));
                        return result;
                    }catch (Exception e) {
                        log.error("accountApiManage.resetEnableTradeTime,error",e);
                        throw e;
                    }
                }
            });
            apiResponse = future.get(resetSubApiKeyTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("resetSubApiKeyTimeout", e);
        }

        if (apiResponse == null || APIResponse.Status.ERROR.equals(apiResponse.getStatus())) {
            log.error("apiManageService resetEnableTradeTimeRet error:{}", apiResponse==null?null:apiResponse.getErrorData());
            throw new BusinessException("resetEnableTradeTimeRet error");
        }
        resetEnableTradeTimeRet.setUpdateNum(apiResponse.getData());
        return resetEnableTradeTimeRet;
    }


    private Date setCountResetEnableTradeDate(int countDownDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -countDownDays);
        Date date = cal.getTime();
        log.info("date={},timestamp={}", date, date.getTime());
        return date;
    }

    public void deleteApiKeyForSub(HttpServletRequest request, Long parentUserId, Long subUserId, String apiKey) throws Exception {
        if (splitDeleteApiKeySwitch) {
            Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
            DeleteApiKeyForSubRequest deleteApiKeyForSubRequest = new DeleteApiKeyForSubRequest();
            deleteApiKeyForSubRequest.setParentUserId(parentUserId);
            deleteApiKeyForSubRequest.setSubUserId(subUserId);
            deleteApiKeyForSubRequest.setApiKey(apiKey);
            deleteApiKeyForSubRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(request));
            deleteApiKeyForSubRequest.setDeviceInfo(org.springframework.util.CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
            log.info("deleteApiKeyForSub use account-apimanage request = {}", JsonUtils.toJsonNotNullKey(deleteApiKeyForSubRequest));
            APIResponse<DeleteApiKeyForSubResponse> response = accountApiManageDeleteApi.deleteApiKeyForSub(APIRequest.instance(deleteApiKeyForSubRequest));
            checkResponse(response);
        } else {
            deleteApiKey(request, subUserId, apiKey);
        }
    }

    public void deleteApiKey(HttpServletRequest request, Long userId, String apiKey) throws Exception {
        Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
        com.binance.accountapimanage.vo.apimanage.request.DeleteApiKeyRequest deleteApiKeyRequest = new com.binance.accountapimanage.vo.apimanage.request.DeleteApiKeyRequest();
        deleteApiKeyRequest.setUserId(userId);
        deleteApiKeyRequest.setApiKey(apiKey);
        deleteApiKeyRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(request));
        deleteApiKeyRequest.setDeviceInfo(org.springframework.util.CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
        log.info("deleteApiKey use account-apimanage request = {}", JsonUtils.toJsonNotNullKey(deleteApiKeyRequest));
        APIResponse<DeleteApiKeyResponse> response = accountApiManageDeleteApi.deleteApiKey(APIRequest.instance(deleteApiKeyRequest));
        checkResponse(response);
    }

    public void deleteAllApiKey(HttpServletRequest request) throws Exception {
        Map<String, String> deviceInfo = apiHelper.logDeviceInfo();
        com.binance.accountapimanage.vo.apimanage.request.DeleteAllApiKeyRequest deleteAllApiKeyRequest = new com.binance.accountapimanage.vo.apimanage.request.DeleteAllApiKeyRequest();
        deleteAllApiKeyRequest.setUserId(getUserId());
        deleteAllApiKeyRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(request));
        deleteAllApiKeyRequest.setDeviceInfo(org.springframework.util.CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
        APIResponse<DeleteAllApiKeyResponse> response = accountApiManageDeleteApi.deleteAllApiKey(APIRequest.instance(deleteAllApiKeyRequest));
        checkResponse(response);
    }
}
