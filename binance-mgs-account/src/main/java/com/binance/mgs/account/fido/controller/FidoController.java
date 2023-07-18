package com.binance.mgs.account.fido.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account2fa.api.UserFidoApi;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.account2fa.vo.request.UserBindFidoRequest;
import com.binance.account2fa.vo.response.VerifyFidoResponse;
import com.binance.fido2.api.Fido2Api;
import com.binance.fido2.vo.DeleteCredentialRequest;
import com.binance.fido2.vo.DeleteCredentialResponse;
import com.binance.fido2.vo.FinishRegisterRequest;
import com.binance.fido2.vo.FinishRegisterResponse;
import com.binance.fido2.vo.GetAllCredentialsRequest;
import com.binance.fido2.vo.GetAllCredentialsResponse;
import com.binance.fido2.vo.RenameRequest;
import com.binance.fido2.vo.StartAuthenticateRequest;
import com.binance.fido2.vo.StartAuthenticateResponse;
import com.binance.fido2.vo.StartRegisterRequest;
import com.binance.fido2.vo.StartRegisterResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.LogMaskUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.mgs.account.fido.vo.DeleteCredentialArg;
import com.binance.mgs.account.fido.vo.FinishRegArg;
import com.binance.mgs.account.fido.vo.RenameArg;
import com.binance.mgs.account.fido.vo.StartRegArg;
import com.binance.mgs.account.fido.vo.UserCredentialSimplifyVo;
import com.binance.mgs.account.fido.vo.UserCredentialVo;
import com.binance.mgs.account.fido.vo.VerifyFidoArg;
import com.binance.mgs.account.service.Account2FaService;
import com.binance.mgs.account.service.RiskService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class FidoController extends AccountBaseAction {
    
    @Resource
    protected BaseHelper baseHelper;

    @Autowired
    private Fido2Api fido2Api;
    
    @Autowired
    private Account2FaService account2FaService;
    
    @Autowired
    private UserFidoApi userFidoApi;

    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    
    @Autowired
    private UserDeviceHelper userDeviceHelper;

    @Autowired
    private RiskService riskService;
    
    @Value("${fido.rpid:binance.com}")
    private String rpId;
    
    @Value("${fido.rpname:binance.com}")
    private String rpName;
    
    @Value("${fido.origin:https://binance.com}")
    private String origin;

    @Value("${account2fa.device.fido.query.switch:false}")
    private Boolean fidoQueryLocalSwitch;
    
    @PostMapping("/v1/private/account/fido2/start-register")
    @DDoSPreMonitor(action = "startRegister")
    public CommonRet<StartRegisterResponse> startRegister() {
        StartRegisterRequest request = new StartRegisterRequest();
        request.setClientType(this.baseHelper.getClientType());
        request.setRpId(this.rpId);

        request.setUserId(this.baseHelper.getUserIdStr());
        request.setUserName(this.baseHelper.getUserEmail());
        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        request.setClientId(clientId);
        request.setClientType(this.baseHelper.getClientType());

        // 从header里拼用户的displayName
        HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        String deviceName = deviceInfo.getOrDefault("device_name", "");
        if (StringUtils.isBlank(deviceName)) {
            request.setUserDisplayName(getUserIdStr() + "'s Device");
        } else {
            request.setUserDisplayName(deviceName);
        }

        log.info("fido start register request uid:{}, {}", request.getUserId(), LogMaskUtils.maskJsonString2(JSON.toJSONString(request), "userName"));
        APIResponse<StartRegisterResponse> startRegister = this.fido2Api.startRegister(APIRequest.instance(request));
        log.info("fido start register response uid:{}, {}", request.getUserId(),JSON.toJSONString(startRegister));
        this.baseHelper.checkResponse(startRegister);
        return new CommonRet<StartRegisterResponse>(startRegister.getData());

    }

    @PostMapping("/v2/private/account/fido2/start-register")
    @DDoSPreMonitor(action = "startRegisterV2")
    public CommonRet<StartRegisterResponse> startRegisterV2(@RequestBody StartRegArg startRegArg) {
        StartRegisterRequest request = new StartRegisterRequest();
        request.setClientType(this.baseHelper.getClientType());
        request.setRpId(this.rpId);
        
        request.setUserId(this.baseHelper.getUserIdStr());
        request.setUserName(this.baseHelper.getUserEmail());
        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        request.setClientId(clientId);
        request.setClientType(this.baseHelper.getClientType());

        String flag = WebUtils.getHeader(BinanceMgsAccountConstant.RISK_CHALLENGE_FLAG);
        if (StringUtils.isNotBlank(flag)) {
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            riskService.getRiskChallengeTimeOut(this.baseHelper.getUserId(), deviceInfo, BizSceneEnum.BIND_FIDO.name());
        }

        // 名称优先前端传入
        if (startRegArg != null && StringUtils.isNotBlank(startRegArg.getDisplayName())) {
            request.setUserDisplayName(startRegArg.getDisplayName());    
        } else {
            // 如果前端没有，从header里取用户的device_name
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            String deviceName = deviceInfo.getOrDefault("device_name", "");
            if (StringUtils.isBlank(deviceName)) {
                request.setUserDisplayName(getUserIdStr() + "'s Device");
            } else {
                request.setUserDisplayName(deviceName);
            }
        }

        log.info("fido start register request uid:{}, {}", request.getUserId(), LogMaskUtils.maskJsonString2(JSON.toJSONString(request), "userName"));
        APIResponse<StartRegisterResponse> startRegister = this.fido2Api.startRegister(APIRequest.instance(request));
        log.info("fido start register response uid:{}, {}", request.getUserId(),JSON.toJSONString(startRegister));
        this.baseHelper.checkResponse(startRegister);
        return new CommonRet<StartRegisterResponse>(startRegister.getData());
      
    }

    @PostMapping("/v1/private/account/fido2/finish-register")
    @DDoSPreMonitor(action = "finishRegister")
    @UserOperation(name = "绑定fido", eventName = "bind_fido", requestKeys = {"requestId", "bizScene"},
            requestKeyDisplayNames = {"requestId", "bizScene"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> finishRegister(@RequestBody @Valid FinishRegArg regArg) throws Exception {
        Long userId = getUserId();
        BizSceneEnum bizScene = regArg.getBizScene();
        if (bizScene != null) {
            if (!StringUtils.equalsAny(bizScene.name(), BizSceneEnum.BIND_FIDO.name(), BizSceneEnum.BIND_EXTERNAL_FIDO.name(), BizSceneEnum.BIND_PASSKEYS.name())) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
        } else {
            bizScene = BizSceneEnum.BIND_FIDO;    
        }
        String flag = WebUtils.getHeader(BinanceMgsAccountConstant.RISK_CHALLENGE_FLAG);

        if (StringUtils.isNotBlank(flag)) {
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            riskService.getRiskChallengeTimeOut(this.baseHelper.getUserId(), deviceInfo, BizSceneEnum.BIND_FIDO.name());
        } else {
            account2FaService.verify2FaToken(userId, bizScene.name(), regArg.getVerifyToken());
        }

        FinishRegisterRequest request = new FinishRegisterRequest();
        request.setServerPublicKeyCredential(regArg.getCreateOpt());
        request.setRequestId(regArg.getRequestId());
        request.setOrigin(this.origin);
        request.setRpId(this.rpId);
        log.info("fido finish register request:{}", JSON.toJSONString(request));
        APIResponse<FinishRegisterResponse> finishRegister = this.fido2Api.finishRegister(APIRequest.instance(request));
        log.info("fido finish register response:{}", JSON.toJSONString(finishRegister));
        this.baseHelper.checkResponse(finishRegister);
        // 记录bind类型
        UserOperationHelper.log(ImmutableMap.of(BinanceMgsAccountConstant.BIND_FIDO_TYPE_KEY, getTransportType(finishRegister.getData().getTranTypes())));

        // 更新用户状态为已绑定fido
        UserBindFidoRequest userBindFidoRequest = new UserBindFidoRequest();
        userBindFidoRequest.setUserId(userId);
        userBindFidoRequest.setClientId(finishRegister.getData().getClientId());
        userBindFidoRequest.setClientType(commonUserDeviceHelper.getClientType());
        userBindFidoRequest.setRpId(finishRegister.getData().getRpid());
        userBindFidoRequest.setCredentialId(finishRegister.getData().getCredentialId());
        userBindFidoRequest.setTransports(finishRegister.getData().getTranTypes());
        APIResponse<Void> bindResp = userFidoApi.bindFido(getInstance(userBindFidoRequest));
        this.baseHelper.checkResponse(bindResp);
        return new CommonRet<String>();
    }

    @PostMapping("/v1/protect/account/fido2/start-auth")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"},
            forwardedCookies = {"p20t", "cr20", "s9r1", "d1og", "r2o1", "f30l"})
    @DDoSPreMonitor(action = "startAuthenticateFido")
    public CommonRet<StartAuthenticateResponse> startAuthenticate() throws Exception {
        StartAuthenticateRequest request = new StartAuthenticateRequest();
        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        //这里clientid暂时保留，架构组没改，但是不会用clientid做条件过滤
        request.setClientId(clientId);
        request.setClientType(this.baseHelper.getClientType());
        request.setRpId(this.rpId);
        request.setUserId(getLoginUserId().toString());
        log.info("fido start Authenticate request:{}", JSON.toJSONString(request));
        APIResponse<StartAuthenticateResponse> startAuth = this.fido2Api.startAuth(APIRequest.instance(request));
        log.info("fido start Authenticate response:{}", JSON.toJSONString(startAuth));
        this.baseHelper.checkResponse(startAuth);
        return new CommonRet<StartAuthenticateResponse>(startAuth.getData());
    }

    /**
     * 查询所有fido证书详细信息列表
     * 目前前端fido管理页面使用
     * @return
     */
    @PostMapping("/v1/private/account/fido2/all-credentials")
    public CommonRet<List<UserCredentialVo>> allCredential() {
        GetAllCredentialsRequest request = new GetAllCredentialsRequest();
        request.setRpId(this.rpId);
        request.setUserId(this.baseHelper.getUserIdStr());
        log.info("fido allCredential request:{}", JSON.toJSONString(request));
        APIResponse<List<GetAllCredentialsResponse>> allCredential = this.fido2Api.allCredential(APIRequest.instance(request));
        log.info("fido allCredential response:{}", JSON.toJSONString(allCredential));
        this.baseHelper.checkResponse(allCredential);
        
        String currentClientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        List<UserCredentialVo> resultList = allCredential.getData().stream().map(x -> {
            UserCredentialVo userCredentialVo = CopyBeanUtils.fastCopy(x, UserCredentialVo.class);
            userCredentialVo.setTransportType(getTransportType(x.getTransTypes()));
            if (fidoQueryLocalSwitch) {
                userCredentialVo.setCurrentDevice(false);   
            } else {
                userCredentialVo.setCurrentDevice((!"passkeys".equalsIgnoreCase(userCredentialVo.getTransportType())) && x.getClientId().equals(currentClientId));
            }
            return userCredentialVo;
        }).collect(Collectors.toList());
        
        return new CommonRet<List<UserCredentialVo>>(resultList);
    }

    /**
     * 查询fido证书简单信息列表
     * 目前给前端fido-precheck使用
     * 后端下发fido验证项后，小程序端需要做fido-precheck，调用native接口和本接口查询所有fido证书，对比判断当前设备是否可验fido
     * @return
     */
    @PostMapping("/v1/protect/account/fido2/all-credentials")
    @AccountDefenseResource(name = "FidoController.allCredentialProtect")
    public CommonRet<List<UserCredentialSimplifyVo>> allCredentialProtect() throws Exception {
        GetAllCredentialsRequest request = new GetAllCredentialsRequest();
        request.setRpId(this.rpId);
        request.setUserId(getLoginUserId().toString());
        log.info("fido allCredentialProtect request:{}", JSON.toJSONString(request));
        APIResponse<List<GetAllCredentialsResponse>> allCredential = this.fido2Api.allCredential(APIRequest.instance(request));
        log.info("fido allCredentialProtect response:{}", JSON.toJSONString(allCredential));
        this.baseHelper.checkResponse(allCredential);

        List<UserCredentialSimplifyVo> resultList = allCredential.getData().stream().map(x -> {
            UserCredentialSimplifyVo userCredentialSimplifyVo = CopyBeanUtils.fastCopy(x, UserCredentialSimplifyVo.class);
            userCredentialSimplifyVo.setTransportType(getTransportType(x.getTransTypes()));
            userCredentialSimplifyVo.setAuthInd(x.getAuthInd());
            return userCredentialSimplifyVo;
        }).collect(Collectors.toList());

        return new CommonRet<>(resultList);
    }

    private String getTransportType(List<String> transports) {
        if (CollectionUtils.isEmpty(transports)) {
            return "internal";
        }

        boolean internal = false;
        boolean passkeys = false;
        // 此处未使用List.contains是为了不区分大小写
        for (String transport : transports) {
            if (transport.equalsIgnoreCase("internal")) {
                internal = true;
            }
            if(transport.equalsIgnoreCase("passkeys")) {
                passkeys = true;
            }
        }
        if(passkeys) {
            return "passkeys";
        } else if (internal) {
            return "internal";
        }
        return "external";
    }

    @PostMapping("/v1/private/account/fido2/del-credential")
    @UserOperation(name = "解绑fido", eventName = "unbind_fido", requestKeys = {"credentialId", "bizScene"},
            requestKeyDisplayNames = {"credentialId", "bizScene"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "deleteCredential")
    public CommonRet<DeleteCredentialResponse> deleteCredential(@RequestBody @Valid DeleteCredentialArg arg) throws Exception {
        Long userId = getUserId();
        BizSceneEnum bizScene = arg.getBizScene();
        if (bizScene != null) {
            if (!StringUtils.equalsAny(bizScene.name(), BizSceneEnum.UNBIND_FIDO.name(), BizSceneEnum.UNBIND_EXTERNAL_FIDO.name(), BizSceneEnum.UNBIND_PASSKEYS.name())) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
        } else {
            bizScene = BizSceneEnum.UNBIND_FIDO;
        }

        String flag = WebUtils.getHeader(BinanceMgsAccountConstant.RISK_CHALLENGE_FLAG);
        if (StringUtils.isNotBlank(flag)) {
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            riskService.getRiskChallengeTimeOut(userId, deviceInfo, bizScene.name());
        } else {
            account2FaService.verify2FaToken(userId, bizScene.name(), arg.getVerifyToken());
        }

        DeleteCredentialRequest request = new DeleteCredentialRequest();
        request.setRpId(this.rpId);
        request.setCredentialId(arg.getCredentialId());
        request.setUserId(getUserId().toString());
        log.info("fido deleteCredential request:{}", JSON.toJSONString(request));
        APIResponse<DeleteCredentialResponse> deleteCredential = this.fido2Api.deleteCredential(APIRequest.instance(request));
        log.info("fido deleteCredential response:{}", JSON.toJSONString(deleteCredential));
        this.baseHelper.checkResponse(deleteCredential);

        // 记录解绑fido
        DeleteCredentialResponse deleteDate = deleteCredential.getData();
        UserBindFidoRequest userBindFidoRequest = new UserBindFidoRequest();
        userBindFidoRequest.setUserId(userId);
        userBindFidoRequest.setClientId(deleteDate.getClientId());
        userBindFidoRequest.setRpId(deleteDate.getRpId());
        userBindFidoRequest.setCredentialId(deleteDate.getCredentialId());
        userBindFidoRequest.setTransports(deleteDate.getTransTypes());
        APIResponse<Void> bindResp = userFidoApi.unBindFido(getInstance(userBindFidoRequest));
        this.baseHelper.checkResponse(bindResp);
        
        return new CommonRet<DeleteCredentialResponse>(deleteCredential.getData());
    }

    @PostMapping("/v1/private/account/fido2/verify-fido-code")
    @DDoSPreMonitor(action = "verifyFidoCode")
    public CommonRet<VerifyFidoResponse> verifyFidoCode(@RequestBody @Valid VerifyFidoArg arg) throws Exception {
        Long userId = getUserId();
        VerifyFidoResponse response = account2FaService.verifyFido(userId, arg.getCode());
        String bizNo = WebUtils.getHeader(BinanceMgsAccountConstant.RISK_CHALLENGE_BIZ_NO_HEADER_KEY);
        if (StringUtils.isNotBlank(bizNo)) {
            riskService.noticeRiskChallengeResult(userId, bizNo, arg.getFidoType().name());
        }
        return new CommonRet<>(response);
    }

    @PostMapping("/v1/private/account/fido2/rename")
    @DDoSPreMonitor(action = "renameFido")
    public CommonRet<Boolean> rename(@RequestBody @Valid RenameArg arg) throws Exception {
        RenameRequest request = new RenameRequest();
        request.setRpId(this.rpId);
        request.setUserId(String.valueOf(getUserId()));
        request.setCredId(arg.getCredentialId());
        request.setNewName(arg.getNewName());
        log.info("fido rename request:{}", JSON.toJSONString(request));
        APIResponse<Boolean> renameResult = fido2Api.rename(APIRequest.instance(request));
        log.info("fido rename response:{}", JSON.toJSONString(renameResult));
        this.baseHelper.checkResponse(renameResult);
        return new CommonRet<>(renameResult.getData());
    }

    @PostMapping("/v2/private/account/fido2/rename")
    @DDoSPreMonitor(action = "renameFidoV2")
    public CommonRet<Boolean> renameFidoV2(@RequestBody @Valid RenameArg arg) throws Exception {
        HashMap<String, String> deviceInfo =
                commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
        riskService.getRiskChallengeTimeOut(getUserId(), deviceInfo, BizSceneEnum.RENAME_FIDO.name());

        RenameRequest request = new RenameRequest();
        request.setRpId(this.rpId);
        request.setUserId(String.valueOf(getUserId()));
        request.setCredId(arg.getCredentialId());
        request.setNewName(arg.getNewName());
        log.info("fido renameV2 request:{}", JSON.toJSONString(request));
        APIResponse<Boolean> renameResult = fido2Api.rename(APIRequest.instance(request));
        log.info("fido renameV2 response:{}", JSON.toJSONString(renameResult));
        this.baseHelper.checkResponse(renameResult);
        return new CommonRet<>(renameResult.getData());
    }

}
