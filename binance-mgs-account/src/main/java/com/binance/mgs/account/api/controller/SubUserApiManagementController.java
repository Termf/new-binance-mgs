package com.binance.mgs.account.api.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.apimanage.response.ApiIpTradeCheckResponse;
import com.binance.account.vo.apimanage.response.QueryApiKeyWhitelistResponse;
import com.binance.account.vo.device.response.AddUserDeviceResponse;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.request.VerificationTwoV3Request;
import com.binance.account.vo.security.response.VerificationTwoV3Response;
import com.binance.account.vo.subuser.request.BindingParentSubUserEmailReq;
import com.binance.account.vo.subuser.request.BindingParentSubUserReq;
import com.binance.account.vo.subuser.response.BindingParentSubUserEmailResp;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.accountadmin.api.AccountAdminUserApi;
import com.binance.accountadmin.vo.batchsearch.request.GetMergeUserListRequest;
import com.binance.accountadmin.vo.batchsearch.vo.UserVo;
import com.binance.accountapimanage.api.AccountApiManage;
import com.binance.accountapimanage.api.AccountApiManageQueryApi;
import com.binance.accountapimanage.api.AccountApiManageUpdateApi;
import com.binance.accountapimanage.vo.apimanage.request.CreateSubUserApiKeyRequest;
import com.binance.accountapimanage.vo.apimanage.request.GetApiListByParentRequest;
import com.binance.accountapimanage.vo.apimanage.request.QueryApiByApiKeyRequest;
import com.binance.accountapimanage.vo.apimanage.request.UpdateSubUserApiKeyRequest;
import com.binance.accountapimanage.vo.apimanage.response.PagingResult;
import com.binance.accountapimanage.vo.apimanage.response.QueryApiByApiKeyResponse;
import com.binance.accountapimanage.vo.apimanage.response.SaveApiKeyResponse;
import com.binance.accountapimanage.vo.apimanage.response.UpdateApiKeyResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.accountsubuser.api.FlexLineSubApi;
import com.binance.accountsubuser.vo.subuser.request.FlexLineQuerySubUserReq;
import com.binance.accountsubuser.vo.subuser.response.FlexLineQuerySubUserResp;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.LogMaskUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.controller.client.SubUserClient;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.ApiKeyKycCheck;
import com.binance.mgs.account.api.helper.ApiHelper;
import com.binance.mgs.account.api.vo.AllApiInfoPageRet;
import com.binance.mgs.account.api.vo.ApiIpTradeCheckRet;
import com.binance.mgs.account.api.vo.CheckSubUserApiTradeIpArg;
import com.binance.mgs.account.api.vo.QuerySubUserApiKeyWhitelistArg;
import com.binance.mgs.account.api.vo.QuerySubUserApiKeyWhitelistRet;
import com.binance.mgs.account.api.vo.ResetEnableTradeTimeArg;
import com.binance.mgs.account.api.vo.ResetEnableTradeTimeRet;
import com.binance.mgs.account.api.vo.SaveApiV2Ret;
import com.binance.mgs.account.api.vo.SubUserApiInfoRet;
import com.binance.mgs.account.api.vo.SubUserDeleteApiArg;
import com.binance.mgs.account.api.vo.SubUserGetAllApiInfoArg;
import com.binance.mgs.account.api.vo.SubUserSaveApiArg;
import com.binance.mgs.account.api.vo.SubUserUpdateApiArg;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.service.AccountApiManageClient;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.SystemMaintenance;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.CommonUtil;
import com.binance.userbigdata.vo.kyc.response.KycBriefInfoResp;
import com.binance.userbigdata.vo.subuser.request.GetSubUserApisRequest;
import com.binance.userbigdata.vo.subuser.request.GetSubUserBindsRequest;
import com.binance.userbigdata.vo.subuser.response.ApiManageResponse;
import com.binance.userbigdata.vo.subuser.response.SubUserBindingVo;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@Slf4j
public class SubUserApiManagementController extends AccountBaseAction {
    @Resource
    private ApiHelper apiHelper;

