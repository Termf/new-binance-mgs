package com.binance.mgs.account.service;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.util.BitUtils;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.subuser.response.DisableManagerSubUserResp;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.accountsubuser.api.ManagerSubUserApi;
import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserClearPositionRequest;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserClearPositionResponse;
import com.binance.accountsubuser.vo.managersubuser.UnbindTradeParentPreCheckReq;
import com.binance.accountsubuser.vo.managersubuser.UnbindTradeParentPreCheckResponse;
import com.binance.accountsubuser.vo.managersubuser.UnbindTradeParentRequest;
import com.binance.accountsubuser.vo.managersubuser.UnbindTradeParentResponse;
import com.binance.accountsubuser.vo.managersubuser.vo.DisableManagerSubUserVo;
import com.binance.accountsubuser.vo.managersubuser.vo.ManagerSubUserInfoVo;
import com.binance.accountsubuser.vo.managersubuser.vo.QueryManagerSubUserInfoVo;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserMultiTransferArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserClearPositionArg;
import com.binance.mgs.account.account.vo.subuser.ManagerUserAvailableBalanceListArg;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.enums.MgsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author sean w
 * @date 2022/2/28
 **/
@Slf4j
@Service
public class ManagerSubUserRelatedService extends BaseAction {

    @Autowired
    private ManagerSubUserApi managerSubUserApi;

    @Autowired
    private UserSecurityApi userSecurityApi;

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Autowired
    private UserApi userApi;

    /**
     * 校验是否是母子关系及是否绑定交易团队
     *
     * @param rootUserId 母账号ID
     * @param managerSubUserId 托管子账号ID
     * @throws Exception
     */
    public ManagerSubUserInfoVo getManagerSubUserInfo(Long rootUserId, Long managerSubUserId) throws Exception{
        QueryManagerSubUserInfoVo queryManagerSubUserInfoVo = new QueryManagerSubUserInfoVo();
        queryManagerSubUserInfoVo.setManagerSubUserId(managerSubUserId);
        queryManagerSubUserInfoVo.setRootUserId(rootUserId);
        APIResponse<ManagerSubUserInfoVo> apiResponse = managerSubUserApi.getManagerSubUserInfo(APIRequest.instance(queryManagerSubUserInfoVo));
        log.info("valid bind trade parent request: {} response: {}", JSONObject.toJSONString(queryManagerSubUserInfoVo), JSONObject.toJSONString(apiResponse));
        if (null == apiResponse || APIResponse.Status.ERROR == apiResponse.getStatus()) {
            log.error("ManagerSubUserRelatedService managerSubUserApi.checkParentChildAndBindTradeParent:rootUserId={}, managerSubUserId={}, error:{}", rootUserId, managerSubUserId, apiResponse.getErrorData());
            throw new BusinessException("managerSubUserApi checkParentChildAndBindTradeParent");
        }
        return apiResponse.getData();
    }

    public APIResponse<DisableManagerSubUserResp> disableManagerSubUser(Long rootUserId, Long managerSubUserId) throws Exception{
        DisableManagerSubUserVo req = new DisableManagerSubUserVo();
        req.setRootUserId(rootUserId);
        req.setManagerSubUserId(managerSubUserId);
        return managerSubUserApi.disableManagerSubUser(getInstance(req));
    }

    public APIResponse<UnbindTradeParentResponse> unbindTradeParent(Long rootUserId, Long managerSubUserId, Long bindTradeParentId) throws Exception {
        UnbindTradeParentRequest unbindTradeParentRequest = new UnbindTradeParentRequest();
        unbindTradeParentRequest.setManagerSubUserId(managerSubUserId);
        unbindTradeParentRequest.setRootUserId(rootUserId);
        unbindTradeParentRequest.setBindTradeParentId(bindTradeParentId);
        return managerSubUserApi.unbindTradeParent(getInstance(unbindTradeParentRequest));
    }

    public APIResponse<UnbindTradeParentPreCheckResponse> unbindTradeParentPreCheck(Long rootUserId, Long managerSubUserId) throws Exception {
        UnbindTradeParentPreCheckReq preCheckReq = new UnbindTradeParentPreCheckReq();
        preCheckReq.setRootUserId(rootUserId);
        preCheckReq.setManagerSubUserId(managerSubUserId);
        return managerSubUserApi.unbindPreCheck(getInstance(preCheckReq));
    }

