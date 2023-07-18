package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.subuser.FuturePositionRiskVO;
import com.binance.account.vo.subuser.response.DisableManagerSubUserResp;
import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.accountsubuser.api.ManagerSubUserApi;
import com.binance.accountsubuser.vo.asset.request.QueryUserAvailableBalanceListRequest;
import com.binance.accountsubuser.vo.asset.response.QueryUserAvailableBalanceResp;
import com.binance.accountsubuser.vo.delivery.DeliveryPositionRiskVO;
import com.binance.accountsubuser.vo.delivery.request.QueryDeliveryPositionRiskRequest;
import com.binance.accountsubuser.vo.future.request.QueryFuturesPositionRiskRequest;
import com.binance.accountsubuser.vo.managersubuser.BindManagerAccountReq;
import com.binance.accountsubuser.vo.managersubuser.BindManagerAccountResp;
import com.binance.accountsubuser.vo.managersubuser.CreateManagerAccountReq;
import com.binance.accountsubuser.vo.managersubuser.CreateManagerAccountResp;
import com.binance.accountsubuser.vo.managersubuser.GetManagerSubUserTransferHistoryInfoReq;
import com.binance.accountsubuser.vo.managersubuser.GetManagerSubUserTransferHistoryInfoResp;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserClearPositionResponse;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserDeliveryAccountSummaryReq;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserDeliveryAccountSummaryResp;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserFutureAccountSummaryReq;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserFutureAccountSummaryResp;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserTransferRequest;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubUserTransferResponse;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserAssetDetailReq;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserAssetDetailResp;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserInfoReq;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserInfoResp;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserReq;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserResp;
import com.binance.accountsubuser.vo.managersubuser.QueryManagerSubUserTransferHistoryReq;
import com.binance.accountsubuser.vo.managersubuser.UnbindTradeParentPreCheckResponse;
import com.binance.accountsubuser.vo.managersubuser.UnbindTradeParentResponse;
import com.binance.accountsubuser.vo.managersubuser.UpdateManagerAccountRemarkReq;
import com.binance.accountsubuser.vo.managersubuser.UpdateManagerAccountRemarkResponse;
import com.binance.accountsubuser.vo.managersubuser.vo.CheckManagerSubUserExistVo;
import com.binance.accountsubuser.vo.managersubuser.vo.ManagerSubUserInfoVo;
import com.binance.accountsubuser.vo.spot.ClearPositionFailedOrderVO;
import com.binance.accountsubuser.vo.spot.EnableClearPositionRequest;
import com.binance.accountsubuser.vo.spot.MultiFunctionSubUserTransferRequest;
import com.binance.accountsubuser.vo.spot.MultiFunctionSubUserTransferResponse;
import com.binance.assetservice.api.IProductApi;
import com.binance.assetservice.api.IUserAssetApi;
import com.binance.assetservice.enums.SubAccountTransferEnum;
import com.binance.assetservice.vo.request.asset.SelectByUserIdsCodeRequest;
import com.binance.assetservice.vo.request.product.PriceConvertRequest;
import com.binance.assetservice.vo.response.asset.SelectUserAssetResponse;
import com.binance.assetservice.vo.response.product.PriceConvertResponse;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.LogMaskUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.ManagerSubUserBalanceHelper;
import com.binance.mgs.account.account.vo.AccountAssetRet;
import com.binance.mgs.account.account.vo.ManagerFuturesAccountSummaryArg;
import com.binance.mgs.account.account.vo.subuser.BindManagerAccountArg;
import com.binance.mgs.account.account.vo.subuser.ClearPositionFailedOrderVo;
import com.binance.mgs.account.account.vo.subuser.ClearPositionRet;
import com.binance.mgs.account.account.vo.subuser.CreateManagerSubUserArg;
import com.binance.mgs.account.account.vo.subuser.DisableManagerSubUserArg;
import com.binance.mgs.account.account.vo.subuser.GetManagerSubInfoArg;
import com.binance.mgs.account.account.vo.subuser.GetManagerSubUserTransferLogArg;
import com.binance.mgs.account.account.vo.subuser.ManagerAccountMaxWithdrawArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserAssetArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserClearPositionArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserMultiTransferArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserTransferArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserTransferLogArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserTransferLogRet;
import com.binance.mgs.account.account.vo.subuser.ManagerUserAvailableBalanceListArg;
import com.binance.mgs.account.account.vo.subuser.QueryDeliveryAccountAssetRiskSummaryVo;
import com.binance.mgs.account.account.vo.subuser.QueryDeliveryPositionRiskArg;
import com.binance.mgs.account.account.vo.subuser.QueryFuturesAccountAssetRiskSummaryVo;
import com.binance.mgs.account.account.vo.subuser.QueryFuturesPositionRiskArg;
import com.binance.mgs.account.account.vo.subuser.QueryManagerSubUserAssetDetailArg;
import com.binance.mgs.account.account.vo.subuser.QueryManagerSubUserInfoArg;
import com.binance.mgs.account.account.vo.subuser.UnbindManagerSubUserArg;
import com.binance.mgs.account.account.vo.subuser.UnbindManagerSubUserRet;
import com.binance.mgs.account.account.vo.subuser.UpdateManagerAccountRemarkArg;
import com.binance.mgs.account.account.vo.subuser.UserAvailableBalanceListRet;
import com.binance.mgs.account.advice.SubAccountForbidden;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.service.ManagerSubUserRelatedService;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.enums.AccountType;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.binance.platform.mgs.enums.AccountType.DELIVERY;
import static com.binance.platform.mgs.enums.AccountType.FUTURE;
import static com.binance.platform.mgs.enums.AccountType.ISOLATED_MARGIN;
import static com.binance.platform.mgs.enums.AccountType.MAIN;
import static com.binance.platform.mgs.enums.AccountType.MARGIN;