    @Autowired
    private SubUserClient subUserClient;

    @Autowired
    private UserSecurityApi userSecurityApi;

    @Autowired
    private FlexLineSubApi flexLineSubApi;

    @Autowired
    private AccountApiManage accountApiManage;
    @Autowired
    private AccountApiManageUpdateApi accountApiManageUpdateApi;

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Value("${sub.user.get.all.flexline.parent:true}")
    private Boolean querySubUserToGetAllFlexLineParentId;

    @Autowired
    private AccountApiManageQueryApi accountApiManageQueryApi;

    @Autowired
    private AccountApiManageClient accountApiManageClient;

    @Autowired
    private AccountAdminUserApi accountAdminUserApi;

    @Value("${reset.sub.api.key.num:10}")
    private int resetSubApiKeyNum;
    @Value("${update.api.ip.length:500}")
    private int apiIpLength;

    private static final String IP = "0.0.0.0";


    @ApiOperation(value = "获取用户所有API")
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/api/all")
    public CommonRet<AllApiInfoPageRet> getAllApi(@Valid @RequestBody SubUserGetAllApiInfoArg arg) throws Exception {
        Long parentUserId = getUserId();
        String email = arg.getSubUserEmail();
        log.info("getSubUserApiList parentId={},request={}", getUserIdStr(), JsonUtils.toJsonHasNullKey(arg));

        FlexLineQuerySubUserResp flexLineSubData = new FlexLineQuerySubUserResp();
        if(querySubUserToGetAllFlexLineParentId){
            APIResponse<List<Long>> allAvailableFlexLineParent = flexLineSubApi.getAllAvailableFlexLineParent();
            checkResponse(allAvailableFlexLineParent);
            List<Long> flexLineParentUserIds = allAvailableFlexLineParent.getData();
            if(!CollectionUtils.isEmpty(flexLineParentUserIds) && flexLineParentUserIds.contains(parentUserId)){
                FlexLineQuerySubUserReq flexLineQuerySubUserReq = new FlexLineQuerySubUserReq();
                flexLineQuerySubUserReq.setParentUserId(parentUserId);
                APIResponse<FlexLineQuerySubUserResp> queryFlexLineSub = flexLineSubApi.queryFlexLineSub(APIRequest.instance(flexLineQuerySubUserReq));
                checkResponse(queryFlexLineSub);
                flexLineSubData = queryFlexLineSub.getData();
            }
        }
        FlexLineQuerySubUserResp finalFlexLineSubData = flexLineSubData;

        if (StringUtils.isNotBlank(arg.getApiKey())) {
            return querySubUserApiKeyByApiKey(arg.getApiKey(), parentUserId, finalFlexLineSubData);
        }

        GetSubUserApisRequest apisRequest = new GetSubUserApisRequest();
        apisRequest.setParentUserId(parentUserId);
        if (StringUtils.isNotBlank(email)){
            Long subUserId = getSubUserIdAndCheckRelation(parentUserId, email);
            apisRequest.setSubUserId(subUserId);
        }
        apisRequest.setApiName(arg.getApiName());
        apisRequest.setPage(arg.getPage());
        apisRequest.setRows(arg.getRows());

        List<ApiManageResponse> apiManageList = Lists.newArrayList();
        GetApiListByParentRequest getApiListByParentRequest = CopyBeanUtils.fastCopy(apisRequest, GetApiListByParentRequest.class);
        APIResponse<PagingResult<com.binance.accountapimanage.vo.apimanage.response.ApiModelResponse>> response = accountApiManageQueryApi.getApiListByParent(APIRequest.instance(getApiListByParentRequest));
        checkResponse(response);
        long total = response.getData().getTotal();
        for (com.binance.accountapimanage.vo.apimanage.response.ApiModelResponse row : response.getData().getRows()) {
            ApiManageResponse apiManageResponse = new ApiManageResponse();
            BeanUtils.copyProperties(row, apiManageResponse);
            apiManageResponse.setId(Long.valueOf(row.getId()));
            apiManageList.add(apiManageResponse);
        }

        List<Long> subUserIds = apiManageList.stream().mapToLong(v -> Long.parseLong(v.getUserId())).boxed().collect(Collectors.toList());
        GetSubUserBindsRequest bindsRequest = new GetSubUserBindsRequest();
        bindsRequest.setParentUserId(parentUserId);
        Map<Long, Long> userIdToStatusExtraMap = Maps.newHashMap();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(subUserIds)) {
            Map<String, Object> params = Maps.newHashMap();
            params.put("subUserIds", subUserIds);
            bindsRequest.setQueryParams(params);

            GetMergeUserListRequest getMergeUserListRequest = new GetMergeUserListRequest();
            getMergeUserListRequest.setUserIds(subUserIds);
            APIResponse<List<UserVo>> userStatusByUserIds = accountAdminUserApi.getUserStatusByUserIds(APIRequest.instance(getMergeUserListRequest));
            checkResponse(userStatusByUserIds);
            List<UserVo> userStatusVos = userStatusByUserIds.getData();
            userIdToStatusExtraMap = userStatusVos.stream().collect(Collectors.toMap(UserVo::getUserId, UserVo::getStatusExtra, (key1, key2) -> key2));
        }
        APIResponse<List<SubUserBindingVo>> subUserBindingsApiResp = subUserClient.getSubUserBindingsByParent(APIRequest.instance(bindsRequest));
        checkResponse(subUserBindingsApiResp);

