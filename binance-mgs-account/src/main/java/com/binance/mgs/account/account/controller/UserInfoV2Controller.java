package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.BindPhishingCodeV2Request;
import com.binance.account.vo.security.request.CloseWithdrawWhiteStatusV2Request;
import com.binance.account.vo.security.request.OpenWithdrawWhiteStatusV2Request;
import com.binance.account.vo.security.response.CloseWithdrawWhiteStatusV2Response;
import com.binance.account.vo.security.response.OpenWithdrawWhiteStatusV2Response;
import com.binance.account2fa.api.User2FaApi;
import com.binance.account2fa.vo.request.User2FaStatusReq;
import com.binance.account2fa.vo.response.User2FaStatusResp;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.account.vo.*;
import com.binance.mgs.account.advice.SubAccountForbidden;
import com.binance.mgs.account.service.Account2FaService;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.javasimon.aop.Monitored;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
@Monitored
public class UserInfoV2Controller extends AccountBaseAction {
    @Resource
    private UserSecurityApi userSecurityApi;

    @Autowired
    private Account2FaService account2FaService;
    @Autowired
    private User2FaApi user2FaApi;
    @Autowired
    private UserDeviceHelper userDeviceHelper;
    
    /**
     * 开启提现白名单
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/v2/private/account/user/open-withdraw-white-status")
    @UserOperation(eventName = "openWhiteList", name = "开启提现白名单", logDeviceOperation = true,
            deviceOperationNoteField = {"authType"}, responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"})
    @SubAccountForbidden
    public CommonRet<OpenWithdrawWhiteStatusRet> openWithdrawWhiteStatus(HttpServletRequest request,
                                                                         @RequestBody @Validated OpenWithdrawWhiteStatusArg arg) throws Exception {
        OpenWithdrawWhiteStatusV2Request openReq = new OpenWithdrawWhiteStatusV2Request();
        openReq.setUserId(getUserId());
        openReq.setMobileVerifyCode(arg.getMobileVerifyCode());
        openReq.setEmailVerifyCode(arg.getEmailVerifyCode());
        openReq.setGoogleVerifyCode(arg.getGoogleVerifyCode());
        openReq.setYubikeyVerifyCode(arg.getYubikeyVerifyCode());
        APIResponse<OpenWithdrawWhiteStatusV2Response> apiResponse =
                userSecurityApi.openWithdrawWhiteStatusV2(getInstance(openReq));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }

    /**
     * 关闭提现白名单
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/v2/private/account/user/close-withdraw-white-status")
    @UserOperation(eventName = "closeWhiteList", name = "关闭提现白名单", logDeviceOperation = true,
            deviceOperationNoteField = {}, responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @SubAccountForbidden
    public CommonRet<CloseWithdrawWhiteStatusRet> closeWithdrawWhiteStatus(HttpServletRequest request,
                                                                           @RequestBody @Validated CloseWithdrawWhiteStatusArg arg) throws Exception {
        CloseWithdrawWhiteStatusV2Request closeReq = new CloseWithdrawWhiteStatusV2Request();
        closeReq.setUserId(getUserId());
        closeReq.setMobileVerifyCode(arg.getMobileVerifyCode());
        closeReq.setEmailVerifyCode(arg.getEmailVerifyCode());
        closeReq.setGoogleVerifyCode(arg.getGoogleVerifyCode());
        closeReq.setYubikeyVerifyCode(arg.getYubikeyVerifyCode());
        APIResponse<CloseWithdrawWhiteStatusV2Response> apiResponse =
                userSecurityApi.closeWithdrawWhiteStatusV2(getInstance(closeReq));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }

    /**
     * Description: 获取用户登录log
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/v2/private/account/user/set-anti-phishing-code")
    @UserOperation(name = "设置防钓鱼码", eventName = "aouAntiPhishingCode", logDeviceOperation = true,
            deviceOperationNoteField = {}, responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<SetAntiPhishingCodeV2Ret> setAntiPhishingCode(@RequestBody @Validated SetAntiPhishingCodeV2Arg setAntiPhishingCodeArg)
            throws Exception {
        BindPhishingCodeV2Request bindPhishingCodeRequest = new BindPhishingCodeV2Request();
        BeanUtils.copyProperties(setAntiPhishingCodeArg, bindPhishingCodeRequest);
        bindPhishingCodeRequest.setUserId(getUserId());
        APIResponse<Integer> apiResponse = userSecurityApi.aouAntiPhishingCodeV2(getInstance(bindPhishingCodeRequest));
        checkResponseAndLog2fa(apiResponse);
        return new CommonRet<>();
    }

    /**
     * Description: 获取用户2fa的绑定状态
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/v1/private/account/user/user2FaStatus")
    public CommonRet<User2FaStatusResp> user2FaStatus(@RequestBody @Validated User2FaStatusArg user2FaStatusArg) {
        User2FaStatusReq user2FaStatusReq = new User2FaStatusReq();
        user2FaStatusReq.setUserId(getUserId());
        
        String credentialId = user2FaStatusArg.getCredentialId();
        if (StringUtils.isBlank(credentialId)) {
            String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
            user2FaStatusReq.setClientId(clientId);
        } else {
            user2FaStatusReq.setClientId(credentialId);    
        }
        
        APIResponse<User2FaStatusResp> apiResponse = user2FaApi.user2FaStatus(getInstance(user2FaStatusReq));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }
}
