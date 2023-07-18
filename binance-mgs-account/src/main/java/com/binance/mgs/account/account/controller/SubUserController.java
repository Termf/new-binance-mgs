package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.SubUserApi;
import com.binance.account.api.UserInfoApi;
import com.binance.account.api.UserLVTApi;
import com.binance.account.vo.security.UserSecurityLogVo;
import com.binance.account.vo.security.response.GetUserSecurityLogResponse;
import com.binance.account.vo.subuser.CreateNoEmailSubUserReq;
import com.binance.account.vo.subuser.FutureAccountSummaryInfoVo;
import com.binance.account.vo.subuser.FuturePositionRiskVO;
import com.binance.account.vo.subuser.MarginAccountSummaryInfoVo;
import com.binance.account.vo.subuser.SubUserInfoVo;
import com.binance.account.vo.subuser.enums.MarginPeriodType;
import com.binance.account.vo.subuser.enums.SubAccountSummaryQueryType;
import com.binance.account.vo.subuser.request.BindingParentSubUserEmailReq;
import com.binance.account.vo.subuser.request.BindingParentSubUserReq;
import com.binance.account.vo.subuser.request.CheckParentAndSubUserBindingRequest;
import com.binance.account.vo.subuser.request.DeleteSubUserRequest;
import com.binance.account.vo.subuser.request.ModifySubAccountRequest;
import com.binance.account.vo.subuser.request.OpenOrCloseSubUserReq;
import com.binance.account.vo.subuser.request.QueryFuturesPositionRiskRequest;
import com.binance.account.vo.subuser.request.QuerySubAccountFutureAccountRequest;
import com.binance.account.vo.subuser.request.QuerySubAccountFutureAccountSummaryRequest;
import com.binance.account.vo.subuser.request.QuerySubAccountMarginAccountRequest;
import com.binance.account.vo.subuser.request.QuerySubAccountMarginAccountSummaryRequest;
import com.binance.account.vo.subuser.request.QuerySubUserRequest;
import com.binance.account.vo.subuser.request.ResetSecondValidationRequest;
import com.binance.account.vo.subuser.request.SubAccountFuturesEnableRequest;
import com.binance.account.vo.subuser.request.SubAccountMarginEnableRequest;
import com.binance.account.vo.subuser.request.SubUserSecurityLogReq;
import com.binance.account.vo.subuser.request.UpdatePassWordRequest;
import com.binance.account.vo.subuser.request.UpdateSubUserRemarkRequest;
import com.binance.account.vo.subuser.request.UserIdReq;
import com.binance.account.vo.subuser.response.BindingParentSubUserEmailResp;
import com.binance.account.vo.subuser.response.CreateNoEmailSubUserResp;
import com.binance.account.vo.subuser.response.DeleteSubUserResp;
import com.binance.account.vo.subuser.response.QuerySubAccountFutureAccountSummaryResp;
import com.binance.account.vo.subuser.response.QuerySubAccountMarginAccountResp;
import com.binance.account.vo.subuser.response.QuerySubAccountMarginAccountSummaryResp;
import com.binance.account.vo.subuser.response.SubAccountFuturesEnableResp;
import com.binance.account.vo.subuser.response.SubAccountMarginEnableResp;
import com.binance.account.vo.subuser.response.SubUserInfoResp;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.request.UserIdRequest;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.accountsubuser.api.FlexLineSubApi;
import com.binance.accountsubuser.api.MerchantSubUserCommonApi;
import com.binance.accountsubuser.api.SubUserAccountApi;
import com.binance.accountsubuser.api.SubUserAssetApi;
import com.binance.accountsubuser.api.SubUserDeliveryApi;
import com.binance.accountsubuser.api.SubUserFutureApi;
import com.binance.accountsubuser.api.SubUserMarginApi;
import com.binance.accountsubuser.api.SubUserSpotApi;
import com.binance.accountsubuser.core.annotation.RolePermissionCheck;
import com.binance.accountsubuser.core.helper.RolePermissionCheckHelper;
import com.binance.accountsubuser.vo.asset.request.SubUserBalanceRequest;
import com.binance.accountsubuser.vo.asset.response.ParentSubUserCoinAmountResp;
import com.binance.accountsubuser.vo.constants.Constant;
import com.binance.accountsubuser.vo.delivery.DeliveryPositionRiskVO;
import com.binance.accountsubuser.vo.delivery.request.DeliveryTotalAccountSummaryRequest;
import com.binance.accountsubuser.vo.delivery.request.QueryDeliveryPositionRiskRequest;
import com.binance.accountsubuser.vo.delivery.request.QuerySubAccountDeliverySummaryRequest;
import com.binance.accountsubuser.vo.delivery.response.DeliveryTotalAccountSummaryResp;
import com.binance.accountsubuser.vo.delivery.response.QuerySubAccountDeliveryAccountSummaryResp;
import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import com.binance.accountsubuser.vo.managersubuser.CreateCommMerchantSubUserReq;
import com.binance.accountsubuser.vo.managersubuser.CreateSubUserRes;
import com.binance.accountsubuser.vo.managersubuser.DeleteCommMerchantSubUserReq;
import com.binance.accountsubuser.vo.margin.request.MainMarginAccountTransferRequest;
import com.binance.accountsubuser.vo.margin.response.MainMarginAccountTransferResponse;
import com.binance.accountsubuser.vo.merchant.CommMercSearchResult;
import com.binance.accountsubuser.vo.merchant.MerchantSubUserVo;
import com.binance.accountsubuser.vo.merchant.request.QueryCommMerchantSubUserReq;
import com.binance.accountsubuser.vo.spot.ClearPositionFailedOrderVO;
import com.binance.accountsubuser.vo.spot.ClearPositionRequest;
import com.binance.accountsubuser.vo.spot.ClearPositionResponse;
import com.binance.accountsubuser.vo.spot.EnableClearPositionRequest;
import com.binance.accountsubuser.vo.spot.MultiFunctionSubUserTransferRequest;
import com.binance.accountsubuser.vo.spot.MultiFunctionSubUserTransferResponse;
import com.binance.accountsubuser.vo.subuser.request.DeleteSubUserPreCheckReq;
import com.binance.accountsubuser.vo.subuser.request.FlexLineQuerySubUserReq;
import com.binance.accountsubuser.vo.subuser.request.QueryStpTypeReq;
import com.binance.accountsubuser.vo.subuser.request.StpAccountSettingReq;
import com.binance.accountsubuser.vo.subuser.response.DeleteSubUserPreCheckResponse;
import com.binance.accountsubuser.vo.subuser.response.FlexLineQuerySubUserResp;
import com.binance.accountsubuser.vo.subuser.response.QueryStpTypeResp;
import com.binance.accountsubuser.vo.subuser.response.StpAccountSettingResp;
import com.binance.assetservice.api.IProductApi;
import com.binance.assetservice.api.ITranApi;
import com.binance.assetservice.api.IUserAssetApi;
import com.binance.assetservice.vo.request.GetTranRequest;
import com.binance.assetservice.vo.request.asset.SelectByUserIdsCodeRequest;
import com.binance.assetservice.vo.request.product.PriceConvertRequest;
import com.binance.assetservice.vo.response.asset.SelectUserAssetResponse;
import com.binance.assetservice.vo.response.product.PriceConvertResponse;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.delivery.periphery.api.admin.DeliveryAdminBalanceApi;
import com.binance.future.api.BalanceApi;
import com.binance.future.api.request.GetMaxWithdrawAmountReq;
import com.binance.margin.api.transfer.TransferApi;
import com.binance.master.enums.AuthTypeEnum;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.LogMaskUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.controller.client.SubUserClient;
import com.binance.mgs.account.account.enums.SubUserBizType;
import com.binance.mgs.account.account.enums.SubUserTransferKindType;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.AccountTransferHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.helper.IsolatedMarginHelper;
import com.binance.mgs.account.account.vo.AccountMaxWithdrawArg;
import com.binance.mgs.account.account.vo.BigDecimalWrapper;
import com.binance.mgs.account.account.vo.SubAccountDeliveryTransferArg;
import com.binance.mgs.account.account.vo.SubAccountFuturePreTransferArg;
import com.binance.mgs.account.account.vo.SubAccountFutureTransferArg;
import com.binance.mgs.account.account.vo.SubAccountMarginPreTransferArg;
import com.binance.mgs.account.account.vo.SubAccountMarginTransferArg;
import com.binance.mgs.account.account.vo.subuser.ClearPositionArg;
import com.binance.mgs.account.account.vo.subuser.ClearPositionFailedOrderVo;
import com.binance.mgs.account.account.vo.subuser.ClearPositionRet;
import com.binance.mgs.account.account.vo.subuser.CreateCommMerchantSubUserArg;
import com.binance.mgs.account.account.vo.subuser.CreateCommMerchantSubUserRet;
import com.binance.mgs.account.account.vo.subuser.CreateNoEmailSubUserArg;
import com.binance.mgs.account.account.vo.subuser.DeleteCommMerchantSubUserArg;
import com.binance.mgs.account.account.vo.subuser.DeleteSubUserArg;
import com.binance.mgs.account.account.vo.subuser.DeleteSubUserRet;
import com.binance.mgs.account.account.vo.subuser.EnableDisableSubUserArg;
import com.binance.mgs.account.account.vo.subuser.EnableSubAccountFuturesArg;
import com.binance.mgs.account.account.vo.subuser.EnableSubAccountMarginArg;
import com.binance.mgs.account.account.vo.subuser.GetSubUserInfoArg;
import com.binance.mgs.account.account.vo.subuser.IsolatedMarginAccountDetailsRet;
import com.binance.mgs.account.account.vo.subuser.IsolatedMarginParentAndSubAccountSummaryRet;
import com.binance.mgs.account.account.vo.subuser.MarginParentAndSubaccountSummaryRet;
import com.binance.mgs.account.account.vo.subuser.MerchantSubUserVoRet;
import com.binance.mgs.account.account.vo.subuser.ParentAndSubaccountSummaryRet;
import com.binance.mgs.account.account.vo.subuser.ProfitSummaryRet;
import com.binance.mgs.account.account.vo.subuser.QueryCommMerchantSubUserArg;
import com.binance.mgs.account.account.vo.subuser.QueryDeliveryAccountAssetRiskSummaryVo;
import com.binance.mgs.account.account.vo.subuser.QueryDeliveryPositionRiskArg;
import com.binance.mgs.account.account.vo.subuser.QueryFuturesAccountAssetRiskSummaryVo;
import com.binance.mgs.account.account.vo.subuser.QueryFuturesAccountSummaryArg;
import com.binance.mgs.account.account.vo.subuser.QueryFuturesPositionRiskArg;
import com.binance.mgs.account.account.vo.subuser.QueryIsolatedMarginAccountSummaryArg;
import com.binance.mgs.account.account.vo.subuser.QueryMarginAccountArg;
import com.binance.mgs.account.account.vo.subuser.QueryMarginAccountAssetRiskSummaryVo;
import com.binance.mgs.account.account.vo.subuser.QueryMarginAccountSummaryArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserCountConfigArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserIsolatedMarginDetailArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserIsolatedMarginProfitArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserIsolatedMarginSummaryRet;
import com.binance.mgs.account.account.vo.subuser.ResetSubUser2FaArg;
import com.binance.mgs.account.account.vo.subuser.StpAccountSettingArg;
import com.binance.mgs.account.account.vo.subuser.SubUserBalanceArg;
import com.binance.mgs.account.account.vo.subuser.SubUserCountLevelConfigRet;
import com.binance.mgs.account.account.vo.subuser.SubUserIdArg;
import com.binance.mgs.account.account.vo.subuser.SubUserIdEmailListArg;
import com.binance.mgs.account.account.vo.subuser.SubUserIdEmailListRet;
import com.binance.mgs.account.account.vo.subuser.SubUserIdEmailRet;
import com.binance.mgs.account.account.vo.subuser.SubUserInfoRet;
import com.binance.mgs.account.account.vo.subuser.SubUserLoginHistoryArg;
import com.binance.mgs.account.account.vo.subuser.SubUserLoginHistoryRet;
import com.binance.mgs.account.account.vo.subuser.SubUserModifyReq;
import com.binance.mgs.account.account.vo.subuser.SubUserTransferIsolatedMarginArg;
import com.binance.mgs.account.account.vo.subuser.UpdateSubUserPswArg;
import com.binance.mgs.account.account.vo.subuser.UpdateSubUserRemarkArg;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.OperateType;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.DateUtil;
import com.binance.transfer.api.IWalletTransferApi;
import com.binance.transfer.enums.CommonStatus;
import com.binance.transfer.enums.KindType;
import com.binance.transfer.vo.transfer.WalletAssetTransferRequest;
import com.binance.userbigdata.api.SubUserCountConfigApi;
import com.binance.userbigdata.vo.subuser.SubUserCountConfigVo;
import com.binance.userbigdata.vo.subuser.request.AllSubUserCountConfigRequest;
import com.binance.userbigdata.vo.subuser.request.CountSubUserBindsRequest;
import com.binance.userbigdata.vo.subuser.request.GetSubUserBindsRequest;
import com.binance.userbigdata.vo.subuser.request.QueryAvailableSubUserCountRequest;
import com.binance.userbigdata.vo.subuser.response.QueryAvailableSubUserCountResp;
import com.binance.userbigdata.vo.subuser.response.SubUserBindingVo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.binance.accountsubuser.vo.enums.SubAccountSummaryQueryType.ONLY_PARENT_ACCOUNT;
import static com.binance.accountsubuser.vo.enums.SubAccountSummaryQueryType.ONLY_SUB_ACCOUNT;