        List<SubUserBindingVo> subUserBindingVos = subUserBindingsApiResp.getData();
        Map<Long, SubUserBindingVo> subUserBindingVoMap = Maps.uniqueIndex(subUserBindingVos, SubUserBindingVo::getSubUserId);

        AllApiInfoPageRet ret = new AllApiInfoPageRet();
        if (!CollectionUtils.isEmpty(apiManageList)) {
            List<SubUserApiInfoRet> apiInfos = Lists.newArrayList();
            for (ApiManageResponse apiManageResponse : apiManageList) {
                SubUserApiInfoRet apiInfoRet = new SubUserApiInfoRet();
                BeanUtils.copyProperties(apiManageResponse, apiInfoRet);

                Long userId = Long.valueOf(apiManageResponse.getUserId());
                SubUserBindingVo bindingVo = subUserBindingVoMap.get(userId);
                UserStatusEx userStatusEx = new UserStatusEx(bindingVo.getStatus(), userIdToStatusExtraMap.get(userId));

                apiInfoRet.setId(apiManageResponse.getId().toString());
                apiInfoRet.setEmail(bindingVo.getEmail());
                apiInfoRet.setIsExistMarginAccount(userStatusEx.getIsExistMarginAccount());
                apiInfoRet.setIsExistFutureAccount(userStatusEx.getIsExistFutureAccount());
                apiInfoRet.setIsPortfolioMarginRetailUser(userStatusEx.getIsPortfolioMarginRetailUser());
                if (IP.equalsIgnoreCase(apiInfoRet.getWithdrawIp())) {
                    apiInfoRet.setWithdrawIp("");
                }
                if (IP.equalsIgnoreCase(apiInfoRet.getTradeIp())) {
                    apiInfoRet.setTradeIp("");
                }
                if(finalFlexLineSubData.getCreditSubUserId()!=null){
                    if(Objects.equals(userId, finalFlexLineSubData.getCreditSubUserId())){
                        apiInfoRet.setIsFlexLineCreditUser(true);
                    }
                }
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(finalFlexLineSubData.getTradingSubUserIds())){
                    if(finalFlexLineSubData.getTradingSubUserIds().contains(userId)){
                        apiInfoRet.setIsFlexLineTradingUser(true);
                    }
                }
                apiInfoRet.setCanResetEnableTradeTime(accountApiManageClient.checkCanResetEnableTradeTime(apiManageResponse.getEnableTradeTime(), apiManageResponse.getTradeIp()));
                apiInfos.add(apiInfoRet);
            }
            ret.setApiInfos(apiInfos);
        }
        ret.setCurrentIp(WebUtils.getRequestIp());
        ret.setTotal(total);
        return new CommonRet<>(ret);
    }

    @ApiOperation(value = "保存API")
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/api/save")
    @UserOperation(eventName = "saveApi", name = "保存api",
            logDeviceOperation = true, deviceOperationNoteField = {"apiName"},
            requestKeys = {"apiName"}, requestKeyDisplayNames = {"api名字"},
            responseKeys = {"$.success", }, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    @AntiBotCaptchaValidate(bizType = {BizType.CREATE_APIKEY})
    public CommonRet<SaveApiV2Ret> subUserSaveApi(HttpServletRequest httpRequest, HttpServletResponse response, @Valid @RequestBody SubUserSaveApiArg saveApiArg) throws Exception {
        Long parentUserId = getUserId();
        log.info("subUserSaveApi parentId={},email={}",parentUserId,saveApiArg.getSubUserEmail());
        if (StringUtils.isBlank(saveApiArg.getApiName()) || CommonUtil.isContainSpecialChar(saveApiArg.getApiName())) {
            log.info("apiname = {} contains special char", saveApiArg.getApiName());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
        if (!(userStatusEx.getIsBrokerSubUserFunctionEnabled() || userStatusEx.getIsBrokerSubUser())) {
            KycBriefInfoResp resp = apiHelper.checkPassAboveIntermediateKyc(parentUserId);
            if (!resp.isPass()) {
                throw new BusinessException(AccountMgsErrorCode.API_NEED_KYC_COMPLETE);
            }
        }
        //check 2fa
        VerificationTwoV3Request verificationTwoV3Request = new VerificationTwoV3Request();
        verificationTwoV3Request.setUserId(parentUserId);
        verificationTwoV3Request.setBizScene(BizSceneEnum.API_KEY_MANAGE);
        verificationTwoV3Request.setGoogleVerifyCode(saveApiArg.getGoogleVerifyCode());
        verificationTwoV3Request.setMobileVerifyCode(saveApiArg.getMobileVerifyCode());
        verificationTwoV3Request.setEmailVerifyCode(saveApiArg.getEmailVerifyCode());
        verificationTwoV3Request.setYubikeyVerifyCode(saveApiArg.getYubikeyVerifyCode());
        APIResponse<VerificationTwoV3Response> verifyResult = userSecurityApi.verificationsTwoV3(APIRequest.instance(verificationTwoV3Request));
        checkResponse(verifyResult);

        //check 子母账号关系
        Long subUserId=getSubUserIdAndCheckRelation(parentUserId,saveApiArg.getSubUserEmail());
        //不允许操作资管子账号
        checkIfAssetSubUser(subUserId);
        Map<String, String> deviceInfo = this.apiHelper.logDeviceInfo();
        UserStatusEx userStatus = getUserStatusByUserId(subUserId);

        CommonRet<SaveApiV2Ret> ret = new CommonRet<>();
        CreateSubUserApiKeyRequest createSubUserApiKeyRequest = new CreateSubUserApiKeyRequest();
        createSubUserApiKeyRequest.setParentUserId(getLoginUserId());
        createSubUserApiKeyRequest.setSubUserId(subUserId);
        createSubUserApiKeyRequest.setApiName(saveApiArg.getApiName());
        createSubUserApiKeyRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
        createSubUserApiKeyRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(httpRequest));
        createSubUserApiKeyRequest.setPublicKey(saveApiArg.getPublicKey());
        createSubUserApiKeyRequest.setIsFromFE(saveApiArg.getIsFromFE());
        APIResponse<SaveApiKeyResponse> apiResponse = accountApiManage.createSubApiKey(APIRequest.instance(createSubUserApiKeyRequest));
        checkResponseAndLog2fa(apiResponse);
        if (apiResponse.getData() != null) {
            SaveApiV2Ret data = new SaveApiV2Ret();
            BeanUtils.copyProperties(apiResponse.getData(), data);

            if (userStatus != null) {
                data.setIsExistFutureAccount(userStatus.getIsExistFutureAccount());
                data.setIsExistMarginAccount(userStatus.getIsExistMarginAccount());
                data.setIsFlexLineCreditUser(userStatus.getIsFlexLineCreditUser());
                data.setIsFlexLineTradingUser(userStatus.getIsFlexLineTradingUser());
            }

            if (IP.equalsIgnoreCase(data.getWithdrawIp())) {
                data.setWithdrawIp("");
            }
            if (IP.equalsIgnoreCase(data.getTradeIp())) {
                data.setTradeIp("");
            }

            ret.setData(data);
        }
        AddUserDeviceResponse addUserDeviceResponse = this.apiHelper.associateSensitiveDevice("create_api",deviceInfo);
        if(addUserDeviceResponse!=null){
            log.info("add user device when creating api. userId={}, deviceId={}, ",
                    addUserDeviceResponse == null ? "null" : addUserDeviceResponse.getUserId(),
                    addUserDeviceResponse == null ? "null" : addUserDeviceResponse.getDeviceId());
        }
        return ret;
    }


    @ApiOperation(value = "修改API")
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/api/update")
    @UserOperation(eventName = "updateApi", name = "修改api",
            logDeviceOperation = true, deviceOperationNoteField = {"keyId", "apiName"},
            requestKeys = {"keyId"}, requestKeyDisplayNames = {"keyId"},
            responseKeys = {"$.success", }, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    public CommonRet<String> subUserUpdateApi(HttpServletRequest httpRequest, HttpServletResponse response,@Valid @RequestBody SubUserUpdateApiArg updateApiArg) throws Exception {
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

        log.info("subUserUpdateApi parentId={},email={}",getUserId(),updateApiArg.getSubUserEmail());
        UserOperationHelper.log("ruleId", updateApiArg.getRuleId());
        UserOperationHelper.log("symbols", updateApiArg.getSymbols());
        //check 子母账号关系
        Long subUserId=getSubUserIdAndCheckRelation(getUserId(),updateApiArg.getSubUserEmail());
        //不允许操作资管子账号
        checkIfAssetSubUser(subUserId);

        UpdateSubUserApiKeyRequest updateSubUserApiKeyRequest = new UpdateSubUserApiKeyRequest();
        updateSubUserApiKeyRequest.setParentUserId(getUserIdStr());
        updateSubUserApiKeyRequest.setSubUserId(subUserId.toString());
        updateSubUserApiKeyRequest.setApiName(updateApiArg.getApiName());
        updateSubUserApiKeyRequest.setRuleId(Long.valueOf(updateApiArg.getRuleId()));
        updateSubUserApiKeyRequest.setStatus(updateApiArg.getStatus());
        updateSubUserApiKeyRequest.setIp(updateApiArg.getIp());
        updateSubUserApiKeyRequest.setSymbols(updateApiArg.getSymbols());
        updateSubUserApiKeyRequest.setApiManageIpConfigId(updateApiArg.getApiManageIpConfigId());
        updateSubUserApiKeyRequest.setFvideoId(CommonUserDeviceHelper.getFVideoId(httpRequest));
        Map<String, String> deviceInfo = this.apiHelper.logDeviceInfo();
        updateSubUserApiKeyRequest.setDeviceInfo(CollectionUtils.isEmpty(deviceInfo) ? Maps.newHashMap() : (HashMap<String, String>) deviceInfo);
        updateSubUserApiKeyRequest.setEmailVerifyCode(updateApiArg.getEmailVerifyCode());
        updateSubUserApiKeyRequest.setMobileVerifyCode(updateApiArg.getMobileVerifyCode());
        updateSubUserApiKeyRequest.setGoogleVerifyCode(updateApiArg.getGoogleVerifyCode());
        updateSubUserApiKeyRequest.setYubikeyVerifyCode(updateApiArg.getYubikeyVerifyCode());
        updateSubUserApiKeyRequest.setKeyId(updateApiArg.getKeyId());
        updateSubUserApiKeyRequest.setIsFromFE(updateApiArg.getIsFromFE());
        APIResponse<UpdateApiKeyResponse> apiResponse = accountApiManageUpdateApi.updateSubUserApiKey(APIRequest.instance(updateSubUserApiKeyRequest));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }

    @ApiOperation(value = "根据API-ID删除API")
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/api/del")
    @UserOperation(eventName = "deleteApi", name = "删除api",
            logDeviceOperation = true, deviceOperationNoteField = {"keyId"},
            requestKeys = {"keyId"}, requestKeyDisplayNames = {"keyId"},
            responseKeys = {"$.success", }, responseKeyDisplayNames = {"success"})
    @SystemMaintenance(isApi = true)
    public CommonRet<Void> subUserDeleteApi(HttpServletRequest request, @Valid @RequestBody SubUserDeleteApiArg deleteApiArg) throws Exception {
        Long parentUserId = getUserId();
        log.info("subUserDeleteApi parentId={},request={}",parentUserId, JsonUtils.toJsonHasNullKey(deleteApiArg));
        this.apiHelper.logDeviceInfo();
        //check 子母账号关系
        Long subUserId=getSubUserIdAndCheckRelation(parentUserId,deleteApiArg.getSubUserEmail());
        //不允许操作资管子账号
        checkIfAssetSubUser(subUserId);
        accountApiManageClient.deleteApiKeyForSub(request,parentUserId, subUserId, deleteApiArg.getApiKey());
        return new CommonRet<>();
    }

    @ApiOperation(value = "检查trade权限的ip限制")
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/api/trade-ip-check")
    @SystemMaintenance(isApi = true)
    public CommonRet<ApiIpTradeCheckRet> querySubUserApiIpTradeCheck(@Valid @RequestBody CheckSubUserApiTradeIpArg apiTradeIpArg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        // check 子母账号关系
        Long subUserId = getSubUserIdAndCheckRelation(parentUserId, apiTradeIpArg.getSubUserEmail());

        com.binance.accountapimanage.vo.apimanage.request.CheckApiIpTradeRequest checkApiIpTradeRequest = new com.binance.accountapimanage.vo.apimanage.request.CheckApiIpTradeRequest();
        checkApiIpTradeRequest.setUserId(subUserId);
        checkApiIpTradeRequest.setKeyId(apiTradeIpArg.getKeyId());
        APIResponse<com.binance.accountapimanage.vo.apimanage.response.ApiIpTradeCheckResponse> resp = accountApiManageQueryApi.queryApiIpTradeCheck(APIRequest.instance(checkApiIpTradeRequest));
        checkResponse(resp);
        ApiIpTradeCheckResponse data = CopyBeanUtils.fastCopy(resp.getData(), ApiIpTradeCheckResponse.class);

        ApiIpTradeCheckRet ret = new ApiIpTradeCheckRet();
        BeanUtils.copyProperties(data, ret);
        return new CommonRet<>(ret);
    }


    public Long getSubUserIdAndCheckRelation(Long parentUserId,String subUserEmail) throws Exception{
        // 校验邮箱格式
        if (StringUtils.isBlank(subUserEmail) || !timeOutRegexUtils.validateEmailForSub(subUserEmail)) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        BindingParentSubUserEmailReq request = new BindingParentSubUserEmailReq();
        request.setParentUserId(parentUserId);
        request.setSubUserEmail(subUserEmail);
        log.info("getSubUserIdAndCheckRelationl start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<BindingParentSubUserEmailResp> apiResponse = subUserClient.checkRelationByParentSubUserEmail(getInstance(request));
        checkResponse(apiResponse);
        return apiResponse.getData().getSubUserId();
    }

    protected void checkIfAssetSubUser(Long userId)throws Exception{
        UserIdRequest request = new UserIdRequest();
        request.setUserId(userId);
        log.info("checkIfAssetSubUser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        UserStatusEx userStatusEx = getUserStatusByUserId(userId);
        if(null!=userStatusEx.getIsAssetSubUser() && userStatusEx.getIsAssetSubUser().booleanValue()){
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
    }

    @ApiOperation(value = "查询API币种白名单")
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/apiKey-whitelist/query")
    @SystemMaintenance(isApi = true)
    @ApiKeyKycCheck
    public CommonRet<QuerySubUserApiKeyWhitelistRet> querySubUserApiKeyWhitelist(@Valid @RequestBody QuerySubUserApiKeyWhitelistArg arg) throws Exception {
        Long parentUserId = getUserId();
        String subUserEmail = arg.getSubUserEmail();
        // check 子母账号关系
        Long subUserId = getSubUserIdAndCheckRelation(parentUserId, subUserEmail);

        com.binance.accountapimanage.vo.apimanage.request.GetApiKeyWhitelistRequest getApiKeyWhitelistRequest = new com.binance.accountapimanage.vo.apimanage.request.GetApiKeyWhitelistRequest();
        getApiKeyWhitelistRequest.setKeyId(arg.getKeyId());
        getApiKeyWhitelistRequest.setUserId(subUserId);
        APIResponse<com.binance.accountapimanage.vo.apimanage.response.QueryApiKeyWhitelistResponse> resp = accountApiManageQueryApi.getApiKeyWhitelist(APIRequest.instance(getApiKeyWhitelistRequest));
        checkResponse(resp);
        QueryApiKeyWhitelistResponse data = CopyBeanUtils.fastCopy(resp.getData(), QueryApiKeyWhitelistResponse.class);

        QuerySubUserApiKeyWhitelistRet ret = new QuerySubUserApiKeyWhitelistRet();
        BeanUtils.copyProperties(data, ret);
        return new CommonRet<>(ret);
    }

    @ApiOperation(value = "重置 enableTradeTime 低于10天的apikey(针对未设置IP的)")
    @UserOperation(eventName = "resetEnableTradeTime", name = "一键重置交易过期时间", logDeviceOperation = true, responseKeys = {"$.success", }, responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/v1/private/account/subUser/api-mgmt/api/resetEnableTradeTime")
    public CommonRet<ResetEnableTradeTimeRet> resetEnableTradeTime(HttpServletRequest request, @Valid @RequestBody ResetEnableTradeTimeArg arg) throws Exception {
        Long parentUserId = getUserId();
        ResetEnableTradeTimeRet resetEnableTradeTimeRet;
        String lockKey = CacheConstant.ACCOUNT_MGS_SUB_USER_RESET_TRADE_TIME + ":" + parentUserId;
        boolean resetFlag = ShardingRedisCacheUtils.setNX(lockKey, parentUserId.toString(), 2, TimeUnit.SECONDS);
        if (!resetFlag) {
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
        try {
            UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
            // 不支持 broker 母账户操作
            if (userStatusEx.getIsBrokerSubUserFunctionEnabled()) {
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
            // 更新apikey最大数量
            if (CollectionUtils.isEmpty(arg.getResetApiModeRequests()) || arg.getResetApiModeRequests().size() > resetSubApiKeyNum) {
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
            resetEnableTradeTimeRet = accountApiManageClient.resetEnableTradeTimeRet(request, parentUserId, arg.getResetApiModeRequests());
        } finally {
            ShardingRedisCacheUtils.del(lockKey);
        }
        return new CommonRet<>(resetEnableTradeTimeRet);
    }

    private CommonRet<AllApiInfoPageRet> querySubUserApiKeyByApiKey(String apiKey, Long parentUserId, FlexLineQuerySubUserResp finalFlexLineSubData) throws Exception {
        AllApiInfoPageRet ret = new AllApiInfoPageRet();
        ret.setCurrentIp(WebUtils.getRequestIp());

        QueryApiByApiKeyRequest queryApiByApiKeyRequest = new QueryApiByApiKeyRequest();
        queryApiByApiKeyRequest.setApiKey(apiKey);
        APIResponse<QueryApiByApiKeyResponse> response = accountApiManageQueryApi.queryApiByApiKey(APIRequest.instance(queryApiByApiKeyRequest));
        if (!baseHelper.isOk(response)) {
            if (StringUtils.equals(response.getCode(), GeneralCode.KEY_API_KEY_NOT_EXIST.getCode())) {
                ret.setTotal(0L);
                return new CommonRet<>(ret);
            } else {
                checkResponse(response);
            }
        }
        QueryApiByApiKeyResponse apiKeyData = response.getData();
        List<SubUserApiInfoRet> subUserApiInfoRets = Lists.newArrayList();
        ret.setApiInfos(subUserApiInfoRets);
        if (apiKeyData == null) {
            ret.setTotal(0L);
            return new CommonRet<>(ret);
        }
        Long subUserId = Long.valueOf(apiKeyData.getUserId());
        BindingParentSubUserReq bindingParentSubUserReq = new BindingParentSubUserReq();
        bindingParentSubUserReq.setParentUserId(parentUserId);
        bindingParentSubUserReq.setSubUserId(subUserId);
        APIResponse<Boolean> bindingParentSubUserResponse = subUserClient.checkRelationByParentSubUserIds(APIRequest.instance(bindingParentSubUserReq));
        checkResponse(bindingParentSubUserResponse);
        if (!Boolean.TRUE.equals(bindingParentSubUserResponse.getData())) {
            ret.setTotal(0L);
            return new CommonRet<>(ret);
        }

        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(subUserId);
        APIResponse<GetUserResponse> userResponse = userApi.getUserById(APIRequest.instance(userIdRequest));
        checkResponse(userResponse);

        SubUserApiInfoRet apiInfoRet = new SubUserApiInfoRet();
        BeanUtils.copyProperties(apiKeyData, apiInfoRet);

        com.binance.account.vo.user.UserVo subUser = userResponse.getData().getUser();
        UserStatusEx userStatusEx = new UserStatusEx(subUser.getStatus(), subUser.getStatusExtra());

        apiInfoRet.setSecretKey("xxxxxxxxxx");
        apiInfoRet.setEmail(subUser.getEmail());
        apiInfoRet.setIsExistMarginAccount(userStatusEx.getIsExistMarginAccount());
        apiInfoRet.setIsExistFutureAccount(userStatusEx.getIsExistFutureAccount());
        apiInfoRet.setIsPortfolioMarginRetailUser(userStatusEx.getIsPortfolioMarginRetailUser());
        if (IP.equalsIgnoreCase(apiInfoRet.getWithdrawIp())) {
            apiInfoRet.setWithdrawIp("");
        }
        if (IP.equalsIgnoreCase(apiInfoRet.getTradeIp())) {
            apiInfoRet.setTradeIp("");
        }
        if (finalFlexLineSubData.getCreditSubUserId() != null && (Objects.equals(subUserId, finalFlexLineSubData.getCreditSubUserId()))) {
                apiInfoRet.setIsFlexLineCreditUser(true);
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(finalFlexLineSubData.getTradingSubUserIds()) && (finalFlexLineSubData.getTradingSubUserIds().contains(subUserId))) {
                apiInfoRet.setIsFlexLineTradingUser(true);
        }
        apiInfoRet.setCanResetEnableTradeTime(accountApiManageClient.checkCanResetEnableTradeTime(apiKeyData.getEnableTradeTime(), apiKeyData.getTradeIp()));
        subUserApiInfoRets.add(apiInfoRet);
        ret.setTotal(1L);
        return new CommonRet<>(ret);
    }
}
