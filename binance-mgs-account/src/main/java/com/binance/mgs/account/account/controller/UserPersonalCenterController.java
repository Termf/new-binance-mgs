package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.accountpersonalcenter.api.UserPersonalConfigApi;
import com.binance.accountpersonalcenter.vo.enums.UserPersonalConfigEnum;
import com.binance.accountpersonalcenter.vo.userpersonalconfig.*;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.account.vo.ModifyAnalyticsAndAdvertisingResp;
import com.binance.mgs.account.account.vo.ModifyMarketingAnalyticsArg;
import com.binance.mgs.account.account.vo.ModifyWithdrawalWhitelistArg;
import com.binance.mgs.account.account.vo.ModifyWithdrawalWhitelistResp;
import com.binance.mgs.account.account.vo.QueryAnalyticsAndAdvertisingResp;
import com.binance.mgs.account.account.vo.userpersonalconfig.request.UnifiedBatchQueryUserPersonalConfigArg;
import com.binance.mgs.account.account.vo.userpersonalconfig.request.UnifiedModifyUserPersonalConfigArg;
import com.binance.mgs.account.account.vo.userpersonalconfig.request.UnifiedQueryUserPersonalConfigArg;
import com.binance.mgs.account.account.vo.userpersonalconfig.response.UnifiedBatchQueryUserPersonalConfigRet;
import com.binance.mgs.account.account.vo.userpersonalconfig.response.UnifiedQueryUserPersonalConfigRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.AbstractBaseAction;
import com.google.api.client.util.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping(value = "/v1/private/account")
public class UserPersonalCenterController extends AbstractBaseAction {

    @Autowired
    private UserPersonalConfigApi userPersonalConfigApi;

    @GetMapping(value = "/user-personal-config/get-marketing-analytics-status")
    public CommonRet<QueryAnalyticsAndAdvertisingResp> queryMarketingAnalyticsSwitch() throws Exception {
        Long userId = checkAndGetUserId();
        QueryAnalyticsAndAdvertisingResp resp = new QueryAnalyticsAndAdvertisingResp();
        resp.setAdvertising(true);
        resp.setAnalytics(true);

        BatchGetUserPersonalConfigRequest request = new BatchGetUserPersonalConfigRequest();
        request.setUserId(userId);
        List<String> configTypes = Lists.newArrayList();
        configTypes.add(UserPersonalConfigEnum.ADVERTISING.getType());
        configTypes.add(UserPersonalConfigEnum.ANALYTICS.getType());
        request.setConfigTypeList(configTypes);
        APIResponse<List<UserPersonalConfigResp>> apiResponse = userPersonalConfigApi.batchGetUserPersonalConfigs(baseHelper.getInstance(request));
        log.info("queryMarketingAnalyticsSwitch-response={}", JSON.toJSONString(apiResponse.getData()));
        checkResponse(apiResponse);

        for (UserPersonalConfigResp configResp : apiResponse.getData()) {
            if (UserPersonalConfigEnum.ADVERTISING.getType().equalsIgnoreCase(configResp.getConfigType())){
                resp.setAdvertising(Boolean.parseBoolean(configResp.getConfigValue()));
            }
            if (UserPersonalConfigEnum.ANALYTICS.getType().equalsIgnoreCase(configResp.getConfigType())){
                resp.setAnalytics(Boolean.parseBoolean(configResp.getConfigValue()));
            }
        }
        return new CommonRet<>(resp);
    }

    @PostMapping(value = "/user-personal-config/modify-marketing-analytics-status")
    public CommonRet<ModifyAnalyticsAndAdvertisingResp> modifyMarketingAnalyticsSwitch(@RequestBody ModifyMarketingAnalyticsArg requestArg) throws Exception {
        Long userId = checkAndGetUserId();
        PostUserPersonalConfigRequest request = new PostUserPersonalConfigRequest();
        request.setUserId(userId);
        if (requestArg.getAdvertising() != null) {
            request.setConfigType(UserPersonalConfigEnum.ADVERTISING.getType());
            request.setConfigValue(String.valueOf(requestArg.getAdvertising()));
            APIResponse<PostUserPersonalConfigResp> apiResponse = userPersonalConfigApi.modifyUserPersonalConfig(baseHelper.getInstance(request));
            checkResponse(apiResponse);
        }
        if (requestArg.getAnalytics() != null) {
            request.setConfigType(UserPersonalConfigEnum.ANALYTICS.getType());
            request.setConfigValue(String.valueOf(requestArg.getAnalytics()));
            APIResponse<PostUserPersonalConfigResp> apiResponse = userPersonalConfigApi.modifyUserPersonalConfig(baseHelper.getInstance(request));
            checkResponse(apiResponse);
        }
        return new CommonRet<>(new ModifyAnalyticsAndAdvertisingResp());
    }