/**
 *
 * pcx
 */
@Log4j2
@RestController
@RequestMapping(value = "/v1/private/account/managersubuser")
public class ManagerSubUserController extends AccountBaseAction {
    @Autowired
    private SysConfigHelper sysConfigHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private UserSecurityApi userSecurityApi;
    @Autowired
    private ManagerSubUserApi managerSubUserApi;
    @Autowired
    protected IUserAssetApi userAssetApi;
    @Autowired
    private ManagerSubUserBalanceHelper managerSubUserBalanceHelper;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;
    @Autowired
    private IProductApi productApi;
    @Autowired
    private ManagerSubUserRelatedService managerSubUserRelatedService;
    @Autowired
    private VerifyRelationService verifyRelationService;

    @Value("${enable.email.lowerCase.switch:true}")
    private boolean enableEmailLowerCaseSwitch;

    @ApiOperation(value = "主账户创建托管子账号")
    @PostMapping(value = "/create")
    @DDoSPreMonitor(action = "createManagerSub")
    public CommonRet<CreateManagerAccountResp> createManagerAccount(@RequestBody @Validated CreateManagerSubUserArg arg, HttpServletRequest request) throws Exception {
        CommonRet<CreateManagerAccountResp> ret = new CommonRet<>();
        // 主账号登陆状态校验
        Long roottUserId = checkAndGetUserId();
        // 判断注册通道是否已关闭
        boolean registerOpen = Boolean.parseBoolean(sysConfigHelper.getCodeByDisplayName("register_open"));
        if (!registerOpen) {
            throw new BusinessException(MgsErrorCode.REGISTER_CLOSE);
        }
        String clientType = baseHelper.getClientType();
        CreateManagerAccountReq req = new CreateManagerAccountReq();
        BeanUtils.copyProperties(arg,req);
        req.setRootUserId(roottUserId);
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, roottUserId.toString(), null);
        req.setDeviceInfo(deviceInfo);
        req.setTerminal(TerminalEnum.WEB);
        // 请求新account微服务，创建账号
        log.info("ManagerSubUserController.createSubUserByParent start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)));
        final APIResponse<CreateManagerAccountResp> apiResponse = managerSubUserApi.createManagerAccount(getInstance(req));
        log.info("ManagerSubUserController.createSubUserByParent end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(req)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    @ApiOperation(value = "将托管账号分配给交易团队母账号")
    @PostMapping(value = "/bind")
    @DDoSPreMonitor(action = "bindManagerSub")
    public CommonRet<Void> bind(@RequestBody @Validated BindManagerAccountArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 主账号登陆状态校验
        Long rootUserId = checkAndGetUserId();

        if (enableEmailLowerCaseSwitch) {
            arg.setManagerSubUserEmail(arg.getManagerSubUserEmail().trim().toLowerCase());
            arg.setTradeParentEmail(arg.getTradeParentEmail().trim().toLowerCase());
        }

        // 查询用户userid
        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
        getUserIdReq.setEmail(arg.getTradeParentEmail());
        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
        if(arg.getTradeParentUserId().longValue()!=getUserIdResp.getData().getUserId().longValue()){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // 查询用户userid
        getUserIdReq.setEmail(arg.getManagerSubUserEmail());
        getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
        checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
        Long managerSubUserId=getUserIdResp.getData().getUserId().longValue();
        //bind
        BindManagerAccountReq req = new BindManagerAccountReq();
        BeanUtils.copyProperties(arg,req);
        req.setRootUserId(rootUserId);
        req.setTradeParentUserId(arg.getTradeParentUserId());
        req.setManagerSubUserId(managerSubUserId);
        // 请求新account微服务，创建账号
        final APIResponse<BindManagerAccountResp> apiResponse = managerSubUserApi.bind(getInstance(req));
        checkResponse(apiResponse);
        return ret;
    }

    @ApiOperation(value = "更新托管账号备注")
    @PostMapping(value = "/updateRemark")
    @DDoSPreMonitor(action = "updateManagerSubRemark")
    public CommonRet<Void> updateRemark(@RequestBody @Validated UpdateManagerAccountRemarkArg arg, HttpServletRequest request) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 主账号登陆状态校验
        Long rootUserId = checkAndGetUserId();
        UpdateManagerAccountRemarkReq req = new UpdateManagerAccountRemarkReq();
        BeanUtils.copyProperties(arg,req);
        req.setRootUserId(rootUserId);
        APIResponse<UpdateManagerAccountRemarkResponse> apiResponse = managerSubUserApi.updateRemark(getInstance(req));
        checkResponse(apiResponse);
        return ret;
    }

    @ApiOperation(value = "禁用托管子账号")
    @PostMapping(value = "/disable")
    @DDoSPreMonitor(action = "disableManagerSub")
    public CommonRet<Void> disable(@RequestBody @Validated DisableManagerSubUserArg arg, HttpServletRequest request) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 主账号登陆状态校验
        Long rootUserId = checkAndGetUserId();
        // 获取托管子账户userId
        Long managerSubUserId = managerSubUserRelatedService.checkAndGetUserId(arg.getManagerSubUserEmail());
        ManagerSubUserInfoVo infoVo = managerSubUserRelatedService.getManagerSubUserInfo(rootUserId, managerSubUserId);
        // 是否是母子关系
        if (infoVo == null) {
            throw new BusinessException(AccountMgsErrorCode.TWO_USER_ID_NOT_MANAGER_SUB_USER_BOUND);
        }
        // 是否绑定交易团队
        if (infoVo.getBindParentUserId() != null) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        // 调用 account-sub 禁用托管子账户
        final APIResponse<DisableManagerSubUserResp> apiResponse = managerSubUserRelatedService.disableManagerSubUser(rootUserId, managerSubUserId);
        log.info("managerSubUserController disableManagerSubUser request managerSubUserId:{}, rootUserId:{} response:{}", managerSubUserId, rootUserId, JSONObject.toJSONString(apiResponse));
        checkResponse(apiResponse);
        return ret;
    }

    @ApiOperation(value = "托管子账户解绑交易团队")
    @PostMapping(value = "/unbind")
    @UserOperation(name = "托管子账户解绑交易团队", eventName = "managerSubUserUnbindTradeParent", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"}, requestKeys = {"managerSubUserEmail"}
            , requestKeyDisplayNames = {"托管子账户邮箱"})
    @DDoSPreMonitor(action = "unbindManagerSub")
    public CommonRet<UnbindManagerSubUserRet> unbindManagerSubUser(@RequestBody @Validated UnbindManagerSubUserArg arg) throws Exception {
        // 主账号登陆状态校验
        Long rootUserId = checkAndGetUserId();
        // 获取托管子账户userId
        Long managerSubUserId = managerSubUserRelatedService.checkAndGetUserId(arg.getManagerSubUserEmail());

        // 调用 account-sub 解绑交易团队前置校验
        final APIResponse<UnbindTradeParentPreCheckResponse> preCheckApiResponse = managerSubUserRelatedService.unbindTradeParentPreCheck(rootUserId, managerSubUserId);
        checkResponse(preCheckApiResponse);

        // 调用 account-sub 解绑交易团队
        final APIResponse<UnbindTradeParentResponse> apiResponse = managerSubUserRelatedService.unbindTradeParent(rootUserId, managerSubUserId, preCheckApiResponse.getData().getTradeParentUserId());
        log.info("managerSubUserController unbindTradeParent request managerSubUserId:{}, rootUserId:{} response:{}", managerSubUserId, rootUserId, JSONObject.toJSONString(apiResponse));
        checkResponse(apiResponse);
        UserOperationHelper.log("parentUserId", rootUserId);
        UserOperationHelper.log("managerSubUserId", managerSubUserId);
        UserOperationHelper.log("tradeParentUserId", preCheckApiResponse.getData().getTradeParentUserId());
        return new CommonRet<>();
    }

    @ApiOperation(value = "查询托管账号信息分页")
    @PostMapping(value = "/queryManagerSubUserInfo")
    public CommonRet<QueryManagerSubUserInfoResp> queryManagerSubUserInfo(@RequestBody @Validated QueryManagerSubUserInfoArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<QueryManagerSubUserInfoResp> ret = new CommonRet<>();
        Long rootUserId = checkAndGetUserId();
        QueryManagerSubUserInfoReq req = new QueryManagerSubUserInfoReq();
        BeanUtils.copyProperties(arg,req);
        req.setRootUserId(rootUserId);
        APIResponse<QueryManagerSubUserInfoResp> apiResponse = managerSubUserApi.queryManagerSubUserInfo(getInstance(req));
        checkResponse(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }

    @ApiOperation(value = "托管账户列表及单个托管账户BTC总值")
    @PostMapping(value = "/query/managersubuser/asset/detail")
    public CommonRet<QueryManagerSubUserAssetDetailResp> queryManagerSubUserAssetDetail(@RequestBody @Validated QueryManagerSubUserAssetDetailArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<QueryManagerSubUserAssetDetailResp> ret = new CommonRet<>();
        Long rootUserId = checkAndGetUserId();
        QueryManagerSubUserAssetDetailReq req = new QueryManagerSubUserAssetDetailReq();
        BeanUtils.copyProperties(arg,req);
        req.setRootUserId(rootUserId);
        APIResponse<QueryManagerSubUserAssetDetailResp> apiResponse = managerSubUserApi.queryManagerSubUserAssetDetail(getInstance(req));
        checkResponse(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }


    @ApiOperation(value = "托管账户划转接口")
    @PostMapping(value = "/managersubuser/transfer")
    public CommonRet<ManagerSubUserTransferResponse> managerSubUserTransfer(@RequestBody @Validated ManagerSubUserTransferArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<ManagerSubUserTransferResponse> ret = new CommonRet<>();
        Long rootUserId = checkAndGetUserId();
        ManagerSubUserTransferRequest req = new ManagerSubUserTransferRequest();
        BeanUtils.copyProperties(arg,req);
        req.setRootUserId(rootUserId);
        APIResponse<ManagerSubUserTransferResponse> apiResponse = managerSubUserApi.managerSubUserTransfer(getInstance(req));
        //安全要求, 返回账户不存在等这种信息的, 模糊错误信息
        managerSubUserRelatedService.checkApiResponse(apiResponse);
        ret.setData(apiResponse.getData());
        return ret;
    }


    /**
     * 指定账户的最大额度
     */
    @PostMapping("/getMaxWithdrawAmount")
    public CommonRet<String> getMaxWithdrawAmount(@Valid @RequestBody ManagerAccountMaxWithdrawArg arg) throws Exception {
        Long rootUserId = checkAndGetUserId();
        String managerSubUserEmail = arg.getManagerSubEmail().trim();

        String rootUserEmail=getUserEmail();
        Long queryUserId=rootUserId;
        log.info("managersubuser getMaxWithdrawAmount rootUserId={},rootUserEmail={},managerSubUserEmail={}", rootUserId, rootUserEmail, managerSubUserEmail);
        if (!rootUserEmail.equalsIgnoreCase(managerSubUserEmail)) {
            CommonRet<String> ret = new CommonRet<>();
            QueryManagerSubUserInfoReq req = new QueryManagerSubUserInfoReq();
            req.setRootUserId(rootUserId);
            req.setManagerSubUserEmail(managerSubUserEmail);
            APIResponse<QueryManagerSubUserInfoResp> apiResponse = managerSubUserApi.queryManagerSubUserInfo(getInstance(req));
            checkResponseMaskUseNotExits(apiResponse);
            if(apiResponse.getData().getTotal().longValue()==0){
                ret.setData(BigDecimal.ZERO.toPlainString());
                return ret;
            }
            queryUserId=apiResponse.getData().getManagerSubUserInfoVoList().get(0).getManagersubUserId();
        }
        log.info("getMaxWithdrawAmount.userInInfo rootUserId={},queryUserId={}", rootUserId,queryUserId);
        switch (arg.getAccountType()){
            case SPOT:
                SelectByUserIdsCodeRequest spotRequest = new SelectByUserIdsCodeRequest();
                spotRequest.setAsset(arg.getAsset());
                spotRequest.setUserIds(Lists.newArrayList(queryUserId));
                APIResponse<List<SelectUserAssetResponse>> spotResponse = userAssetApi.getUserAssetByUserIdsCode(APIRequest.instance(spotRequest));
                checkResponse(spotResponse);
                if (spotResponse.getData() == null || spotResponse.getData().size() == 0) {
                    return new CommonRet<>(BigDecimal.ZERO.toPlainString());
                }
                return new CommonRet<>(spotResponse.getData().get(0).getFree().toPlainString());
            default:
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
    }

    @ApiOperation(value = "母账户获取子账户资产分布情况")
    @PostMapping(value = "/asset/overview")
    public CommonRet<List<AccountAssetRet>> getAssetOverview(@Valid @RequestBody ManagerSubUserAssetArg managerSubUserAssetArg) throws Exception {

        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setEmail(managerSubUserAssetArg.getManagerSubUserEmail());
        APIResponse<Long> managerSubUser = userApi.getUserIdByEmail(APIRequest.instance(getUserRequest));
        checkResponse(managerSubUser);
        Long parentUserId = checkAndGetUserId();
        CheckManagerSubUserExistVo param = new CheckManagerSubUserExistVo();
        param.setRootUserId(parentUserId);
        param.setManagerSubUserId(managerSubUser.getData());
        APIResponse<Boolean> response = managerSubUserApi.checkManagerSubUserExist(APIRequest.instance(param));
        if (response == null || !response.getData()) {
            throw new BusinessException(MgsErrorCode.MANAGER_SUB_USER_NOT_EXIST);
        }

        CommonRet<List<AccountAssetRet>> ret = new CommonRet<>();
        List<AccountType> accountTypeList = Lists.newArrayList(MAIN, MARGIN, ISOLATED_MARGIN, FUTURE, DELIVERY);
        if (CollectionUtils.isNotEmpty(managerSubUserAssetArg.getAccountTypes())) {
            accountTypeList = managerSubUserAssetArg.getAccountTypes();
        }
        String host = WebUtils.getHeader("HOST");
        List<AccountAssetRet> walletBalanceRets = managerSubUserBalanceHelper.getWalletBalanceRets(accountTypeList,
                managerSubUser.getData(), managerSubUserAssetArg.isNeedBalanceDetail(), host);
        ret.setData(walletBalanceRets);
        return ret;
    }

    @ApiOperation(value = "托管子账户划转历史")
    @PostMapping("/transfer/log/list")
    public CommonPageRet<ManagerSubUserTransferLogRet> getManagerSubUserTransferLogList(@RequestBody @Validated ManagerSubUserTransferLogArg arg) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        if (StringUtils.isNotEmpty(arg.getTransfers())) {
            if (!SubAccountTransferEnum.FROM.name().equalsIgnoreCase(arg.getTransfers()) && !SubAccountTransferEnum.TO.name().equalsIgnoreCase(arg.getTransfers())) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM.getCode(), "transfers must be from or to");
            }
        }
        Long startTime = arg.getStartTime();
        Long endTime = arg.getEndTime();

        GetManagerSubUserTransferHistoryInfoReq request = new GetManagerSubUserTransferHistoryInfoReq();
        request.setRootUserId(parentUserId);

        if (StringUtils.isNotBlank(arg.getManagerSubUserEmail())) {
            if (!timeOutRegexUtils.validateEmail(arg.getManagerSubUserEmail())) {
                throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
            }
            GetUserRequest req = new GetUserRequest();
            req.setEmail(arg.getManagerSubUserEmail());
            APIResponse<Long> userIdResponse = userApi.getUserIdByEmail(APIRequest.instance(req));
            if (APIResponse.Status.OK.equals(userIdResponse.getStatus())) {
                if (userIdResponse.getData() == null) {
                    // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
                    throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
                }
                request.setUserId(userIdResponse.getData());
            } else {
                // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
        }

        request.setTransfers(arg.getTransfers());
        request.setPage(arg.getPage());
        request.setLimit(arg.getRows());
        // 检查开始时间、结束时间
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setSenderFunctionAccountType(arg.getSenderFunctionAccountType());
        // 调用托管子账户接口
        APIResponse<GetManagerSubUserTransferHistoryInfoResp> apiResponse = managerSubUserApi.getManagerSubUserTransferHistoryInfo(getInstance(request));
        // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
        managerSubUserRelatedService.checkApiResponse(apiResponse);
        CommonPageRet<ManagerSubUserTransferLogRet> commonPageRet = new CommonPageRet<>();
        List<ManagerSubUserTransferLogRet> resultList = apiResponse.getData().getResult().stream().map(x -> {
            ManagerSubUserTransferLogRet ret = new ManagerSubUserTransferLogRet();
            BeanUtils.copyProperties(x, ret);
            // 新的接口是不存在id的
            if (null != x.getId()) {
                ret.setId(x.getId().toString());
            }
            ret.setAmount(x.getAmount().toPlainString());
            ret.setTransactionId(x.getTransactionId().toString());
            return ret;
        }).collect(Collectors.toList());
        commonPageRet.setData(resultList);
        commonPageRet.setTotal(apiResponse.getData().getCount());
        return commonPageRet;
    }

    @ApiOperation(value = "查询托管子账户的Futures账户汇总")
    @PostMapping(value = "/query/futures/accountSummary")
    public CommonRet<QueryFuturesAccountAssetRiskSummaryVo> queryFuturesAccountSummary(@RequestBody @Validated ManagerFuturesAccountSummaryArg arg) throws Exception {
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        ManagerSubUserFutureAccountSummaryReq req = new ManagerSubUserFutureAccountSummaryReq();
        req.setRootUserId(parentUserId);
        req.setEmail(arg.getEmail());
        APIResponse<ManagerSubUserFutureAccountSummaryResp> apiResponse = managerSubUserApi.queryFuturesAccountSummary(getInstance(req));
        checkResponse(apiResponse);

        ManagerSubUserFutureAccountSummaryResp resp = apiResponse.getData();
        QueryFuturesAccountAssetRiskSummaryVo vo = new QueryFuturesAccountAssetRiskSummaryVo();
        BeanUtils.copyProperties(resp, vo);
        UserStatusEx userStatusEx = new UserStatusEx(resp.getUserStatus());
        vo.setIsSubUserEnabled(userStatusEx.getIsSubUserEnabled());
        return new CommonRet<>(vo);
    }

    @ApiOperation(value = "查询托管子账户的Futures持仓信息")
    @PostMapping(value = "/query/futures/positionRisk")
    public CommonRet<List<FuturePositionRiskVO>> queryFuturesPositionRisk(@RequestBody @Validated QueryFuturesPositionRiskArg arg) throws Exception{
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        QueryFuturesPositionRiskRequest req = new QueryFuturesPositionRiskRequest();
        req.setParentUserId(parentUserId);
        req.setEmail(arg.getEmail());
        APIResponse<List<com.binance.accountsubuser.vo.future.FuturePositionRiskVO>> apiResponse = managerSubUserApi.queryFuturesPositionRisk(getInstance(req));
        checkResponse(apiResponse);

        List<FuturePositionRiskVO> riskVOList = Collections.emptyList();
        if(CollectionUtils.isNotEmpty(apiResponse.getData())) {
            riskVOList = apiResponse.getData().stream()
                    .map(x -> {
                        FuturePositionRiskVO riskVO = new FuturePositionRiskVO();
                        BeanUtils.copyProperties(x, riskVO);
                        return riskVO;
                    })
                    .collect(Collectors.toList());
        }
        return new CommonRet<>(riskVOList);
    }

    @ApiOperation(value = "托管子账户币本位合约资产")
    @PostMapping(value = "/query/delivery/accountSummary")
    public CommonRet<QueryDeliveryAccountAssetRiskSummaryVo> deliverySubAccountSummary(@RequestBody @Validated ManagerFuturesAccountSummaryArg arg) throws Exception {
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        ManagerSubUserDeliveryAccountSummaryReq req = new ManagerSubUserDeliveryAccountSummaryReq();
        req.setRootUserId(parentUserId);
        req.setEmail(arg.getEmail());
        APIResponse<ManagerSubUserDeliveryAccountSummaryResp> apiResponse = managerSubUserApi.queryDeliveryAccountSummary(getInstance(req));
        checkResponse(apiResponse);

        // 汇率转换
        BigDecimal exchangeRate = this.priceConvert("USDT","BTC");

        ManagerSubUserDeliveryAccountSummaryResp resp = apiResponse.getData();
        QueryDeliveryAccountAssetRiskSummaryVo summaryVo = new QueryDeliveryAccountAssetRiskSummaryVo();
        summaryVo.setEmail(resp.getEmail());
        UserStatusEx userStatusEx = new UserStatusEx(resp.getUserStatus());
        summaryVo.setIsSubUserEnabled(userStatusEx.getIsSubUserEnabled());
        summaryVo.setMarginBalance(new BigDecimal(resp.getMarginBalance()).multiply(exchangeRate).setScale(8, RoundingMode.DOWN).toPlainString());
        summaryVo.setUnrealizedProfit(new BigDecimal(resp.getUnrealizedProfit()).multiply(exchangeRate).setScale(8, RoundingMode.DOWN).toPlainString());
        summaryVo.setWalletBalance(new BigDecimal(resp.getWalletBalance()).multiply(exchangeRate).setScale(8, RoundingMode.DOWN).toPlainString());
        summaryVo.setMaintenanceMargin(new BigDecimal(resp.getMaintenanceMargin()).multiply(exchangeRate).setScale(8,RoundingMode.CEILING).toPlainString());
        return new CommonRet<>(summaryVo);
    }

    /**
     * 币种汇率转换
     */
    private BigDecimal priceConvert(String from, String to) {
        try {
            PriceConvertRequest request = new PriceConvertRequest();
            request.setFrom(from);
            request.setTo(to);
            request.setAmount(BigDecimal.ONE);
            APIResponse<PriceConvertResponse> apiResponse = productApi.priceConvert(APIRequest.instance(request));
            if (apiResponse.getData() != null) {
                return apiResponse.getData().getPrice();
            }
        } catch (Exception e) {
            // 正常不应该走到该逻辑，仅仅为了防御
            log.error("转换失败失败,from={},to={}", from, to);
        }
        return BigDecimal.ZERO;
    }

    @ApiOperation(value = "查询托管子账户的币本位合约持仓信息")
    @PostMapping(value = "/query/delivery/positionRisk")
    public CommonRet<List<DeliveryPositionRiskVO>> deliveryPositionRisk(@RequestBody @Validated QueryDeliveryPositionRiskArg arg) throws Exception {
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        QueryDeliveryPositionRiskRequest req = new QueryDeliveryPositionRiskRequest();
        req.setParentUserId(parentUserId);
        req.setEmail(arg.getEmail());
        req.setPair(arg.getPair());
        APIResponse<List<DeliveryPositionRiskVO>> apiResponse = managerSubUserApi.queryDeliveryPositionRisk(getInstance(req));
        checkResponse(apiResponse);

        return new CommonRet<>(apiResponse.getData());
    }

    @ApiOperation(value = "母账户一键平仓托管子账户")
    @PostMapping(value = "/clearPosition")
    @UserOperation(name = "托管母账户一键平仓托管子账户", eventName = "managerSubUserClearPosition", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"}, requestKeys = {"managerSubUserEmail", "symbol", "accountType"}
            , requestKeyDisplayNames = {"托管子账户邮箱", "交易对", "账户类型"})
    @SubAccountForbidden
    @DDoSPreMonitor(action = "clearManagerSubPosition")
    public CommonRet<ClearPositionRet> clearPosition(@RequestBody @Validated ManagerSubUserClearPositionArg arg) throws Exception {

        Long parentUserId = checkAndGetUserId();
        UserStatusEx parentStatusEx = getUserStatusByUserId(parentUserId);
        //判断有没有开启一键平仓的权限
        if (!parentStatusEx.getIsAbleManagerSubUserClearPosition()) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        // 校验参数
        managerSubUserRelatedService.validClearPositionArgParam(arg);
        // 获取托管子账户userId
        Long managerSubUserId = managerSubUserRelatedService.checkAndGetUserId(arg.getManagerSubUserEmail());
        ManagerSubUserInfoVo infoVo = managerSubUserRelatedService.getManagerSubUserInfo(parentUserId, managerSubUserId);
        // 是否是母子关系
        if (infoVo == null) {
            throw new BusinessException(AccountMgsErrorCode.TWO_USER_ID_NOT_MANAGER_SUB_USER_BOUND);
        }
        APIResponse<ManagerSubUserClearPositionResponse> resp = managerSubUserRelatedService.clearPosition(parentUserId, managerSubUserId, arg);
        ClearPositionRet ret = new ClearPositionRet();
        List<ClearPositionFailedOrderVo> failedOrders = Lists.newArrayList();
        for (ClearPositionFailedOrderVO clearPositionFailedOrder : resp.getData().getFailOrderList()) {
            ClearPositionFailedOrderVo failedOrder = new ClearPositionFailedOrderVo();
            BeanUtils.copyProperties(clearPositionFailedOrder, failedOrder);
            failedOrders.add(failedOrder);
        }
        ret.setFailOrderList(failedOrders);
        UserOperationHelper.log("parentUserId", parentUserId);
        UserOperationHelper.log("managerSubUserId", managerSubUserId);
        UserOperationHelper.log("symbol", arg.getSymbol());
        UserOperationHelper.log("accountType", arg.getAccountType());
        return new CommonRet<>(ret);
    }

    @ApiOperation("母账户开启一键平仓权限")
    @PostMapping("/enable/clearPosition")
    @SubAccountForbidden
    @DDoSPreMonitor(action = "enableTradeParentClearPosition")
    public CommonRet<Boolean> enableClearPosition() throws Exception {
        Long parentUserId = checkAndGetUserId();

        UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
        if (!userStatusEx.getIsManagerSubUserFunctionEnabled()) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }

        EnableClearPositionRequest req = new EnableClearPositionRequest();
        req.setParentUserId(parentUserId);
        APIResponse<Boolean> resp = managerSubUserApi.enableClearPosition(APIRequest.instance(req));
        checkResponse(resp);
        return new CommonRet<>(resp.getData());
    }
    @ApiOperation(value = "托管账户划转接口(FUTURE/DELIVERY_FUTURE)")
    @PostMapping(value = "/multiFunctionAccountTransfer")
    @UserOperation(name = "托管子账户向母账户划转", eventName = "multiFunctionAccountTransfer", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"},
            requestKeys = {"senderUserId", "recipientUserId", "asset", "amount"},
            requestKeyDisplayNames = {"转出方userId", "转入方userId", "币种", "数量"})
    @DDoSPreMonitor(action = "managerMultiFunctionAccountTransfer")
    public CommonRet<String> multiFunctionAccountTransfer(@RequestBody @Validated ManagerSubUserMultiTransferArg arg, HttpServletResponse resp) throws Exception {

        Long rootUserId = checkAndGetUserId();
        UserVo recipientUser = managerSubUserRelatedService.checkAndGetUser(arg.getRecipientUserEmail()).getUser();
        // 只支持转入到母账户
        if (!recipientUser.getUserId().equals(rootUserId)) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        UserVo senderUser = managerSubUserRelatedService.checkAndGetUser(arg.getSenderUserEmail()).getUser();
        // 校验绑定关系
        ManagerSubUserInfoVo infoVo = managerSubUserRelatedService.getManagerSubUserInfo(recipientUser.getUserId(), senderUser.getUserId());
        if (infoVo == null) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        // 校验划转及用户是否存在划转type对应的账户
        managerSubUserRelatedService.validTransferWay(arg, senderUser.getStatus());
        // 调用 account-sub 划转接口
        MultiFunctionSubUserTransferRequest request = new MultiFunctionSubUserTransferRequest();
        request.setParentUserId(rootUserId);
        request.setSenderUserId(senderUser.getUserId());
        request.setRecipientUserId(recipientUser.getUserId());
        request.setAsset(arg.getAsset().toUpperCase());
        request.setAmount(arg.getAmount());
        request.setSenderFunctionAccountType(arg.getSenderFunctionAccountType());
        request.setRecipientFunctionAccountType(arg.getRecipientFunctionAccountType());
        request.setSenderIsolatedMarginSymbol(arg.getSenderIsolatedMarginSymbol());
        request.setRecipientIsolatedMarginSymbol(arg.getRecipientIsolatedMarginSymbol());
        APIResponse<MultiFunctionSubUserTransferResponse> apiResponse = managerSubUserApi.multiFunctionAccountTransfer(getInstance(request));
        log.info("managerSubUserApi.multiFunctionAccountTransfer request:{}, response:{}", JSONObject.toJSONString(request), JSONObject.toJSONString(apiResponse));
        // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
        managerSubUserRelatedService.checkApiResponse(apiResponse);
        CommonRet<String> commonRet = new CommonRet<String>();
        commonRet.setData(apiResponse.getData().getTransactionId().toString());
        UserOperationHelper.log("parentUserId", request.getParentUserId());
        UserOperationHelper.log("senderUserId", request.getSenderUserId());
        UserOperationHelper.log("recipientUserId", request.getRecipientUserId());
        UserOperationHelper.log("asset", request.getAsset());
        UserOperationHelper.log("amount", request.getAmount());
        return commonRet;
    }

    @ApiOperation(value = "查询账户FUTURE/DELIVERY/MARGIN/ISOLATED有资产币种的可用余额")
    @PostMapping(value = "/coin/list")
    public CommonRet<List<UserAvailableBalanceListRet>> getSubUserCoinBalanceList(@RequestBody ManagerUserAvailableBalanceListArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        UserVo managerSubUser = managerSubUserRelatedService.checkAndGetUser(arg.getEmail()).getUser();
        // 校验划转及用户是否存在划转type对应的账户
        managerSubUserRelatedService.validGetCoinAccountType(arg, managerSubUser.getStatus());
        // 校验绑定关系
        ManagerSubUserInfoVo infoVo = managerSubUserRelatedService.getManagerSubUserInfo(parentUserId, managerSubUser.getUserId());
        if (infoVo == null) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }

        CommonRet<List<UserAvailableBalanceListRet>> commonRet = new CommonRet<>();
        QueryUserAvailableBalanceListRequest req = new QueryUserAvailableBalanceListRequest();
        req.setEmail(arg.getEmail());
        req.setParentUserId(parentUserId);
        req.setType(arg.getType());
        req.setSymbol(arg.getSymbol());
        APIResponse<List<QueryUserAvailableBalanceResp>> response = managerSubUserApi.queryUserAvailableBalanceList(APIRequest.instance(req));
        checkResponse(response);

        List<QueryUserAvailableBalanceResp> userAvailableBalances = response.getData();
        if (CollectionUtils.isEmpty(userAvailableBalances)) {
            return commonRet;
        }
        List<UserAvailableBalanceListRet> balanceList = userAvailableBalances.stream().map(
                x -> {
                    UserAvailableBalanceListRet availableBalanceListRet = new UserAvailableBalanceListRet();
                    availableBalanceListRet.setCoin(x.getCoin());
                    availableBalanceListRet.setFree(x.getFree().toPlainString());
                    availableBalanceListRet.setEtf(x.isEtf());
                    return availableBalanceListRet;
                }
        ).collect(Collectors.toList());
        commonRet.setData(balanceList);
        return commonRet;
    }

