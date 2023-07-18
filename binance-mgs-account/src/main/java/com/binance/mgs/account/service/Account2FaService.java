package com.binance.mgs.account.service;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.vo.request.PublicTokenVerifyRequest;
import com.binance.account2fa.vo.request.VerifyFidoRequest;
import com.binance.account2fa.vo.request.VerifyWithoutUserIdRequest;
import com.binance.account2fa.vo.response.Verify2FaResponse;
import com.binance.account2fa.vo.response.VerifyFidoResponse;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.models.APIRequest;
import com.binance.master.utils.LogMaskUtils;
import com.binance.mgs.account.account.vo.VerifyCodeWithoutUserIdArg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.binance.account2fa.api.Verify2FaApi;
import com.binance.account2fa.vo.request.Verify2FaTokenRequest;
import com.binance.account2fa.vo.response.Verify2FaTokenResponse;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.util.VersionUtil;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Account2FaService extends BaseHelper {

    @Autowired
    private Verify2FaApi verify2FaApi;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;

    public void verify2FaToken(Long userId, String bizScene, String verifyToken) throws Exception {
        String clientType = userDeviceHelper.getClientType();
        TerminalEnum terminal = this.getTerminal();
        String version = VersionUtil.getVersion(terminal);
        
        Verify2FaTokenRequest verify2FaTokenRequest = new Verify2FaTokenRequest();
        verify2FaTokenRequest.setUserId(userId);
        verify2FaTokenRequest.setBizScene(com.binance.account2fa.enums.BizSceneEnum.valueOf(bizScene));
        verify2FaTokenRequest.setVerifyToken(verifyToken);
        verify2FaTokenRequest.setClientType(clientType);
        verify2FaTokenRequest.setVersion(version);
        log.info("verify2FaToken request = {}", JSONObject.toJSONString(verify2FaTokenRequest));
        APIResponse<Verify2FaTokenResponse> apiResponse = verify2FaApi.verify2FaToken(getInstance(verify2FaTokenRequest));
        log.info("verify2FaToken response = {}", JSONObject.toJSONString(apiResponse));
        checkResponse(apiResponse);
    }

    public VerifyFidoResponse verifyFido(Long userId, String fidoCode) throws Exception {
        VerifyFidoRequest request = new VerifyFidoRequest();
        request.setUserId(userId);
        request.setCode(fidoCode);
        log.info("verifyFido request = {}", request);
        APIResponse<VerifyFidoResponse> apiResponse = verify2FaApi.verifyFido(getInstance(request));
        log.info("verify2FaToken response = {}", apiResponse);
        checkResponse(apiResponse);
        return apiResponse.getData();
    }
    
    public Verify2FaResponse verifyCodeWithoutUserId(VerifyCodeWithoutUserIdArg arg, BizSceneEnum bizScene) {
        VerifyWithoutUserIdRequest request = new VerifyWithoutUserIdRequest();
        request.setBizScene(bizScene.name());
        request.setEmail(arg.getEmail());
        request.setMobile(arg.getMobile());
        request.setMobileCode(arg.getMobileCode());
        request.setVerifyType(arg.getVerifyType());
        request.setVerifyCode(arg.getVerifyCode());
        log.info("verifyCodeWithoutUserId request = {}", LogMaskUtils.maskJsonObject(request, "verifyToken", "email"));
        APIResponse<Verify2FaResponse> apiResponse = verify2FaApi.verifyCodeWithoutUserId(APIRequest.instance(request));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }


    public void publicTokenVerify(String email, String mobileCode, String mobile, String verifyToken, String bizScene) {
        PublicTokenVerifyRequest request = new PublicTokenVerifyRequest();
        request.setEmail(email);
        request.setMobile(mobile);
        request.setMobileCode(mobileCode);
        request.setVerifyToken(verifyToken);
        request.setBizScene(bizScene);
        log.info("publicTokenVerify request = {}", LogMaskUtils.maskJsonObject(request, "verifyToken", "email"));
        APIResponse<Void> apiResponse = verify2FaApi.publicTokenVerify(APIRequest.instance(request));
        checkResponse(apiResponse);
    }
}
