package com.binance.mgs.account.api.controller;

import com.binance.account.vo.apimanage.response.ApiIpTradeCheckResponse;
import com.binance.account.vo.apimanage.response.ApiModelResponse;
import com.binance.account.vo.apimanage.response.QueryApiKeyWhitelistResponse;
import com.binance.account.vo.device.response.AddUserDeviceResponse;
import com.binance.account.vo.subuser.request.UserIdReq;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.accountapimanage.api.AccountApiManage;
import com.binance.accountapimanage.api.AccountApiManageIpConfigApi;
import com.binance.accountapimanage.api.AccountApiManageQueryApi;
import com.binance.accountapimanage.api.AccountApiManageUpdateApi;
import com.binance.accountapimanage.vo.apimanage.request.QueryAllApiManageIpConfigRequest;
import com.binance.accountapimanage.vo.apimanage.request.QueryApiListByUserIdRequest;
import com.binance.accountapimanage.vo.apimanage.request.SaveApiKeyRequest;
import com.binance.accountapimanage.vo.apimanage.request.UpdateApiKeyRequest;
import com.binance.accountapimanage.vo.apimanage.response.QueryAllApiManageIpConfigResponse;
import com.binance.accountapimanage.vo.apimanage.response.QueryApiListByUserIdResponse;
import com.binance.accountapimanage.vo.apimanage.response.SaveApiKeyResponse;
import com.binance.accountapimanage.vo.apimanage.response.UpdateApiKeyResponse;
import com.binance.accountpersonalcenter.api.UserPersonalConfigApi;
import com.binance.accountpersonalcenter.vo.enums.UserPersonalConfigEnum;
import com.binance.accountpersonalcenter.vo.userpersonalconfig.PostUserPersonalConfigRequest;
import com.binance.accountpersonalcenter.vo.userpersonalconfig.PostUserPersonalConfigResp;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.ApiKeyKycCheck;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.api.vo.AllApiInfoRet;
import com.binance.mgs.account.api.vo.AllApiManageIpConfigRet;
import com.binance.mgs.account.api.vo.ApiInfoRet;
import com.binance.mgs.account.api.vo.ApiIpTradeCheckRet;
import com.binance.mgs.account.api.vo.ApiKeyRiskDisclaimerArg;
import com.binance.mgs.account.api.vo.ApiKycCheckRet;
import com.binance.mgs.account.api.vo.ApiManageIpConfigRet;
import com.binance.mgs.account.api.vo.BaseApiArg;
import com.binance.mgs.account.api.vo.CheckApiKycArg;
import com.binance.mgs.account.api.vo.CheckApiTradeIpArg;
import com.binance.mgs.account.api.vo.DeleteApiArg;
import com.binance.mgs.account.api.vo.QueryAllApiManageIpConfigArg;
import com.binance.mgs.account.api.vo.QueryApiKeyWhitelistArg;
import com.binance.mgs.account.api.vo.QuerySubUserApiKeyWhitelistRet;
import com.binance.mgs.account.api.vo.SaveApiV2Arg;
import com.binance.mgs.account.api.vo.SaveApiV2Ret;
import com.binance.mgs.account.api.vo.UpdateApiV2Arg;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.service.AccountApiManageClient;
import com.binance.mgs.account.service.RiskService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.SystemMaintenance;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.utils.CommonUtil;
import com.binance.userbigdata.vo.kyc.response.KycBriefInfoResp;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@Slf4j
public class ApiManagementController extends AccountBaseAction {
    @Resource
    private ApiHelper apiHelper;
    @Autowired
    private AccountApiManage accountApiManage;

    @Autowired
    private AccountApiManageQueryApi accountApiManageQueryApi;

    @Autowired
    private AccountApiManageUpdateApi accountApiManageUpdateApi;
    @Value("${update.api.ip.length:500}")
    private int apiIpLength;

    @Autowired
    private AccountApiManageClient accountApiManageClient;

    @Autowired
    private AccountApiManageIpConfigApi accountApiManageIpConfigApi;
    @Autowired
    private UserPersonalConfigApi userPersonalConfigApi;
    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Autowired
    private RiskService riskService;

    private static final String IP = "0.0.0.0";