    @PostMapping(value = "/user-personal-config/withdrawal-whitelist")
    public CommonRet<ModifyWithdrawalWhitelistResp> modifyWithdrawalWhitelist(@RequestBody ModifyWithdrawalWhitelistArg requestArg) throws Exception {
        Long userId = checkAndGetUserId();
        PostUserPersonalConfigRequest request = new PostUserPersonalConfigRequest();
        request.setUserId(userId);
        if (StringUtils.isNotBlank(requestArg.getWithdrawalWhiteTime())) {
            request.setConfigType(UserPersonalConfigEnum.WITHDRAW_LIST_EFFECT_DELAY.getType());
            request.setConfigValue(String.valueOf(requestArg.getWithdrawalWhiteTime()));
            APIResponse<PostUserPersonalConfigResp> apiResponse = userPersonalConfigApi.modifyUserPersonalConfig(baseHelper.getInstance(request));
            checkResponse(apiResponse);
        }
        return new CommonRet<>(new ModifyWithdrawalWhitelistResp());
    }

    @PostMapping(value = "/user-personal-config/unified-query")
    public CommonRet<UnifiedQueryUserPersonalConfigRet> unifiedQuery(@RequestBody @Validated UnifiedQueryUserPersonalConfigArg arg) throws Exception {
        Long userId = checkAndGetUserId();
        GetUserPersonalConfigRequest request = new GetUserPersonalConfigRequest();
        request.setUserId(userId);
        request.setConfigType(arg.getConfigType());
        APIResponse<UserPersonalConfigResp> apiResponse = userPersonalConfigApi.queryUserPersonalConfig(baseHelper.getInstance(request));
        log.info("query UserPersonalConfig resp={}", JSON.toJSONString(apiResponse));
        checkResponse(apiResponse);
        UnifiedQueryUserPersonalConfigRet ret = null;
        if (Objects.nonNull(apiResponse.getData()) && StringUtils.isNoneBlank(apiResponse.getData().getConfigType(),apiResponse.getData().getConfigValue())) {
            ret = CopyBeanUtils.fastCopy(apiResponse.getData(), UnifiedQueryUserPersonalConfigRet.class);
        }
        return new CommonRet<>(ret);
    }

    @PostMapping(value = "/user-personal-config/unified-batch-query")
    public CommonRet<UnifiedBatchQueryUserPersonalConfigRet> unifiedBatchQuery(@RequestBody @Validated UnifiedBatchQueryUserPersonalConfigArg arg) throws Exception {
        Long userId = checkAndGetUserId();
        validConfigTypes(arg.getConfigTypes());
        List<String> configTypes = arg.getConfigTypes().stream().distinct().collect(Collectors.toList());
        BatchGetUserPersonalConfigRequest request = new BatchGetUserPersonalConfigRequest();
        request.setUserId(userId);
        request.setConfigTypeList(configTypes);
        log.info("batchGetUserPersonalConfigs request={}", JSON.toJSONString(request));
        APIResponse<List<UserPersonalConfigResp>> apiResponse = userPersonalConfigApi.batchGetUserPersonalConfigs(baseHelper.getInstance(request));
        log.info("batchGetUserPersonalConfigs resp={}", JSON.toJSONString(apiResponse));
        checkResponse(apiResponse);
        // ios 不支持data为null的这种解析，需要填充一下数据
        Map<String, String> configKeyValueMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(apiResponse.getData())){
            configKeyValueMap = apiResponse.getData().stream().filter(userPersonalConfigResp -> StringUtils.isNoneBlank(userPersonalConfigResp.getConfigType(),userPersonalConfigResp.getConfigValue()))
                    .collect(Collectors.toMap(UserPersonalConfigResp::getConfigType, UserPersonalConfigResp::getConfigValue));
        }
        UnifiedBatchQueryUserPersonalConfigRet ret = new UnifiedBatchQueryUserPersonalConfigRet();
        List<UnifiedQueryUserPersonalConfigRet> configRets = new ArrayList<>();
        ret.setConfigRets(configRets);
        for (String configType : configTypes) {
            UnifiedQueryUserPersonalConfigRet unifiedQueryUserPersonalConfigRet = new UnifiedQueryUserPersonalConfigRet();
            unifiedQueryUserPersonalConfigRet.setConfigType(configType);
            unifiedQueryUserPersonalConfigRet.setConfigValue(configKeyValueMap.get(configType));
            configRets.add(unifiedQueryUserPersonalConfigRet);
        }
        return new CommonRet<>(ret);
    }

    private void validConfigTypes(List<String> configTypes) {
        if(CollectionUtils.isEmpty(configTypes)){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        for (String configType : configTypes) {
            UserPersonalConfigEnum userPersonalConfigEnum = UserPersonalConfigEnum.parseByType(configType);
            if(userPersonalConfigEnum == null){
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
        }
    }

    @PostMapping(value = "/user-personal-config/unified-modify")
    public CommonRet<Void> unifiedModify(@RequestBody @Validated UnifiedModifyUserPersonalConfigArg arg) throws Exception {
        Long userId = checkAndGetUserId();

        PostUserPersonalConfigRequest request = new PostUserPersonalConfigRequest();
        request.setUserId(userId);
        request.setConfigType(arg.getConfigType());
        request.setConfigValue(arg.getConfigValue());
        APIResponse<PostUserPersonalConfigResp> apiResponse = userPersonalConfigApi.modifyUserPersonalConfig(baseHelper.getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }
}
