package com.binance.mgs.account;


import com.alibaba.fastjson.JSON;
import com.binance.account.api.BrokerApi;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.subuser.request.GetSubbindingInfoReq;
import com.binance.account.vo.subuser.response.GetrSubUserBindingsResp;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.platform.mgs.business.AbstractBaseAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

import static com.binance.master.utils.WebUtils.getHeader;

@Slf4j
public abstract class AccountBaseAction extends AbstractBaseAction {

    @Resource
    protected UserApi userApi;

    @Autowired
    private BrokerApi brokerApi;
    @Autowired
    private UserSecurityApi userSecurityApi;


    /**
     * 故意干扰黑客防止撞库
     */
    public void checkResponseMisleaderUseNotExitsErrorForPublicInterface(APIResponse<?> apiResponse) {
        log.info("response = {} ", logFilter(JSON.toJSONString(apiResponse)));
        if (!baseHelper.isOk(apiResponse)) {
            // 若返回的是【用户不存在】则返回其他报错
            if (StringUtils.equals(apiResponse.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
                throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
            } else {
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
        }
    }

    /**
     * 故意干扰黑客防止撞库
     */
    public void checkResponseMisleaderUseNotExitsError(APIResponse<?> apiResponse) {
        log.info("response = {} ", logFilter(JSON.toJSONString(apiResponse)));
        if (!baseHelper.isOk(apiResponse)) {
            // 若返回的是【用户不存在】则返回其他报错
            if (StringUtils.equals(apiResponse.getCode(), GeneralCode.USER_NOT_EXIST.getCode())) {
                throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
            } else {
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
        }
    }

    protected boolean isBinanceCom() {
        return StringUtils.equalsIgnoreCase(System.getProperty("env"), "BIN_PROD");
    }




    protected UserStatusEx getUserStatusByUserId(Long userId) throws Exception{
        UserIdRequest userIdRequest=new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<UserStatusEx> apiResponse= userApi.getUserStatusByUserId(getInstance(userIdRequest));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    protected GetrSubUserBindingsResp getSubBindInfoByParentUserIdAndSubUserId(Long parentUserId, Long subUserId) throws Exception{
        GetSubbindingInfoReq getSubbindingInfoReq=new GetSubbindingInfoReq();
        getSubbindingInfoReq.setParentUserId(parentUserId);
        getSubbindingInfoReq.setSubUserId(subUserId);
        APIResponse<GetrSubUserBindingsResp> apiResponse= brokerApi.getSubBindingInfo(getInstance(getSubbindingInfoReq));
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    public <T> APIRequest<T> getInstanceByAccountVersion(T body) {
        APIRequest<T> request = this.baseHelper.getInstance(body);
        log.info("request = {} ", this.logFilter(request));
        return request;
    }

    public String getAppVersion(String clientType) {
        if ("ios".equalsIgnoreCase(clientType)) {
            return getHeader("versionCode");
        } else if ("android".equalsIgnoreCase(clientType)) {
            return getHeader("versionName");
        }
        return "";
    }

    protected Long getLoginUserId() throws Exception {
        Long userId = getUserId();
        if (userId != null) {
            return userId;
        }
        String email = getTrueUserEmail();
        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(email);
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        if (!baseHelper.isOk(getUserIdResp)) {
            log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,request={}, response={}", getUserIdReq, getUserIdResp);
            checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
        }
        userId = getUserIdResp.getData().getUserId();
        return userId;
    }


}
