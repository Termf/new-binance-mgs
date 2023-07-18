package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserEmailChangeApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.request.VarificationTwoRequest;
import com.binance.account.vo.user.UserChangeEmailEnum;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.NewEmailCaptchaRequest;
import com.binance.account.vo.user.request.NewEmailConfirmRequest;
import com.binance.account.vo.user.request.OldEmailCaptchaRequest;
import com.binance.account.vo.user.request.UserEmailChangeInitFlowRequest;
import com.binance.account.vo.user.response.OldEmailCaptchaResponse;
import com.binance.account.vo.user.response.UserEmailChangeInitResponse;
import com.binance.master.enums.AuthTypeEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.UserReqInterceptHelper;
import com.binance.mgs.account.account.vo.NewEmailCaptchaArg;
import com.binance.mgs.account.account.vo.UserEmailChangeInitArg;
import com.binance.mgs.account.account.vo.UserNewEmailConfirmArg;
import com.binance.mgs.account.account.vo.UserOldEmailCaptchaArg;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 更换邮箱接口
 */
@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class UserEmailChangeController extends AccountBaseAction {

    @Resource
    protected BaseHelper baseHelper;

    @Resource
    private UserEmailChangeApi userEmailChangeApi;

    @Autowired
    private AuthHelper authHelper;

    @Resource
    private UserSecurityApi userSecurityApi;


    @Value("${reset2fa.next.request.count:50}")
    private int reset2faNextReqCount;

    @Value("${reset2fa.next.request.expireTime:3600}")
    private int reset2faNextReqExpireTime;

    @Value("${reset2fa.next.request.lockTime:86400}")
    private int retReset2faNextReqLockTime;

    @Value("${user.email.check.request.count.switch:false}")
    private boolean userEmailCheckRequestCountSwitch;


    @Resource
    private UserReqInterceptHelper reqInterceptHelper;

    private static final String CHANGE_EMAIL_LIMIT_PRIX = "user:email:change:limit:userId:";


    @ApiOperation(value = "创建更换邮箱流程")
    @PostMapping(value = "/private/account/user/email/init")
    @UserOperation(eventName = "initFlow", name = "初始化流程", responseKeys = {"$.success", "$.code"}, responseKeyDisplayNames = {"success", "code"})
    public CommonRet<UserEmailChangeInitResponse> initFlow(@Valid @RequestBody UserEmailChangeInitArg initFlowArg){

        CommonRet<UserEmailChangeInitResponse> commonRet = new CommonRet<>();
        UserEmailChangeInitFlowRequest userEmailChangeInitFlowRequest = new UserEmailChangeInitFlowRequest();
        Long userId = getUserId();
        String email = null;
        try {
            email = getUserEmailAndMobileByUserId(userId).getEmail();
            if (email==null){
                log.error("getUserEmailAndMobileByUserId not get email,uid is {}",userId);
                commonRet.setCode(GeneralCode.SYS_ERROR.getCode());
                return commonRet;
            }
        } catch (Exception e) {
            log.error("getUserEmailAndMobileByUserId not get email,uid is {} ,error is {}",userId,e);
            commonRet.setCode(GeneralCode.SYS_ERROR.getCode());
            return commonRet;
        }

        // 检查频繁请求
        try {
            checkRequestCount(userId);
        } catch (BusinessException e) {
            log.error("user email request count limit userid is {} ,error is {}", userId, e);
            commonRet.setCode(GeneralCode.USER_EMAIL_SUCCESS_COUNT.getCode());
            return commonRet;
        }


        userEmailChangeInitFlowRequest.setUserId(userId);
        userEmailChangeInitFlowRequest.setEmail(email);
        userEmailChangeInitFlowRequest.setAvailableType(initFlowArg.getAvailableType());

        APIResponse<UserEmailChangeInitResponse> response = userEmailChangeApi.initFlow(getInstance(userEmailChangeInitFlowRequest));
        checkResponse(response);
        commonRet.setCode(response.getCode());
        commonRet.setData(response.getData());

        return commonRet;
    }


    @ApiOperation(notes = "验证老邮箱验证码", nickname = "validOldEmailCaptcha", value = "验证老邮箱验证码")
    @PostMapping("/private/account/userEmailChange/validOldEmailCaptcha")
    @UserOperation(eventName = "validOldEmailCaptcha", name = "验证老邮箱验证码", responseKeys = {"$.success", "$.code"}, responseKeyDisplayNames = {"success", "code"})
    CommonRet<OldEmailCaptchaResponse> validOldEmailCaptcha(@Valid @RequestBody UserOldEmailCaptchaArg oldEmailCaptchaArg) {
        Long userId = getUserId();
        OldEmailCaptchaRequest oldEmailCaptchaRequest = new OldEmailCaptchaRequest();
        BeanUtils.copyProperties(oldEmailCaptchaArg, oldEmailCaptchaRequest);
        oldEmailCaptchaRequest.setUserId(userId);
        APIResponse<OldEmailCaptchaResponse> response = userEmailChangeApi.validOldEmailCaptcha(APIRequest.instance(oldEmailCaptchaRequest));
        checkResponse(response);
        CommonRet<OldEmailCaptchaResponse> commonRet = new CommonRet<>();
        commonRet.setCode(response.getCode());
        commonRet.setData(response.getData());

        return commonRet;
    }


    @ApiOperation(notes = "验证码版本 确认新邮箱", nickname = "confirmNewEmailV3", value = "验证码版本 确认新邮箱")
    @PostMapping("/private/account/userEmailChange/confirmNewEmailV3")
    @UserOperation(eventName = "confirmNewEmailV3", name = "验证码版本确认新邮箱", responseKeys = {"$.success", "$.code"}, responseKeyDisplayNames = {"success", "code"})
    CommonRet<String> confirmNewEmailV3(@Valid @RequestBody UserNewEmailConfirmArg newEmailConfirmArg) {
        Long userId = getUserId();
        NewEmailConfirmRequest newEmailConfirmRequest = new NewEmailConfirmRequest();
        BeanUtils.copyProperties(newEmailConfirmArg, newEmailConfirmRequest);
        newEmailConfirmRequest.setUserId(userId);

        APIResponse<String> response = userEmailChangeApi.confirmNewEmailV3(APIRequest.instance(newEmailConfirmRequest));
        checkResponse(response);
        CommonRet<String> commonRet = new CommonRet<>();
        commonRet.setCode(response.getCode());
        commonRet.setData(response.getData());
        return commonRet;
    }

    @ApiOperation(notes = "验证新邮箱的几种验证码", nickname = "validNewEmailCaptcha", value = "验证新邮箱的几种验证码")
    @PostMapping("/private/account/userEmailChange/validNewEmailCaptcha")
    @UserOperation(eventName = "validNewEmailCaptcha", name = "验证新邮箱的几种验证码", responseKeys = {"$.success", "$.code"}, responseKeyDisplayNames = {"success", "code"})
    CommonRet<UserEmailChangeInitResponse> validNewEmailCaptcha(@Validated @RequestBody NewEmailCaptchaArg newEmailCaptchaArg, HttpServletRequest httpServletRequest){
        Long userId = getUserId();
        NewEmailCaptchaRequest newEmailCaptchaRequest = new NewEmailCaptchaRequest();
        BeanUtils.copyProperties(newEmailCaptchaArg,newEmailCaptchaRequest);
        newEmailCaptchaRequest.setUserId(userId);

        APIResponse<UserEmailChangeInitResponse> respone = userEmailChangeApi.validNewEmailCaptcha(APIRequest.instance(newEmailCaptchaRequest));
        checkResponse(respone);
        if (respone.getData() != null && respone.getData().getFlowStatus() == UserChangeEmailEnum.PASS.getStatus()) {
            // 当通过后，踢出用户登陆
            authHelper.logoutAll(userId);
        }

        CommonRet<UserEmailChangeInitResponse> commonRet = new CommonRet<>();
        commonRet.setCode(respone.getCode());
        commonRet.setData(respone.getData());
        return commonRet;
    }

    @Override
    public void checkSecurity(Long userId, String operationType, String verifyCode) throws Exception {
        // 是否进行2FA绑定
        UserIdRequest userIdReq = new UserIdRequest();
        userIdReq.setUserId(userId);
        APIResponse<UserStatusEx> apiResponse = userApi.getUserStatusByUserId(getInstance(userIdReq));
        checkResponse(apiResponse);
        UserStatusEx userStatusEx = apiResponse.getData();
        if (!(userStatusEx.getIsUserMobile() || userStatusEx.getIsUserGoogle())) {
            throw new BusinessException(GeneralCode.USER_GOOGLE_VERIFY_NO);
        }

        // 2FA verify
        VarificationTwoRequest twoFAReq = new VarificationTwoRequest();
        if ("google".equalsIgnoreCase(operationType)) {
            twoFAReq.setAuthType(AuthTypeEnum.GOOGLE);
        } else if ("mobile".equalsIgnoreCase(operationType) || "sms".equalsIgnoreCase(operationType)) {
            twoFAReq.setAuthType(AuthTypeEnum.SMS);
        } else {
            throw new BusinessException(MgsErrorCode.INVALID_2FA_TYPE);
        }

        twoFAReq.setCode(verifyCode);
        twoFAReq.setUserId(userId);
        twoFAReq.setAutoDel(Boolean.TRUE);
        APIResponse<String> verifyResponse = userSecurityApi.verificationsTwo(getInstance(twoFAReq));
        checkResponse(verifyResponse);
    }


    /**
     * 检查用户调用次数
     *
     * @param userId
     */
    private void checkRequestCount(Long userId) throws BusinessException {
        if (userEmailCheckRequestCountSwitch) {
            // 检查多次调用问题
            reqInterceptHelper.reqIntercept(CHANGE_EMAIL_LIMIT_PRIX, userId.toString(), reset2faNextReqCount, reset2faNextReqExpireTime,
                    retReset2faNextReqLockTime);
            // 操作日志
            UserOperationHelper.log(ImmutableMap.of("userEmailChange", userId));
        }
    }


}
