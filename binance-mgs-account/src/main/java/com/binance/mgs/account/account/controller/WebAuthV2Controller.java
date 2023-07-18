package com.binance.mgs.account.account.controller;


import com.alibaba.fastjson.JSON;
import com.binance.account.api.WebAuthnApi;
import com.binance.account.vo.yubikey.FinishRegisterRequestV2;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.webauthn.FinishRegisterV2Arg;
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
public class WebAuthV2Controller extends AccountBaseAction {

    @Resource
    private WebAuthnApi webAuthnApi;

    /**
     * 完成绑定yubikey
     *
     * @return
     * @throws Exception
     */
    @UserOperation(eventName = "finishRegisterYubikey", name = "验证注册Yubikey",
            requestKeys = {"nickname"}, requestKeyDisplayNames = {"nickname"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/v2/private/account/web-authn/register/finish")
    public CommonRet<Void> finishRegister(@Validated @RequestBody FinishRegisterV2Arg finishRegisterArg) {
        FinishRegisterRequestV2 finishRegisterRequest = new FinishRegisterRequestV2();
        finishRegisterRequest.setNickname(finishRegisterArg.getNickname());
        finishRegisterRequest.setUserId(getUserId());
        finishRegisterRequest.setFinishDetail(JSON.toJSONString(finishRegisterArg.getFinishDetail()));
        finishRegisterRequest.setEmailVerifyCode(finishRegisterArg.getEmailVerifyCode());
        finishRegisterRequest.setGoogleVerifyCode(finishRegisterArg.getGoogleVerifyCode());
        finishRegisterRequest.setMobileVerifyCode(finishRegisterArg.getMobileVerifyCode());
        finishRegisterRequest.setYubikeyVerifyCode(finishRegisterArg.getYubikeyVerifyCode());

        APIResponse<Long> apiResponse;
        CommonRet<Void> ret = new CommonRet<>();
        try {
            apiResponse = webAuthnApi.finishRegisterV2(APIRequest.instance(finishRegisterRequest));
        } catch (Exception e) {
            log.error("finishRegister error", e);
            ret.setCode(GeneralCode.SYS_ERROR.getCode());
            return ret;
        }
        checkResponseAndLog2fa(apiResponse);
        return ret;
    }
}