    /**
     * 根据邮箱校验是否存在并获取userId
     *
     * @param userEmail 邮箱
     * @return Long
     * @throws Exception
     */
    public Long checkAndGetUserId(String userEmail) throws Exception{
        // 校验邮箱格式
        if (!timeOutRegexUtils.validateEmail(userEmail)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // 根据邮箱查询用户userid
        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(userEmail);
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        log.info("managerSubUser userSecurityApi getUserIdByMobileOrEmail request:{}, response:{}", userEmail, JSONObject.toJSONString(getUserIdReq));
        checkResponse(getUserIdResp);
        return getUserIdResp.getData().getUserId();
    }

    public void validClearPositionArgParam(ManagerSubUserClearPositionArg arg) {
        if (StringUtils.isBlank(arg.getManagerSubUserEmail())
                || !timeOutRegexUtils.validateEmail(arg.getManagerSubUserEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        if (!FunctionAccountType.FUTURE.equals(arg.getAccountType())
                && !FunctionAccountType.DELIVERY_FUTURE.equals(arg.getAccountType())) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
    }

    public APIResponse<ManagerSubUserClearPositionResponse> clearPosition(Long parentUserId, Long managerSubUserId, ManagerSubUserClearPositionArg arg) throws Exception{
        ManagerSubUserClearPositionRequest req = new ManagerSubUserClearPositionRequest();
        req.setParentUserId(parentUserId);
        req.setManagerSubUserId(managerSubUserId);
        req.setAccountType(arg.getAccountType());
        req.setSymbol(arg.getSymbol());
        APIResponse<ManagerSubUserClearPositionResponse> resp = managerSubUserApi.clearPosition(APIRequest.instance(req));
        checkResponse(resp);
        return resp;
    }

    /**
     * 根据邮箱校验是否存在并获取用户信息
     *
     * @param userEmail 邮箱
     * @return Long
     * @throws Exception
     */
    public GetUserResponse checkAndGetUser(String userEmail) throws Exception{
        // 校验邮箱格式
        if (!timeOutRegexUtils.validateEmail(userEmail)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // 根据邮箱查询用户userid
        GetUserRequest getUserReq = new GetUserRequest();
        getUserReq.setEmail(userEmail);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserByEmail(APIRequest.instance(getUserReq));
        log.info("managerSubUser userSecurityApi getUserIdByMobileOrEmail request:{}, response:{}", userEmail, JSONObject.toJSONString(apiResponse));
        checkApiResponse(apiResponse);
        return apiResponse.getData();
    }

    /**
     * 校验划转方式（支持托管子账户FUTURE/DELIVERY_FUTURE/MARGIN/ISOLATED到SPOT）
     * @param arg 划转方式
     */
    public void validTransferWay(ManagerSubUserMultiTransferArg arg, Long userStatus) {
        if (FunctionAccountType.getByCode(arg.getSenderFunctionAccountType().getAccountType()) == null || arg.getSenderFunctionAccountType().equals(FunctionAccountType.SPOT)) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        if (!arg.getRecipientFunctionAccountType().equals(FunctionAccountType.SPOT)) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }

        // 如果划转类型是 FUTURE or DELIVERY_FUTURE 校验托管子账户是否存在合约账户
        if (arg.getSenderFunctionAccountType().equals(FunctionAccountType.FUTURE) || arg.getSenderFunctionAccountType().equals(FunctionAccountType.DELIVERY_FUTURE)) {
            if (BitUtils.isFalse(userStatus, Constant.USER_IS_EXIST_FUTURE_ACCOUNT)) {
                throw new BusinessException(MgsErrorCode.FUTURE_ACCT_OR_SUBRELATION_NOT_EXIST);
            }
        }
        // 如果划转类型是 MARGIN or ISOLATED_MARGIN 校验托管子账户是否存在杠杆账户
        if (arg.getSenderFunctionAccountType().equals(FunctionAccountType.MARGIN) || arg.getSenderFunctionAccountType().equals(FunctionAccountType.ISOLATED_MARGIN)) {
            if (BitUtils.isFalse(userStatus, Constant.USER_IS_EXIST_MARGIN_ACCOUNT)) {
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
        }
        // 如果是逐仓的话, 校验是否传交易对
        if (arg.getSenderFunctionAccountType().equals(FunctionAccountType.ISOLATED_MARGIN) && StringUtils.isBlank(arg.getSenderIsolatedMarginSymbol())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
    }

    /**
     * 如果调用 account或者account-subuser 返回的错误信息包括用户不存在或者不是母子关系，这种信息特殊处理
     * @param apiResponse 接口返回
     */
    public void checkApiResponse(APIResponse<?> apiResponse) {
        // 安全要求, 如果未查询到用户或者两者不是母子关系, 错误信息提示不支持(模糊 用户不存在或者不是母子关系)
        if (apiResponse!=null && apiResponse.getStatus()!=APIResponse.Status.OK && (apiResponse.getCode().equals(GeneralCode.USER_NOT_EXIST.getCode())
        || apiResponse.getCode().equals(GeneralCode.TWO_USER_ID_NOT_BOUND.getCode()))) {
            log.info("call api specified response:{} ", JSONObject.toJSONString(apiResponse));
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        checkResponse(apiResponse);
    }

    public void validGetCoinAccountType(ManagerUserAvailableBalanceListArg arg, Long userStatus) {
        // 查询币种不支持 SPOT
        if (FunctionAccountType.getByCode(arg.getType().getAccountType()) == null || arg.getType().equals(FunctionAccountType.SPOT)) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        // 如果划转类型是 FUTURE or DELIVERY_FUTURE 校验托管子账户是否存在合约账户
        if (arg.getType().equals(FunctionAccountType.FUTURE) || arg.getType().equals(FunctionAccountType.DELIVERY_FUTURE)) {
            if (BitUtils.isFalse(userStatus, Constant.USER_IS_EXIST_FUTURE_ACCOUNT)) {
                throw new BusinessException(MgsErrorCode.FUTURE_ACCT_OR_SUBRELATION_NOT_EXIST);
            }
        }
        // 如果划转类型是 MARGIN or ISOLATED_MARGIN 校验托管子账户是否存在杠杆账户
        if (arg.getType().equals(FunctionAccountType.MARGIN) || arg.getType().equals(FunctionAccountType.ISOLATED_MARGIN)) {
            if (BitUtils.isFalse(userStatus, Constant.USER_IS_EXIST_MARGIN_ACCOUNT)) {
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
        }
        // 如果是逐仓的话, 校验是否传交易对
        if (arg.getType().equals(FunctionAccountType.ISOLATED_MARGIN) && StringUtils.isBlank(arg.getSymbol())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
    }
}