    @ApiOperation(value = "获取用户所有API")
    @PostMapping(value = "/v1/private/api-mgmt/api/all")
    public CommonRet<AllApiInfoRet> getAllApi() throws Exception {
        List<ApiModelResponse> apiModelResponseList = Lists.newArrayList();
        QueryApiListByUserIdRequest queryApiListByUserIdRequest = new QueryApiListByUserIdRequest();
        queryApiListByUserIdRequest.setUserId(getUserId());
        APIResponse<QueryApiListByUserIdResponse> response = accountApiManageQueryApi.getApiListByUserId(APIRequest.instance(queryApiListByUserIdRequest));
        checkResponse(response);
        for (com.binance.accountapimanage.vo.apimanage.response.ApiModelResponse apiModel : response.getData().getApiModels()) {
            apiModelResponseList.add(CopyBeanUtils.fastCopy(apiModel, ApiModelResponse.class));
        }

        apiModelResponseList = apiHelper.filterUnconfirmed(apiModelResponseList);
        CommonRet<AllApiInfoRet> ret = new CommonRet<>();
        AllApiInfoRet data = new AllApiInfoRet();
        data.setCurrentIp(WebUtils.getRequestIp());
        ret.setData(data);
        if (!CollectionUtils.isEmpty(apiModelResponseList)) {
            List<ApiInfoRet> apiInfos = Lists.newArrayList();
            for (ApiModelResponse apiModelResponse : apiModelResponseList) {
                if (IP.equalsIgnoreCase(apiModelResponse.getWithdrawIp())) {
                    apiModelResponse.setWithdrawIp("");
                }
                if (IP.equalsIgnoreCase(apiModelResponse.getTradeIp())) {
                    apiModelResponse.setTradeIp("");
                }
                ApiInfoRet apiInfoRet = new ApiInfoRet();
                BeanUtils.copyProperties(apiModelResponse, apiInfoRet);
                apiInfoRet.setTaxReport(StringUtils.equals(apiModelResponse.getTag(),"Tax Report"));
                apiInfos.add(apiInfoRet);
            }
            data.setApiInfos(apiInfos);
        }
        return ret;
    }

    @ApiOperation(value = "保存API")
    @PostMapping(value = "/v2/private/account/api-mgmt/api/save")
    @UserOperation(eventName = "saveApiV2", name = "保存api",
            logDeviceOperation = true, deviceOperationNoteField = {"apiName"},
            requestKeys = {"apiName"}, requestKeyDisplayNames = {"api名字"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    @AntiBotCaptchaValidate(bizType = {BizType.CREATE_APIKEY})
    public CommonRet<SaveApiV2Ret> saveApi(HttpServletRequest request, HttpServletResponse response,@Valid @RequestBody SaveApiV2Arg saveApiArg) throws Exception {
        if (StringUtils.isBlank(saveApiArg.getApiName()) || CommonUtil.isContainSpecialChar(saveApiArg.getApiName())) {
            log.info("apiname = {} contains special char", saveApiArg.getApiName());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        UserStatusEx userStatusEx = getUserStatusByUserId(getUserId());
        if (!(userStatusEx.getIsBrokerSubUserFunctionEnabled() || userStatusEx.getIsBrokerSubUser())) {
            KycBriefInfoResp resp = apiHelper.checkPassAboveIntermediateKyc(getUserId());
            if (!resp.isPass()) {
                throw new BusinessException(AccountMgsErrorCode.API_NEED_KYC_COMPLETE);
            }
        }
        CommonRet<SaveApiV2Ret> ret = new CommonRet<>();
        SaveApiV2Ret data = new SaveApiV2Ret();
        Map<String, String> deviceInfo = this.apiHelper.logDeviceInfo();
        SaveApiKeyRequest saveApiKeyRequest = new SaveApiKeyRequest();
        BeanUtils.copyProperties(saveApiArg, saveApiKeyRequest);
        saveApiKeyRequest.setLoginUserId(getUserIdStr());
        saveApiKeyRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
        saveApiKeyRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(request));
        APIResponse<SaveApiKeyResponse> apiResponse = accountApiManage.createApiKey(APIRequest.instance(saveApiKeyRequest));
        checkResponseAndLog2fa(apiResponse);
        if (apiResponse.getData() != null) {
            BeanUtils.copyProperties(apiResponse.getData(), data);
            if (IP.equalsIgnoreCase(data.getWithdrawIp())) {
                data.setWithdrawIp("");
            }
            if (IP.equalsIgnoreCase(data.getTradeIp())) {
                data.setTradeIp("");
            }
            data.setTaxReport(StringUtils.equals(apiResponse.getData().getTag(), "Tax Report"));
            data.setIsFlexLineCreditUser(userStatusEx.getIsFlexLineCreditUser());
            data.setIsFlexLineTradingUser(userStatusEx.getIsFlexLineTradingUser());
            ret.setData(data);
        }
        if (apiResponse.getData() != null && apiResponse.getData().getUserId() != null) {
            UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, apiResponse.getData().getUserId()));
        }
        AddUserDeviceResponse addUserDeviceResponse = this.apiHelper.associateSensitiveDevice("create_api", deviceInfo);
        if (addUserDeviceResponse != null) {
            log.info("add user device when creating api. userId={}, deviceId={}, ",
                    addUserDeviceResponse == null ? "null" : addUserDeviceResponse.getUserId(),
                    addUserDeviceResponse == null ? "null" : addUserDeviceResponse.getDeviceId());
        }
        return ret;
    }

