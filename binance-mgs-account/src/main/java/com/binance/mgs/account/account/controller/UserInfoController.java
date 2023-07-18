package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserAgreementApi;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserConfigApi;
import com.binance.account.api.UserInfoApi;
import com.binance.account.api.UserLVTApi;
import com.binance.account.api.UserMiningApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.api.UserSecurityLogApi;
import com.binance.account.common.enums.UserConfigTypeEnum;
import com.binance.account.constant.AccountCommonConstant;
import com.binance.account.error.AccountErrorCode;
import com.binance.account.vo.mining.request.CreateMingAccountRequest;
import com.binance.account.vo.mining.response.CreateMiningUserResponse;
import com.binance.account.vo.security.UserSecurityLogVo;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.request.CreateMarginAccountRequest;
import com.binance.account.vo.security.request.GetUserSecurityLogRequest;
import com.binance.account.vo.security.request.GetUserStatusByUserIdRequest;
import com.binance.account.vo.security.request.OpenOrCloseBNBFeeRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.security.request.VerificationTwoV3Request;
import com.binance.account.vo.security.response.GetUserSecurityLogResponse;
import com.binance.account.vo.security.response.VerificationTwoV3Response;
import com.binance.account.vo.subuser.request.UserIdReq;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.account.vo.user.enums.UserTransferWalletEnum;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.BaseDetailRequest;
import com.binance.account.vo.user.request.OrderConfirmStatusRequestV2;
import com.binance.account.vo.user.request.OrderConfrimStatusRequest;
import com.binance.account.vo.user.request.SelectUserConfigRequest;
import com.binance.account.vo.user.request.SetUserConfigRequest;
import com.binance.account.vo.user.request.SetUserTransferWalletRequest;
import com.binance.account.vo.user.request.UpdateNickNameRequest;
import com.binance.account.vo.user.request.UpdateResidentCountryRequest;
import com.binance.account.vo.user.request.UserSignatureConfigRequest;
import com.binance.account.vo.user.response.BaseDetailResponse;
import com.binance.account.vo.user.response.CreateMarginUserResponse;
import com.binance.account.vo.user.response.UpdateResidentCountryResponse;
import com.binance.account.vo.user.response.UserAgreementResponse;
import com.binance.account.vo.user.response.UserConfigResponse;
import com.binance.account.vo.user.response.UserRegisterChoiceResponse;
import com.binance.account.vo.user.response.UserSAQResponse;
import com.binance.account.vo.user.response.UserSignatureConfigResponse;
import com.binance.account.vo.user.response.UserTransferWalletResponse;
import com.binance.accountlog.api.AccountLogUserSecurityLogApi;
import com.binance.accountlog.vo.security.response.GetSecurityLogWithRequestResponse;
import com.binance.accountpersonalcenter.api.UserPersonalConfigApi;
import com.binance.accountpersonalcenter.vo.userpersonalconfig.QueryOneStepWithdrawalSwitchRequest;
import com.binance.accountpersonalcenter.vo.userpersonalconfig.SaveOneStepWithdrawalSwitchRequest;
import com.binance.certification.api.KycCertificateApi;
import com.binance.certification.response.UserCmeCheckRequest;
import com.binance.certification.response.UserCmeCheckResponse;
import com.binance.margin.api.bookkeeper.MarginAccountApi;
import com.binance.margin.api.bookkeeper.request.MarginAccountRequest;
import com.binance.margin.api.bookkeeper.response.MarginAccountResponse;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.BitUtils;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mbxgateway.api.IAccountApi;
import com.binance.mbxgateway.vo.request.SetGasRequest;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.AccountDdosRedisHelper;
import com.binance.mgs.account.account.helper.UserConfigHelper;
import com.binance.mgs.account.account.service.UserConfigService;
import com.binance.mgs.account.account.vo.BaseDetailRet;
import com.binance.mgs.account.account.vo.GetUserConfigArg;
import com.binance.mgs.account.account.vo.GetUserConfigRet;
import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import com.binance.mgs.account.account.vo.OneStepWithdrawalCloseArg;
import com.binance.mgs.account.account.vo.OneStepWithdrawalInfoRet;
import com.binance.mgs.account.account.vo.OneStepWithdrawalModifyLimitArg;
import com.binance.mgs.account.account.vo.OneStepWithdrawalOpenArg;
import com.binance.mgs.account.account.vo.OrderConfirmStatusV2Arg;
import com.binance.mgs.account.account.vo.OrderConfrimStatusArg;
import com.binance.mgs.account.account.vo.SaveUserConfigArg;
import com.binance.mgs.account.account.vo.UpdateCommissionStatusArg;
import com.binance.mgs.account.account.vo.UpdateNickNameArg;
import com.binance.mgs.account.account.vo.UserCmeFlagRet;
import com.binance.mgs.account.account.vo.UserConfigTypeArg;
import com.binance.mgs.account.account.vo.UserRegisterChoiceRet;
import com.binance.mgs.account.account.vo.UserSecurityLogRet;
import com.binance.mgs.account.account.vo.UserSpecialSignatureConfigResponse;
import com.binance.mgs.account.account.vo.UserStatusRet;
import com.binance.mgs.account.account.vo.UserTransferWalletLogRet;
import com.binance.mgs.account.account.vo.UserTransferWalletRet;
import com.binance.mgs.account.account.vo.UserTransferWalletSetArg;
import com.binance.mgs.account.account.vo.kyc.UpdateResidentCountryArg;
import com.binance.mgs.account.account.vo.kyc.UpdateResidentCountryRet;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.OperateType;
import com.binance.mgs.account.service.UserKycService;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.business.account.vo.LocationInfo;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonPageArg;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.ListTransformUtil;
import com.binance.platform.mgs.utils.MaskUtil;
import com.binance.platform.mgs.utils.StringUtil;
import com.binance.quota.center.api.QuotaManagerUserSpecApi;
import com.binance.quota.center.api.QuotaQueryApi;
import com.binance.quota.center.api.dto.LimitDetail;
import com.binance.quota.center.api.dto.QuotaLimitDetail;
import com.binance.quota.center.api.dto.QuotaLimitQueryRequest;
import com.binance.quota.center.api.dto.UserSpecQuotaRuleReq;
import com.binance.userbigdata.api.BigDataUserApi;
import com.binance.userbigdata.vo.user.response.FinanceFlagResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.javasimon.aop.Monitored;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
@Monitored
public class UserInfoController extends AccountBaseAction {
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[^\\\\~`!！\\$￥…#%@\\^&\\(\\)\\+\\.\\[\\]【】?\\{\\}\\|<>]+$");

    private static final ExecutorService CME_SAVE_EXECUTOR = Executors.newFixedThreadPool(3);

    private static final String ONE_STEP_WITHDRAW_QUOTA_BIZ_TYPE = "Crypto";
    private static final String ONE_STEP_WITHDRAW_QUOTA_SUB_BIZ_TYPE = "2faFreeWithdraw";

    @Resource
    private UserSecurityLogApi userSecurityLogApi;
    @Autowired
    private AccountLogUserSecurityLogApi accountLogUserSecurityLogApi;

    @Resource
    private UserSecurityApi userSecurityApi;

    @Resource
    private UserConfigService userConfigService;

    @Resource
    private UserApi userApi;
    @Resource
    private UserLVTApi userLVTApi;
    @Resource
    private UserAgreementApi userAgreementApi;
    @Resource
    private UserConfigApi userConfigApi;
    @Resource
    private UserInfoApi userInfoApi;
    @Resource
    private IAccountApi accountApi;

    @Resource
    private SysConfigHelper sysConfigHelper;

    @Resource
    private UserConfigHelper userConfigHelper;
    @Resource
    private KycCertificateApi kycCertificateApi;
    @Resource
    private BigDataUserApi bigDataUserApi;
    @Resource
    private UserPersonalConfigApi userPersonalConfigApi;
    @Resource
    private QuotaQueryApi quotaQueryApi;
    @Resource
    private QuotaManagerUserSpecApi quotaManagerUserSpecApi;

    @Value("${user.config.recommend.switch:false}")
    private boolean recommendSwitch;

    @Resource
    private UserMiningApi userMiningApi;

    @Value("${user.level.withdraw.switch:false}")
    private boolean userlevelWithdrawSwitch;
    @Value("${user.cme.confirm.switch:false}")
    private boolean userCmeConfirmSwitch;
    @Value("${user.cme.confirm.beginTime:}")
    private String userCmeConfigBeginTime;

    @Resource
    private MarginAccountApi marginAccountApi;
    /**
     * margin 账户创建切流到 margin 接口开关， true表示切换到margin
     */
    @Value("${margin.account.create.switch:false}")
    private boolean createMarginAccountSwitch;

    @Value("${financeFlag.userbigdata.switch:0}")
    private int financeFlagQuerySwitch;

    @Value("${user-security-log.accountlog.switch:false}")
    private Boolean accountlogSwitch;

    @Value("${user.nickname.update.switch:false}")
    private Boolean userNicknameUpdateSwitch;
    
    @Value("${account.check.before.use.bnb.fee:true}")
    private Boolean accountCheckBeforeUseBnbFee;

    @Value("${oneStepWithdraw.quota.dailyLimit:10000}")
    private BigDecimal oneStepWithdrawQuotaDailyLimit;

    @Value("#{'${saveuserconfig.logtype.list:preferLang}'.split(',')}")
    private List<String> saveUserConfigLogTypeList;

    @Value("${oneStepWithdraw.maxScale:8}")
    private Integer oneStepWithdrawMaxScale;
    @Resource
    private UserKycService userKycService;

    private RedisTemplate<String, Object> accountMgsRedisTemplate= AccountDdosRedisHelper.getInstance();



    @PostMapping(value = "/private/account/user/base-detail")
    public CommonRet<BaseDetailRet> getUserDetail() throws Exception {
        CommonRet<BaseDetailRet> ret = new CommonRet<>();
        BaseDetailRequest baseDetailRequest = new BaseDetailRequest();
        baseDetailRequest.setUserId(getUserId());
        APIResponse<BaseDetailResponse> apiResponse = userApi.baseDetail(getInstance(baseDetailRequest));
        checkResponse(apiResponse);
        BaseDetailResponse baseDetailResponse = apiResponse.getData();
        if (baseDetailResponse != null) {
            UserStatusEx userStatusEx = baseDetailResponse.getUserStatusEx();
            BaseDetailRet data = new BaseDetailRet();
            ret.setData(data);
            BeanUtils.copyProperties(baseDetailResponse, data);
            // 判断是否有margin账户
            data.setIsExistMarginAccount(Objects.isNull(baseDetailResponse.getMarginUserId()) ? false : true);
            // 判断是否有future账户
            data.setIsExistFutureAccount(baseDetailResponse.getUserStatusEx().getIsExistFutureAccount());
            // 交易级别")
            data.setTradeLevel(baseDetailResponse.getTradeLevel() == null ? 0 : baseDetailResponse.getTradeLevel());
            // 手机")
            if(baseHelper.isFromWeb()){
                data.setMobileNo(MaskUtil.maskMobileNo(baseDetailResponse.getMobile()));
            }else{
                data.setMobileNo(baseDetailResponse.getMobile());
            }
            //资产子账户
            data.setIsAssetSubAccount(BitUtils.isEnable(baseDetailResponse.getStatus(), Constant.USER_IS_ASSET_SUBUSER));
            data.setIsExistMiningAccount(null!=userStatusEx.getIsExistMiningAccount()?userStatusEx.getIsExistMiningAccount().booleanValue():false);

            data.setIsSignedLVTRiskAgreement(null!=userStatusEx.getIsSignedLVTRiskAgreement()?userStatusEx.getIsSignedLVTRiskAgreement().booleanValue():false);
            data.setUserFastWithdrawEnabled(null!=userStatusEx.getUserFastWithdrawEnabled()?userStatusEx.getUserFastWithdrawEnabled().booleanValue():false);
            data.setUserId(getUserIdStr());
            // 用户安全级别:1:普通,2:身份认证,3:?")
            data.setLevel(baseDetailResponse.getSecurityLevel());
            data.setIsNoEmailSubUser(userStatusEx.getIsNoEmailSubUser() == null?false:userStatusEx.getIsNoEmailSubUser());
            if (userStatusEx != null) {
                // 协议确认
                data.setIsUserProtocol(userStatusEx.getIsUserProtocol());
                // 谷歌二次验证是否开启")
                data.setGauth(userStatusEx.getIsUserGoogle());
                // 手机二次验证是否开启")
                data.setMobileSecurity(userStatusEx.getIsUserMobile());
                // 是否母账号")
                data.setParentUser(userStatusEx.getIsSubUserFunctionEnabled());
                // 是否子账号")
                data.setSubUser(userStatusEx.getIsSubUser());
                // 子账号是否已启用")
                data.setSubUserEnabled(userStatusEx.getIsSubUserEnabled());
                // BNB手续费开关是否开启")
                data.setCommissionStatus(userStatusEx.getIsUserFee() ? 1 : 0);
                // 是否开启提币白名单提现
                data.setWithdrawWhiteStatus(userStatusEx.getIsWithdrawWhite());
                // 是否提交过返佣设置表格
                data.setIsReferralSettingSubmitted(userStatusEx.getIsReferralSettingSubmitted());
                log.info("forbid_app_trade");
                // 限制APP交易开关")
                String forbidAppTrade = sysConfigHelper.getCodeByDisplayName("forbid_app_trade");
                if (StringUtils.isNotBlank(forbidAppTrade)) {
                    data.setForbidAppTrade(forbidAppTrade);
                } else {
                    data.setForbidAppTrade(userStatusEx.getIsUserTradeApp() ? "0" : "1");
                }
                // 判断是否有fiat账户
                data.setIsExistFiatAccount(userStatusEx.getIsExistFiatAccount());
                data.setIsBindEmail(!userStatusEx.getIsUserNotBindEmail().booleanValue());
                data.setIsMobileUser(userStatusEx.getIsMobileUser().booleanValue());
                data.setIsExistCardAccount(userStatusEx.getIsExistCardAccount());
                data.setBrokerParentUser(userStatusEx.getIsBrokerSubUserFunctionEnabled());
                data.setBrokerSubUser(userStatusEx.getIsBrokerSubUser());
                data.setIsManagerSubUserFunctionEnabled(userStatusEx.getIsManagerSubUserFunctionEnabled());
                data.setIsAllowBatchAddWhiteAddress(userStatusEx.getIsAllowBatchAddWhiteAddress()==null?false:userStatusEx.getIsAllowBatchAddWhiteAddress());
                data.setIsLockWhiteAddress(userStatusEx.getIsLockWhiteAddress()==null?false:userStatusEx.getIsLockWhiteAddress());
                data.setIsUserNeedKyc(userStatusEx.getIsUserNeedKyc());
                data.setIsCommonMerchantSubUser(userStatusEx.getIsCommonMerchantSubUser());
                data.setIsEnterpriseRoleUser(userStatusEx.getIsEnterpriseRoleUser());
                data.setIsOneButtonClearPosition(userStatusEx.getIsUserAbleClearPosition());
                data.setIsPortfolioMarginRetailUser(userStatusEx.getIsPortfolioMarginRetailUser());
                data.setIsOneButtonManagerSubUserClearPosition(userStatusEx.getIsAbleManagerSubUserClearPosition());
                data.setIsUserPersonalOrEnterpriseAccount(userStatusEx.getIsUserPersonalOrEnterpriseAccount());
                data.setIsExistOptionAccount(userStatusEx.getIsOpenEOptionsAccount());
            }
            UserSecurityLogVo userSecurityLogVo = baseDetailResponse.getLastUserSecurityLog();
            if (userSecurityLogVo != null) {
                // 最后一次登录IP")
                data.setLastLoginIp(userSecurityLogVo.getIp());
                // 最后一次登录时间")
                data.setLastLoginTime(userSecurityLogVo.getOperateTime());
                // 根据ip计算最后一次登录城市
                if (StringUtils.isNotBlank(userSecurityLogVo.getIp())) {
                    LocationInfo ipLocation = Ip2LocationSwitchUtils.getDetail(userSecurityLogVo.getIp());
                    if (ipLocation != null) {
                        data.setLastLoginCountry(ipLocation.getCountryShort());
                        data.setLastLoginCity(ipLocation.getCity());
                    }
                }
            }

            final Integer certificateStatus = baseDetailResponse.getCertificateStatus();
            if (certificateStatus == null) {
                // 证件照片")
                data.setIdPhoto("-1");
            } else {
                data.setIdPhoto(String.valueOf(certificateStatus));
                if (certificateStatus.intValue() == 2) {// 身份证审核被拒绝
                    // 身份证审核被拒绝原因")
                    data.setIdPhotoMsg(baseDetailResponse.getCertificateMessage());
                }
                data.setAuthenticationType(baseDetailResponse.getCertificateType());
            }
            log.info("jumio_enable");
            String jumioEnable = sysConfigHelper.getCodeByDisplayName("jumio_enable");
            // 开启jumio身份认证 1:enable 0 disable")
            if (BooleanUtils.toBoolean(jumioEnable)) {
                data.setJumioEnable(1);
            } else {
                data.setJumioEnable(0);
            }
            log.info("address_verification_switch");
            String userAddressSwitch = sysConfigHelper.getCodeByDisplayName("address_verification_switch");
            if ("ON".equalsIgnoreCase(userAddressSwitch)) {
                data.setCertificateAddress(baseDetailResponse.getCertificateAddress());
            }
            if(userlevelWithdrawSwitch){
                data.setLevelWithdraw(baseDetailResponse.getLevelWithdraw());
            }else{
                com.binance.account.vo.user.request.UserIdRequest userIdRequest =
                        new com.binance.account.vo.user.request.UserIdRequest();
                userIdRequest.setUserId(getUserIdStr());
                APIResponse<List<BigDecimal>> apiUserLevelResponse =
                        userInfoApi.getUserLevelWithdreaw(getInstance(userIdRequest));
                checkResponse(apiUserLevelResponse);
                // 不同级别对应的提现额度
                data.setLevelWithdraw(apiUserLevelResponse.getData());
            }

            // 用户下单确认状态开关
            data.setOrderConfirmStatus(baseDetailResponse.getOrderConfirmStatus());
        }
        return ret;
    }

    /**
     * Description: 获取用户登录log
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user/get-user-login-log")
    public CommonPageRet<UserSecurityLogRet> getLoginLog(@RequestBody @Validated CommonPageArg page) throws Exception {
        CommonPageRet<UserSecurityLogRet> ret = new CommonPageRet<>();
        GetUserSecurityLogResponse getUserSecurityLogResponse;
        if (accountlogSwitch){
            com.binance.accountlog.vo.security.request.GetUserSecurityLogRequest getUserSecurityLogRequest = new com.binance.accountlog.vo.security.request.GetUserSecurityLogRequest();
            getUserSecurityLogRequest.setUserId(getUserId());
            getUserSecurityLogRequest.setLimit(page.getRows());
            getUserSecurityLogRequest.setOffset((page.getPage() - 1) * page.getRows());
            getUserSecurityLogRequest.setOperateType(OperateType.login.name());
            APIResponse<com.binance.accountlog.vo.security.response.GetUserSecurityLogResponse>  apiResponse = accountLogUserSecurityLogApi.getUserSecurityLogList(getInstance(getUserSecurityLogRequest));
            checkResponse(apiResponse);
            getUserSecurityLogResponse = CopyBeanUtils.copy(apiResponse.getData(), GetUserSecurityLogResponse.class);
        } else {
            GetUserSecurityLogRequest getUserSecurityLogRequest = new GetUserSecurityLogRequest();
            getUserSecurityLogRequest.setUserId(getUserId());
            getUserSecurityLogRequest.setLimit(page.getRows());
            getUserSecurityLogRequest.setOffset((page.getPage() - 1) * page.getRows());
            getUserSecurityLogRequest.setOperateType(OperateType.login.name());
            APIResponse<GetUserSecurityLogResponse> apiResponse = userSecurityLogApi.getUserSecurityLogList(getInstance(getUserSecurityLogRequest));
            checkResponse(apiResponse);
            getUserSecurityLogResponse = apiResponse.getData();
        }
        if (getUserSecurityLogResponse != null && !CollectionUtils.isEmpty(getUserSecurityLogResponse.getResult())) {
            ret.setTotal(getUserSecurityLogResponse.getCount());
            ret.setData(ListTransformUtil.transform(getUserSecurityLogResponse.getResult(), UserSecurityLogRet.class));
        }
        return ret;
    }

    /**
     * Description: 获取用户防钓鱼码
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user/get-anti-phishing-code")
    public CommonRet<String> getAntiPhishingCode() throws Exception {
        CommonRet<String> ret = new CommonRet<>();
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(getUserId());
        APIResponse<String> apiResponse = userSecurityApi.selectAntiPhishingCode(getInstance(userIdRequest));
        checkResponse(apiResponse);
        // 返回正常，则对api返回的数据进行裁剪
        ret.setData(MaskUtil.maskPhishingCode(apiResponse.getData()));
        return ret;
    }

    /**
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user/set-commission-status")
    public CommonRet<String> setCommissionStatus(
            @RequestBody @Validated UpdateCommissionStatusArg updateCommissionStatusArg) throws Exception {
        String bnbFeeSwitch = sysConfigHelper.getCodeByDisplayName("bnb_fee_switch");
        // BNB燃烧开关关闭
        if ("0".equals(bnbFeeSwitch) || "off".equalsIgnoreCase(bnbFeeSwitch)
                || "false".equalsIgnoreCase(bnbFeeSwitch)) {
            log.info("BNB燃烧开关bnb_fee_switch未打开");
            throw new BusinessException(GeneralCode.BNB_FEE_CLOSE);
        }

        // 前置校验，判断撮合账户是否开通
        if(accountCheckBeforeUseBnbFee){
            com.binance.account.vo.user.request.UserIdRequest userIdRequest = new com.binance.account.vo.user.request.UserIdRequest();
            userIdRequest.setUserId(getUserIdStr());
            APIResponse<UserInfoVo> userInfoVoResponse = userInfoApi.getUserInfoByUserId(getInstance(userIdRequest));
            checkResponse(userInfoVoResponse);
            //当前账号没有激活
            if(userInfoVoResponse.getData().getTradingAccount() == null){
                throw new BusinessException(AccountErrorCode.PLEASE_DEPOSIT_FIRST);
            }
        }

        // 1. 更新matchbox
        SetGasRequest request = new SetGasRequest();
        request.setIsUseBnbFee(updateCommissionStatusArg.isUseBnbFee());
        request.setUserId(getUserIdStr());
        APIResponse<Void> apiResponse = accountApi.setGas(getInstance(request));
        checkResponse(apiResponse);
        OpenOrCloseBNBFeeRequest openOrCloseBNBFeeRequest = new OpenOrCloseBNBFeeRequest();
        openOrCloseBNBFeeRequest.setUserId(getUserId());
        APIResponse<Integer> accountApiResponse = null;
        if (updateCommissionStatusArg.isUseBnbFee()) {
            // 2. 更新account
            accountApiResponse = userSecurityApi.openBNBFee(getInstance(openOrCloseBNBFeeRequest));
        } else {
            // 2. 更新account
            accountApiResponse = userSecurityApi.closeBNBFee(getInstance(openOrCloseBNBFeeRequest));
        }
        checkResponse(accountApiResponse);
        return new CommonRet<>();
    }

    @PostMapping(value = "/private/account/user/order-confirm-status")
    public CommonRet<Integer> updateOrderConfirmStatus(
            @RequestBody @Validated OrderConfrimStatusArg orderConfrimStatusArg) throws Exception {
        OrderConfrimStatusRequest orderConfrimStatusRequest = new OrderConfrimStatusRequest();
        BeanUtils.copyProperties(orderConfrimStatusArg, orderConfrimStatusRequest);
        orderConfrimStatusRequest.setUserId(getUserId());
        APIResponse<Integer> apiResponse = userApi.updateOrderConfirmStatus(getInstance(orderConfrimStatusRequest));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }



    @PostMapping(value = "/private/account/user/order-confirm-statusV2")
    public CommonRet<Integer> updateOrderConfirmStatusV2(
            @RequestBody @Validated OrderConfirmStatusV2Arg orderConfirmStatus) throws Exception {
        OrderConfirmStatusRequestV2 orderConfirmStatusRequest = new OrderConfirmStatusRequestV2();
        orderConfirmStatusRequest.setUserId(getUserId());
        orderConfirmStatusRequest.setOrderConfirmList(
                orderConfirmStatus.getOrderConfirmList()
                        .stream()
                        .map(e -> CopyBeanUtils.fastCopy(e, OrderConfirmStatusRequestV2.OrderConfirmItem.class))
                        .collect(Collectors.toList())
        );
        APIResponse<Integer> apiResponse = userApi.updateOrderConfirmStatusV2(getInstance(orderConfirmStatusRequest));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }

    /**
     * 设置用户默认配置项
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user-config/save")
    @AccountDefenseResource(name = "UserInfoController.saveUserConfig", paramHint = "configType")
    public CommonRet<String> saveUserConfig(@RequestBody @Validated SaveUserConfigArg saveUserConfigArg)
            throws Exception {
        SetUserConfigRequest request = new SetUserConfigRequest();
        BeanUtils.copyProperties(saveUserConfigArg, request);
        request.setUserId(getUserId());
        if (!CollectionUtils.isEmpty(saveUserConfigLogTypeList) && saveUserConfigLogTypeList.contains(saveUserConfigArg.getConfigType())) {
            return userConfigService.saveUserOperationLogAndConfig(request);
        } else {
            APIResponse<Integer> apiResponse = userInfoApi.saveUserConfig(getInstance(request));
            checkResponse(apiResponse);
        }
        return new CommonRet<>();
    }

    /**
     * 查询用户默认配置项
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user-config/get")
    public CommonRet<List<GetUserConfigRet>> getUserConfig(@RequestBody @Validated GetUserConfigArg getUserConfigArg)
            throws Exception {
        SelectUserConfigRequest request = new SelectUserConfigRequest();
        BeanUtils.copyProperties(getUserConfigArg, request);
        request.setUserId(getUserId());
        APIResponse<List<UserConfigResponse>> apiResponse = userInfoApi.selectUserConfig(getInstance(request));
        checkResponse(apiResponse);
        CommonRet<List<GetUserConfigRet>> ret = new CommonRet<>();
        if (!CollectionUtils.isEmpty(apiResponse.getData())) {
            ret.setData(ListTransformUtil.transform(apiResponse.getData(), GetUserConfigRet.class));
        }
        // 设置推荐
        if (recommendSwitch) {
            userConfigHelper.appendRecConfig(getUserConfigArg, ret);
        }
        return ret;
    }

    /**
     * Description: 创建margin账户
     *
     * @return
     * @throws Exception
     */
    @UserOperation(eventName = "createMarginAccount", name = "创建margin账户", responseKeys = {
            "$.success",},
            responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/private/account/user/createMarginAccount")
    public CommonRet<Map<String,Object>> createMarginAccount() throws Exception {
        CommonRet<Map<String,Object>> ret = new CommonRet<>();
        Long userId = getUserId();
        Map<String,Object> map= Maps.newHashMap();

        // margin 账户创建切流到 margin 接口开关
        if (createMarginAccountSwitch) {
            MarginAccountRequest marginAccountRequest=new MarginAccountRequest();
            marginAccountRequest.setMajorUid(userId);
            log.info("createMarginAccount margin userId={}", userId);
            APIResponse<MarginAccountResponse> apiResponse = marginAccountApi.createMarginAccount(getInstance(marginAccountRequest));
            checkResponse(apiResponse);
            map.put("rootUserId",apiResponse.getData().getRootUserId());
            map.put("marginUserId",apiResponse.getData().getMarginUserId());
        } else {
            // account 接口
            CreateMarginAccountRequest createMarginAccountRequest=new CreateMarginAccountRequest();
            createMarginAccountRequest.setUserId(userId);
            log.info("createMarginAccount account userId={}", userId);
            APIResponse<CreateMarginUserResponse> apiResponse = userApi.createMarginAccount(getInstance(createMarginAccountRequest));
            checkResponse(apiResponse);
            map.put("rootUserId",apiResponse.getData().getRootUserId());
            map.put("marginUserId",apiResponse.getData().getMarginUserId());
        }
        ret.setData(map);
        return ret;
    }


    /**
     * 设置昵称
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user/update-nickname")
    public CommonRet<Void> updateNickName(@Valid @RequestBody UpdateNickNameArg updateNickNameArg) throws Exception {
        String nickNameStr = StringUtil.filterEmoji(updateNickNameArg.getNickName());
        Matcher matcher = NICKNAME_PATTERN.matcher(updateNickNameArg.getNickName());
        if (updateNickNameArg.getNickName().length() > 20 || !matcher.matches() || nickNameStr.length() != updateNickNameArg.getNickName().length()) {
            throw new BusinessException(AccountMgsErrorCode.NICKNAME_NOT_VALID);
        }
        if(userNicknameUpdateSwitch){
            com.binance.account.vo.user.request.UserIdRequest userIdRequest = new com.binance.account.vo.user.request.UserIdRequest();
            userIdRequest.setUserId(String.valueOf(getUserId()));
            APIResponse<UserInfoVo> userInfoVoAPIResponse = userInfoApi.getUserInfoByUserId(getInstance(userIdRequest));
            checkResponse(userInfoVoAPIResponse);
            String nickName = userInfoVoAPIResponse.getData().getNickName();
            if(StringUtils.isNotBlank(nickName)){
                throw new BusinessException(AccountMgsErrorCode.NICKNAME_CANNOT_MODIFY);
            }
        }
        CommonRet<Void> ret = new CommonRet<>();
        UpdateNickNameRequest updateNickNameRequest = new UpdateNickNameRequest();
        BeanUtils.copyProperties(updateNickNameArg, updateNickNameRequest);
        updateNickNameRequest.setUserId(getUserId());
        APIResponse<Integer> apiResponse = userApi.updateNickName(getInstance(updateNickNameRequest));
        checkResponse(apiResponse);
        return ret;
    }


    /**
     * Description: 创建矿池账户
     *
     * @return
     * @throws Exception
     */
    @UserOperation(eventName = "createMiningAccount", name = "创建矿池账户", responseKeys = {
            "$.success",},
            responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/private/account/user/createMiningAccount")
    public CommonRet<Map<String,Object>> createMiningAccount() throws Exception {
        CommonRet<Map<String,Object>> ret = new CommonRet<>();
        CreateMingAccountRequest createMingAccountRequest=new CreateMingAccountRequest();
        createMingAccountRequest.setUserId(getUserId());
        log.info("createMiningAccount userId={}",getUserId());
        APIResponse<CreateMiningUserResponse> apiResponse = userMiningApi.createMiningAccount(getInstance(createMingAccountRequest));
        checkResponse(apiResponse);
        Map<String,Object> map= Maps.newHashMap();
        map.put("rootUserId",apiResponse.getData().getRootUserId());
        map.put("miningUserId",apiResponse.getData().getMiningUserId());
        ret.setData(map);
        return ret;
    }

    /**
     * 签署leverage token协议
     *
     * @return
     * @throws Exception
     */
    @UserOperation(eventName = "signLVTRiskAgreement", name = "签署LVT协议", responseKeys = {
            "$.success",},
            responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/private/account/user/signLVTRiskAgreement")
    public CommonRet<Void> signLVTRiskAgreement() throws Exception {
        CommonRet<Void> ret = new CommonRet<>();
        Long userId = checkAndGetUserId();
        log.info("signLVTRiskAgreement userId={}",userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);
        APIResponse<Boolean> apiResponse = userLVTApi.signLVTRiskAgreement(getInstance(userIdReq));
        log.info("signLVTRiskAgreement response={}", JSONObject.toJSONString(apiResponse));
        if (!baseHelper.isOk(apiResponse)) {
            if (apiResponse.getCode().equals(AccountErrorCode.NOT_ALLOW_SIGNLVT_ERROR.getCode())) {
                throw new BusinessException(AccountMgsErrorCode.NOT_ALLOW_SIGNLVT_ERROR);
            }
            throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
        }
        return ret;
    }

    /**
     * 查询用户财务、kyc、资产等状态
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/user/financeFlag")
    public CommonRet<FinanceFlagResponse> financeFlag() throws Exception {
        CommonRet<FinanceFlagResponse> ret = new CommonRet<>();
        final Long userId = checkAndGetUserId();
        log.info("financeFlag request userId={}",userId);
        if (financeFlagQuerySwitch()) {
            com.binance.userbigdata.vo.user.request.UserIdReq userIdReq = new com.binance.userbigdata.vo.user.request.UserIdReq();
            userIdReq.setUserId(userId);
            APIResponse<FinanceFlagResponse> apiResponse = bigDataUserApi.financeFlag(getInstance(userIdReq));
            log.info("bigDataUserApi.financeFlag userId={} response={}",userId, JSONObject.toJSONString(apiResponse));
            checkResponse(apiResponse);
            ret.setData(apiResponse.getData());
        } else {
            UserIdReq userIdReq = new UserIdReq();
            userIdReq.setUserId(userId);
            APIResponse<com.binance.account.vo.user.response.FinanceFlagResponse> apiResponse = userApi.financeFlag(getInstance(userIdReq));
            log.info("userApi.financeFlag userId={} response={}",userId, JSONObject.toJSONString(apiResponse));
            checkResponse(apiResponse);
            FinanceFlagResponse respData = new FinanceFlagResponse();
            BeanUtils.copyProperties(apiResponse.getData(), respData);
            ret.setData(respData);
        }
        return ret;
    }

    private Boolean financeFlagQuerySwitch() {
        // 生成[1,10]的随机数
        int randomNum = RandomUtils.nextInt(10)+1;
        // 根据配置切流
        return financeFlagQuerySwitch >= randomNum;
    }

    /**
     * 查询用户调查问卷
     */
    @PostMapping(value = "/private/account/user/LVT-SAQ/check")
    public CommonRet<UserSAQResponse> checkLVTSAQ() throws Exception {
        Long userId = checkAndGetUserId();
        log.info("checkLVTSAQ userId={}", userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        APIResponse<UserSAQResponse> apiResponse = userApi.queryLVTSurveyStatus(getInstance(userIdReq));
        checkResponse(apiResponse);

        CommonRet<UserSAQResponse> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    /**
     * 更新用户调查问卷状态
     */
    @PostMapping(value = "/private/account/user/LVT-SAQ/update")
    public CommonRet<Boolean> updateLVTSAQ() throws Exception {
        Long userId = checkAndGetUserId();
        log.info("updateLVTSAQ userId={}", userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        APIResponse<Boolean> apiResponse = userApi.finishLVTSAQ(getInstance(userIdReq));
        checkResponse(apiResponse);

        CommonRet<Boolean> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    /**
     * 查询高风险协议
     */
    @PostMapping(value = "/private/account/user/queryHighRiskAgreement")
    public CommonRet<UserAgreementResponse> queryHighRiskAgreementStatus() throws Exception {
        Long userId = checkAndGetUserId();
        log.info("queryHighRiskAgreement userId={}", userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        APIResponse<UserAgreementResponse> apiResponse = userAgreementApi.queryHighRiskAgreementStatus(getInstance(userIdReq));
        checkResponse(apiResponse);

        CommonRet<UserAgreementResponse> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    /**
     * 签署高风险协议
     */
    @PostMapping(value = "/private/account/user/signHighRiskAgreement")
    public CommonRet<Void> signHighRiskAgreement() throws Exception {
        Long userId = checkAndGetUserId();
        log.info("signHighRiskAgreement userId={}", userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        APIResponse<Boolean> apiResponse = userAgreementApi.signHighRiskAgreementStatus(getInstance(userIdReq));
        checkResponse(apiResponse);

        return new CommonRet<>();
    }

    /**
     * 查询用户是否签署策略交易风险揭示书
     */
    @PostMapping(value = "/private/account/user/queryStrategicTradingRiskAgreement")
    public CommonRet<UserAgreementResponse> queryStrategicTradingRiskAgreementStatus() throws Exception {
        Long userId = checkAndGetUserId();
        log.info("queryStrategicTradingRiskAgreement userId={}", userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        APIResponse<UserAgreementResponse> apiResponse = userAgreementApi.queryStrategicTradingRiskAgreementStatus(getInstance(userIdReq));
        checkResponse(apiResponse);

        CommonRet<UserAgreementResponse> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    /**
     * 策略交易风险揭示书签署状态更新
     */
    @PostMapping(value = "/private/account/user/updateStrategicTradingRiskAgreement")
    public CommonRet<Boolean> updateStrategicTradingRiskAgreementStatus() throws Exception {
        Long userId = checkAndGetUserId();
        log.info("updateStrategicTradingRiskAgreement userId={}", userId);
        UserIdReq userIdReq = new UserIdReq();
        userIdReq.setUserId(userId);

        APIResponse<Boolean> apiResponse = userAgreementApi.updateStrategicTradingRiskAgreementStatus(getInstance(userIdReq));
        checkResponse(apiResponse);

        CommonRet<Boolean> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    /**
     * 查询用户签署状态
     */
    @PostMapping(value = "/private/account/user/queryUserSignatureConfig")
    public CommonRet<UserSignatureConfigResponse> queryUserSignatureConfig(@Valid @RequestBody UserConfigTypeArg userConfigTypeArg) throws Exception {
        Long userId = checkAndGetUserId();
        log.info("Query signature config, userId={},type={},", userId, userConfigTypeArg.getType());

        UserSignatureConfigRequest request = new UserSignatureConfigRequest();
        request.setUserId(userId);
        request.setType(userConfigTypeArg.getType());
        APIResponse<UserSignatureConfigResponse> apiResponse = userConfigApi.queryUserSignatureConfig(getInstance(request));
        checkResponse(apiResponse);

        CommonRet<UserSignatureConfigResponse> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        if (UserConfigTypeEnum.INDIA_BROKER_MASTER_ACCOUNT_AGREEMENT.name().equals(userConfigTypeArg.getType())) {
            UserSpecialSignatureConfigResponse configResponse = new UserSpecialSignatureConfigResponse();
            BeanUtils.copyProperties(apiResponse.getData(), configResponse);
            configResponse.setNeedSign(userKycService.isNeedSignSpecialIssue(userId));
            commonRet.setData(configResponse);
        }
        return commonRet;
    }

    /**
     * 完成签署
     */
    @PostMapping(value = "/private/account/user/finishUserSignatureConfig")
    public CommonRet<Void> finishUserSignatureConfig(@Valid @RequestBody UserConfigTypeArg userConfigTypeArg) throws Exception {
        Long userId = checkAndGetUserId();
        log.info("Finish user signature config, userId={},type={}", userId, userConfigTypeArg.getType());

        UserSignatureConfigRequest request = new UserSignatureConfigRequest();
        request.setUserId(userId);
        request.setType(userConfigTypeArg.getType());
        APIResponse<Boolean> apiResponse = userConfigApi.finishUserSignatureConfig(getInstance(request));
        checkResponse(apiResponse);

        return new CommonRet<>();
    }

    @GetMapping(value = "/private/account/user/cmeFlag")
    public CommonRet<UserCmeFlagRet> userCmeFlag() throws Exception {
        UserCmeFlagRet ret = new UserCmeFlagRet();
        ret.setCmeConfirm(false);
        if (!userCmeConfirmSwitch) {
            return new CommonRet<>(ret);
        }
        Long userId = getUserId();
        String cacheKey = "user_cme_flag_" + userId;
        Integer cache = (Integer) accountMgsRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            ret.setCmeConfirm(cache.intValue() > 0 ? true : false);
            return new CommonRet<>(ret);
        }
        // 1. 确认是否有config配置，如果存在，直接不提示确认
        SelectUserConfigRequest request = new SelectUserConfigRequest();
        request.setConfigType(AccountCommonConstant.USER_CONFIG_CME_FLAG);
        request.setUserId(userId);
        APIResponse<List<UserConfigResponse>> apiResponse = userInfoApi.selectUserConfig(getInstance(request));
        checkResponse(apiResponse);
        if (apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
            accountMgsRedisTemplate.opsForValue().set(cacheKey, -1, 60, TimeUnit.MINUTES);
            return new CommonRet<>(ret);
        }
        // 2. 确认注册时间是否在配置的某一个时间点之前，如果是，不需要考虑提示
        if (StringUtils.isBlank(userCmeConfigBeginTime)) {
            // 如果没有配置时间，直接退出了
            accountMgsRedisTemplate.opsForValue().set(cacheKey, -1, 60, TimeUnit.MINUTES);
            return new CommonRet<>(ret);
        }
        com.binance.account.vo.user.request.UserIdRequest userIdRequest = new com.binance.account.vo.user.request.UserIdRequest();
        userIdRequest.setUserId(userId.toString());
        APIResponse<UserInfoVo> userInfoVoAPIResponse = userInfoApi.getUserInfoByUserId(getInstance(userIdRequest));
        checkResponse(userInfoVoAPIResponse);
        Date createTime = userInfoVoAPIResponse.getData().getInsertTime();
        if (createTime.before(DateUtils.parseDate(userCmeConfigBeginTime, DateUtils.SIMPLE_PATTERN))) {
            saveUserCmeConfirmFlag(userId, "registerBefore");
            accountMgsRedisTemplate.opsForValue().set(cacheKey, -1, 5, TimeUnit.MINUTES);
            return new CommonRet<>(ret);
        }
        // 3. kyc confirm
        UserCmeCheckRequest checkRequest = new UserCmeCheckRequest();
        checkRequest.setUserId(userId);
        APIResponse<UserCmeCheckResponse> kycResponse = kycCertificateApi.userCmeCheck(getInstance(checkRequest));
        checkResponse(kycResponse);
        UserCmeCheckResponse checkResponse = kycResponse.getData();
        if (checkResponse != null) {
            ret.setCmeConfirm(checkResponse.isCmeConfirm());
        }
        accountMgsRedisTemplate.opsForValue().set(cacheKey, checkResponse.isCmeConfirm() ? 1 : -1, 60, TimeUnit.MINUTES);
        return new CommonRet<>(ret);

    }

    private void saveUserCmeConfirmFlag(Long userId, String configName) {
        final String logId = TrackingUtils.getTrace();
        CME_SAVE_EXECUTOR.execute(() -> {
            try {
                TrackingUtils.saveTrace(logId);
                // 保存一次user-config
                SetUserConfigRequest configRequest = new SetUserConfigRequest();
                configRequest.setUserId(userId);
                configRequest.setConfigType(AccountCommonConstant.USER_CONFIG_CME_FLAG);
                configRequest.setConfigName(configName);
                userInfoApi.saveUserConfig(getInstance(configRequest));
            }catch (Exception e) {
                log.error("save user cme config fail. userId:{}", userId, e);
            }finally {
                TrackingUtils.clearTrace();
            }
        });
    }

    @PostMapping(value = "/private/account/user/status")
    public CommonRet<UserStatusRet> getUserStatus() throws Exception {
        CommonRet<UserStatusRet> ret = new CommonRet<>();
        GetUserStatusByUserIdRequest userIdRequest = new GetUserStatusByUserIdRequest();
        userIdRequest.setUserId(getUserId());
        APIResponse<UserStatusEx> apiResponse = userApi.getUserStatusByUserIdFromReadOrWriteDb(getInstance(userIdRequest));
        checkResponse(apiResponse);

        if (apiResponse.getData() != null) {
            UserStatusEx userStatus = apiResponse.getData();
            UserStatusRet userStatusRet = new UserStatusRet();
            BeanUtils.copyProperties(userStatus, userStatusRet);
            userStatusRet.setIsBindEmail(!userStatus.getIsUserNotBindEmail());
            ret.setData(userStatusRet);
        }
        return ret;
    }


    @PostMapping(value = "/private/account/user/register-config-choice")
    public CommonRet<UserRegisterChoiceRet> queryUserRegisterConfigChoice() throws Exception {
        CommonRet<UserRegisterChoiceRet> ret = new CommonRet<>();

        Long userId = checkAndGetUserId();
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<UserRegisterChoiceResponse> apiResponse = userConfigApi.queryUserRegisterConfigChoice(getInstance(userIdRequest));
        checkResponse(apiResponse);
        if (apiResponse.getData() != null) {
            UserRegisterChoiceRet userStatusRet = new UserRegisterChoiceRet();
            BeanUtils.copyProperties(apiResponse.getData(), userStatusRet);
            ret.setData(userStatusRet);
        }
        return ret;
    }

    @GetMapping("/private/account/user/userTransferWallet")
    public CommonRet<UserTransferWalletRet> getUserTransferWallet() throws Exception {
        Long userId = checkAndGetUserId();

        UserIdReq request = new UserIdReq();
        request.setUserId(userId);
        APIResponse<UserTransferWalletResponse> apiResponse = userConfigApi.getUserTransferWallet(getInstance(request));
        checkResponse(apiResponse);

        UserTransferWalletRet userTransferWalletRet = new UserTransferWalletRet();
        if(apiResponse.getData().getUserTransferWallet() == null) {
            // 默认为MBX
            userTransferWalletRet.setUserTransferWallet(UserTransferWalletEnum.MBX);
        } else {
            userTransferWalletRet.setUserTransferWallet(apiResponse.getData().getUserTransferWallet());
        }
        return new CommonRet<>(userTransferWalletRet);
    }

    @PostMapping("/private/account/user/setUserTransferWallet")
    public CommonRet<Void> setUserTransferWallet(@Valid @RequestBody UserTransferWalletSetArg arg) throws Exception {
        Long userId = checkAndGetUserId();

        SetUserTransferWalletRequest request = new SetUserTransferWalletRequest();
        request.setUserId(userId);
        request.setUserTransferWallet(arg.getUserTransferWallet());
        APIResponse<UserTransferWalletResponse> apiResponse = userConfigApi.setUserTransferWallet(getInstance(request));
        checkResponse(apiResponse);

        return new CommonRet<>();
    }

    @PostMapping("/private/account/user/getUserTransferWalletSetLog")
    public CommonPageRet<UserTransferWalletLogRet> getUserTransferWalletSetLog(@RequestBody @Validated CommonPageArg page) throws Exception {
        Long userId = checkAndGetUserId();

        if(page.getRows() > 200) {
            log.warn("getUserTransferWalletSetLog rows is too large, rows: {}", page.getRows());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        com.binance.accountlog.vo.security.request.GetUserSecurityLogRequest getUserSecurityLogRequest = new com.binance.accountlog.vo.security.request.GetUserSecurityLogRequest();
        getUserSecurityLogRequest.setUserId(userId);
        getUserSecurityLogRequest.setLimit(page.getRows());
        getUserSecurityLogRequest.setOffset((page.getPage() - 1) * page.getRows());
        getUserSecurityLogRequest.setOperateType(OperateType.set_user_transfer_wallet.name());
        APIResponse<GetSecurityLogWithRequestResponse> apiResponse = accountLogUserSecurityLogApi.getSecurityLogWithRequestList(getInstance(getUserSecurityLogRequest));
        checkResponse(apiResponse);

        GetSecurityLogWithRequestResponse securityLogWithRequestResponse = apiResponse.getData();
        CommonPageRet<UserTransferWalletLogRet> commonPageRet = new CommonPageRet<>();
        commonPageRet.setTotal(securityLogWithRequestResponse.getCount());
        if(!CollectionUtils.isEmpty(securityLogWithRequestResponse.getResult())) {
            List<UserTransferWalletLogRet> userTransferWalletLogRets = securityLogWithRequestResponse.getResult().stream()
                    .map(userSecurityLogWithRequestVo -> {
                        UserTransferWalletLogRet userTransferWalletLogRet = new UserTransferWalletLogRet();
                        userTransferWalletLogRet.setOperateTime(userSecurityLogWithRequestVo.getOperateTime());

                        if(StringUtils.isNoneEmpty(userSecurityLogWithRequestVo.getRequest())) {
                            JSONObject jsonObject = JSONObject.parseObject(userSecurityLogWithRequestVo.getRequest());
                            userTransferWalletLogRet.setUserTransferWallet(jsonObject.getString("userTransferWallet"));
                        }
                        return userTransferWalletLogRet;
                    })
                    .collect(Collectors.toList());
            commonPageRet.setData(userTransferWalletLogRets);
        }
        return commonPageRet;
    }

    @GetMapping("/private/account/user/queryOneStepWithdrawalInfo")
    public CommonRet<OneStepWithdrawalInfoRet> queryOneStepWithdrawalInfo() throws Exception {
        Long userId = checkAndGetUserId();

        OneStepWithdrawalInfoRet oneStepWithdrawalInfoRet = new OneStepWithdrawalInfoRet();

        QueryOneStepWithdrawalSwitchRequest queryOneStepWithdrawalSwitchRequest = new QueryOneStepWithdrawalSwitchRequest();
        queryOneStepWithdrawalSwitchRequest.setUserId(userId);
        APIResponse<Boolean> oneStepWithdrawalApiResponse = userPersonalConfigApi.queryOneStepWithdrawalSwitch(getInstance(queryOneStepWithdrawalSwitchRequest));
        checkResponse(oneStepWithdrawalApiResponse);

        oneStepWithdrawalInfoRet.setStatus(oneStepWithdrawalApiResponse.getData() == null ? false : oneStepWithdrawalApiResponse.getData());
        if(oneStepWithdrawalInfoRet.getStatus()) {
            // 开启快捷提币，才去查询额度
            QuotaLimitQueryRequest quotaLimitQueryRequest = new QuotaLimitQueryRequest();
            quotaLimitQueryRequest.setUserId(userId);
            quotaLimitQueryRequest.setBizType(ONE_STEP_WITHDRAW_QUOTA_BIZ_TYPE);
            quotaLimitQueryRequest.setSubBizType(ONE_STEP_WITHDRAW_QUOTA_SUB_BIZ_TYPE);
            APIResponse<List<QuotaLimitDetail>> quotaApiResponse = quotaQueryApi.getUserQuotaLimit(getInstance(quotaLimitQueryRequest));
            checkResponse(quotaApiResponse);

            List<QuotaLimitDetail> quotaLimitDetailList = quotaApiResponse.getData();
            if(!CollectionUtils.isEmpty(quotaLimitDetailList)) {
                for(QuotaLimitDetail quotaLimitDetail : quotaLimitDetailList) {
                    if("dailyLimit".equalsIgnoreCase(quotaLimitDetail.getBizKey())) {
                        oneStepWithdrawalInfoRet.setDailyLimit(quotaLimitDetail.getValue());
                    }
                    if("oneTimeLimit".equalsIgnoreCase(quotaLimitDetail.getBizKey())) {
                        oneStepWithdrawalInfoRet.setOneTimeLimit(quotaLimitDetail.getValue());
                    }
                }
            }

        }
        return new CommonRet<>(oneStepWithdrawalInfoRet);
    }

    @PostMapping("/private/account/user/openOneStepWithdrawal")
    @UserOperation(eventName = "openOneStepWithdrawal", name = "开启快捷提币", logDeviceOperation = true,responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<Void> openOneStepWithdrawal(@Valid @RequestBody OneStepWithdrawalOpenArg arg) throws Exception {
        Long userId = checkAndGetUserId();
        if(arg.getOneTimeLimit().compareTo(oneStepWithdrawQuotaDailyLimit) > 0) {
            log.warn("openOneStepWithdrawal oneTimeLimit is too large, oneTimeLimit: {}", arg.getOneTimeLimit());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // 去除末尾的0
        arg.setOneTimeLimit(new BigDecimal(arg.getOneTimeLimit().stripTrailingZeros().toPlainString()));
        if(arg.getOneTimeLimit().scale() > oneStepWithdrawMaxScale) {
            log.warn("modifyOneStepWithdrawalLimit oneTimeLimit scale is too large, oneTimeLimit: {}", arg.getOneTimeLimit());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        // 2fa校验
        verify2fa(userId, BizSceneEnum.CRYPTO_WITHDRAW_FAST, arg);

        // 先保存额度，即使额度保存成功，快捷提币开关保存失败，也不影响业务，用户重试即可
        upsertUserSpecQuota(userId, arg.getOneTimeLimit());

        // 保存 快捷提币开关
        saveOneStepWithdrawalSwitch(userId, true);

        return new CommonRet<>();
    }

    private void verify2fa(Long userId, BizSceneEnum bizSceneEnum, MultiCodeVerifyArg arg) throws Exception {
        VerificationTwoV3Request verificationTwoV3Request = new VerificationTwoV3Request();
        verificationTwoV3Request.setUserId(userId);
        verificationTwoV3Request.setBizScene(bizSceneEnum);
        verificationTwoV3Request.setEmailVerifyCode(arg.getEmailVerifyCode());
        verificationTwoV3Request.setMobileVerifyCode(arg.getMobileVerifyCode());
        verificationTwoV3Request.setGoogleVerifyCode(arg.getGoogleVerifyCode());
        verificationTwoV3Request.setYubikeyVerifyCode(arg.getYubikeyVerifyCode());
        APIResponse<VerificationTwoV3Response> verificationTwoV3ResponseAPIResponse = userSecurityApi.verificationsTwoV3(getInstance(verificationTwoV3Request));
        if (!baseHelper.isOk(verificationTwoV3ResponseAPIResponse)) {
            log.warn("verifyCode is illegal,bizScene: {}, verificationTwoV3ResponseAPIResponse={}", bizSceneEnum, JsonUtils.toJsonNotNullKey(verificationTwoV3ResponseAPIResponse));
            UserOperationHelper.log(ImmutableMap.of("success", Boolean.FALSE.toString()));
            checkResponse(verificationTwoV3ResponseAPIResponse);
        }
    }

    private void upsertUserSpecQuota(Long userId, BigDecimal oneTimeLimit) {
        UserSpecQuotaRuleReq userSpecQuotaRuleReq = new UserSpecQuotaRuleReq();
        userSpecQuotaRuleReq.setUserId(String.valueOf(userId));
        userSpecQuotaRuleReq.setBizType(ONE_STEP_WITHDRAW_QUOTA_BIZ_TYPE);
        userSpecQuotaRuleReq.setSubBizType(ONE_STEP_WITHDRAW_QUOTA_SUB_BIZ_TYPE);
        userSpecQuotaRuleReq.setAsset("BUSD");
        // 底层使用的是upsert接口，无论新增还是更新都需要传所有的额度参数
        userSpecQuotaRuleReq.addLimitDetail(new LimitDetail("dailyLimit", oneStepWithdrawQuotaDailyLimit.toString()));
        userSpecQuotaRuleReq.addLimitDetail(new LimitDetail("oneTimeLimit", oneTimeLimit.toPlainString()));
        userSpecQuotaRuleReq.setOperator(String.valueOf(userId));
        APIResponse<Boolean> quotaApiResponse = quotaManagerUserSpecApi.upsertUserSpec(userSpecQuotaRuleReq);
        log.info("quotaManagerUserSpecApi.upsertUserSpec, request: {}, response: {}", JsonUtils.toJsonNotNullKey(userSpecQuotaRuleReq), JsonUtils.toJsonNotNullKey(quotaApiResponse));
        UserOperationHelper.log(ImmutableMap.of("oneTimeLimit", oneTimeLimit.toPlainString()));
        checkResponse(quotaApiResponse);
    }

    private void saveOneStepWithdrawalSwitch(Long userId, boolean oneStepWithdrawalSwitch) throws Exception {
        SaveOneStepWithdrawalSwitchRequest saveOneStepWithdrawalSwitchRequest = new SaveOneStepWithdrawalSwitchRequest();
        saveOneStepWithdrawalSwitchRequest.setUserId(userId);
        saveOneStepWithdrawalSwitchRequest.setOneStepWithdrawalSwitch(oneStepWithdrawalSwitch);
        APIResponse<Void> oneStepWithdrawalApiResponse = userPersonalConfigApi.saveOneStepWithdrawalSwitch(getInstance(saveOneStepWithdrawalSwitchRequest));
        checkResponse(oneStepWithdrawalApiResponse);
    }

    @PostMapping("/private/account/user/closeOneStepWithdrawal")
    @UserOperation(eventName = "closeOneStepWithdrawal", name = "关闭快捷提币", logDeviceOperation = true, responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<Void> closeOneStepWithdrawal(@Valid @RequestBody OneStepWithdrawalCloseArg arg) throws Exception {
        Long userId = checkAndGetUserId();

        saveOneStepWithdrawalSwitch(userId, false);

        return new CommonRet<>();
    }

    @PostMapping("/private/account/user/modifyOneStepWithdrawalLimit")
    @UserOperation(eventName = "modifyOneStepWithdrawalLimit", name = "修改快捷提币额度", logDeviceOperation = true, responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<Void> modifyOneStepWithdrawalLimit(@Valid @RequestBody OneStepWithdrawalModifyLimitArg arg) throws Exception {
        Long userId = checkAndGetUserId();
        if(arg.getOneTimeLimit().compareTo(oneStepWithdrawQuotaDailyLimit) > 0) {
            log.warn("modifyOneStepWithdrawalLimit oneTimeLimit is too large, oneTimeLimit: {}", arg.getOneTimeLimit());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // 去除末尾的0
        arg.setOneTimeLimit(new BigDecimal(arg.getOneTimeLimit().stripTrailingZeros().toPlainString()));
        if(arg.getOneTimeLimit().scale() > oneStepWithdrawMaxScale) {
            log.warn("modifyOneStepWithdrawalLimit oneTimeLimit scale is too large, oneTimeLimit: {}", arg.getOneTimeLimit());
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        QueryOneStepWithdrawalSwitchRequest queryOneStepWithdrawalSwitchRequest = new QueryOneStepWithdrawalSwitchRequest();
        queryOneStepWithdrawalSwitchRequest.setUserId(userId);
        APIResponse<Boolean> oneStepWithdrawalApiResponse = userPersonalConfigApi.queryOneStepWithdrawalSwitch(getInstance(queryOneStepWithdrawalSwitchRequest));
        checkResponse(oneStepWithdrawalApiResponse);
        boolean oneStepWithdrawalSwitch = oneStepWithdrawalApiResponse.getData() == null ? false : oneStepWithdrawalApiResponse.getData();
        if(!oneStepWithdrawalSwitch) {
            log.warn("modifyOneStepWithdrawalLimit oneStepWithdrawalSwitch is closed, userId: {}", userId);
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        QuotaLimitQueryRequest quotaLimitQueryRequest = new QuotaLimitQueryRequest();
        quotaLimitQueryRequest.setUserId(userId);
        quotaLimitQueryRequest.setBizType(ONE_STEP_WITHDRAW_QUOTA_BIZ_TYPE);
        quotaLimitQueryRequest.setSubBizType(ONE_STEP_WITHDRAW_QUOTA_SUB_BIZ_TYPE);
        APIResponse<List<QuotaLimitDetail>> quotaApiResponse = quotaQueryApi.getUserQuotaLimit(getInstance(quotaLimitQueryRequest));
        checkResponse(quotaApiResponse);

        List<QuotaLimitDetail> quotaLimitDetailList = quotaApiResponse.getData();
        BigDecimal oldOneTimeLimit = new BigDecimal(0);
        if(!CollectionUtils.isEmpty(quotaLimitDetailList)) {
            for(QuotaLimitDetail quotaLimitDetail : quotaLimitDetailList) {
                if("oneTimeLimit".equalsIgnoreCase(quotaLimitDetail.getBizKey())) {
                    oldOneTimeLimit = quotaLimitDetail.getValue();
                }
            }
        }

        if(arg.getOneTimeLimit().compareTo(oldOneTimeLimit) > 0) {
            // 只有单次金额调大才校验2fa
            verify2fa(userId, BizSceneEnum.CRYPTO_WITHDRAW_FAST_LIMIT, arg);
        }

        // 保存额度
        upsertUserSpecQuota(userId, arg.getOneTimeLimit());

        return new CommonRet<>();
    }

    @PostMapping(value = "/private/account/updateResidentCountry")
    @UserOperation(eventName = "updateResidentCountry", name = "部分地区用户重置居住国",
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"})
    public CommonRet<UpdateResidentCountryRet> updateResidentCountry(@RequestBody @Validated UpdateResidentCountryArg request)
            throws Exception {

        UpdateResidentCountryRequest req = new UpdateResidentCountryRequest();
        req.setUserId(getUserId());
        req.setCode(request.getCode());
        APIResponse<UpdateResidentCountryResponse> resp = userConfigApi.updateResidentCountry(APIRequest.instance(req));
        checkResponse(resp);
        return new CommonRet<>();
    }
}
