package com.binance.mgs.account.account.controller;

import com.binance.account.api.AccountDeleteApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.accountdelete.request.DeleteAccountRequest;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.accountdelete.AccountDeleteArg;
import com.binance.mgs.account.account.vo.accountdelete.AccountDeleteCheckRet;
import com.binance.mgs.account.constant.BinanceMgsAccountConstant;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.service.Account2FaService;
import com.binance.mgs.account.service.RiskService;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.userbigdata.api.AccountDeleteCheckApi;
import com.binance.userbigdata.vo.accountdelete.request.AccountDeleteCheckRequest;
import com.binance.userbigdata.vo.accountdelete.response.AccountDeleteCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author rudy.c
 * @date 2022-08-01 19:48
 */
@RestController
@RequestMapping(value = "/v1/private/account/delete")
@Slf4j
public class AccountDeleteController extends AccountBaseAction {
    @Autowired
    private AccountDeleteCheckApi accountDeleteCheckApi;

    @Autowired
    private UserSecurityApi userSecurityApi;
    @Autowired
    private AccountDeleteApi accountDeleteApi;
    @Autowired
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Autowired
    private RiskService riskService;
    @Autowired
    private Account2FaService account2FaService;

    @Value("${account.delete.cache.switch:true}")
    private boolean accountDeleteCacheSwitch;

    @PostMapping(value = "/check")
    @UserOperation(eventName = "checkAccountDelete", name = "检查账号删除条件", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<AccountDeleteCheckRet> check() throws Exception {
        final Long userId = getUserId();
        HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());

        AccountDeleteCheckRequest deleteCheckRequest = new AccountDeleteCheckRequest();
        deleteCheckRequest.setUserId(userId);
        deleteCheckRequest.setDeviceInfo(deviceInfo);
        APIResponse<AccountDeleteCheckResponse> apiResponse = accountDeleteCheckApi.check(APIRequest.instance(deleteCheckRequest));
        this.baseHelper.checkResponse(apiResponse);

        AccountDeleteCheckResponse accountDeleteCheckResponse = apiResponse.getData();
        AccountDeleteCheckRet accountDeleteCheckRet = new AccountDeleteCheckRet();
        accountDeleteCheckRet.setCanDelete(accountDeleteCheckResponse.getCanDelete());
        accountDeleteCheckRet.setBtcAsset(accountDeleteCheckResponse.getBtcAsset());
        accountDeleteCheckRet.setNftNumber(accountDeleteCheckResponse.getNftNumber());
        return new CommonRet<>(accountDeleteCheckRet);
    }

    @PostMapping(value = "/deleteAccount")
    @UserOperation(eventName = "deleteAccount", name = "删除账号", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "deleteAccount")
    public CommonRet<Void> deleteAccount(@RequestBody @Validated AccountDeleteArg arg) throws Exception {
        Long userId = getUserId();
        BizSceneEnum bizScene = BizSceneEnum.ACCOUNT_DELETE;
        String flag = WebUtils.getHeader(BinanceMgsAccountConstant.RISK_CHALLENGE_FLAG);
        if (StringUtils.isNotBlank(flag)) {
            HashMap<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail());
            riskService.getRiskChallengeTimeOut(userId, deviceInfo, bizScene.name());
        } else {
            account2FaService.verify2FaToken(userId, bizScene.name(), arg.getVerifyToken());
        }
        //查询User信息，删除email，mobile 缓存用到
        UserSecurityVo userSecurityVo = null;
        if (accountDeleteCacheSwitch) {
            UserIdRequest userIdRequest = new UserIdRequest();
            userIdRequest.setUserId(userId);
            APIResponse<UserSecurityVo> userSecurityVoAPIResponse = userSecurityApi.getUserSecurityByUserId(getInstance(userIdRequest));
            this.baseHelper.checkResponse(userSecurityVoAPIResponse);
            userSecurityVo = userSecurityVoAPIResponse.getData();
        }
        DeleteAccountRequest deleteAccountRequest = new DeleteAccountRequest();
        deleteAccountRequest.setUserId(userId);
        deleteAccountRequest.setReason(arg.getReason());
        deleteAccountRequest.setGiveUpAsset(arg.getGiveUpAsset());
        APIResponse<Void> apiResponse = accountDeleteApi.deleteAccount(APIRequest.instance(deleteAccountRequest));
        this.baseHelper.checkResponse(apiResponse);
        //删除email，mobile 缓存
        if (accountDeleteCacheSwitch && Objects.nonNull(userSecurityVo)) {
            String identifyPrefix = CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_KEY_PREFIX + ":";
            if (StringUtils.isNotBlank(userSecurityVo.getEmail())) {
                String emailIdentify = identifyPrefix + userSecurityVo.getEmail();
                ShardingRedisCacheUtils.del(emailIdentify);
            }
            if (StringUtils.isNotBlank(userSecurityVo.getMobileCode()) && StringUtils.isNotBlank(userSecurityVo.getMobile())) {
                String mobileIdentify = identifyPrefix + userSecurityVo.getMobileCode() + "-" + userSecurityVo.getMobile();
                ShardingRedisCacheUtils.del(mobileIdentify);
            }
        }
        return new CommonRet<>();
    }
}
