package com.binance.mgs.account.account.controller;


import com.alibaba.fastjson.JSON;
import com.binance.account.api.WebAuthnApi;
import com.binance.account.vo.yubikey.DeregisterV3Request;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.webauthn.DeregisterV3Arg;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class WebAuthV3Controller extends AccountBaseAction {

    @Resource
    private WebAuthnApi webAuthnApi;

    @UserOperation(eventName = "deregisterYubikey", name = "解绑Yubikey",
            requestKeys = { "origin" }, requestKeyDisplayNames = {"origin"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = { "$.success" }, responseKeyDisplayNames = { "success" })
    @PostMapping(value = "/v3/private/account/web-authn/deregister")
    public CommonRet<Void> deregister(@Validated @RequestBody DeregisterV3Arg deregisterArg) {
        DeregisterV3Request deregisterRequest = new DeregisterV3Request();
        deregisterRequest.setFinishDetail(JSON.toJSONString(deregisterArg.getFinishDetail()));
        deregisterRequest.setUserId(getUserId());
        deregisterRequest.setCredentialId(deregisterArg.getCredentialId());
        deregisterRequest.setMobileVerifyCode(deregisterArg.getMobileVerifyCode());
        deregisterRequest.setEmailVerifyCode(deregisterArg.getEmailVerifyCode());
        deregisterRequest.setGoogleVerifyCode(deregisterArg.getGoogleVerifyCode());
        deregisterRequest.setYubikeyVerifyCode(deregisterArg.getYubikeyVerifyCode());
        APIResponse<Void> apiResponse;
        CommonRet<Void> ret = new CommonRet<>();
        try {
            apiResponse = webAuthnApi.deregisterV3(APIRequest.instance(deregisterRequest));
        } catch (Exception e) {
            log.error("finishRegister error", e);
            ret.setCode(GeneralCode.SYS_ERROR.getCode());
            return ret;
        }
        checkResponseAndLog2fa(apiResponse);
        return ret;
    }
}
