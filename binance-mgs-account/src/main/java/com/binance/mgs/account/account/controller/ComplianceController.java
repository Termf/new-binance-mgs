package com.binance.mgs.account.account.controller;

import com.binance.compliance.vo.request.UserIdRequest;
import com.binance.compliance.vo.response.UserCountDownResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binance.compliance.api.UserComplianceApi;
import com.binance.compliance.vo.request.UserComplianceCheckRequest;
import com.binance.compliance.vo.response.UserComplianceCheckResponse;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ComplianceController extends AccountBaseAction {

    @Value("${homepageCheck.productLine:MAINSITE_WEB_APP}")
    private String homepageCheckProductLine;

    @Value("${homepageCheck.operation:HOMEPAGE_CHECK}")
    private String homepageCheckOperation;
    
    @Autowired
    private UserComplianceApi userComplianceApi;
    

    /**
     * app端、web端首页合规校验接口
     * 埋点在3端首页（web端埋在页面头）适用于覆盖全站页面的用于引导用户的合规判断
     * @return
     * @throws Exception
     */
    @PostMapping("/v1/private/account/compliance/homepageCheck")
    public CommonRet<UserComplianceCheckResponse> homepageCheck()throws Exception {
        UserComplianceCheckRequest request = new UserComplianceCheckRequest();
        request.setUserId(getUserId());
        request.setProductLine(homepageCheckProductLine);
        request.setOperation(homepageCheckOperation);
        request.setFront(true);
        APIResponse<UserComplianceCheckResponse> response = userComplianceApi.userComplianceCheck(getInstance(request));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * 查询用户是否在衍生品合规的倒计时中
     * @return
     * @throws Exception
     */
    @PostMapping("/v1/private/account/compliance/derivativeCountDown")
    public CommonRet<UserCountDownResponse> derivativeCountDown()throws Exception {
        UserIdRequest request = new UserIdRequest();
        request.setUserId(getUserId());
        APIResponse<UserCountDownResponse> response = userComplianceApi.checkInDerivativeCountDown(getInstance(request));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