/**
 * Created by Fei.Huang on 2018/11/6.
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/private/account/subuser")
public class SubUserController extends AccountBaseAction {

    private static final Pattern NOEMAILSUBUSER_REG_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,20}$");

    private static final Integer initPage = 1;//分页默认起始页
    private static final Integer pageLimit = 20;//分页默认条数
    private static final String TRANSFER_SOURCE = "BINANCE-MGS-ACCOUNT";

    /**
     * future、margin一些查询开关，true-走account-subuser false-老的方式，走account
     */
    @Value("${future_margin.subuser.query.switch:true}")
    private boolean queryFutureMarginSwitch;

    /**
     * 子账号开通margin账号开关，true-走account-subuser false-老的方式，走account
     */
    @Value("${margin.subuser.create.switch:true}")
    private boolean createSubUserMarginSwitch;

    @Value("${query.subuser.page.size:100}")
    private int querySubUserPageSize;

    @Value("${sub.account.action.limit.count:100}")
    private int subAccountActionCount;

    @Value("${merchant.sub.account.action.limit.count:200}")
    private int merchantSubAccountActionCount;

    @Value("${merchant.sub.account.ddos.expire.time:3600}")
    private int merchantSubAccountDdosExpireTime;

    @Value("${flex.line.sub.main.margin.transfer.switch:true}")
    protected boolean checkFlexLineSubMainMarginTransfer;

    @Autowired
    private SubUserApi subUserApi;
    @Autowired
    private SubUserClient subUserClient;
    @Autowired
    private com.binance.userbigdata.api.SubUserApi subUserApiV2;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private AccountHelper accountHelper;
    @Resource
    private SysConfigHelper sysConfigHelper;
    @Resource
    protected IUserAssetApi userAssetApi;
    @Autowired
    private ITranApi tranApi;
    @Autowired
    private BalanceApi balanceApi;
    @Autowired
    private DeliveryAdminBalanceApi deliveryAdminBalanceApi;
    @Autowired
    private UserInfoApi userInfoApi;
    @Autowired
    private TransferApi transferApi;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private SubUserFutureApi subUserFutureApi;
    @Autowired
    private SubUserMarginApi subUserMarginApi;
    @Autowired
    private UserLVTApi userLVTApi;
    @Autowired
    private SubUserAssetApi subUserAssetApi;
    @Autowired
    private SubUserDeliveryApi subUserDeliveryApi;
    @Autowired
    private IProductApi productApi;
    @Autowired
    private SubUserSpotApi subUserSpotApi;
    @Autowired
    private SubUserCountConfigApi subUserCountConfigApi;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    private AccountTransferHelper accountTransferHelper;
    @Autowired
    private IsolatedMarginHelper isolatedMarginHelper;
    @Autowired
    private com.binance.margin.isolated.api.transfer.TransferApi isolatedMarginTransferApi;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;
    @Autowired
    private IWalletTransferApi walletTransferApi;
    @Autowired
    private SubUserAccountApi subUserAccountApi;
    @Autowired
    private FlexLineSubApi flexLineSubApi;
    @Autowired
    private MerchantSubUserCommonApi merchantSubUserCommonApi;
    @Value("${sub.user.future.pass.tutorial.switch:false}")
    private Boolean subUserFuturePassTutorialSwitch;
    @Value("${sub.user.get.all.flexline.parent:true}")
    private Boolean querySubUserToGetAllFlexLineParentId;

    @ApiOperation(value = "母账户启用子账户(单个/批量)")
    @PostMapping(value = "/enable")
    @DDoSPreMonitor(action = "enableSubUser")
    public CommonRet<Void> enableSubUser(@RequestBody @Validated EnableDisableSubUserArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        // 验证参数
        String[] userIds = arg.getSubUserIds();
        if (null == userIds || userIds.length < 1) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        if (ddosCacheSeviceHelper.subAccountActionCount(parentUserId, "enable", 300) > subAccountActionCount) {
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }

        // 请求account api
        OpenOrCloseSubUserReq request = new OpenOrCloseSubUserReq();
        request.setParentUserId(parentUserId);
        List<Long> subUserIds = Arrays.stream(userIds).map(x -> Long.valueOf(x)).collect(Collectors.toList());
        request.setUserIds(subUserIds);
        log.info("SubUserController.enableSubUser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<Integer> apiResponse = subUserApi.enableSubUser(getInstance(request));
        log.info("SubUserController.enableSubUser end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(request)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        return ret;
    }

    @ApiOperation(value = "母账户创建无邮箱子账户")
    @PostMapping(value = "/createNoEmailSubUser")
    @DDoSPreMonitor(action = "createNoEmailSubUserByParent")
    public CommonRet<Void> createNoEmailSubUserByParent(@RequestBody @Validated CreateNoEmailSubUserArg arg, HttpServletRequest request) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        // 判断注册通道是否已关闭
        boolean registerOpen = Boolean.parseBoolean(sysConfigHelper.getCodeByDisplayName("register_open"));
        if (!registerOpen) {
            throw new BusinessException(MgsErrorCode.REGISTER_CLOSE);
        }

        // 校验邮箱格式
        if (StringUtils.isBlank(arg.getUserName()) || !NOEMAILSUBUSER_REG_PATTERN.matcher(arg.getUserName()).matches()) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        String clientType = baseHelper.getClientType();
        CreateNoEmailSubUserReq req = new CreateNoEmailSubUserReq();
        req.setParentUserId(parentUserId);
        req.setUserName(arg.getUserName().trim());
        req.setTrackSource(accountHelper.getRegChannel(request.getParameter("ts")));
        req.setPassword(arg.getPassword());
        req.setConfirmPassword(arg.getConfirmPassword());
        req.setRemark(request.getParameter("remark"));
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, null, arg.getUserName().trim());
        req.setDeviceInfo(deviceInfo);
        req.setTerminal(TerminalEnum.findByCode(StringUtils.isNotEmpty(clientType) ? clientType : "OTHER"));
        // 自定义发送到邮箱中的链接
        String customEmailLink =
                String.format("%sgateway-api/v1/public/account/user/register-confirm?userId={userId}&verifyCode={code}",
                        getBaseUrl());
        req.setCustomEmailLink(customEmailLink);

        // 请求新account微服务，创建账号
        log.info("SubUserController.createSubUserByParent start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)));
        final APIResponse<CreateNoEmailSubUserResp> apiResponse = subUserApi.createNoEmailSubUser(getInstance(req));
        log.info("SubUserController.createSubUserByParent end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(req)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        return ret;
    }

    @ApiOperation(value = "母账户禁用子账户(单个/批量)")
    @PostMapping(value = "/disable")
    @DDoSPreMonitor(action = "disableSubUser")
    public CommonRet<Void> disableSubUser(@RequestBody @Validated EnableDisableSubUserArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        // 验证参数
        String[] userIds = arg.getSubUserIds();
        if (null == userIds || userIds.length < 1) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        if (ddosCacheSeviceHelper.subAccountActionCount(parentUserId, "disable", 300) > subAccountActionCount) {
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }

        // 请求account api
        OpenOrCloseSubUserReq request = new OpenOrCloseSubUserReq();
        request.setParentUserId(parentUserId);
        List<Long> subUserIds = Arrays.stream(userIds).map(x -> Long.valueOf(x)).collect(Collectors.toList());
        request.setUserIds(subUserIds);
        log.info("SubUserController.disableSubUser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<Integer> apiResponse = subUserApi.disableSubUser(getInstance(request));
        log.info("SubUserController.disableSubUser end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(request)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        return ret;
    }

    @ApiOperation(value = "母账户重置子账户2fa")
    @PostMapping(value = "/2fa/reset")
    @DDoSPreMonitor(action = "resetSubSecondValidation")
    public CommonRet<Void> resetSecondValidation(@RequestBody @Validated ResetSubUser2FaArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        // 参数校验
        String subUserId = arg.getSubUserId();
        // 子账户重置类型:"GOOGLE":谷歌验证;"SMS":手机验证
        String subType = arg.getSubType();
        //母账户必须开启2fa
        String parentCode = arg.getParentCode();
        //2fa认证类型
        String parentAuthType = arg.getParentAuthType();

        if (!StringUtils.isNumeric(subUserId)
                || StringUtils.isBlank(subType) || StringUtils.isBlank(parentCode)
                || StringUtils.isAllBlank(parentAuthType)) {
            log.error("resetSecondValidation参数为空");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        if (!"GOOGLE".equalsIgnoreCase(parentAuthType) && !"SMS".equalsIgnoreCase(parentAuthType)) {
            log.error("resetSecondValidation parentAuthType参数错误");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        if (!"GOOGLE".equalsIgnoreCase(subType) && !"SMS".equalsIgnoreCase(subType)) {
            log.error("resetSecondValidation subType参数错误");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        ResetSecondValidationRequest request = new ResetSecondValidationRequest();
        request.setParentUserId(parentUserId);
        request.setParentCode(parentCode);
        request.setParentAuthType(AuthTypeEnum.valueOf(parentAuthType.toUpperCase()));
        request.setSubUserId(Long.valueOf(subUserId));
        request.setSubType(AuthTypeEnum.valueOf(subType.toUpperCase()));
        log.info("SubUserController.resetSecondValidation start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<Integer> apiResponse = subUserApi.resetSecondValidation(getInstance(request));
        log.info("SubUserController.resetSecondValidation end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(request)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        authHelper.logoutAllWithoutDeleteCookies(Long.valueOf(subUserId));
        return ret;
    }

    @ApiOperation(value = "母账户修改子账户密码")
    @PostMapping(value = "/psw/update")
    @DDoSPreMonitor(action = "updateSubUserPsw")
    public CommonRet<Void> updateSubUserPsw(@RequestBody @Validated UpdateSubUserPswArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        //子账户userId
        String subUserId = arg.getSubUserId();
        String password = arg.getPassword();
        String confirmPassword = arg.getConfirmPassword();

        //母账户必须开启2fa
        String parentCode = arg.getParentCode();
        //2fa认证类型
        String parentAuthType = arg.getParentAuthType();

        //参数校验
        if (!StringUtils.isNumeric(subUserId)
                || StringUtils.isBlank(password) || StringUtils.isBlank(confirmPassword)
                || StringUtils.isBlank(parentCode) || StringUtils.isAllBlank(parentAuthType)) {
            log.error("updateSubUserPsw参数为空");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        UpdatePassWordRequest request = new UpdatePassWordRequest();
        request.setParentUserId(parentUserId);
        request.setParentCode(parentCode);
        request.setParentAuthType(AuthTypeEnum.valueOf(parentAuthType.toUpperCase()));
        request.setSubUserId(Long.valueOf(subUserId));
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        log.info("SubUserController.updateSubUserPsw start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<Integer> apiResponse = subUserApi.updateSubUserPwd(getInstance(request));
        log.info("SubUserController.updateSubUserPsw end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(request)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        authHelper.logoutAllWithoutDeleteCookies(Long.valueOf(subUserId));
        return ret;
    }

    @ApiOperation(value = "分页查询子账户列表")
    @PostMapping(value = "/info/list")
    public CommonPageRet<SubUserInfoRet> getSubUserInfoList(@RequestBody @Validated GetSubUserInfoArg arg, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 组装参数调用account接口
        QuerySubUserRequest querySubUserRequest = new QuerySubUserRequest();
        querySubUserRequest.setEmail(arg.getEmail());
        querySubUserRequest.setParentUserId(parentUserId);
        if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
            querySubUserRequest.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
        }
        int intPage = arg.getPage();
        int intLimit = arg.getRows();
        querySubUserRequest.setOffset((intPage - 1) * intLimit);
        querySubUserRequest.setLimit(intLimit);
        log.info("SubUserController.getSubUserInfoList start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(querySubUserRequest)));
        APIResponse<SubUserInfoResp> apiResponse = subUserClient.selectSubUserInfo(getInstance(querySubUserRequest));
        log.info("SubUserController.getSubUserInfoList end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(querySubUserRequest)), apiResponse);
        checkResponse(apiResponse);

        FlexLineQuerySubUserResp flexLineSubData = new FlexLineQuerySubUserResp();
        if(querySubUserToGetAllFlexLineParentId){
            APIResponse<List<Long>> allAvailableFlexLineParent = flexLineSubApi.getAllAvailableFlexLineParent();
            checkResponse(allAvailableFlexLineParent);
            List<Long> flexLineParentUserIds = allAvailableFlexLineParent.getData();
            if(!org.springframework.util.CollectionUtils.isEmpty(flexLineParentUserIds) && flexLineParentUserIds.contains(parentUserId)){
                FlexLineQuerySubUserReq flexLineQuerySubUserReq = new FlexLineQuerySubUserReq();
                flexLineQuerySubUserReq.setParentUserId(parentUserId);
                APIResponse<FlexLineQuerySubUserResp> queryFlexLineSub = flexLineSubApi.queryFlexLineSub(APIRequest.instance(flexLineQuerySubUserReq));
                checkResponse(queryFlexLineSub);
                flexLineSubData = queryFlexLineSub.getData();
            }
        }
        FlexLineQuerySubUserResp finalFlexLineSubData = flexLineSubData;
        //3 拼接返回值
        CommonPageRet<SubUserInfoRet> commonPageRet = new CommonPageRet<>();
        commonPageRet.setTotal(apiResponse.getData().getCount());
        List<SubUserInfoRet> subUserInfoRetList = Lists.newArrayList();
        for (SubUserInfoVo subUserInfoVo : apiResponse.getData().getResult()) {
            SubUserInfoRet subUserInfoRet = new SubUserInfoRet();
            BeanUtils.copyProperties(subUserInfoVo, subUserInfoRet);
            subUserInfoRet.setSubUserId(subUserInfoVo.getSubUserId().toString());
            if(finalFlexLineSubData.getCreditSubUserId()!=null){
                if(Objects.equals(subUserInfoVo.getSubUserId(), finalFlexLineSubData.getCreditSubUserId())){
                    subUserInfoRet.setIsFlexLineCreditUser(true);
                }
            }
            if (CollectionUtils.isNotEmpty(finalFlexLineSubData.getTradingSubUserIds())){
                if(finalFlexLineSubData.getTradingSubUserIds().contains(subUserInfoVo.getSubUserId())){
                    subUserInfoRet.setIsFlexLineTradingUser(true);
                }
            }
            subUserInfoRetList.add(subUserInfoRet);
        }
        commonPageRet.setData(subUserInfoRetList);
        return commonPageRet;
    }

    @ApiOperation(value = "获取母账户下的所有子账户UserID和EMAIL列表")
    @PostMapping(value = "/id-email/list")
    public CommonRet<List<SubUserIdEmailRet>> getSubUserIdAndEmailList(HttpServletResponse resp) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        QuerySubUserRequest request = new QuerySubUserRequest();
        request.setParentUserId(parentUserId);
        log.info("SubUserController.selectSubUserInfo start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<SubUserInfoResp> apiResponse = subUserClient.selectSubUserInfo(getInstance(request));
        checkResponse(apiResponse);
        if (apiResponse.getData() == null || CollectionUtils.isEmpty(apiResponse.getData().getResult())) {
            return new CommonRet<>(new ArrayList<>());
        }

        // 组装返回
        CommonRet<List<SubUserIdEmailRet>> commonRet = new CommonRet<List<SubUserIdEmailRet>>();
        List<SubUserIdEmailRet> retList = apiResponse.getData().getResult().stream().map(x -> {
            SubUserIdEmailRet ret = new SubUserIdEmailRet();
            ret.setUserId(x.getSubUserId().toString());
            ret.setEmail(x.getEmail());
            ret.setIsAssetSubUser(x.getIsAssetSubUser());
            ret.setIsAssetSubUserEnabled(x.getIsAssetSubUserEnabled());
            ret.setIsNoEmailSubUser(x.getIsNoEmailSubUser());
            ret.setIsFutureEnabled(x.getIsFutureEnabled());
            ret.setIsMarginEnabled(x.getIsMarginEnabled());
            ret.setIsManagerSubUser(x.getIsManagerSubUser());
            return ret;
        }).collect(Collectors.toList());
        commonRet.setData(retList);
        return commonRet;
    }

    @ApiOperation(value = "获取母账户下的子账户列表")
    @PostMapping(value = "/sub-user/list")
    public CommonRet<SubUserIdEmailListRet> getSubUserList(@RequestBody @Validated SubUserIdEmailListArg arg) throws Exception {
        // 母账号登陆状态校验
        Pair<Long, Boolean> pair = getRealParentUserId();
        final Long parentUserId = pair.getLeft();
        log.info("SubUserController subUserIdEmailListArg={}", JsonUtils.toJsonNotNullKey(arg));

        GetSubUserBindsRequest getSubUserBindsRequest = new GetSubUserBindsRequest();
        getSubUserBindsRequest.setParentUserId(parentUserId);
        Map<String, Object> queryParams = Maps.newHashMap();
        if (StringUtils.isNotBlank(arg.getEmail())) {
            queryParams.put("email", arg.getEmail());
            queryParams.put("isEmailLike", arg.getIsEmailLike());
        }
        if (arg.getAccountType() != null && (arg.getAccountType() == FunctionAccountType.FUTURE || arg.getAccountType() == FunctionAccountType.DELIVERY_FUTURE)) {
            queryParams.put("accountType", "FUTURE");
        } else if (arg.getAccountType() != null && (arg.getAccountType() == FunctionAccountType.MARGIN || arg.getAccountType() == FunctionAccountType.ISOLATED_MARGIN)) {
            queryParams.put("accountType", "MARGIN");
        }
        getSubUserBindsRequest.setPage(arg.getPage());
        getSubUserBindsRequest.setRows(querySubUserPageSize);
        getSubUserBindsRequest.setQueryParams(queryParams);
        APIResponse<List<SubUserBindingVo>> apiResponse = subUserApiV2.getSubUserBindingsByParentFromTiDB(APIRequest.instance(getSubUserBindsRequest));
        /*APIResponse<List<SubUserBindingVo>> apiResponse = subUserClient.getSubUserBindingsByParentFromMergeDB(APIRequest.instance(getSubUserBindsRequest));*/
        checkResponse(apiResponse);

        CountSubUserBindsRequest countSubUserBindsRequest = new CountSubUserBindsRequest();
        countSubUserBindsRequest.setParentUserId(parentUserId);
        countSubUserBindsRequest.setQueryParams(queryParams);
        APIResponse<Integer> countResp = subUserApiV2.countSubUserBindingsByParentFromTiDB(APIRequest.instance(countSubUserBindsRequest));
        /*APIResponse<Integer> countResp = subUserClient.countSubUserBindingsByParentFromMergeDB(APIRequest.instance(countSubUserBindsRequest));*/
        checkResponse(apiResponse);
        FlexLineQuerySubUserResp flexLineSubData = new FlexLineQuerySubUserResp();
        if(querySubUserToGetAllFlexLineParentId){
            APIResponse<List<Long>> allAvailableFlexLineParent = flexLineSubApi.getAllAvailableFlexLineParent();
            checkResponse(allAvailableFlexLineParent);
            List<Long> flexLineParentUserIds = allAvailableFlexLineParent.getData();
            if(CollectionUtils.isNotEmpty(flexLineParentUserIds) && flexLineParentUserIds.contains(parentUserId)){
                FlexLineQuerySubUserReq flexLineQuerySubUserReq = new FlexLineQuerySubUserReq();
                flexLineQuerySubUserReq.setParentUserId(parentUserId);
                APIResponse<FlexLineQuerySubUserResp> queryFlexLineSub = flexLineSubApi.queryFlexLineSub(APIRequest.instance(flexLineQuerySubUserReq));
                checkResponse(queryFlexLineSub);
                flexLineSubData = queryFlexLineSub.getData();
            }
        }
        FlexLineQuerySubUserResp finalFlexLineSubData = flexLineSubData;
        List<SubUserIdEmailRet> rets = apiResponse.getData().stream().map(x -> {
            SubUserIdEmailRet ret = new SubUserIdEmailRet();
            UserStatusEx statusEx = new UserStatusEx(x.getStatus());
            ret.setUserId(x.getSubUserId().toString());
            ret.setEmail(x.getEmail());
            ret.setIsAssetSubUser(statusEx.getIsAssetSubUser());
            ret.setIsAssetSubUserEnabled(statusEx.getIsAssetSubUserEnabled());
            ret.setIsNoEmailSubUser(statusEx.getIsNoEmailSubUser());
            ret.setIsFutureEnabled(statusEx.getIsExistFutureAccount());
            ret.setIsMarginEnabled(statusEx.getIsExistMarginAccount());
            ret.setIsManagerSubUser(statusEx.getIsManagerSubUser());
            if(finalFlexLineSubData.getCreditSubUserId()!=null){
                if(Objects.equals(x.getSubUserId(), finalFlexLineSubData.getCreditSubUserId())){
                    ret.setIsFlexLineCreditUser(true);
                }
            }
            if (CollectionUtils.isNotEmpty(finalFlexLineSubData.getTradingSubUserIds())){
                if(finalFlexLineSubData.getTradingSubUserIds().contains(x.getSubUserId())){
                    ret.setIsFlexLineTradingUser(true);
                }
            }
            return ret;
        }).collect(Collectors.toList());

        //当前登录账号如果isCommonMerchantSubUser = true, 则将其对应的母账户信息也返回
        if(pair.getRight()){
            GetUserResponse getUserResponse = this.getUserById(parentUserId);
            SubUserIdEmailRet ret = new SubUserIdEmailRet();
            UserStatusEx statusEx = new UserStatusEx(getUserResponse.getUser().getStatus(),getUserResponse.getUser().getStatusExtra());
            ret.setUserId(getUserResponse.getUser().getUserId().toString());
            ret.setEmail(getUserResponse.getUser().getEmail());
            ret.setIsAssetSubUser(statusEx.getIsAssetSubUser());
            ret.setIsAssetSubUserEnabled(statusEx.getIsAssetSubUserEnabled());
            ret.setIsNoEmailSubUser(statusEx.getIsNoEmailSubUser());
            ret.setIsFutureEnabled(statusEx.getIsExistFutureAccount());
            ret.setIsMarginEnabled(statusEx.getIsExistMarginAccount());
            ret.setIsManagerSubUser(statusEx.getIsManagerSubUser());
            rets.add(ret);
        }

        SubUserIdEmailListRet listRet = new SubUserIdEmailListRet();
        listRet.setRetList(rets);
        listRet.setPageSize(querySubUserPageSize);
        listRet.setTotal(Long.valueOf(countResp.getData()));
        // 组装返回
       CommonRet<SubUserIdEmailListRet> commonRet = new CommonRet<>();
       commonRet.setData(listRet);
       return commonRet;
    }

    @ApiOperation(value = "获取母账户下所有子账户登录历史<分页条件查询,查询条件时间必传>")
    @PostMapping(value = "/login/history")
    public CommonPageRet<SubUserLoginHistoryRet> getSubUserLoginHistory(@RequestBody SubUserLoginHistoryArg arg, HttpServletResponse resp) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        Long startTime = arg.getStartTime();
        Long endTime = arg.getEndTime();
        Integer page = arg.getPage();
        Integer limit = arg.getRows();
        //参数校验
        if (page == null || page <= 0) {
            page = initPage;
        }
        if (limit == null || limit <= 0) {
            limit = pageLimit;
        }

        SubUserSecurityLogReq request = new SubUserSecurityLogReq();
        request.setParentUserId(parentUserId);
        if (StringUtils.isNotBlank(arg.getSubUserId())) {
            request.setSubUserId(Long.valueOf(arg.getSubUserId()));
        }
        request.setStartOperateTime(checkAndGetStartTime(startTime, endTime));
        request.setEndOperateTime(checkAndGetEndTime(endTime));
        request.setOperateType(OperateType.login.name());
        request.setLimit(arg.getRows());
        request.setOffset((page - 1) * limit);

        log.info("SubUserController.loginHistoryList start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<GetUserSecurityLogResponse> apiResponse = subUserApi.loginHistoryList(getInstance(request));
        checkResponse(apiResponse);

        // 组装返回
        CommonPageRet<SubUserLoginHistoryRet> commonPageRet = new CommonPageRet<SubUserLoginHistoryRet>();
        commonPageRet.setTotal(apiResponse.getData().getCount());

        List<UserSecurityLogVo> loginHistoryList = apiResponse.getData().getResult();
        if (CollectionUtils.isEmpty(loginHistoryList)) {
            commonPageRet.setData(new ArrayList<>());
        } else {
            List<SubUserLoginHistoryRet> retList = apiResponse.getData().getResult().stream().map(x -> {
                SubUserLoginHistoryRet ret = new SubUserLoginHistoryRet();
                BeanUtils.copyProperties(x, ret);
                ret.setUserId(x.getUserId().toString());
                ret.setId(x.getId().toString());
                ret.setOperateTime(String.valueOf(x.getOperateTime().getTime()));
                return ret;
            }).collect(Collectors.toList());
            commonPageRet.setData(retList);
        }
        return commonPageRet;
    }

    @ApiOperation(value = "母账户修改子账户信息")
    @PostMapping(value = "/modifySubAccount")
    @DDoSPreMonitor(action = "modifySubAccount")
    public CommonRet<Void> modifySubAccount(@RequestBody @Validated SubUserModifyReq req, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 校验邮箱格式
        if (StringUtils.isBlank(req.getModifyEmail()) || !timeOutRegexUtils.validateEmailForChangeEmail(req.getModifyEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        //3 简单校验完毕后直接往account发送
        ModifySubAccountRequest request = new ModifySubAccountRequest();
        request.setParentUserId(parentUserId);
        request.setSubAccountUserId(req.getSubAccountUserId());
        request.setModifyEmail(req.getModifyEmail());
        request.setAuthType(AuthTypeEnum.valueOf(req.getAuthType()));
        request.setCode(req.getCode());
        request.setEmailVerifyCode(req.getEmailVerifyCode());
        log.info("SubUserController.modifySubAccount start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<Integer> apiResponse = subUserApi.modifySubAccount(getInstance(request));
        log.info("SubUserController.modifySubAccount end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
        checkResponse(apiResponse);
        authHelper.logoutAllWithoutDeleteCookies(req.getSubAccountUserId());
        return new CommonRet<>();
    }


    @ApiOperation(value = "母账户帮子账户开通Margin")
    @PostMapping(value = "/margin/enable")
    @DDoSPreMonitor(action = "subAccountMarginEnable")
    public CommonRet<Void> subAccountMarginEnable(@RequestBody @Validated EnableSubAccountMarginArg req, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account或account-subuser发送
        if (createSubUserMarginSwitch) {
            // 新的方式，account-subuser直接访问margin-api
            com.binance.accountsubuser.vo.margin.request.SubAccountMarginEnableRequest request = new com.binance.accountsubuser.vo.margin.request.SubAccountMarginEnableRequest();
            request.setParentUserId(parentUserId);
            request.setEmail(req.getEmail());
            log.info("SubUserController.subAccountMarginEnable to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.margin.response.SubAccountMarginEnableResp> apiResponse = subUserMarginApi.subAccountMarginEnable(getInstance(request));
            log.info("SubUserController.subAccountMarginEnable to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
        } else {
            SubAccountMarginEnableRequest request = new SubAccountMarginEnableRequest();
            request.setParentUserId(parentUserId);
            request.setEmail(req.getEmail());
            log.info("SubUserController.subAccountMarginEnable start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<SubAccountMarginEnableResp> apiResponse = subUserApi.subAccountMarginEnable(getInstance(request));
            log.info("SubUserController.subAccountMarginEnable end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
        }
        return new CommonRet<>();
    }


    @ApiOperation(value = "母账户帮子账户开通Futures")
    @PostMapping(value = "/futures/enable")
    @DDoSPreMonitor(action = "subAccountFuturesEnable")
    public CommonRet<Void> subAccountFuturesEnable(@RequestBody @Validated EnableSubAccountFuturesArg req, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account发送
        SubAccountFuturesEnableRequest request = new SubAccountFuturesEnableRequest();
        request.setParentUserId(parentUserId);
        request.setEmail(req.getEmail());
        if(subUserFuturePassTutorialSwitch){
            request.setPassTutorial(true);
        }
        log.info("SubUserController.subAccountFuturesEnable start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<SubAccountFuturesEnableResp> apiResponse = subUserApi.subAccountFuturesEnable(getInstance(request));
        log.info("SubUserController.subAccountFuturesEnable end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
        checkResponse(apiResponse);
        return new CommonRet<>();
    }

    @ApiOperation(value = "查询母账户和子账户的Futuress账户汇总")
    @PostMapping(value = "/query/futures/parentAndSubaccountSummary")
    public CommonRet<ParentAndSubaccountSummaryRet> futureParentAndSubaccountSummary(HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account发送,查询主账户
        if (queryFutureMarginSwitch) {
            com.binance.accountsubuser.vo.future.request.FutureTotalAccountSummaryRequest request = new com.binance.accountsubuser.vo.future.request.FutureTotalAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            request.setSubAccountSummaryQueryType(ONLY_PARENT_ACCOUNT);
            log.info("SubUserController.parentAndSubaccountSummary to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.future.response.FutureTotalAccountSummaryResp> parentApiResponse = subUserFutureApi.futuresTotalAccountSummaryResp(getInstance(request));
            log.info("SubUserController.parentAndSubaccountSummary to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), parentApiResponse);
            checkResponse(parentApiResponse);
            //3 简单校验完毕后直接往account发送,查询所有子账户账户
            request.setSubAccountSummaryQueryType(ONLY_SUB_ACCOUNT);
            log.info("SubUserController.parentAndSubaccountSummary start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.future.response.FutureTotalAccountSummaryResp> subAccountApiResponse = subUserFutureApi.futuresTotalAccountSummaryResp(getInstance(request));
            log.info("SubUserController.parentAndSubaccountSummary end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), subAccountApiResponse);
            checkResponse(subAccountApiResponse);
            CommonRet<ParentAndSubaccountSummaryRet> ret = new CommonRet<>();
            ParentAndSubaccountSummaryRet parentAndSubaccountSummaryRet = new ParentAndSubaccountSummaryRet();
            parentAndSubaccountSummaryRet.setParentTotalMarginBalance(parentApiResponse.getData().getTotalMarginBalance());
            parentAndSubaccountSummaryRet.setAllSubAccountTotalMarginBalance(subAccountApiResponse.getData().getTotalMarginBalance());
            ret.setData(parentAndSubaccountSummaryRet);
            return ret;
        } else {
            QuerySubAccountFutureAccountSummaryRequest request = new QuerySubAccountFutureAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            request.setSubAccountSummaryQueryType(SubAccountSummaryQueryType.ONLY_PARENT_ACCOUNT);
            log.info("SubUserController.parentAndSubaccountSummary start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<QuerySubAccountFutureAccountSummaryResp> parentApiResponse = subUserApi.queryFuturesAccountSummary(getInstance(request));
            log.info("SubUserController.parentAndSubaccountSummary end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), parentApiResponse);
            checkResponse(parentApiResponse);
            //3 简单校验完毕后直接往account发送,查询所有子账户账户
            request.setSubAccountSummaryQueryType(SubAccountSummaryQueryType.ONLY_SUB_ACCOUNT);
            log.info("SubUserController.parentAndSubaccountSummary start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<QuerySubAccountFutureAccountSummaryResp> subAccountApiResponse = subUserApi.queryFuturesAccountSummary(getInstance(request));
            log.info("SubUserController.parentAndSubaccountSummary end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), subAccountApiResponse);
            checkResponse(subAccountApiResponse);
            CommonRet<ParentAndSubaccountSummaryRet> ret = new CommonRet<>();
            ParentAndSubaccountSummaryRet parentAndSubaccountSummaryRet = new ParentAndSubaccountSummaryRet();
            parentAndSubaccountSummaryRet.setParentTotalMarginBalance(parentApiResponse.getData().getParentAccount().getTotalMarginBalance());
            parentAndSubaccountSummaryRet.setAllSubAccountTotalMarginBalance(subAccountApiResponse.getData().getTotalMarginBalance());
            ret.setData(parentAndSubaccountSummaryRet);
            return ret;
        }
    }


    @ApiOperation(value = "分页查询子账户的Futuress账户汇总")
    @PostMapping(value = "/query/futures/accountSummary")
    public CommonPageRet<QueryFuturesAccountAssetRiskSummaryVo> queryFuturesAccountSummary(@RequestBody @Validated QueryFuturesAccountSummaryArg arg, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account发送,查询主账户
        if (queryFutureMarginSwitch) {
            com.binance.accountsubuser.vo.future.request.QuerySubAccountFutureAccountSummaryRequest request = new com.binance.accountsubuser.vo.future.request.QuerySubAccountFutureAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            request.setSubAccountSummaryQueryType(ONLY_SUB_ACCOUNT);
            if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
                request.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
            }
            if (StringUtils.isNotBlank(arg.getEmail())) {
                request.setEmail(arg.getEmail());
            }
            request.setPage(arg.getPage());
            request.setRows(arg.getRows());
            log.info("SubUserController.queryFuturesAccountSummary to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.future.response.QuerySubAccountFutureAccountSummaryResp> apiResponse = subUserFutureApi.queryFuturesAccountSummary(getInstance(request));
            log.info("SubUserController.queryFuturesAccountSummary to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //3 组装参数
            List<QueryFuturesAccountAssetRiskSummaryVo> subAccountList = Lists.newArrayList();
            for (com.binance.accountsubuser.vo.future.FutureAccountSummaryInfoVo futureAccountSummaryInfoVo : apiResponse.getData().getSubAccountList()) {
                QueryFuturesAccountAssetRiskSummaryVo vo = new QueryFuturesAccountAssetRiskSummaryVo();
                BeanUtils.copyProperties(futureAccountSummaryInfoVo, vo);
                vo.setIsSubUserEnabled(futureAccountSummaryInfoVo.getUserStatusEx().getIsSubUserEnabled());
                subAccountList.add(vo);
            }
            CommonPageRet<QueryFuturesAccountAssetRiskSummaryVo> result = new CommonPageRet<QueryFuturesAccountAssetRiskSummaryVo>();
            result.setData(subAccountList);
            result.setTotal(apiResponse.getData().getTotalSubAccountSize());
            return result;
        } else {
            QuerySubAccountFutureAccountSummaryRequest request = new QuerySubAccountFutureAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            request.setSubAccountSummaryQueryType(SubAccountSummaryQueryType.ONLY_SUB_ACCOUNT);
            if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
                request.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
            }
            if (StringUtils.isNotBlank(arg.getEmail())) {
                request.setEmail(arg.getEmail());
            }
            request.setPage(arg.getPage());
            request.setRows(arg.getRows());
            log.info("SubUserController.queryFuturesAccountSummary start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<QuerySubAccountFutureAccountSummaryResp> apiResponse = subUserApi.queryFuturesAccountSummary(getInstance(request));
            log.info("SubUserController.queryFuturesAccountSummary end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //3 组装参数
            List<QueryFuturesAccountAssetRiskSummaryVo> subAccountList = Lists.newArrayList();
            for (FutureAccountSummaryInfoVo futureAccountSummaryInfoVo : apiResponse.getData().getSubAccountList()) {
                QueryFuturesAccountAssetRiskSummaryVo vo = new QueryFuturesAccountAssetRiskSummaryVo();
                BeanUtils.copyProperties(futureAccountSummaryInfoVo, vo);
                vo.setIsSubUserEnabled(futureAccountSummaryInfoVo.getUserStatusEx().getIsSubUserEnabled());
                subAccountList.add(vo);
            }
            CommonPageRet<QueryFuturesAccountAssetRiskSummaryVo> result = new CommonPageRet<QueryFuturesAccountAssetRiskSummaryVo>();
            result.setData(subAccountList);
            result.setTotal(apiResponse.getData().getTotalSubAccountSize());
            return result;
        }
    }


    @ApiOperation(value = "查询子账户的Futures持仓信息")
    @PostMapping(value = "/query/futures/positionRisk")
    public CommonRet<List<FuturePositionRiskVO>> queryFuturesPositionRisk(@RequestBody @Validated QueryFuturesPositionRiskArg req, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account或account-subuser发送
        if (queryFutureMarginSwitch) {
            com.binance.accountsubuser.vo.future.request.QueryFuturesPositionRiskRequest request = new com.binance.accountsubuser.vo.future.request.QueryFuturesPositionRiskRequest();
            request.setParentUserId(parentUserId);
            request.setEmail(req.getEmail());
            log.info("SubUserController.queryFuturesPositionRisk to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<List<com.binance.accountsubuser.vo.future.FuturePositionRiskVO>> apiResponse = subUserFutureApi.queryFuturesPositionRisk(getInstance(request));
            log.info("SubUserController.queryFuturesPositionRisk to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //组装返回值
            CommonRet<List<FuturePositionRiskVO>> ret = new CommonRet<>();
            List<FuturePositionRiskVO> list = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(apiResponse.getData())) {
                list = apiResponse.getData().stream().map(x -> {
                    FuturePositionRiskVO riskVO = new FuturePositionRiskVO();
                    BeanUtils.copyProperties(x, riskVO);
                    return riskVO;
                }).collect(Collectors.toList());
            }
            ret.setData(list);
            return ret;
        } else {
            QueryFuturesPositionRiskRequest request = new QueryFuturesPositionRiskRequest();
            request.setParentUserId(parentUserId);
            request.setEmail(req.getEmail());
            log.info("SubUserController.queryFuturesPositionRisk start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<List<FuturePositionRiskVO>> apiResponse = subUserApi.queryFuturesPositionRisk(getInstance(request));
            log.info("SubUserController.queryFuturesPositionRisk end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //组装返回值
            CommonRet<List<FuturePositionRiskVO>> ret = new CommonRet<>();
            ret.setData(apiResponse.getData());
            return ret;
        }
    }

    @ApiOperation(value = "币本位母账户和子账户资产汇总")
    @PostMapping(value = "/query/delivery/parentAndSubAccountSummary")
    public CommonRet<ParentAndSubaccountSummaryRet> deliveryParentAndSubAccountSummary() throws Exception {
        Long parentUserId = checkAndGetUserId();

        DeliveryTotalAccountSummaryRequest request = new DeliveryTotalAccountSummaryRequest();
        request.setParentUserId(parentUserId);
        request.setSubAccountSummaryQueryType(ONLY_PARENT_ACCOUNT);
        log.info("Query Delivery.parentAccountSummary request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<DeliveryTotalAccountSummaryResp> parentApiResponse = subUserDeliveryApi.deliveryTotalAccountSummaryResp(getInstance(request));
        checkResponse(parentApiResponse);

        request.setSubAccountSummaryQueryType(ONLY_SUB_ACCOUNT);
        log.info("Query Delivery.subAccountSummary request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<DeliveryTotalAccountSummaryResp> subApiResponse = subUserDeliveryApi.deliveryTotalAccountSummaryResp(getInstance(request));
        checkResponse(subApiResponse);

        ParentAndSubaccountSummaryRet summaryRet = new ParentAndSubaccountSummaryRet();
        // 汇率转换
        BigDecimal exchangeRate = this.priceConvert("USDT","BTC");
        summaryRet.setParentTotalMarginBalance(new BigDecimal(parentApiResponse.getData().getTotalMarginBalance()).multiply(exchangeRate).setScale(8, RoundingMode.CEILING).toPlainString());
        summaryRet.setAllSubAccountTotalMarginBalance(new BigDecimal(subApiResponse.getData().getTotalMarginBalance()).multiply(exchangeRate).setScale(8, RoundingMode.CEILING).toPlainString());
        return new CommonRet<>(summaryRet);
    }

    @ApiOperation(value = "子账户币本位合约资产列表")
    @PostMapping(value = "/query/delivery/accountSummary")
    public CommonPageRet<QueryDeliveryAccountAssetRiskSummaryVo> deliverySubAccountSummary(@RequestBody @Validated QueryFuturesAccountSummaryArg arg)
            throws Exception {
        Long parentUserId = checkAndGetUserId();

        QuerySubAccountDeliverySummaryRequest request = new QuerySubAccountDeliverySummaryRequest();
        if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
            request.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
        }
        if (StringUtils.isNotBlank(arg.getEmail())) {
            request.setEmail(arg.getEmail());
        }
        request.setParentUserId(parentUserId);
        request.setPage(arg.getPage());
        request.setRows(arg.getRows());
        request.setSubAccountSummaryQueryType(ONLY_SUB_ACCOUNT);
        log.info("Query deliveryAccountSummary request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<QuerySubAccountDeliveryAccountSummaryResp> apiResponse = subUserDeliveryApi.queryDeliveryAccountSummary(getInstance(request));
        checkResponse(apiResponse);
        log.info("Query deliveryAccountSummary response, subAccount size={} ", apiResponse.getData().getSubAccountList().size());
        List<QueryDeliveryAccountAssetRiskSummaryVo> subAccountList = Lists.newArrayList();

        // 汇率转换
        BigDecimal exchangeRate = this.priceConvert("USDT","BTC");
        apiResponse.getData().getSubAccountList().forEach(vo -> {
            QueryDeliveryAccountAssetRiskSummaryVo summaryVo = new QueryDeliveryAccountAssetRiskSummaryVo();
            summaryVo.setEmail(vo.getEmail());
            summaryVo.setIsSubUserEnabled(vo.getUserStatusEx().getIsSubUserEnabled());
            summaryVo.setMarginBalance(new BigDecimal(vo.getMarginBalance()).multiply(exchangeRate).setScale(8, RoundingMode.DOWN).toPlainString());
            summaryVo.setUnrealizedProfit(new BigDecimal(vo.getUnrealizedProfit()).multiply(exchangeRate).setScale(8, RoundingMode.DOWN).toPlainString());
            summaryVo.setWalletBalance(new BigDecimal(vo.getWalletBalance()).multiply(exchangeRate).setScale(8, RoundingMode.DOWN).toPlainString());
            summaryVo.setMaintenanceMargin(new BigDecimal(vo.getMaintenanceMargin()).multiply(exchangeRate).setScale(8,RoundingMode.CEILING).toPlainString());
            subAccountList.add(summaryVo);
        });
        return new CommonPageRet<>(subAccountList, apiResponse.getData().getTotalSubAccountSize());
    }


    @ApiOperation(value = "查询子账户的币本位合约持仓信息")
    @PostMapping(value = "/query/delivery/positionRisk")
    public CommonRet<List<DeliveryPositionRiskVO>> deliveryPositionRisk(@RequestBody @Validated QueryDeliveryPositionRiskArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        QueryDeliveryPositionRiskRequest request = new QueryDeliveryPositionRiskRequest();
        request.setParentUserId(parentUserId);
        request.setEmail(arg.getEmail());
        request.setPair(arg.getPair());
        log.info("Query deliveryPositionRisk request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<List<DeliveryPositionRiskVO>> apiResponse = subUserDeliveryApi.queryDeliveryPositionRisk(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }


    @ApiOperation(value = "查询母账户和子账户的Margin账户汇总")
    @PostMapping(value = "/query/margin/parentAndSubaccountSummary")
    public CommonRet<MarginParentAndSubaccountSummaryRet> marginParentAndSubaccountSummary(HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account或account-subuser发送
        if (queryFutureMarginSwitch) {
            com.binance.accountsubuser.vo.margin.request.QuerySubAccountMarginAccountSummaryRequest request = new com.binance.accountsubuser.vo.margin.request.QuerySubAccountMarginAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            log.info("SubUserController.marginParentAndSubaccountSummary to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.margin.response.QuerySubAccountMarginAccountSummaryResp> apiResponse = subUserMarginApi.queryMarginAccountSummary(getInstance(request));
            log.info("SubUserController.marginParentAndSubaccountSummary to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            CommonRet<MarginParentAndSubaccountSummaryRet> ret = new CommonRet<>();
            MarginParentAndSubaccountSummaryRet parentAndSubaccountSummaryRet = new MarginParentAndSubaccountSummaryRet();
            parentAndSubaccountSummaryRet.setMasterAccountNetAssetOfBtc(apiResponse.getData().getMasterAccountNetAssetOfBtc());
            parentAndSubaccountSummaryRet.setTotalAssetOfBtc(apiResponse.getData().getTotalAssetOfBtc());
            parentAndSubaccountSummaryRet.setTotalLiabilityOfBtc(apiResponse.getData().getTotalLiabilityOfBtc());
            parentAndSubaccountSummaryRet.setTotalNetAssetOfBtc(apiResponse.getData().getTotalNetAssetOfBtc());
            ret.setData(parentAndSubaccountSummaryRet);
            return ret;
        } else {
            QuerySubAccountMarginAccountSummaryRequest request = new QuerySubAccountMarginAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            log.info("SubUserController.marginParentAndSubaccountSummary start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<QuerySubAccountMarginAccountSummaryResp> apiResponse = subUserApi.queryMarginAccountSummary(getInstance(request));
            log.info("SubUserController.marginParentAndSubaccountSummary end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            CommonRet<MarginParentAndSubaccountSummaryRet> ret = new CommonRet<>();
            MarginParentAndSubaccountSummaryRet parentAndSubaccountSummaryRet = new MarginParentAndSubaccountSummaryRet();
            parentAndSubaccountSummaryRet.setMasterAccountNetAssetOfBtc(apiResponse.getData().getMasterAccountNetAssetOfBtc());
            parentAndSubaccountSummaryRet.setTotalAssetOfBtc(apiResponse.getData().getTotalAssetOfBtc());
            parentAndSubaccountSummaryRet.setTotalLiabilityOfBtc(apiResponse.getData().getTotalLiabilityOfBtc());
            parentAndSubaccountSummaryRet.setTotalNetAssetOfBtc(apiResponse.getData().getTotalNetAssetOfBtc());
            ret.setData(parentAndSubaccountSummaryRet);
            return ret;
        }
    }


    @ApiOperation(value = "分页查询子账户的Margin账户汇总")
    @PostMapping(value = "/query/margin/accountSummary")
    public CommonPageRet<QueryMarginAccountAssetRiskSummaryVo> queryMarginAccountSummary(@RequestBody @Validated QueryMarginAccountSummaryArg arg, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account或account-subuser发送,查询主账户
        if (queryFutureMarginSwitch) {
            com.binance.accountsubuser.vo.margin.request.QuerySubAccountMarginAccountSummaryRequest request = new com.binance.accountsubuser.vo.margin.request.QuerySubAccountMarginAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
                request.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
            }
            if (StringUtils.isNotBlank(arg.getEmail())) {
                request.setEmail(arg.getEmail());
            }
            request.setPage(arg.getPage());
            request.setRows(arg.getRows());
            log.info("SubUserController.queryMarginAccountSummary to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.margin.response.QuerySubAccountMarginAccountSummaryResp> apiResponse = subUserMarginApi.queryMarginAccountSummary(getInstance(request));
            log.info("SubUserController.queryMarginAccountSummary to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            FlexLineQuerySubUserResp flexLineSubData = new FlexLineQuerySubUserResp();
            Map<String,Long> emailAndUserIdMap = new HashMap<>();
            Map<Long,String> userIdAndSubTypeMap = new HashMap<>();
            if(querySubUserToGetAllFlexLineParentId){
                APIResponse<List<Long>> allAvailableFlexLineParent = flexLineSubApi.getAllAvailableFlexLineParent();
                checkResponse(allAvailableFlexLineParent);
                List<Long> flexLineParentUserIds = allAvailableFlexLineParent.getData();
                if(!org.springframework.util.CollectionUtils.isEmpty(flexLineParentUserIds) && flexLineParentUserIds.contains(parentUserId)){
                    FlexLineQuerySubUserReq flexLineQuerySubUserReq = new FlexLineQuerySubUserReq();
                    flexLineQuerySubUserReq.setParentUserId(parentUserId);
                    APIResponse<FlexLineQuerySubUserResp> queryFlexLineSub = flexLineSubApi.queryFlexLineSub(APIRequest.instance(flexLineQuerySubUserReq));
                    checkResponse(queryFlexLineSub);
                    flexLineSubData = queryFlexLineSub.getData();
                }
                if(flexLineSubData.getCreditSubUserId()!=null){
                    GetUserResponse creditSubUserResp = getUserById(flexLineSubData.getCreditSubUserId());
                    emailAndUserIdMap.put(creditSubUserResp.getUser().getEmail(),creditSubUserResp.getUser().getUserId());
                    userIdAndSubTypeMap.put(creditSubUserResp.getUser().getUserId(),"creditSub");
                }
                if (CollectionUtils.isNotEmpty(flexLineSubData.getTradingSubUserIds())){
                    for (Long tradingSubUserId : flexLineSubData.getTradingSubUserIds()) {
                        GetUserResponse tradingSubUserResp = getUserById(tradingSubUserId);
                        emailAndUserIdMap.put(tradingSubUserResp.getUser().getEmail(),tradingSubUserResp.getUser().getUserId());
                        userIdAndSubTypeMap.put(tradingSubUserResp.getUser().getUserId(),"tradingSub");
                    }
                }
            }

            //3 组装参数
            List<QueryMarginAccountAssetRiskSummaryVo> subAccountList = Lists.newArrayList();
            for (com.binance.accountsubuser.vo.margin.MarginAccountSummaryInfoVo marginAccountSummaryInfoVo : apiResponse.getData().getSubAccountList()) {
                QueryMarginAccountAssetRiskSummaryVo vo = new QueryMarginAccountAssetRiskSummaryVo();
                BeanUtils.copyProperties(marginAccountSummaryInfoVo, vo);
                vo.setIsSubUserEnabled(marginAccountSummaryInfoVo.getUserStatusEx().getIsSubUserEnabled());
                Long flexLineSub = emailAndUserIdMap.get(marginAccountSummaryInfoVo.getEmail());
                if(flexLineSub != null){
                    String subType = userIdAndSubTypeMap.get(flexLineSub);
                    if(StringUtils.equals("creditSub",subType)){
                        vo.setIsFlexLineCreditUser(true);
                    }
                    if (StringUtils.equals("tradingSub",subType)){
                        vo.setIsFlexLineTradingUser(true);
                    }
                }
                subAccountList.add(vo);
            }
            CommonPageRet<QueryMarginAccountAssetRiskSummaryVo> result = new CommonPageRet<QueryMarginAccountAssetRiskSummaryVo>();
            result.setData(subAccountList);
            result.setTotal(apiResponse.getData().getTotalSubAccountSize());
            return result;
        } else {
            QuerySubAccountMarginAccountSummaryRequest request = new QuerySubAccountMarginAccountSummaryRequest();
            request.setParentUserId(parentUserId);
            if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
                request.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
            }
            if (StringUtils.isNotBlank(arg.getEmail())) {
                request.setEmail(arg.getEmail());
            }
            request.setPage(arg.getPage());
            request.setRows(arg.getRows());
            log.info("SubUserController.queryMarginAccountSummary start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<QuerySubAccountMarginAccountSummaryResp> apiResponse = subUserApi.queryMarginAccountSummary(getInstance(request));
            log.info("SubUserController.queryMarginAccountSummary end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //3 组装参数
            List<QueryMarginAccountAssetRiskSummaryVo> subAccountList = Lists.newArrayList();
            for (MarginAccountSummaryInfoVo marginAccountSummaryInfoVo : apiResponse.getData().getSubAccountList()) {
                QueryMarginAccountAssetRiskSummaryVo vo = new QueryMarginAccountAssetRiskSummaryVo();
                BeanUtils.copyProperties(marginAccountSummaryInfoVo, vo);
                vo.setIsSubUserEnabled(marginAccountSummaryInfoVo.getUserStatusEx().getIsSubUserEnabled());
                subAccountList.add(vo);
            }
            CommonPageRet<QueryMarginAccountAssetRiskSummaryVo> result = new CommonPageRet<QueryMarginAccountAssetRiskSummaryVo>();
            result.setData(subAccountList);
            result.setTotal(apiResponse.getData().getTotalSubAccountSize());
            return result;
        }
    }


    @ApiOperation(value = "查询子账户的Margin账户详情")
    @PostMapping(value = "/query/margin/account")
    public CommonRet<QuerySubAccountMarginAccountResp> queryMarginAccount(@RequestBody @Validated QueryMarginAccountArg req, HttpServletRequest httpServletRequest, HttpServletResponse resp) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 简单校验完毕后直接往account或account-subuser发送
        if (queryFutureMarginSwitch) {
            com.binance.accountsubuser.vo.margin.request.QuerySubAccountMarginAccountRequest request = new com.binance.accountsubuser.vo.margin.request.QuerySubAccountMarginAccountRequest();
            request.setParentUserId(parentUserId);
            request.setEmail(req.getEmail());
            if (null == req.getMarginPeriodType()) {
                request.setMarginPeriodType(com.binance.accountsubuser.vo.enums.MarginPeriodType.TODAY);
            } else {
                request.setMarginPeriodType(com.binance.accountsubuser.vo.enums.MarginPeriodType.valueOf(req.getMarginPeriodType().name()));
            }
            log.info("SubUserController.queryMarginAccount to account-subuser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<com.binance.accountsubuser.vo.margin.response.QuerySubAccountMarginAccountResp> apiResponse = subUserMarginApi.queryMarginAccount(getInstance(request));
            log.info("SubUserController.queryMarginAccount to account-subuser end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //组装返回值
            CommonRet<QuerySubAccountMarginAccountResp> ret = new CommonRet<>();
            QuerySubAccountMarginAccountResp querySubAccountMarginAccountResp = new QuerySubAccountMarginAccountResp();
            BeanUtils.copyProperties(apiResponse.getData(), querySubAccountMarginAccountResp);
            ret.setData(querySubAccountMarginAccountResp);
            return ret;
        } else {
            QuerySubAccountMarginAccountRequest request = new QuerySubAccountMarginAccountRequest();
            request.setParentUserId(parentUserId);
            request.setEmail(req.getEmail());
            if (null == req.getMarginPeriodType()) {
                request.setMarginPeriodType(MarginPeriodType.TODAY);
            } else {
                request.setMarginPeriodType(req.getMarginPeriodType());
            }
            log.info("SubUserController.queryMarginAccount start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
            APIResponse<QuerySubAccountMarginAccountResp> apiResponse = subUserApi.queryMarginAccount(getInstance(request));
            log.info("SubUserController.queryMarginAccount end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)), apiResponse);
            checkResponse(apiResponse);
            //组装返回值
            CommonRet<QuerySubAccountMarginAccountResp> ret = new CommonRet<>();
            ret.setData(apiResponse.getData());
            return ret;
        }
    }


    @ApiOperation(value = "母账户修改子账户备注")
    @PostMapping(value = "/updateSubUserRemark")
    @DDoSPreMonitor(action = "updateSubUserRemark")
    public CommonRet<Void> updateSubUserRemark(@RequestBody @Validated UpdateSubUserRemarkArg arg, HttpServletResponse resp) throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        //子账户email
        String subUserEmail = arg.getSubUserEmail();
        String remark = arg.getRemark();
        //参数校验
        if (StringUtils.isBlank(subUserEmail)) {
            log.error("updateSubUserRemark参数不合法");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        UpdateSubUserRemarkRequest request = new UpdateSubUserRemarkRequest();
        request.setParentUserId(parentUserId);
        request.setSubUserEmail(subUserEmail);
        request.setRemark(remark);
        log.info("SubUserController.updateSubUserRemark start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(request)));
        APIResponse<Integer> apiResponse = subUserApi.updateSubUserRemark(getInstance(request));
        log.info("SubUserController.updateSubUserRemark end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(request)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        return ret;
    }

    /**
     * future-main划转
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/subMainFutureAccountTransfer")
    @DDoSPreMonitor(action = "subMainFutureAccountTransfer")
    public CommonRet<String> subMainFutureAccountTransfer(@Valid @RequestBody SubAccountFutureTransferArg subAccountFutureTransferArg) throws Exception {
        Long parentUserId = getUserId();
        QuerySubAccountFutureAccountRequest querySubAccountFutureAccountRequest = new QuerySubAccountFutureAccountRequest();
        querySubAccountFutureAccountRequest.setParentUserId(parentUserId);
        querySubAccountFutureAccountRequest.setEmail(subAccountFutureTransferArg.getEmail());
        /*APIResponse<Long> response = this.subUserApi.checkRelationAndFutureAccountEnable(this.accountHelper.getInstance(querySubAccountFutureAccountRequest));*/
        APIResponse<Long> response = subUserClient.checkRelationAndFutureAccountEnable(this.accountHelper.getInstance(querySubAccountFutureAccountRequest));
        checkResponse(response);
        if (response == null || response.getData() == null) {
            throw new BusinessException(MgsErrorCode.FUTURE_ACCT_OR_SUBRELATION_NOT_EXIST);
        }
        GetTranRequest getTranRequest = new GetTranRequest();
        getTranRequest.setTranType(73);
        Date now = DateUtil.truncateSecond(new Date().getTime());
        getTranRequest.setTime(now);
        getTranRequest.setDescription("userId=" + response.getData());
        APIResponse<Long> tranApiResponse = tranApi.getTranId(getInstance(getTranRequest));
        checkResponse(tranApiResponse);

        // 新的划转接口
        WalletAssetTransferRequest walletAssetTransferRequest = new WalletAssetTransferRequest();
        walletAssetTransferRequest.setAsset(subAccountFutureTransferArg.getAsset());
        walletAssetTransferRequest.setAmount(subAccountFutureTransferArg.getAmount());
        walletAssetTransferRequest.setKindType(subAccountFutureTransferArg.getType() == 1 ?KindType.MAIN_FUTURE:KindType.FUTURE_MAIN);
        walletAssetTransferRequest.setUserId(response.getData());
        walletAssetTransferRequest.setTranId(tranApiResponse.getData());
        walletAssetTransferRequest.setSource(TRANSFER_SOURCE);
        log.info("call newsubMainFutureAccountTransfer:" + LogMaskUtils.maskJsonString(JsonUtils.toJsonNotNullKey(walletAssetTransferRequest)));
        APIResponse<com.binance.transfer.vo.transfer.WalletAssetTransferResponse> apiResponse = walletTransferApi.walletAssetTransfer(getInstance(walletAssetTransferRequest));
        log.info("call newsubMainFutureAccountTransfer:" + LogMaskUtils.maskJsonString(JsonUtils.toJsonNotNullKey(apiResponse)));
        checkResponse(apiResponse);
        if (apiResponse.getData().getFromStatus() == CommonStatus.PROCESS
                || apiResponse.getData().getToStatus() == CommonStatus.PROCESS) {
            throw new BusinessException(MgsErrorCode.TRANSFER_PENDING);
        } else if (apiResponse.getData().getFromStatus() == CommonStatus.FAILURE
                && apiResponse.getData().getToStatus() == CommonStatus.FAILURE) {
            throw new BusinessException(MgsErrorCode.TRANSFER_FAIL);
        }
        return ok();
    }

    /**
     * delivery-main划转
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/subMainDeliveryAccountTransfer")
    @UserOperation(name = "母账户持仓划转", eventName = "subUserInternalTransfer", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"},
            requestKeys = {"senderUserId", "recipientUserId", "asset", "amount"},
            requestKeyDisplayNames = {"转出方userId", "转入方userId", "币种", "数量"})
    @DDoSPreMonitor(action = "subMainDeliveryAccountTransfer")
    public CommonRet<String> subMainDeliveryAccountTransfer(@Valid @RequestBody SubAccountDeliveryTransferArg subAccountDeliveryTransferArg) throws Exception {
        Long parentUserId = getUserId();
        QuerySubAccountFutureAccountRequest querySubAccountFutureAccountRequest = new QuerySubAccountFutureAccountRequest();
        querySubAccountFutureAccountRequest.setParentUserId(parentUserId);
        querySubAccountFutureAccountRequest.setEmail(subAccountDeliveryTransferArg.getEmail());

        // 返回子账户userId
        /*APIResponse<Long> response = this.subUserApi.checkRelationAndFutureAccountEnable(this.accountHelper.getInstance(querySubAccountFutureAccountRequest));*/
        APIResponse<Long> response = subUserClient.checkRelationAndFutureAccountEnable(this.accountHelper.getInstance(querySubAccountFutureAccountRequest));
        checkResponse(response);
        if (response == null || response.getData() == null) {
            throw new BusinessException(MgsErrorCode.FUTURE_ACCT_OR_SUBRELATION_NOT_EXIST);
        }
        Long subUserId = response.getData();

        MultiFunctionSubUserTransferRequest request = new MultiFunctionSubUserTransferRequest();
        request.setParentUserId(parentUserId);
        request.setSenderUserId(subUserId);
        request.setRecipientUserId(subUserId);
        request.setAsset(subAccountDeliveryTransferArg.getAsset().toUpperCase());
        request.setAmount(subAccountDeliveryTransferArg.getAmount());
        if (subAccountDeliveryTransferArg.getType()==1){
            request.setSenderFunctionAccountType(FunctionAccountType.SPOT);
            request.setRecipientFunctionAccountType(FunctionAccountType.DELIVERY_FUTURE);
        } else if (subAccountDeliveryTransferArg.getType()==2){
            request.setSenderFunctionAccountType(FunctionAccountType.DELIVERY_FUTURE);
            request.setRecipientFunctionAccountType(FunctionAccountType.SPOT);
        } else {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        APIResponse<MultiFunctionSubUserTransferResponse> apiResponse = subUserSpotApi.multiFunctionAccountTransfer(getInstance(request));
        checkResponse(apiResponse);
        CommonRet<String> commonRet = new CommonRet<String>();
        commonRet.setData(apiResponse.getData().getTransactionId().toString());
        //给风控打数据
        UserOperationHelper.log("parentUserId", request.getParentUserId());
        UserOperationHelper.log("senderUserId", request.getSenderUserId());
        UserOperationHelper.log("recipientUserId", request.getRecipientUserId());
        UserOperationHelper.log("asset", request.getAsset());
        UserOperationHelper.log("amount", request.getAmount());
        return commonRet;
    }

    /**
     * margin-main划转
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/subMainMarginAccountTransfer")
    @DDoSPreMonitor(action = "subMainMarginAccountTransfer")
    public CommonRet<String> subMainMarginAccountTransfer(@Valid @RequestBody SubAccountMarginTransferArg subAccountMarginTransferArg) throws Exception {
        MainMarginAccountTransferRequest req = new MainMarginAccountTransferRequest();
        req.setParentUserId(getUserId());
        req.setSubEmail(subAccountMarginTransferArg.getEmail());
        req.setType(subAccountMarginTransferArg.getType());
        req.setAsset(subAccountMarginTransferArg.getAsset());
        req.setAmount(subAccountMarginTransferArg.getAmount());
        log.info("call subMainMarginAccountTransfer:" + LogMaskUtils.maskJsonString(JsonUtils.toJsonNotNullKey(req)));
        APIResponse<MainMarginAccountTransferResponse> response = this.subUserMarginApi.subMainMarginAccountTransfer(this.accountHelper.getInstance(req));
        checkResponse(response);
        log.info("call subMainMarginAccountTransfer result:" + LogMaskUtils.maskJsonString(JsonUtils.toJsonNotNullKey(response)));
        return ok(String.valueOf(response.getData().getTranId()));
    }


    /**
     * future转出最大额度
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/getFutureMaxWithdrawAmount")
    public CommonRet<BigDecimal> getFutureMaxWithdrawAmount(@Valid @RequestBody SubAccountFuturePreTransferArg request) throws Exception {
        Long parentUserId = getUserId();
        QuerySubAccountFutureAccountRequest querySubAccountFutureAccountRequest = new QuerySubAccountFutureAccountRequest();
        querySubAccountFutureAccountRequest.setParentUserId(parentUserId);
        querySubAccountFutureAccountRequest.setEmail(request.getEmail());
        /*APIResponse<Long> response = this.subUserApi.checkRelationAndFutureAccountEnable(this.accountHelper.getInstance(querySubAccountFutureAccountRequest));*/
        APIResponse<Long> response = subUserClient.checkRelationAndFutureAccountEnable(this.accountHelper.getInstance(querySubAccountFutureAccountRequest));
        checkResponse(response);
        if (response == null) {
            throw new BusinessException(MgsErrorCode.FUTURE_ACCT_OR_SUBRELATION_NOT_EXIST);
        }
        //没有future账户
        if (response.getData() == null) {
            return new CommonRet<>(BigDecimal.ZERO);
        }
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(String.valueOf(response.getData()));
        APIResponse<UserInfoVo> subUserInfoRes = userInfoApi.getUserInfoByUserId(APIRequest.instance(userIdRequest));
        if (subUserInfoRes == null || subUserInfoRes.getData() == null) {
            throw new BusinessException(MgsErrorCode.FUTURE_ACCT_OR_SUBRELATION_NOT_EXIST);
        }
        GetMaxWithdrawAmountReq getMaxWithdrawAmountReq = new GetMaxWithdrawAmountReq();
        if (subUserInfoRes.getData().getFutureUserId() == null) {
            return new CommonRet<>(BigDecimal.ZERO);
        }
        getMaxWithdrawAmountReq.setFutureUid(subUserInfoRes.getData().getFutureUserId());
        getMaxWithdrawAmountReq.setAsset(request.getAsset());
        APIResponse<BigDecimal> withdrawResponse = balanceApi.getMaxWithdrawAmount(APIRequest.instance(getMaxWithdrawAmountReq));
        checkResponse(withdrawResponse);
        return new CommonRet<>(withdrawResponse.getData());
    }

    /**
     * marign转出最大额度
     *
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping("/getMarginMaxWithdrawAmount")
    public CommonRet<BigDecimalWrapper> maxTransferable(@Valid @RequestBody SubAccountMarginPreTransferArg arg) throws Exception {
        BindingParentSubUserEmailReq bindingParentSubUserEmailReq = new BindingParentSubUserEmailReq();
        bindingParentSubUserEmailReq.setSubUserEmail(arg.getEmail());
        bindingParentSubUserEmailReq.setParentUserId(getUserId());
        APIResponse<BindingParentSubUserEmailResp> bindResponse = subUserClient.checkRelationByParentSubUserEmail(APIRequest.instance(bindingParentSubUserEmailReq));
        checkResponse(bindResponse);
        APIResponse<BigDecimal> response = transferApi.transferableOfOut(bindResponse.getData().getSubUserId(), arg.getAsset());
        checkResponse(response);
        return ok(BigDecimalWrapper.of(response.getData()));
    }

    /**
     * 指定账户的最大额度
     */
    @PostMapping("/getMaxWithdrawAmount")
    public CommonRet<String> getMaxWithdrawAmount(@Valid @RequestBody AccountMaxWithdrawArg request) throws Exception {
        Long loginUserId = getUserId();
        String email = request.getEmail().trim();

        GetUserRequest userRequest = new GetUserRequest();
        userRequest.setEmail(email);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserByEmail(getInstance(userRequest));
        checkResponse(apiResponse);
        if (apiResponse == null){
            return new CommonRet<>(BigDecimal.ZERO.toPlainString());
        }

        UserInfoVo queryUserInfo = apiResponse.getData().getUserInfo();
        log.info("getMaxWithdrawAmount.userInInfo loginUserId={},masterUserId={},futureUserId={},accountType={}", loginUserId,
                queryUserInfo.getUserId(), queryUserInfo.getFutureUserId(), request.getAccountType());
        // 如果查询的邮箱不是母账户本身，则检查母子绑定关系
        if (!loginUserId.equals(queryUserInfo.getUserId())) {
            BindingParentSubUserEmailReq req = new BindingParentSubUserEmailReq();
            req.setParentUserId(loginUserId);
            req.setSubUserEmail(email);
            APIResponse<BindingParentSubUserEmailResp> response = subUserClient.checkRelationByParentSubUserEmail(getInstance(req));
            checkResponse(response);
            if (response == null || response.getData() == null || response.getData().getSubUserId() == null) {
                return new CommonRet<>(BigDecimal.ZERO.toPlainString());
            }
        }

        switch (request.getAccountType()){
            case SPOT:
                SelectByUserIdsCodeRequest spotRequest = new SelectByUserIdsCodeRequest();
                spotRequest.setAsset(request.getAsset());
                spotRequest.setUserIds(Lists.newArrayList(queryUserInfo.getUserId()));
                APIResponse<List<SelectUserAssetResponse>> spotResponse = userAssetApi.getUserAssetByUserIdsCode(APIRequest.instance(spotRequest));
                checkResponse(spotResponse);
                if (spotResponse.getData() == null || spotResponse.getData().size() == 0) {
                    return new CommonRet<>(BigDecimal.ZERO.toPlainString());
                }
                return new CommonRet<>(spotResponse.getData().get(0).getFree().toPlainString());
            case MARGIN:
                APIResponse<BigDecimal> marginResponse = transferApi.transferableOfOut(queryUserInfo.getUserId(), request.getAsset());
                checkResponse(marginResponse);
                if (marginResponse.getData() == null) {
                    return new CommonRet<>(BigDecimal.ZERO.toPlainString());
                }
                return new CommonRet<>(marginResponse.getData().toPlainString());
            case ISOLATED_MARGIN:
                APIResponse<BigDecimal> response = isolatedMarginTransferApi.transferableAmount(queryUserInfo.getUserId(), request.getSymbol(), request.getAsset());
                checkResponse(response);
                return response.getData() == null ? new CommonRet<>(BigDecimal.ZERO.toPlainString()) : new CommonRet<>(response.getData().toPlainString());
            case FUTURE:
                GetMaxWithdrawAmountReq futureRequest = new GetMaxWithdrawAmountReq();
                futureRequest.setFutureUid(queryUserInfo.getFutureUserId());
                futureRequest.setAsset(request.getAsset());
                APIResponse<BigDecimal> futureResponse = balanceApi.getMaxWithdrawAmount(APIRequest.instance(futureRequest));
                checkResponse(futureResponse);
                if (futureResponse.getData() == null) {
                    return new CommonRet<>(BigDecimal.ZERO.toPlainString());
                }
                return new CommonRet<>(futureResponse.getData().toPlainString());
            case DELIVERY_FUTURE:
                com.binance.delivery.periphery.api.request.core.GetMaxWithdrawAmountReq deliveryRequest = new com.binance.delivery.periphery.api.request.core.GetMaxWithdrawAmountReq();
                deliveryRequest.setFutureUid(queryUserInfo.getFutureUserId());
                deliveryRequest.setAsset(request.getAsset());
                APIResponse<BigDecimal> deliveryResponse = deliveryAdminBalanceApi.getMaxWithdrawAmount(APIRequest.instance(deliveryRequest));
                checkResponse(deliveryResponse);
                if (deliveryResponse.getData() == null) {
                    return new CommonRet<>(BigDecimal.ZERO.toPlainString());
                }
                return new CommonRet<>(deliveryResponse.getData().toPlainString());
            default:
                throw new BusinessException(AccountMgsErrorCode.INVALID_ACCOUNT_TYPE);
        }
    }

    @ApiOperation(value = "根据母账户查所有子账户相应币种的可用余额(可根据子账户的邮箱搜索)")
    @PostMapping(value = "/balance-amount")
    @RolePermissionCheck(permissionNames = {Constant.EnterpriseBasePermission.ASSET_TRANSFER})
    public CommonRet<ParentSubUserCoinAmountResp> getAllSubUserCoinBalance(@RequestBody @Validated SubUserBalanceArg arg) throws Exception {
        // 母账号登陆状态校验
        checkAndGetUserId();
        Long parentUserId = RolePermissionCheckHelper.getEnterpriseId();

        if (arg.getAccountType().equals(FunctionAccountType.ISOLATED_MARGIN)&& StringUtils.isBlank(arg.getSymbol())){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        SubUserBalanceRequest request = new SubUserBalanceRequest();
        request.setParentUserId(parentUserId);
        request.setAccountType(arg.getAccountType());
        request.setAsset(arg.getCoin());
        request.setSymbol(arg.getSymbol());
        request.setEmail(arg.getEmail());
        request.setEmailLike(arg.isEmailLike());
        request.setPage(arg.getPage());
        request.setRows(arg.getRows());
        log.info("call queryParentSubUserCoinAmount:" + LogMaskUtils.maskJsonString(JsonUtils.toJsonNotNullKey(request)));
        APIResponse<ParentSubUserCoinAmountResp> apiResponse= subUserAssetApi.queryParentSubUserCoinAmount(APIRequest.instance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping(value = "/subUserSignLVT")
    public CommonRet<String> subUserSignLVT(@RequestBody @Validated SubUserIdArg subUserIdArg)
            throws Exception {
        Long parentUserId = checkAndGetUserId();
        log.info("母账号给子账号签署lvt, parentUserId:{} subUserId:{}", parentUserId, subUserIdArg.getSubUserId());

        // 校验母子关系
        BindingParentSubUserReq relationCheckReq = new BindingParentSubUserReq();
        relationCheckReq.setParentUserId(parentUserId);
        relationCheckReq.setSubUserId(subUserIdArg.getSubUserId());
        APIResponse<Boolean> relationCheckResp = subUserClient.checkRelationByParentSubUserIds(getInstance(relationCheckReq));
        checkResponse(relationCheckResp);
        if (relationCheckResp.getData() == null || !relationCheckResp.getData()) {
            throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
        }

        com.binance.account.vo.security.request.UserIdRequest userIdRequest = new com.binance.account.vo.security.request.UserIdRequest();
        userIdRequest.setUserId(subUserIdArg.getSubUserId());
        APIResponse<UserStatusEx> subUserStatusResp = userApi.getUserStatusByUserId(getInstance(userIdRequest));
        checkResponseWithoutLog(relationCheckResp);
        UserStatusEx subUserStatus = subUserStatusResp.getData();
        // 校验子账号激活状态
        if (!subUserStatus.getIsUserActive()) {
            throw new BusinessException(AccountMgsErrorCode.SUBUSER_SIGNLVT_NOT_ACTIVE);
        }
        if (subUserStatus.getIsSignedLVTRiskAgreement()){
            throw new BusinessException(AccountMgsErrorCode.SUBUSER_ALREADY_SIGNED_LVT);
        }

        // 签署lvt
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(subUserIdArg.getSubUserId());
        APIResponse<Boolean> lvtResponse = userLVTApi.signLVTRiskAgreement(getInstance(userIdReq));
        checkResponse(lvtResponse);

        try {
            // 完成问卷调查
            APIResponse<Boolean> saqResponse = userApi.finishLVTSAQ(getInstance(userIdReq));
            checkResponse(saqResponse);
        } catch (Exception e) {
            log.error("subUserSignLVT finishLVTSAQ error", e);
        }
        return new CommonRet<>();
    }

    @PostMapping(value = "/count-level-config")
    public CommonRet<SubUserCountLevelConfigRet> subUserCountLevelConfig(@RequestBody @Validated QuerySubUserCountConfigArg arg)
            throws Exception {
        Long parentUserId = checkAndGetUserId();

        QueryAvailableSubUserCountRequest request = new QueryAvailableSubUserCountRequest();
        request.setParentUserId(parentUserId);
        request.setType(arg.getType());
        APIResponse<QueryAvailableSubUserCountResp> availableCountResp = subUserCountConfigApi.queryAvailableSubCountByParent(APIRequest.instance(request));
        checkResponse(availableCountResp);

        AllSubUserCountConfigRequest allRequest = new AllSubUserCountConfigRequest();
        APIResponse<List<SubUserCountConfigVo>> allCountConfigs = subUserCountConfigApi.queryAllSubUserCountConfigs(APIRequest.instance(allRequest));
        checkResponse(allCountConfigs);

        SubUserCountLevelConfigRet ret = new SubUserCountLevelConfigRet();
        List<SubUserCountLevelConfigRet.SubUserCountConfigVo> configVos = Lists.newArrayList();
        BeanUtils.copyProperties(availableCountResp.getData(), ret);
        for (SubUserCountConfigVo vo : allCountConfigs.getData()) {
            SubUserCountLevelConfigRet.SubUserCountConfigVo subUserCountConfigVo = new SubUserCountLevelConfigRet.SubUserCountConfigVo();
            BeanUtils.copyProperties(vo, subUserCountConfigVo);
            configVos.add(subUserCountConfigVo);
        }
        ret.setConfigVos(configVos);
        return new CommonRet<>(ret);
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

    @ApiOperation(value = "查询母账户和子账户的ISOLATED-MARGIN账户汇总")
    @PostMapping(value = "/query/isolated-margin/parentAndSubAccountSummary")
    public CommonRet<IsolatedMarginParentAndSubAccountSummaryRet> queryIsolatedMarginAccountTotalEquity() throws Exception {
        Long parentUserId = getUserId();
        if (parentUserId == null) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
        if(!userStatusEx.getIsSubUserFunctionEnabled()){
            throw new BusinessException(MgsErrorCode.SUBUSER_FEATURE_FORBIDDEN);
        }

        IsolatedMarginParentAndSubAccountSummaryRet summaryRet = new IsolatedMarginParentAndSubAccountSummaryRet();

        String parentTotalAssetOfBtc = isolatedMarginHelper
                .queryIsolatedMarginAccountTotalEquity(parentUserId, ONLY_PARENT_ACCOUNT);

        if (!userStatusEx.getIsBrokerSubUserFunctionEnabled()) {
            String allSubTotalAssetOfBtc = isolatedMarginHelper
                    .queryIsolatedMarginAccountTotalEquity(parentUserId, ONLY_SUB_ACCOUNT);
            summaryRet.setAllSubAccountNetAssetOfBtc(allSubTotalAssetOfBtc);
        }

        summaryRet.setParentAccountNetAssetOfBtc(parentTotalAssetOfBtc);
        return new CommonRet<>(summaryRet);
    }

    @ApiOperation(value = "分页查询子账户的ISOLATED-MARGIN账户汇总详情")
    @PostMapping(value = "/query/isolated-margin/summary")
    public CommonPageRet<QuerySubUserIsolatedMarginSummaryRet> querySubAccountTotalPageSummary(@RequestBody @Validated QueryIsolatedMarginAccountSummaryArg arg) throws Exception {
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
        if(!userStatusEx.getIsSubUserFunctionEnabled()){
            throw new BusinessException(MgsErrorCode.SUBUSER_FEATURE_FORBIDDEN);
        }

        return isolatedMarginHelper.querySubAccountTotalPageSummary(arg);
    }

    @ApiOperation("子账户SPOT - ISOLATED-MARGIN账户划转")
    @PostMapping(value = "/isolated-margin/transfer")
    @DDoSPreMonitor(action = "isolatedMarginTransfer")
    public CommonRet<Void> isolatedMarginTransfer(@RequestBody @Validated SubUserTransferIsolatedMarginArg arg) throws Exception {
        if (!timeOutRegexUtils.validateEmail(arg.getSubUserEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        BindingParentSubUserEmailReq bindingParentSubUserEmailReq = new BindingParentSubUserEmailReq();
        bindingParentSubUserEmailReq.setSubUserEmail(arg.getSubUserEmail());
        bindingParentSubUserEmailReq.setParentUserId(parentUserId);
        APIResponse<BindingParentSubUserEmailResp> bindResponse = subUserClient.checkRelationByParentSubUserEmail(APIRequest.instance(bindingParentSubUserEmailReq));
        checkResponse(bindResponse);

        CheckParentAndSubUserBindingRequest checkRequest = new CheckParentAndSubUserBindingRequest();
        checkRequest.setParentUserId(parentUserId);
        checkRequest.setEmail(arg.getSubUserEmail());
        /*APIResponse<UserVo> checkResponse = subUserApi.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));*/
        APIResponse<UserVo> checkResponse = subUserClient.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));

        if (checkResponse == null || !APIResponse.OK.getStatus().equals(checkResponse.getStatus())) {
            throw new BusinessException(GeneralCode.USER_NOT_EXIST);
        }
        if(checkFlexLineSubMainMarginTransfer){
            UserStatusEx userStatusEx = new UserStatusEx(checkResponse.getData().getStatus(), checkResponse.getData().getStatusExtra());
            if(userStatusEx.getIsFlexLineCreditUser() || userStatusEx.getIsFlexLineTradingUser()){
                log.error("flexLineCreditSubIsNotSupport");
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
        }

        if (SubUserTransferKindType.isFromIsolatedMargin(arg.getKindType())) {
            // 从isolated-margin转出
            accountTransferHelper.TransferOutFromIsolatedMargin(arg, checkResponse.getData().getUserId());
        } else {
            // 转入到isolated-margin
            accountTransferHelper.transferInToIsolatedMargin(arg, checkResponse.getData().getUserId());
        }
        return ok();
    }

    @ApiOperation(value = "查询子账户的ISOLATED-MARGIN账户详情")
    @PostMapping(value = "/query/isolated-margin/subAccountDetail")
    public CommonRet<IsolatedMarginAccountDetailsRet> queryIsolatedMarginDetail(@RequestBody @Validated QuerySubUserIsolatedMarginDetailArg arg) throws Exception {
        if (!timeOutRegexUtils.validateEmail(arg.getSubUserEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        BindingParentSubUserEmailReq bindingParentSubUserEmailReq = new BindingParentSubUserEmailReq();
        bindingParentSubUserEmailReq.setSubUserEmail(arg.getSubUserEmail());
        bindingParentSubUserEmailReq.setParentUserId(parentUserId);
        APIResponse<BindingParentSubUserEmailResp> bindResponse = subUserClient.checkRelationByParentSubUserEmail(APIRequest.instance(bindingParentSubUserEmailReq));
        checkResponse(bindResponse);

        CheckParentAndSubUserBindingRequest checkRequest = new CheckParentAndSubUserBindingRequest();
        checkRequest.setParentUserId(parentUserId);
        checkRequest.setEmail(arg.getSubUserEmail());
        /*APIResponse<UserVo> checkResponse = subUserApi.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));*/
        APIResponse<UserVo> checkResponse = subUserClient.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));

        if (checkResponse == null || !APIResponse.OK.getStatus().equals(checkResponse.getStatus())) {
            throw new BusinessException(GeneralCode.USER_NOT_EXIST);
        }

        return ok(isolatedMarginHelper.querySubAccountIsolatedMarginDetail(arg, checkResponse.getData().getUserId()));
    }

    @ApiOperation("查询子账户盈亏情况")
    @PostMapping("/query/isolated-margin/profit")
    public CommonRet<ProfitSummaryRet> queryIsolatedMarginProfit(@RequestBody @Validated QuerySubUserIsolatedMarginProfitArg arg) throws Exception {

        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }

        UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
        if(!userStatusEx.getIsSubUserFunctionEnabled()){
            throw new BusinessException(MgsErrorCode.SUBUSER_FEATURE_FORBIDDEN);
        }

        CheckParentAndSubUserBindingRequest checkRequest = new CheckParentAndSubUserBindingRequest();
        checkRequest.setParentUserId(parentUserId);
        checkRequest.setEmail(arg.getSubUserEmail());
        /*APIResponse<UserVo> checkResponse = subUserApi.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));*/
        APIResponse<UserVo> checkResponse = subUserClient.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));

        if (checkResponse == null || !APIResponse.OK.getStatus().equals(checkResponse.getStatus())) {
            throw new BusinessException(GeneralCode.USER_NOT_EXIST);
        }

        return ok(isolatedMarginHelper.queryIsolatedMarginProfit(checkResponse.getData().getUserId(), arg.getSymbol(), arg.getPeriodType()));
    }

    @ApiOperation("母账户开启一键平仓权限")
    @PostMapping("/enable/clearPosition")
    @DDoSPreMonitor(action = "enableClearPosition")
    public CommonRet<Boolean> enableClearPosition() throws Exception {
        Long parentUserId = checkAndGetUserId();

        UserStatusEx userStatusEx = getUserStatusByUserId(parentUserId);
        if (!userStatusEx.getIsSubUserFunctionEnabled()) {
            throw new BusinessException(MgsErrorCode.SUBUSER_FEATURE_FORBIDDEN);
        }

        EnableClearPositionRequest req = new EnableClearPositionRequest();
        req.setParentUserId(parentUserId);
        APIResponse<Boolean> resp = subUserAccountApi.enableClearPosition(APIRequest.instance(req));
        checkResponse(resp);
        return new CommonRet<>(resp.getData());
    }

    @ApiOperation("母账户一键平仓子账户")
    @PostMapping("/clearPosition")
    @DDoSPreMonitor(action = "clearSubPosition")
    public CommonRet<ClearPositionRet> clearPosition(@RequestBody @Validated ClearPositionArg clearPositionArg) throws Exception {
        Long parentUserId = checkAndGetUserId();

        if (StringUtils.isBlank(clearPositionArg.getSubUserEmail())
                || !timeOutRegexUtils.validateEmail(clearPositionArg.getSubUserEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        if (!FunctionAccountType.FUTURE.equals(clearPositionArg.getAccountType())
                && !FunctionAccountType.DELIVERY_FUTURE.equals(clearPositionArg.getAccountType())) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }

        CheckParentAndSubUserBindingRequest checkReq = new CheckParentAndSubUserBindingRequest();
        checkReq.setEmail(clearPositionArg.getSubUserEmail());
        checkReq.setParentUserId(parentUserId);
        APIResponse<UserVo> checkResp = subUserApi.checkParentAndSubUserBinding(APIRequest.instance(checkReq));
        checkResponse(checkResp);

        UserVo subUserVo = checkResp.getData();
        ClearPositionRequest req = new ClearPositionRequest();
        req.setParentUserId(parentUserId);
        req.setSubUserId(subUserVo.getUserId());
        req.setAccountType(clearPositionArg.getAccountType());
        req.setSymbol(clearPositionArg.getSymbol());
        APIResponse<ClearPositionResponse> resp = subUserAccountApi.clearPosition(APIRequest.instance(req));
        checkResponse(resp);

        ClearPositionRet ret = new ClearPositionRet();
        List<ClearPositionFailedOrderVo> failedOrders = Lists.newArrayList();
        for (ClearPositionFailedOrderVO clearPositionFailedOrder : resp.getData().getFailOrderList()) {
            ClearPositionFailedOrderVo failedOrder = new ClearPositionFailedOrderVo();
            BeanUtils.copyProperties(clearPositionFailedOrder, failedOrder);
            failedOrders.add(failedOrder);
        }
        ret.setFailOrderList(failedOrders);
        return new CommonRet<>(ret);
    }

    @ApiOperation(value = "母账户删除子账户")
    @PostMapping(value = "/delete")
    @DDoSPreMonitor(action = "deleteSubUser")
    public CommonRet<DeleteSubUserRet> deleteSubUser(@RequestBody @Validated DeleteSubUserArg arg) throws Exception {
        // 验证参数
        String subUserEmail = arg.getSubUserEmail();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        // check binding.
        CheckParentAndSubUserBindingRequest checkRequest = new CheckParentAndSubUserBindingRequest();
        checkRequest.setParentUserId(parentUserId);
        checkRequest.setEmail(subUserEmail);
        /*APIResponse<UserVo> checkSubUserResp = subUserApi.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));*/
        APIResponse<UserVo> checkSubUserResp = subUserClient.checkParentAndSubUserBinding(accountHelper.getInstance(checkRequest));

        checkResponse(checkSubUserResp);

        // disable preCheck.
        DeleteSubUserPreCheckReq preCheckReq = new DeleteSubUserPreCheckReq();
        preCheckReq.setSubUserId(checkSubUserResp.getData().getUserId());
        preCheckReq.setParentUserId(parentUserId);
        APIResponse<DeleteSubUserPreCheckResponse> preCheckResp = subUserAccountApi.deleteSubUserPreCheck(APIRequest.instance(preCheckReq));
        checkResponse(preCheckResp);

        // disable.
        DeleteSubUserRequest req = new DeleteSubUserRequest();
        req.setParentUserId(parentUserId);
        req.setSubUserId(checkSubUserResp.getData().getUserId());
        log.info("SubUserController.deleteSubUser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)));
        APIResponse<DeleteSubUserResp> resp = subUserApi.deleteSubUser(APIRequest.instance(req));
        log.info("SubUserController.deleteSubUser end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(req)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(resp)));
        checkResponse(resp);
        return new CommonRet<>();
    }

    @ApiOperation(value = "商户母账号注册商户子账号")
    @PostMapping(value = "/merchantSubUser/creation")
    @DDoSPreMonitor(action = "createMerchantSubUser")
    public CommonRet<CreateCommMerchantSubUserRet> createMerchantSubUser(@RequestBody @Validated CreateCommMerchantSubUserArg arg, HttpServletRequest request) throws Exception {
        CommonRet<CreateCommMerchantSubUserRet> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        // 校验邮箱格式
        if (StringUtils.isBlank(arg.getEmail()) || !timeOutRegexUtils.validateEmailForRegister(arg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        if (SubUserBizType.getByBizType(arg.getSubUserBizType()) == null) {
            log.error("merchantSubUserCommonApi createMerchantSubUser bizType error, bizType={}", arg.getSubUserBizType());
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        if (ddosCacheSeviceHelper.subAccountActionCount(parentUserId, "createMerchantSub", merchantSubAccountDdosExpireTime) > merchantSubAccountActionCount) {
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }
        CreateCommMerchantSubUserReq createCommMerchantSubUserReq = CopyBeanUtils.fastCopy(arg, CreateCommMerchantSubUserReq.class);
        createCommMerchantSubUserReq.setParentUserId(parentUserId);
        createCommMerchantSubUserReq.setEmail(arg.getEmail().trim());
        createCommMerchantSubUserReq.setRemark(request.getParameter("remark"));
        createCommMerchantSubUserReq.setEmailVerifyCode(arg.getEmailVerifyCode());
        createCommMerchantSubUserReq.setParentGoogleVerifyCode(arg.getParentGoogleVerifyCode());
        createCommMerchantSubUserReq.setParentMobileVerifyCode(arg.getParentMobileVerifyCode());
        log.info("SubUserController.createMerchantSubUser call subUser, request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(createCommMerchantSubUserReq)));
        APIResponse<CreateSubUserRes> apiResponse = merchantSubUserCommonApi.createMerchantSubUser(getInstance(createCommMerchantSubUserReq));
        log.info("SubUserController.createSubUserByParent call subUser end response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        CreateCommMerchantSubUserRet subUserRet = new CreateCommMerchantSubUserRet();
        subUserRet.setUserId(apiResponse.getData().getUserId());
        subUserRet.setEmail(apiResponse.getData().getEmail());
        ret.setData(subUserRet);
        return ret;
    }

    @ApiOperation("商户子账号信息列表")
    @PostMapping("/merchantSubUser/merchantSubUserList")
    public CommonRet<Map<String,Object>> merchantSubUserList(@Validated @RequestBody QueryCommMerchantSubUserArg arg) throws Exception{
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        // 校验邮箱格式
        if (StringUtils.isNotBlank(arg.getEmail()) && !timeOutRegexUtils.validateEmail(arg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        if (SubUserBizType.getByBizType(arg.getSubUserBizType()) == null) {
            log.error("merchantSubUserCommonApi createMerchantSubUser bizType error, bizType={}", arg.getSubUserBizType());
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        QueryCommMerchantSubUserReq req = CopyBeanUtils.fastCopy(arg, QueryCommMerchantSubUserReq.class);
        req.setParentUserId(parentUserId);
        log.info("SubUserController.merchantSubUserList start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)));
        APIResponse<CommMercSearchResult<MerchantSubUserVo>> apiResponse = merchantSubUserCommonApi.merchantSubUserList(getInstance(req));
        log.info("SubUserController.merchantSubUserList end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)), apiResponse);
        checkResponse(apiResponse);
        CommonRet<Map<String,Object>> commonPageRet = new CommonRet<>();
        List<MerchantSubUserVoRet> retList = Lists.newArrayList();
        for (MerchantSubUserVo subUserVo : apiResponse.getData().getRows()) {
            MerchantSubUserVoRet ret = CopyBeanUtils.fastCopy(subUserVo, MerchantSubUserVoRet.class);
            retList.add(ret);
        }
        Map<String,Object> map = new HashMap<>();
        map.put("rows",retList);
        map.put("total",apiResponse.getData().getTotal());
        map.put("maxSubUserNum",apiResponse.getData().getMaxSubUserNum());
        commonPageRet.setData(map);
        return commonPageRet;
    }

    @ApiOperation("删除商户子账号")
    @PostMapping("/merchantSubUser/delete")
    @DDoSPreMonitor(action = "deleteMerchantSubUser")
    public CommonRet<Integer> deleteUser(@Validated @RequestBody DeleteCommMerchantSubUserArg arg) throws Exception {

        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        DeleteCommMerchantSubUserReq req = CopyBeanUtils.fastCopy(arg, DeleteCommMerchantSubUserReq.class);
        req.setParentUserId(parentUserId);
        APIResponse<Integer> apiResponse = merchantSubUserCommonApi.deleteUser(getInstance(req));
        log.info("SubUserController, merchantSubUser.deleteUser end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(req)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @ApiOperation("母账户设置 STP 类型")
    @PostMapping("/modifyStpAccountType")
    @UserOperation(name = "母账户STP设置", eventName = "modifyStpAccountType", responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, requestKeys = {"stpType"}, requestKeyDisplayNames = {"类型"})
    public CommonRet<StpAccountSettingResp> modifyStpAccountType(@RequestBody @Validated StpAccountSettingArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        StpAccountSettingReq settingReq = new StpAccountSettingReq();
        settingReq.setParentUserId(parentUserId);
        settingReq.setParentStpAccountSettingType(arg.getParentStpAccountSettingType());
        APIResponse<StpAccountSettingResp> apiResponse = subUserAccountApi.modifyStpAccountType(APIRequest.instance(settingReq));
        log.info("SubUserController modifyStpAccountType end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(settingReq)), LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        UserOperationHelper.log("stpType", arg.getParentStpAccountSettingType().getStpAccountType());
        return new CommonRet<>(apiResponse.getData());
    }

    @ApiOperation("母账户设置 STP 类型")
    @PostMapping("/queryStpAccountType")
    public CommonRet<QueryStpTypeResp> queryStpAccountType() throws Exception {
        Long parentUserId = checkAndGetUserId();
        QueryStpTypeReq stpTypeReq = new QueryStpTypeReq();
        stpTypeReq.setUserId(parentUserId);
        APIResponse<QueryStpTypeResp> apiResponse = subUserAccountApi.getStpAccountType(APIRequest.instance(stpTypeReq));
        log.info("SubUserController queryStpAccountType end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(stpTypeReq)), LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }


    /**
     *  当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
     */
    private Pair<Long, Boolean> getRealParentUserId() throws Exception {
        Long userId = baseHelper.getUserId();
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        GetUserResponse userResponse = this.getUserById(userId);
        if(userResponse != null && userResponse.getUser() != null ){
            UserStatusEx userStatusEx = new UserStatusEx(userResponse.getUser().getStatus(),userResponse.getUser().getStatusExtra());
            if(userStatusEx.getIsCommonMerchantSubUser() || userStatusEx.getIsEnterpriseRoleUser()){
                return Pair.of(userResponse.getUserInfo().getParent(),true);
            }
        }
        return Pair.of(userId,false);
    }

    public GetUserResponse getUserById(Long userId)throws Exception{
        com.binance.account.vo.security.request.UserIdRequest request = new com.binance.account.vo.security.request.UserIdRequest();
        request.setUserId(userId);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserById(APIRequest.instance(request));
        if (APIResponse.Status.ERROR == apiResponse.getStatus()|| null==apiResponse.getData()) {
            log.error("SubUserSpotController UserApiClient.getUserById :userId=" + userId + "  error" + apiResponse.getErrorData());
            throw new BusinessException("getUserById failed");
        }
        return apiResponse.getData();
    }
}