    @ApiOperation(value = "修改API")
    @PostMapping(value = "/v2/private/account/api-mgmt/api/update")
    @UserOperation(eventName = "updateApi", name = "修改api",
            logDeviceOperation = true, deviceOperationNoteField = {"keyId", "apiName"},
            requestKeys = {"keyId"}, requestKeyDisplayNames = {"keyId"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    public CommonRet<String> updateApiV2(HttpServletRequest request, HttpServletResponse response,  @Valid @RequestBody UpdateApiV2Arg updateApiArg) throws Exception {
        UserStatusEx userStatusEx = getUserStatusByUserId(getUserId());
        if (!(userStatusEx.getIsBrokerSubUserFunctionEnabled() || userStatusEx.getIsBrokerSubUser())) {
            KycBriefInfoResp resp = apiHelper.checkPassAboveIntermediateKyc(getUserId(), true);
            if (!resp.isPass()) {
                throw new BusinessException(AccountMgsErrorCode.API_UPDATE_NEED_KYC_COMPLETE);
            }
        }
        if (StringUtils.isNotBlank(updateApiArg.getIp())&&updateApiArg.getIp().length()>apiIpLength){
            throw new BusinessException(AccountMgsErrorCode.TOO_MANY_IPS);
        }

        UserOperationHelper.log("ruleId", updateApiArg.getRuleId());
        UserOperationHelper.log("symbols", updateApiArg.getSymbols());
        UpdateApiKeyRequest updateApiKeyRequest = new UpdateApiKeyRequest();
        updateApiKeyRequest.setLoginUserId(getUserIdStr());
        updateApiKeyRequest.setApiName(updateApiArg.getApiName());
        updateApiKeyRequest.setRuleId(Long.valueOf(updateApiArg.getRuleId()));
        updateApiKeyRequest.setStatus(updateApiArg.getStatus());
        updateApiKeyRequest.setIp(updateApiArg.getIp());
        updateApiKeyRequest.setSymbols(updateApiArg.getSymbols());
        updateApiKeyRequest.setApiManageIpConfigId(updateApiArg.getApiManageIpConfigId());
        updateApiKeyRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(request));
        Map<String, String> deviceInfo = this.apiHelper.logDeviceInfo();
        updateApiKeyRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
        updateApiKeyRequest.setEmailVerifyCode(updateApiArg.getEmailVerifyCode());
        updateApiKeyRequest.setMobileVerifyCode(updateApiArg.getMobileVerifyCode());
        updateApiKeyRequest.setGoogleVerifyCode(updateApiArg.getGoogleVerifyCode());
        updateApiKeyRequest.setYubikeyVerifyCode(updateApiArg.getYubikeyVerifyCode());
        updateApiKeyRequest.setKeyId(updateApiArg.getKeyId());
        updateApiKeyRequest.setIsFromFE(updateApiArg.getIsFromFE());
        APIResponse<UpdateApiKeyResponse> apiResponse = accountApiManageUpdateApi.updateApiKey(APIRequest.instance(updateApiKeyRequest));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }

    @ApiOperation(value = "根据API-ID删除API")
    @PostMapping(value = "/v1/private/api-mgmt/api/del")
    @UserOperation(eventName = "deleteApi", name = "删除api",
            logDeviceOperation = true, deviceOperationNoteField = {"keyId"},
            requestKeys = {"keyId"}, requestKeyDisplayNames = {"keyId"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    public CommonRet<Void> deleteApi(HttpServletRequest request, @Valid @RequestBody DeleteApiArg deleteApiArg) throws Exception {
        this.apiHelper.logDeviceInfo();
        accountApiManageClient.deleteApiKey(request, getUserId(), deleteApiArg.getApiKey());
        return new CommonRet<>();
    }

    @ApiOperation(value = "删除所有API")
    @PostMapping(value = "/v1/private/api-mgmt/api/delall")
    @UserOperation(eventName = "deleteAllApi", name = "删除全部api",
            logDeviceOperation = true, deviceOperationNoteField = {"id"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    public CommonRet<Void> deleteAllApi(HttpServletRequest request, @Valid @RequestBody BaseApiArg baseApiArg) throws Exception {
        this.apiHelper.logDeviceInfo();
        accountApiManageClient.deleteAllApiKey(request);
        return new CommonRet<>();
    }

    @ApiOperation(value = "检查trade权限的ip限制")
    @PostMapping(value = "/v1/private/api-mgmt/api/trade-ip-check")
    @SystemMaintenance(isApi = true)
    public CommonRet<ApiIpTradeCheckRet> queryApiIpTradeCheck(@Valid @RequestBody CheckApiTradeIpArg apiTradeIpArg) throws Exception {
        Long loginUserId = checkAndGetUserId();

        com.binance.accountapimanage.vo.apimanage.request.CheckApiIpTradeRequest checkApiIpTradeRequest = new com.binance.accountapimanage.vo.apimanage.request.CheckApiIpTradeRequest();
        checkApiIpTradeRequest.setUserId(loginUserId);
        checkApiIpTradeRequest.setKeyId(apiTradeIpArg.getKeyId());
        APIResponse<com.binance.accountapimanage.vo.apimanage.response.ApiIpTradeCheckResponse> resp = accountApiManageQueryApi.queryApiIpTradeCheck(APIRequest.instance(checkApiIpTradeRequest));
        checkResponse(resp);
        ApiIpTradeCheckResponse data = CopyBeanUtils.fastCopy(resp.getData(), ApiIpTradeCheckResponse.class);

        ApiIpTradeCheckRet ret = new ApiIpTradeCheckRet();
        BeanUtils.copyProperties(data, ret);
        return new CommonRet<>(ret);
    }

    @ApiOperation(value = "检查trade权限的ip限制")
    @PostMapping(value = "/v1/private/api-mgmt/kyc-check")
    @SystemMaintenance(isApi = true)
    public CommonRet<ApiKycCheckRet> checkKyc() throws Exception {
        Long loginUserId = checkAndGetUserId();
        UserStatusEx userStatusEx = getUserStatusByUserId(loginUserId);
        ApiKycCheckRet data = new ApiKycCheckRet();
        if (!(userStatusEx.getIsBrokerSubUserFunctionEnabled() || userStatusEx.getIsBrokerSubUser())) {
            KycBriefInfoResp resp = apiHelper.checkPassAboveIntermediateKyc(loginUserId, false);
            BeanUtils.copyProperties(resp, data);
        } else {
            data.setPass(true);
        }
        return new CommonRet<>(data);
    }

    @ApiOperation(value = "检查trade权限的ip限制")
    @PostMapping(value = "/v2/private/api-mgmt/kyc-check")
    @SystemMaintenance(isApi = true)
    public CommonRet<ApiKycCheckRet> checkKycV2(@RequestBody CheckApiKycArg checkApiKycArg) throws Exception {
        Long loginUserId = checkAndGetUserId();
        UserStatusEx userStatusEx = getUserStatusByUserId(loginUserId);
        ApiKycCheckRet data = new ApiKycCheckRet();
        if (!(userStatusEx.getIsBrokerSubUserFunctionEnabled() || userStatusEx.getIsBrokerSubUser())) {
            KycBriefInfoResp resp = apiHelper.checkPassAboveIntermediateKyc(loginUserId, checkApiKycArg.getIsUpdate());
            BeanUtils.copyProperties(resp, data);
        } else {
            data.setPass(true);
        }
        return new CommonRet<>(data);
    }

    @ApiOperation(value = "查询API币种白名单")
    @PostMapping(value = "/v1/private/api-mgmt/api/query-whitelist")
    @SystemMaintenance(isApi = true)
    @ApiKeyKycCheck
    public CommonRet<QuerySubUserApiKeyWhitelistRet> queryApiKeyWhitelist(@Valid @RequestBody QueryApiKeyWhitelistArg arg) throws Exception {
        Long userId = checkAndGetUserId();

        com.binance.accountapimanage.vo.apimanage.request.GetApiKeyWhitelistRequest getApiKeyWhitelistRequest = new com.binance.accountapimanage.vo.apimanage.request.GetApiKeyWhitelistRequest();
        getApiKeyWhitelistRequest.setKeyId(arg.getKeyId());
        getApiKeyWhitelistRequest.setUserId(userId);
        APIResponse<com.binance.accountapimanage.vo.apimanage.response.QueryApiKeyWhitelistResponse> resp = accountApiManageQueryApi.getApiKeyWhitelist(APIRequest.instance(getApiKeyWhitelistRequest));
        checkResponse(resp);
        QueryApiKeyWhitelistResponse data = CopyBeanUtils.fastCopy(resp.getData(), QueryApiKeyWhitelistResponse.class);

        QuerySubUserApiKeyWhitelistRet ret = new QuerySubUserApiKeyWhitelistRet();
        BeanUtils.copyProperties(data, ret);
        return new CommonRet<>(ret);
    }

    @ApiOperation(value = "获取所有第三方ip配置")
    @PostMapping(value = "/v1/private/api-mgmt/api/third-party/ip-configs/all")
    public CommonRet<AllApiManageIpConfigRet> getAllThirdPartyIpConfigs(@Valid @RequestBody QueryAllApiManageIpConfigArg arg) throws Exception {

        QueryAllApiManageIpConfigRequest queryAllApiManageIpConfigRequest = CopyBeanUtils.fastCopy(arg, QueryAllApiManageIpConfigRequest.class);
        APIResponse<List<QueryAllApiManageIpConfigResponse>> response = accountApiManageIpConfigApi.queryAllApiManageIpConfig(APIRequest.instance(queryAllApiManageIpConfigRequest));
        checkResponse(response);

        AllApiManageIpConfigRet ret = new AllApiManageIpConfigRet();
        List<QueryAllApiManageIpConfigResponse> data = response.getData();
        if (CollectionUtils.isEmpty(data)) {
            return new CommonRet<>(ret);
        }

        List<ApiManageIpConfigRet> apiManageIpConfigs = Lists.newArrayList();
        for (QueryAllApiManageIpConfigResponse resp : data) {
            ApiManageIpConfigRet apiManageIpConfigRet = new ApiManageIpConfigRet();
            apiManageIpConfigRet.setId(resp.getId());
            apiManageIpConfigRet.setApiManageIpConfigName(resp.getThirdPartyName());
            apiManageIpConfigs.add(apiManageIpConfigRet);
        }
        ret.setApiManageIpConfigRets(apiManageIpConfigs);
        return new CommonRet<>(ret);
    }

    /**
     * 签署免责协议，子账户不能签，broker不用签
     * 后续判断 子账户看母账户状态，margin账户看spot账户状态，social trading账户看主账户的状态
     * @param arg
     * @return
     * @throws Exception
     */
    @UserOperation(eventName = "signApiKeyRiskDisclaimer", name = "签署apiKey风险免责声明",
            logDeviceOperation = true, deviceOperationNoteField = {"apiName"},
            requestKeys = {"configValue"}, requestKeyDisplayNames = {"configValue"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/v1/private/api-mgmt/sign/disclaimer")
    public CommonRet<Void> signApiKeyRiskDisclaimer(@Valid @RequestBody ApiKeyRiskDisclaimerArg arg) throws Exception {
        Long userId = checkAndGetUserId();
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        UserStatusEx userStatusEx = getUserStatusByUserId(userId);
        if (userStatusEx.getIsBrokerSubUserFunctionEnabled() || userStatusEx.getIsBrokerSubUser() || userStatusEx.getIsSubUser()) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }

        if (StringUtils.equals(arg.getConfigValue(), "opt-out")) {
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            riskService.getRiskChallengeTimeOut(this.baseHelper.getUserId(), deviceInfo, BizSceneEnum.API_KEY_DELETE_CONTRACT_SIGN.name());
        }

        PostUserPersonalConfigRequest request = new PostUserPersonalConfigRequest();
        request.setUserId(userId);
        request.setConfigType(UserPersonalConfigEnum.AGREE_APIKEY_IN_RISK_DECLARATION.getType());
        request.setConfigValue(arg.getConfigValue());
        APIResponse<PostUserPersonalConfigResp> apiResponse = userPersonalConfigApi.modifyUserPersonalConfig(baseHelper.getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }
}