    @ApiOperation(value = "交易团队查询绑定的托管子账户")
    @PostMapping(value = "/getMsaInfoByTradeTeam")
    public CommonRet<QueryManagerSubUserResp> getManagerSubInfoByTradeTeam(@RequestBody @Validated GetManagerSubInfoArg arg) throws Exception {

        Long parentUserId = checkAndGetUserId();
        UserStatusEx parentStatusEx = getUserStatusByUserId(parentUserId);
        //判断有没有开启母账户功能
        if (!parentStatusEx.getIsSubUserFunctionEnabled()) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        QueryManagerSubUserReq subUserReq = new QueryManagerSubUserReq();
        subUserReq.setBindParentUserId(parentUserId);
        BeanUtils.copyProperties(arg, subUserReq);
        APIResponse<QueryManagerSubUserResp> apiResponse = managerSubUserApi.getManagerSubUserInfoByTradeTeam(getInstance(subUserReq));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @ApiOperation(value = "交易团队查询托管子账户划转历史")
    @PostMapping("/tradeTeam/getTransferLog")
    public CommonPageRet<ManagerSubUserTransferLogRet> tradeTeamGetMsaTransferLogList(@RequestBody @Validated GetManagerSubUserTransferLogArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        if (StringUtils.isNotEmpty(arg.getTransfers())) {
            if (!SubAccountTransferEnum.FROM.name().equalsIgnoreCase(arg.getTransfers()) && !SubAccountTransferEnum.TO.name().equalsIgnoreCase(arg.getTransfers())) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM.getCode(), "transfers must be from or to");
            }
        }
        // 校验母子关系(这边是普通子母关系校验)
        QueryManagerSubUserTransferHistoryReq transferHistoryReq = new QueryManagerSubUserTransferHistoryReq();
        transferHistoryReq.setBindParentUseId(parentUserId);
        if (!timeOutRegexUtils.validateEmail(arg.getManagerSubUserEmail())) {
            throw new BusinessException(MgsErrorCode.EMAIL_FORMAT_ERROR);
        }
        Long managerSubUserId = verifyRelationService.checkRelationByParentSubUserEmail(parentUserId, arg.getManagerSubUserEmail());
        transferHistoryReq.setUserId(managerSubUserId);

