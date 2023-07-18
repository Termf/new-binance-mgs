package com.binance.mgs.account.account.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.binance.account2fa.api.User2FaApi;
import com.binance.account2fa.vo.request.QueryPendingRoamingRequest;
import com.binance.account2fa.vo.request.QueryRoamingStatusRequest;
import com.binance.account2fa.vo.request.StartRoamingRequest;
import com.binance.account2fa.vo.request.VerifyRoamingRequest;
import com.binance.account2fa.vo.response.QueryPendingRoamingResponse;
import com.binance.account2fa.vo.response.QueryRoamingStatusResponse;
import com.binance.account2fa.vo.response.StartRoamingResponse;
import com.binance.account2fa.vo.response.VerifyRoamingResponse;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.new2fa.QueryRoamingStatusArg;
import com.binance.mgs.account.account.vo.new2fa.StartRoamingArg;
import com.binance.mgs.account.account.vo.new2fa.VerifyRoamingArg;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class User2FaController extends AccountBaseAction {

    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Autowired
    private User2FaApi user2FaApi;

    @PostMapping(value = "/v1/protect/account/2fa/startRoaming")
    public CommonRet<StartRoamingResponse> startRoaming(@RequestBody @Validated StartRoamingArg arg)
            throws Exception {
        StartRoamingRequest request = new StartRoamingRequest();
        request.setUserId(getLoginUserId());
        request.setBizScene(arg.getBizScene());
        request.setVerificationType(arg.getVerificationType());
        request.setBusinessFlowId(arg.getBusinessFlowId());
        request.setDeviceInfo(commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        log.info("protect startRoaming userId={} bizScene={} businessFlowId={}", request.getUserId(), arg.getBizScene(), arg.getBusinessFlowId());
        APIResponse<StartRoamingResponse> apiResponse = user2FaApi.startRoaming(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping(value = "/v1/protect/account/2fa/queryRoamingStatus")
    @DDoSPreMonitor(action = "2faProtect.queryRoamingStatus")
    public CommonRet<QueryRoamingStatusResponse> queryRoamingStatus(@RequestBody @Validated QueryRoamingStatusArg arg)
            throws Exception {
        QueryRoamingStatusRequest request = new QueryRoamingStatusRequest();
        request.setUserId(getLoginUserId());
        request.setRoamingFlowId(arg.getRoamingFlowId());
        APIResponse<QueryRoamingStatusResponse> apiResponse = user2FaApi.queryRoamingStatus(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping(value = "/v1/private/account/2fa/queryPendingRoaming")
    @DDoSPreMonitor(action = "2faProtect.queryPendingRoaming")
    public CommonRet<QueryPendingRoamingResponse> queryPendingRoaming() throws Exception {
        QueryPendingRoamingRequest request = new QueryPendingRoamingRequest();
        request.setUserId(getLoginUserId());
        APIResponse<QueryPendingRoamingResponse> apiResponse = user2FaApi.queryPendingRoaming(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping(value = "/v1/private/account/2fa/verifyRoaming")
    public CommonRet<VerifyRoamingResponse> verifyRoaming(@RequestBody @Validated VerifyRoamingArg arg)
            throws Exception {
        VerifyRoamingRequest request = new VerifyRoamingRequest();
        request.setUserId(getLoginUserId());
        request.setRoamingFlowId(arg.getRoamingFlowId());
        request.setFidoVerifyCode(arg.getFidoVerifyCode());
        APIResponse<VerifyRoamingResponse> apiResponse = user2FaApi.verifyRoaming(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping(value = "/v1/private/account/2fa/rejectRoaming")
    public CommonRet<Void> rejectRoaming(@RequestBody @Validated VerifyRoamingArg arg)
            throws Exception {
        VerifyRoamingRequest request = new VerifyRoamingRequest();
        request.setUserId(getLoginUserId());
        request.setRoamingFlowId(arg.getRoamingFlowId());
        APIResponse<Void> apiResponse = user2FaApi.rejectRoaming(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }
    
}
