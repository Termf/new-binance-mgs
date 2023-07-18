package com.binance.mgs.account.service;

import com.binance.account.api.SubUserApi;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserMarginApi;
import com.binance.account.vo.margin.request.GetIsolatedMarginUserBySymbolReq;
import com.binance.account.vo.margin.response.GetIsolatedMarginUserBySymbolResp;
import com.binance.account.vo.margin.response.IsolatedMarginUserBindingVo;
import com.binance.account.vo.security.request.GetUserStatusByUserIdRequest;
import com.binance.account.vo.subuser.request.BindingParentSubUserEmailReq;
import com.binance.account.vo.subuser.request.CheckParentAndGetSubUserInfoListRequest;
import com.binance.account.vo.subuser.response.BindingParentSubUserEmailResp;
import com.binance.account.vo.user.enums.UserTypeEnum;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.request.UserIdRequest;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.assetlog.vo.UserAssetLogRequest;
import com.binance.assetlog.vo.UserAssetLogResponse;
import com.binance.account.api.UserInfoApi;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.margin.api.bookkeeper.MarginAccountApi;
import com.binance.margin.api.bookkeeper.dto.MarginAccountDto;
import com.binance.margin.isolated.api.user.UserBridgeApi;
import com.binance.margin.isolated.api.user.response.IsolatedMarginAccountResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.DateUtils;
import com.binance.mgs.account.account.vo.marginRelated.AssetTransferType;
import com.binance.mgs.account.account.vo.marginRelated.MarginCapitalFlowResponseRet;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserCapitalFlowRequest;
import com.binance.mgs.account.account.vo.marginRelated.QueryTradeDetailRequest;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.utils.CommonUtil;
import com.binance.streamer.api.request.trade.QueryTradeDetailsRequest;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author sean w
 * @date 2021/9/29
 **/
@Slf4j
@Service
public class SubUserMarginRelatedService extends BaseAction {

    @Autowired
    private BaseHelper baseHelper;

    @Autowired
    private UserInfoApi userInfoApi;

    @Autowired
    private SubUserApi subUserApi;

    @Resource
    private UserBridgeApi userBridgeApi;

    @Resource
    private UserMarginApi userMarginApi;

    @Resource
    private MarginAccountApi marginAccountApi;

    @Autowired
    private UserApi userApi;

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    /**
     * 根据邮箱获取 全仓marginUserId
     *
     * @param subUserEmail 子账户邮箱
     * @return marginUserId
     */
    public Long getMarginUserIdByEmail(String subUserEmail)  {

        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        // 判断当前是否登陆状态
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }

        // 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
        parentUserId = getRealParentUserId().getLeft();
        parentEmail = getRealParentEmail(parentUserId);

        Long queryUserId = parentUserId;
        //没有子账号邮箱时，默认查母账号的; 参数邮箱也支持输入母账号邮箱
        if (StringUtils.isNotBlank(subUserEmail) && !subUserEmail.equalsIgnoreCase(parentEmail)) {

            // 校验邮箱格式
            if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
            boolean isCommonMerchantSubUser = checkIsCommonMerchantSubUser(getUserIdByEmail(subUserEmail));
            if (!isCommonMerchantSubUser) {
                // 校验所传邮箱和当前账户是否为母子账户信息
                Long subUserId = checkBindRelation(subUserEmail, parentUserId);
                if (Objects.isNull(subUserId)){
                    throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
                }
                queryUserId = subUserId;
            } else {
                log.info("SubUserMarginRelateService getMarginUserId, 当前子账号状态为 isCommonMerchantSubUser = {} ", isCommonMerchantSubUser);
            }
        }

        Long marginUserId = getMarginUserId(queryUserId);
        if (Objects.isNull(marginUserId)){
            throw new BusinessException(AccountMgsErrorCode.USER_NOT_OWN_MARGIN);
        }

