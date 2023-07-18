package com.binance.mgs.account.account.service;

import com.binance.account.api.UserInfoApi;
import com.binance.account.vo.user.request.SetUserConfigRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class UserConfigService extends AccountBaseAction {

    @Resource
    private UserInfoApi userInfoApi;

    // 为了给保存语言单独加入userOperation记录，无业务逻辑 https://jira.toolsfdg.net/browse/COM-7710
    @UserOperation(name = "用户修改设置", eventName = "user_change_settings", requestKeys = {"configName", "configType"},
            requestKeyDisplayNames = {"configName", "configType"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> saveUserOperationLogAndConfig(SetUserConfigRequest setUserConfigRequest) throws Exception {
        APIResponse<Integer> apiResponse = userInfoApi.saveUserConfig(getInstance(setUserConfigRequest));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }
}
