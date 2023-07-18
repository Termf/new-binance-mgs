package com.binance.mgs.account.account.controller;


import com.binance.account.api.UserSecurityApi;
import com.binance.account.api.WebAuthnApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.yubikey.StartAuthenticateRequest;
import com.binance.account.vo.yubikey.StartAuthenticateResponse;
import com.binance.account.vo.yubikey.StartRegisterReponse;
import com.binance.account.vo.yubikey.StartRegisterRequest;
import com.binance.account.vo.yubikey.UserYubikeyVo;
import com.binance.account.vo.yubikey.WebAuthnListRequest;
import com.binance.account.vo.yubikey.WebAuthnOriginSupportedRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.webauthn.OriginArg;
import com.binance.mgs.account.account.vo.webauthn.QueryUserYubikeyArg;
import com.binance.mgs.account.account.vo.webauthn.StartAuthenticateArg;
import com.binance.mgs.account.account.vo.webauthn.StartAuthenticateRet;
import com.binance.mgs.account.account.vo.webauthn.StartRegisterArg;
import com.binance.mgs.account.account.vo.webauthn.StartRegisterRet;
import com.binance.mgs.account.account.vo.webauthn.UserYubikey;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class WebAuthnController extends AccountBaseAction {

    @Resource
    private WebAuthnApi webAuthnApi;

    @Resource
    private UserSecurityApi userSecurityApi;

    /**
     * 开始绑定yubikey
     *
     * @return
     * @throws Exception
     */
    @UserOperation(eventName = "startRegisterYubikey", name = "发起注册Yubikey",
            requestKeys = { "origin" }, requestKeyDisplayNames = {"origin"},
            responseKeys = { "$.success" }, responseKeyDisplayNames = { "success" })
    @PostMapping(value = "/private/account/web-authn/register/start")
    public CommonRet<StartRegisterRet> startRegister(@Validated @RequestBody StartRegisterArg startRegisterArg) {
        StartRegisterRequest startRegisterRequest = new StartRegisterRequest();
        BeanUtils.copyProperties(startRegisterArg, startRegisterRequest);
        startRegisterRequest.setUserId(getUserId());
        APIResponse<StartRegisterReponse> apiResponse = webAuthnApi.startRegister(APIRequest.instance(startRegisterRequest));
        checkResponse(apiResponse);
        StartRegisterRet startRegisterRet = new StartRegisterRet();
        BeanUtils.copyProperties(apiResponse.getData(), startRegisterRet);
        return new CommonRet<>(startRegisterRet);
    }

    /**
     * 开始验证yubikey
     *
     */
    @UserOperation(eventName = "startAuthenticateYubikey", name = "发起验证Yubikey",
            requestKeys = { "origin" }, requestKeyDisplayNames = {"origin"},
            logDeviceOperation = true, deviceOperationNoteField = {},
            responseKeys = { "$.success" }, responseKeyDisplayNames = { "success" })
    @PostMapping(value = "/private/account/web-authn/authenticate/start")
    public CommonRet<StartAuthenticateRet> startAuthenticate(@Validated @RequestBody StartAuthenticateArg startAuthenticateArg) {
        return startAuthenticate(getUserId(), startAuthenticateArg.getOrigin());
    }

    /**
     * 开始验证yubikey
     *
     */
    @UserOperation(eventName = "startAuthenticateYubikey", name = "发起验证Yubikey",
            requestKeys = { "origin" }, requestKeyDisplayNames = {"origin"},
            responseKeys = { "$.success" }, responseKeyDisplayNames = { "success" })
    @PostMapping(value = "/public/account/web-authn/authenticate/start")
    public CommonRet<StartAuthenticateRet> publicStartAuthenticate(@Validated @RequestBody StartAuthenticateArg startAuthenticateArg)  throws Exception {
        if (org.apache.commons.lang3.StringUtils.isAllBlank(startAuthenticateArg.getEmail(),startAuthenticateArg.getMobile(),startAuthenticateArg.getMobileCode())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        //手机号登录授权
        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(startAuthenticateArg.getEmail());
        getUserIdReq.setMobileCode(startAuthenticateArg.getMobileCode());
        getUserIdReq.setMobile(startAuthenticateArg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        if (!baseHelper.isOk(getUserIdResp)) {
            log.warn("publicStartAuthenticate:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
            checkResponseMaskUseNotExits(getUserIdResp);
        }
        Long userId = getUserIdResp.getData().getUserId();
        return startAuthenticate(userId, startAuthenticateArg.getOrigin());
    }

    private CommonRet<StartAuthenticateRet> startAuthenticate(Long userId, String origin) {
        StartAuthenticateRequest startAuthenticateRequest = new StartAuthenticateRequest();
        startAuthenticateRequest.setOrigin(origin);
        startAuthenticateRequest.setUserId(userId);
        APIResponse<StartAuthenticateResponse> apiResponse = webAuthnApi.startAuthenticate(APIRequest.instance(startAuthenticateRequest));
        checkResponse(apiResponse);
        StartAuthenticateRet startAuthenticateRet = new StartAuthenticateRet();
        BeanUtils.copyProperties(apiResponse.getData(), startAuthenticateRet);
        return new CommonRet<>(startAuthenticateRet);
    }

    /**
     *
     * 查询用户绑定的Yubikey
     *
     * @param queryUserYubikeyArg
     * @return
     */
    @PostMapping(value = "/private/account/web-authn/query")
    public CommonRet<List<UserYubikey>> query(@Validated @RequestBody QueryUserYubikeyArg queryUserYubikeyArg) {
        WebAuthnListRequest webAuthnListRequest = new WebAuthnListRequest();
        webAuthnListRequest.setUserId(getUserId());
        webAuthnListRequest.setOrigin(queryUserYubikeyArg.getOrigin());
        APIResponse<List<UserYubikeyVo>> apiResponse = webAuthnApi.getList(APIRequest.instance(webAuthnListRequest));
        checkResponse(apiResponse);
        List<UserYubikey> userYubikeys = Collections.emptyList();
        if (!CollectionUtils.isEmpty(apiResponse.getData())) {
            userYubikeys = apiResponse.getData().stream().map(userYubikeyVo -> {
                UserYubikey userYubikey = new UserYubikey();
                BeanUtils.copyProperties(userYubikeyVo, userYubikey);
                return userYubikey;
            }).collect(Collectors.toList());
        }

        return new CommonRet<>(userYubikeys);
    }

    @PostMapping(value = "/public/account/web-authn/origin-supported")
    public CommonRet<Boolean> query(@Validated @RequestBody OriginArg originArg) {
        WebAuthnOriginSupportedRequest request = new WebAuthnOriginSupportedRequest();
        request.setOrigin(originArg.getOrigin());
        APIResponse<Boolean> response = webAuthnApi.isOriginSupported(APIRequest.instance(request));
        checkResponse(response);

        return new CommonRet<>(response.getData());
    }

}
