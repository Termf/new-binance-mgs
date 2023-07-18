package com.binance.mgs.account.authcenter.controller;

import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account2fa.api.User2FaApi;
import com.binance.account2fa.vo.request.InvalidatePasswordlessloginReq;
import com.binance.account2fa.vo.request.InvalidatePasswordlessloginResp;
import com.binance.c2c.api.MerchantApi;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.authcenter.AuthCenterBaseAction;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class AuthController extends AuthCenterBaseAction {
    @Resource
    private AuthHelper authHelper;
    @Value("${auth.skip.second:60}")
    private int authSkipSecond;
    @Resource
    private MerchantApi merchantApi;

    @Autowired
    private User2FaApi user2FaApi;


    @PostMapping(value = "/public/authcenter/auth")
    @CacheControl(noStore = true, forwardedHeaders = {"Host", "lang", "x-token", "CSRFToken"}, forwardedCookies = {"p20t","cr20"})
    public CommonRet<Integer> auth(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = baseHelper.getToken();
        if (baseHelper.isFromWeb()) {
            String csrfToken = request.getHeader(Constant.COOKIE_CSRFTOKEN);
            if (StringUtils.isEmpty(csrfToken)) {
                log.info("csrftoken is null,token={}", StringUtils.left(token, 12));
                throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
            }
            authHelper.checkToken(request, response, token, csrfToken);
        } else {
            authHelper.checkToken(request, response, token, null);
        }
        return new CommonRet<>(authSkipSecond);
    }

    @PostMapping(value = "/private/authcenter/logout")
    @UserOperation(eventName = "logout", name = "用户登出", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> logout(HttpServletRequest request) throws Exception {
        authHelper.logout(request);
        return new CommonRet<>();
    }

    @PostMapping(value = "/private/authcenter/merchant/logout")
    @UserOperation(eventName = "logout", name = "用户登出", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<String> merchantLogout(HttpServletRequest request) throws Exception {
        // 商户登出需要判断是否可以下线
        com.binance.c2c.vo.UserIdRequest userIdRequest = new com.binance.c2c.vo.UserIdRequest();
        userIdRequest.setUserId(getUserId());
        APIResponse<Void> apiResponse = merchantApi.canMerchantLogout(getInstance(userIdRequest));
        checkResponse(apiResponse);
        authHelper.logout(request);
        return new CommonRet<>();
    }


    @PostMapping(value = "/protect/authcenter/invalidatePasswordlesslogin")
    @UserOperation(eventName = "invalidatePasswordlesslogin", name = "invalidatePasswordlesslogin", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
    logDeviceOperation = true)
    public CommonRet<String> invalidatePasswordlesslogin(HttpServletRequest request) throws Exception {
        String email=authHelper.getUserEmailFromHeader();
        if(StringUtils.isBlank(email)){
            log.warn("invalidatePasswordlesslogin,email is null");
            throw new BusinessException(AccountMgsErrorCode.PLEASE_USE_PASSWORD_LOGIN);
        }
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(getUserRequest));
        if (!baseHelper.isOk(emailApiResponse)) {
            log.warn("userId is illegal,emailApiResponse={}", emailApiResponse);
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(emailApiResponse);
        }
        Long userId = emailApiResponse.getData();
        UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));


        InvalidatePasswordlessloginReq invalidatePasswordlessloginReq = new InvalidatePasswordlessloginReq();
        invalidatePasswordlessloginReq.setUserId(userId);
        APIResponse<InvalidatePasswordlessloginResp> invalidatePasswordlessloginRespAPIResponse = user2FaApi.invalidatePasswordlesslogin(getInstance(invalidatePasswordlessloginReq));
        if (!baseHelper.isOk(invalidatePasswordlessloginRespAPIResponse)) {
            log.warn("invalidatePasswordlesslogin failed,invalidatePasswordlessloginRespAPIResponse={}", JsonUtils.toJsonNotNullKey(invalidatePasswordlessloginRespAPIResponse));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(invalidatePasswordlessloginRespAPIResponse);
        }
        authHelper.logout(request);
        return new CommonRet<>();
    }

}
