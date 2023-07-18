package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserWithdrawPropertyApi;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.withdraw.response.UserWithdrawFaceTipResponse;
import com.binance.certification.api.WithdrawFaceApi;
import com.binance.certification.response.WithdrawFaceCheckResponse;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 *
 * @author liliang1
 * @date 2018-12-21 11:59
 */
@RestController
@RequestMapping(value = "/v1/private/account")
public class UserWithdrawController extends BaseAction {

    @Resource
    private UserWithdrawPropertyApi userWithdrawPropertyApi;
    @Resource
    private WithdrawFaceApi withdrawFaceApi;

    @Value("${withdrawFace.check.toCertification.switch:false}")
    private boolean toCertificateSwitch;


    @GetMapping("/user/check/withdraw-face")
    public CommonRet<UserWithdrawFaceTipResponse> userCheckWithdrawFaceTip() {
        Long userId = getUserId();
        if (toCertificateSwitch) {
            APIResponse<WithdrawFaceCheckResponse> response = withdrawFaceApi.withdrawFaceCheckStatus(userId);
            checkResponse(response);
            UserWithdrawFaceTipResponse tipResponse = new UserWithdrawFaceTipResponse();
            BeanUtils.copyProperties(response.getData(), tipResponse);
            return new CommonRet<>(tipResponse);
        }else {
            UserIdRequest request = new UserIdRequest();
            request.setUserId(getUserId());
            APIResponse<UserWithdrawFaceTipResponse> response = userWithdrawPropertyApi.checkWithdrawFaceStatus(getInstance(request));
            checkResponse(response);
            UserWithdrawFaceTipResponse tipResponse = response.getData();
            return new CommonRet<>(tipResponse);
        }
    }
}