        return marginUserId;
    }

    /**
     * 根据邮箱和交易对获取逐仓 ioslatedMarginUserId
     *
     * @param subUserEmail 子账户邮箱
     * @param symbol 交易对
     * @return 逐仓账户Ids
     */
    public List<Long> getIoslatedMarginUserIdByEmail(String subUserEmail, String symbol) throws Exception {

        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        // 判断当前是否登陆状态
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }

        // 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
        parentUserId = getRealParentUserId().getLeft();
        parentEmail = getRealParentEmail(parentUserId);

        Long queryUserId = parentUserId;
        //没有子账号邮箱时，默认查母账号的; 参数邮箱也支持输入母账号邮箱
        if (StringUtils.isNotBlank(subUserEmail) && !subUserEmail.equalsIgnoreCase(parentEmail)) {

            // 校验邮箱格式
            if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }

            boolean isCommonMerchantSubUser = checkIsCommonMerchantSubUser(getUserIdByEmail(subUserEmail));
            if (!isCommonMerchantSubUser) {
                // 校验所传邮箱和当前账户是否为母子账户信息
                Long subUserId = checkBindRelation(subUserEmail, parentUserId);
                if (Objects.isNull(subUserId)){
                    throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
                }
                queryUserId = subUserId;
            } else {
                log.info("SubUserMarginRelateService getIsolatedMarginUserId, 当前子账号状态为 isCommonMerchantSubUser = {} ", isCommonMerchantSubUser);
            }
        }

        APIRequest<GetIsolatedMarginUserBySymbolReq> request = new APIRequest<>();
        GetIsolatedMarginUserBySymbolReq body = new GetIsolatedMarginUserBySymbolReq();
        body.setRootUserId(queryUserId);
        body.setSymbol(symbol);
        request.setBody(body);
        List<Long> list = new ArrayList<>();
        APIResponse<GetIsolatedMarginUserBySymbolResp> isolatedMarginUserBySymbol = userMarginApi.getIsolatedMarginUserBySymbol(request);
        if (isolatedMarginUserBySymbol != null && CollectionUtils.isNotEmpty(isolatedMarginUserBySymbol.getData().getIsolatedMarginUserBindingVos())) {
            for (IsolatedMarginUserBindingVo bindingVo : isolatedMarginUserBySymbol.getData().getIsolatedMarginUserBindingVos()) {
                if (bindingVo.getIsolatedMarginUserId() != null) {
                    list.add(bindingVo.getIsolatedMarginUserId());
                }
            }
        }

        return list;
    }

    /**
     * 根据邮箱获取 subUserId
     *
     * @param subUserEmail 子账户邮箱
     * @return subUserId
     */
    public Long getSubUserId(String subUserEmail) {

        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        // 判断当前是否登陆状态
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        // 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
        parentUserId = getRealParentUserId().getLeft();
        parentEmail = getRealParentEmail(parentUserId);

        Long subUserId = parentUserId;
        //没有子账号邮箱时，默认查母账号的; 参数邮箱也支持输入母账号邮箱
        if (StringUtils.isNotBlank(subUserEmail) && !subUserEmail.equalsIgnoreCase(parentEmail)) {

            // 校验邮箱格式
            if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }

            boolean isCommonMerchantSubUser = checkIsCommonMerchantSubUser(getUserIdByEmail(subUserEmail));
            if (!isCommonMerchantSubUser) {
                // 校验所传邮箱和当前账户是否为母子账户信息
                subUserId = checkBindRelation(subUserEmail, parentUserId);
                if (Objects.isNull(subUserId)){
                    throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
                }
            } else {
                log.info("SubUserMarginRelateService getSubUserId, 当前子账号状态为 isCommonMerchantSubUser = {} ", isCommonMerchantSubUser);
            }
        }

        return subUserId;
    }

    /**
     * 获取资金流水接口参数 uid
     *
     * @param subUserEmail 子账户邮箱
     * @param symbol 交易对
     * @return uid
     */
    public Long getUid(String subUserEmail, String symbol) {

        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        // 判断当前是否登陆状态
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        // 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
        parentUserId = getRealParentUserId().getLeft();
        parentEmail = getRealParentEmail(parentUserId);

        Long subUserId = parentUserId;
        //没有子账号邮箱时，默认查母账号的; 参数邮箱也支持输入母账号邮箱
        if (StringUtils.isNotBlank(subUserEmail) && !subUserEmail.equalsIgnoreCase(parentEmail)) {
            // 校验邮箱格式
            if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }

            boolean isCommonMerchantSubUser = checkIsCommonMerchantSubUser(getUserIdByEmail(subUserEmail));
            if (!isCommonMerchantSubUser) {
                // 校验所传邮箱和当前账户是否为母子账户信息
                subUserId = checkBindRelation(subUserEmail, parentUserId);
                if (Objects.isNull(subUserId)){
                    throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
                }
            } else {
                log.info("SubUserMarginRelateService getMarginUserId, 当前子账号状态为 isCommonMerchantSubUser = {} ", isCommonMerchantSubUser);
            }
        }
        if (StringUtils.isNotBlank(symbol)) {
            APIResponse<IsolatedMarginAccountResponse> apiResponse = userBridgeApi.queryMarginUid(subUserId, symbol);
            baseHelper.checkResponse(apiResponse);
            subUserId = apiResponse.getData().getUid();
        } else {
            APIResponse<MarginAccountDto> apiResponse = marginAccountApi.getMarginAccountByMajorUid(subUserId);
            baseHelper.checkResponse(apiResponse);
            subUserId = apiResponse.getData().getUid();
        }

        return subUserId;
    }

    /**
     * 获取所有的 全仓 margin 账户
     *
     * @param parentId 母账户
     * @return 所有 marginUserId
     */
    public List<Long> getAllMarginUserId(Long parentId) throws Exception {
        List<Long> list = new ArrayList<>();
        APIRequest<CheckParentAndGetSubUserInfoListRequest> request = new APIRequest<>();
        CheckParentAndGetSubUserInfoListRequest body = new CheckParentAndGetSubUserInfoListRequest();
        body.setParentUserId(parentId);
        // 检查并查询母账号的子账号userInfo
        APIResponse<List<UserInfoVo>> apiResponse = subUserApi.checkParentAndGetSubUserInfoList(request);
        if (apiResponse != null && CollectionUtils.isNotEmpty(apiResponse.getData())) {

            for (UserInfoVo userInfoVo : apiResponse.getData()) {

                // marginUserId 不为空, accountType 为 MARGIN
                if (userInfoVo.getMarginUserId() != null && UserTypeEnum.MARGIN.name().equals(userInfoVo.getAccountType())) {

                    list.add(userInfoVo.getMarginUserId());
                }
            }
        }
        return list;
    }

    public UserAssetLogRequest buildRequest(QuerySubUserCapitalFlowRequest request, Long marginUserId) {
        UserAssetLogRequest assetLogRequest = new UserAssetLogRequest();
        assetLogRequest.setUserId(marginUserId);
        assetLogRequest.setStartTime(new Date(request.getStartTime()));
        assetLogRequest.setEndTime(new Date(request.getEndTime()));
        assetLogRequest.setAsset(request.getAsset());
        assetLogRequest.setTypeList(AssetTransferType.getByCapitalFlowType(request.getType(), StringUtils.isNotBlank(request.getSymbol())));
        assetLogRequest.setPage(request.getPage());
        assetLogRequest.setPageSize(request.getRows());
        return assetLogRequest;
    }

    public List<MarginCapitalFlowResponseRet> processData(List<UserAssetLogResponse> rows, String symbol) {
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.EMPTY_LIST;
        }
        return rows.stream().map(rep -> {
            MarginCapitalFlowResponseRet capitalFlowResponse = new MarginCapitalFlowResponseRet();
            capitalFlowResponse.setTranId(rep.getTranId());
            capitalFlowResponse.setTimestamp(rep.getTime().getTime());
            capitalFlowResponse.setAsset(rep.getAsset());
            capitalFlowResponse.setSymbol(symbol);
            AssetTransferType transferType = AssetTransferType.getByType(rep.getType());
            if (transferType != null && transferType.getCapitalFlowType() != null) {
                capitalFlowResponse.setType(transferType.getCapitalFlowType().name());
            }
            capitalFlowResponse.setAmount(rep.getDelta());
            return capitalFlowResponse;
        }).collect(Collectors.toList());
    }

    private Long getMarginUserId(Long subUserId) {

        APIRequest<UserIdRequest> request = new APIRequest<>();
        UserIdRequest body = new UserIdRequest();
        body.setUserId(Long.toString(subUserId));
        request.setBody(body);
        try{

            APIResponse<UserInfoVo> resp = userInfoApi.getUserInfoByUserId(request);
            if (Objects.nonNull(resp)
                    && Objects.equals( APIResponse.Status.OK,resp.getStatus())
                    && Objects.nonNull(resp.getData())
            ){
                return resp.getData().getMarginUserId();
            }
        }catch (BusinessException e){
            throw e;
        }catch (Exception e1){

            log.error("userInfoApi.getUserInfoByUserId error",e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        return null;
    }

    private Long checkBindRelation(String subUserEmail, Long parentUserId) {

        APIRequest<BindingParentSubUserEmailReq> request = new APIRequest<>();
        BindingParentSubUserEmailReq body = new BindingParentSubUserEmailReq();
        body.setParentUserId(parentUserId);
        body.setSubUserEmail(subUserEmail);
        request.setBody(body);

        try {
            //根据母账户ID和子账户email判断是否母子关系并且返回subUserId
            APIResponse<BindingParentSubUserEmailResp> response = subUserApi.checkRelationByParentSubUserEmail(request);
            if (response != null && APIResponse.Status.OK.equals(response.getStatus())) {
                return response.getData().getSubUserId();
            }
            return null;
        } catch (BusinessException e) {
            throw e;
        }catch (Exception e1){

            log.error("subUserApi checkRelationByParentSubUserEmail error", e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    public boolean getMonthSpace(Long startTime, Long endTime) {
        return (DateUtils.addDays(new Date(startTime), 90)).compareTo(new Date(endTime)) < 0;
    }

    private String getRealParentEmail(Long parentUserId) {
        if(parentUserId != null){
            GetUserResponse getUserResponse = getUserById(parentUserId);
            if(getUserResponse.getUser() != null){
                return getUserResponse.getUser().getEmail();
            }
        }
        return null;
    }

    public GetUserResponse getUserById(Long userId) {
        com.binance.account.vo.security.request.UserIdRequest request = new com.binance.account.vo.security.request.UserIdRequest();
        request.setUserId(userId);
        try {
            APIResponse<GetUserResponse> apiResponse = userApi.getUserById(APIRequest.instance(request));
            if (APIResponse.Status.ERROR == apiResponse.getStatus() || null == apiResponse.getData()) {
                log.error("VerifyRelationService UserApiClient.getUserById :userId=" + userId + "  error" + apiResponse.getErrorData());
                throw new BusinessException("getUserById failed");
            }
            return apiResponse.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1) {
            log.error("subUserApi.checkRelationAndFutureAccountEnable error", e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    public QueryTradeDetailsRequest buildTradeDetailRequest(QueryTradeDetailRequest detailRequest) {
        QueryTradeDetailsRequest queryTradeDetailsRequest = new QueryTradeDetailsRequest();
        Long marginUserId = getMarginUserIdByEmail(detailRequest.getEmail());
        queryTradeDetailsRequest.setUserId(marginUserId);
        queryTradeDetailsRequest.setSymbol(detailRequest.getSymbol());
        queryTradeDetailsRequest.setEndTime(detailRequest.getEndTime());
        queryTradeDetailsRequest.setStartTime(detailRequest.getStartTime());
        queryTradeDetailsRequest.setOrderId(detailRequest.getOrderId());
        return queryTradeDetailsRequest;
    }

    public QueryTradeDetailsRequest buildIsolatedTradeDetailRequest(QueryTradeDetailRequest detailRequest) throws Exception{
        QueryTradeDetailsRequest queryTradeDetailsRequest = new QueryTradeDetailsRequest();
        List<Long> isolatedMarginUserIds = getIoslatedMarginUserIdByEmail(detailRequest.getEmail(), detailRequest.getSymbol());
        queryTradeDetailsRequest.setUserIds(isolatedMarginUserIds);
        queryTradeDetailsRequest.setSymbol(detailRequest.getSymbol());
        queryTradeDetailsRequest.setEndTime(detailRequest.getEndTime());
        queryTradeDetailsRequest.setStartTime(detailRequest.getStartTime());
        queryTradeDetailsRequest.setOrderId(detailRequest.getOrderId());
        return queryTradeDetailsRequest;
    }

    private Long getUserIdByEmail(String email) {
        GetUserRequest getUserReq = new GetUserRequest();
        getUserReq.setEmail(email);
        try {
            APIResponse<Long> apiResponse = userApi.getUserIdByEmail(baseHelper.getInstance(getUserReq));
            if (!baseHelper.isOk(apiResponse)) {
                log.info("getUserInfoByEmail error, response = {} ", apiResponse);
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
            return apiResponse.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1) {
            log.error("userApi.getUserStatusByUserIdFromReadOrWriteDb error", e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    private boolean checkIsCommonMerchantSubUser(Long subUserId) {
        GetUserStatusByUserIdRequest userIdRequest = new GetUserStatusByUserIdRequest();
        userIdRequest.setUserId(subUserId);
        try {
            APIResponse<UserStatusEx> apiResponse = userApi.getUserStatusByUserIdFromReadOrWriteDb(baseHelper.getInstance(userIdRequest));
            if (!baseHelper.isOk(apiResponse)) {
                log.info("checkIsCommonMerchantSubUser error, response = {} ", apiResponse);
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
            return apiResponse.getData().getIsCommonMerchantSubUser();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1){
            log.error("userApi.getUserStatusByUserIdFromReadOrWriteDb error",e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    /**
     * 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
     */
    private Pair<Long, Boolean> getRealParentUserId() {
        GetUserResponse userResponse = this.getUserById(baseHelper.getUserId());
        if(userResponse != null && userResponse.getUser() != null ){
            UserStatusEx userStatusEx = new UserStatusEx(userResponse.getUser().getStatus(),userResponse.getUser().getStatusExtra());
            if(userStatusEx.getIsCommonMerchantSubUser()){
                return Pair.of(userResponse.getUserInfo().getParent(),true);
            }
        }
        return Pair.of(baseHelper.getUserId(),false);
    }

    /*public List<Long> getIsolatedUserIdsByAccountTypeAndSymbol(AccountType accountType, @Nullable String symbol) throws Exception {

        if (AccountType.ISOLATED_MARGIN != accountType) {

            return Lists.newArrayList(commonAccountHelper.getUserIdByAccountType(accountType));
        } else {

            if (StringUtil.isNotBlank(symbol)) {
                // 某一交易对和主账号userId获取逐仓userId
                APIResponse<IsolatedMarginAccountResponse> response = userBridgeApi.queryMarginUid(getUserId(), symbol);
                checkResponse(response);
                if (response == null || response.getData() == null) {
                    log.warn("Isolated margin account not exists");
                    throw new BusinessException(MgsErrorCode.ISOLATED_MARGIN_ACCOUNT_NOT_EXISTS);
                }
                return Lists.newArrayList(response.getData().getUid());

            } else {

                // 所有逐仓userIds
                APIResponse<List<IsolatedMarginAccountResponse>> isolatedMarginUserListApiResp = userBridgeApi.queryMarginUid(getUserId());
                checkResponse(isolatedMarginUserListApiResp);
                if (isolatedMarginUserListApiResp == null || CollectionUtils.isEmpty(isolatedMarginUserListApiResp.getData())) {
                    log.warn("Isolated margin account not exists");
                    throw new BusinessException(MgsErrorCode.ISOLATED_MARGIN_ACCOUNT_NOT_EXISTS);
                }
                return isolatedMarginUserListApiResp.getData().stream().map(IsolatedMarginAccountResponse::getUid).collect(Collectors.toList());
            }
        }
    }*/
}