        transferHistoryReq.setTransfers(arg.getTransfers());
        transferHistoryReq.setPage(arg.getPage());
        transferHistoryReq.setLimit(arg.getRows());
        // 检查开始时间、结束时间
        transferHistoryReq.setStartTime(arg.getStartTime());
        transferHistoryReq.setEndTime(arg.getEndTime());
        transferHistoryReq.setSenderFunctionAccountType(arg.getSenderFunctionAccountType());
        APIResponse<GetManagerSubUserTransferHistoryInfoResp> apiResponse = managerSubUserApi.tradeTeamGetManagerSubTransferLog(APIRequest.instance(transferHistoryReq));
        // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
        managerSubUserRelatedService.checkApiResponse(apiResponse);
        CommonPageRet<ManagerSubUserTransferLogRet> commonPageRet = new CommonPageRet<>();
        List<ManagerSubUserTransferLogRet> resultList = apiResponse.getData().getResult().stream().map(x -> {
            ManagerSubUserTransferLogRet ret = new ManagerSubUserTransferLogRet();
            BeanUtils.copyProperties(x, ret);
            // 新的接口是不存在id的
            if (null != x.getId()) {
                ret.setId(x.getId().toString());
            }
            ret.setAmount(x.getAmount().toPlainString());
            ret.setTransactionId(x.getTransactionId().toString());
            return ret;
        }).collect(Collectors.toList());
        commonPageRet.setData(resultList);
        commonPageRet.setTotal(apiResponse.getData().getCount());
        return commonPageRet;
    }
}
