package com.binance.mgs.account.account.controller;

import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.subuser.request.BindingParentSubUserReq;
import com.binance.account.vo.subuser.request.ParentUserIdReq;
import com.binance.account.vo.subuser.request.SubUserAssetBtcRequest;
import com.binance.account.vo.subuser.response.SubUserAssetBtcResponse;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountsubuser.api.FlexLineSubApi;
import com.binance.accountsubuser.api.SubUserAssetApi;
import com.binance.accountsubuser.api.SubUserSpotApi;
import com.binance.accountsubuser.core.annotation.RolePermissionCheck;
import com.binance.accountsubuser.core.helper.RolePermissionCheckHelper;
import com.binance.accountsubuser.vo.asset.request.QueryUserAvailableBalanceListRequest;
import com.binance.accountsubuser.vo.asset.response.QueryUserAvailableBalanceResp;
import com.binance.accountsubuser.vo.constants.Constant;
import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import com.binance.accountsubuser.vo.spot.GetSubUserTransferHistoryByTransIdRequest;
import com.binance.accountsubuser.vo.spot.GetSubUserTransferHistoryByTransIdResponse;
import com.binance.accountsubuser.vo.subuser.request.FlexLineQuerySubUserReq;
import com.binance.accountsubuser.vo.subuser.response.FlexLineQuerySubUserResp;
import com.binance.assetservice.api.IUserAssetApi;
import com.binance.assetservice.vo.request.GetPrivateUserAssetRequest;
import com.binance.assetservice.vo.request.UserAssetTransferBtcRequest;
import com.binance.assetservice.vo.response.UserAssetResponse;
import com.binance.assetservice.vo.response.UserAssetTransferBtcResponse;
import com.binance.broker.api.BrokerSpotApi;
import com.binance.broker.vo.spot.request.GetBrokerSubUserTransferHistoryByTransIdRequest;
import com.binance.broker.vo.spot.response.GetBrokerSubUserTransferHistoryByTransIdResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.controller.client.SubUserClient;
import com.binance.mgs.account.account.vo.subuser.SubUserAssetDetailsArg;
import com.binance.mgs.account.account.vo.subuser.SubUserAssetInfoArg;
import com.binance.mgs.account.account.vo.subuser.SubUserAssetListRet;
import com.binance.mgs.account.account.vo.subuser.SubUserAssetRet;
import com.binance.mgs.account.account.vo.subuser.SubUserTransferLogRet;
import com.binance.mgs.account.account.vo.subuser.SubUserTxnIdArg;
import com.binance.mgs.account.account.vo.subuser.UserAssetRet;
import com.binance.mgs.account.account.vo.subuser.UserAvailableBalanceListArg;
import com.binance.mgs.account.account.vo.subuser.UserAvailableBalanceListRet;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by pcx
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/private/account/subuser/asset")
public class SubUserAssetController extends AccountBaseAction {

    @Autowired
    private SubUserClient subUserClient;
    @Autowired
    private SubUserSpotApi subUserSpotApi;
    @Autowired
    private BrokerSpotApi brokerSpotApi;
    @Autowired
    private IUserAssetApi userAssetApi;
    @Autowired
    private SubUserAssetApi subUserAssetApi;
    @Autowired
    private FlexLineSubApi flexLineSubApi;

    @Value("${sub.user.get.all.flexline.parent:true}")
    private Boolean querySubUserToGetAllFlexLineParentId;

    @ApiOperation(value = "根据TransactionId获取子母账户划转历史")
    @PostMapping(value = "/transfer/log/txn-id")
    public CommonRet<SubUserTransferLogRet> getTransferById(@RequestBody @Validated SubUserTxnIdArg arg, HttpServletResponse resp) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();


        UserStatusEx userStatusEx= getUserStatusByUserId(parentUserId);
        //如果是broker母账号，那么走broker的接口
        if(userStatusEx.getIsBrokerSubUserFunctionEnabled().booleanValue()){
            GetBrokerSubUserTransferHistoryByTransIdRequest request = new GetBrokerSubUserTransferHistoryByTransIdRequest();
            request.setParentUserId(parentUserId);
            request.setTranId(arg.getTxnId());
            request.setForceQueryMasterDb(true);
            APIResponse<GetBrokerSubUserTransferHistoryByTransIdResponse> apiResponse = brokerSpotApi.getBrokerSubUserTransferHistoryByTransId(getInstance(request));
            checkResponse(apiResponse);
            SubUserTransferLogRet ret = new SubUserTransferLogRet();
            //如果是空直接返回
            if(null==apiResponse.getData()){
                return new CommonRet(ret);
            }
            BeanUtils.copyProperties(apiResponse.getData(), ret);
            ret.setAmount(apiResponse.getData().getAmount().toPlainString());
            //新的接口是不存在id的
            if(null!=apiResponse.getData().getId()){
                ret.setId(apiResponse.getData().getId().toString());
            }
            ret.setTransactionId(apiResponse.getData().getTransactionId().toString());
            return new CommonRet(ret);
        }else{
            GetSubUserTransferHistoryByTransIdRequest request = new GetSubUserTransferHistoryByTransIdRequest();
            request.setParentUserId(parentUserId);
            request.setTranId(arg.getTxnId());
            request.setForceQueryMasterDb(true);
            APIResponse<GetSubUserTransferHistoryByTransIdResponse> apiResponse = subUserSpotApi.getSubUserTransferHistoryByTransId(getInstance(request));
            checkResponse(apiResponse);
            SubUserTransferLogRet ret = new SubUserTransferLogRet();
            //如果是空直接返回
            if(null==apiResponse.getData()){
                return new CommonRet(ret);
            }
            BeanUtils.copyProperties(apiResponse.getData(), ret);
            ret.setAmount(apiResponse.getData().getAmount().toPlainString());
            //新的接口是不存在id的
            if(null!=apiResponse.getData().getId()){
                ret.setId(apiResponse.getData().getId().toString());
            }
            ret.setTransactionId(apiResponse.getData().getTransactionId().toString());
            return new CommonRet(ret);
        }
    }

    @ApiOperation(value = "子账户现货资产列表及BTC总值")
    @PostMapping(value = "/sub-user-spot/btc/list")
    public CommonPageRet<SubUserAssetRet> getSubUserSpotAsset2BtcList(@RequestBody @Validated SubUserAssetInfoArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();

        SubUserAssetBtcRequest request = new SubUserAssetBtcRequest();
        request.setParentUserId(parentUserId);
        request.setEmail(arg.getEmail());
        request.setIsSubUserEnabled(arg.getIsSubUserEnabled());
        request.setPage(arg.getPage());
        request.setLimit(arg.getRows());
        APIResponse<SubUserAssetBtcResponse> apiResponse = subUserSpotApi.subUserAssetBtcList(getInstance(request));
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


        CommonPageRet<SubUserAssetRet> commonPageRet = new CommonPageRet<>();
        List<SubUserAssetRet> result = apiResponse.getData().getResult().stream().map(assetBtcVo -> {
            SubUserAssetRet ret = new SubUserAssetRet();
            ret.setSubUserId(assetBtcVo.getUserId().toString());
            ret.setEmail(assetBtcVo.getEmail());
            ret.setIsSubUserEnabled(assetBtcVo.getIsSubUserEnabled());
            ret.setIsAssetSubUser(assetBtcVo.getIsAssetSubUser());
            ret.setIsAssetSubUserEnabled(assetBtcVo.getIsAssetSubUserEnabled());
            ret.setIsManagerSubUser(assetBtcVo.getIsManagerSubUser());
            ret.setTotalAsset(assetBtcVo.getTotalAsset().toPlainString());
            if(finalFlexLineSubData.getCreditSubUserId()!=null){
                if(Objects.equals(assetBtcVo.getUserId(), finalFlexLineSubData.getCreditSubUserId())){
                    ret.setIsFlexLineCreditUser(true);
                }
            }
            if (CollectionUtils.isNotEmpty(finalFlexLineSubData.getTradingSubUserIds())){
                if(finalFlexLineSubData.getTradingSubUserIds().contains(assetBtcVo.getUserId())){
                    ret.setIsFlexLineTradingUser(true);
                }
            }
            return ret;
        }).collect(Collectors.toList());
        commonPageRet.setData(result);
        commonPageRet.setTotal(apiResponse.getData().getCount());
        return commonPageRet;
    }

    @ApiOperation(value = "母账户下所有子账户现货总资产(折合BTC)")
    @PostMapping(value = "/sub-user-spot/btc")
    public CommonRet<String> getAllSubUserSpotAssetToBtc() throws Exception {
        Long parentUserId = checkAndGetUserId();

        ParentUserIdReq request = new ParentUserIdReq();
        request.setParentUserId(parentUserId);
        APIResponse<BigDecimal> apiResponse = subUserSpotApi.allSubUserAssetBtc(getInstance(request));
        checkResponse(apiResponse);
        CommonRet<String> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData().toPlainString());
        return commonRet;
    }

    @ApiOperation(value = "查询账户SPOT/FUTURE/DELIVERY有资产币种的可用余额")
    @PostMapping(value = "/coin/list")
    @RolePermissionCheck(permissionNames = {Constant.EnterpriseBasePermission.ASSET_TRANSFER})
    public CommonRet<List<UserAvailableBalanceListRet>> getSubUserCoinBalanceList(@RequestBody UserAvailableBalanceListArg arg) throws Exception {
        checkAndGetUserId();
        Long parentUserId = RolePermissionCheckHelper.getEnterpriseId();
        FunctionAccountType accountType = arg.getType();
        String symbol = arg.getSymbol();
        if (accountType.equals(FunctionAccountType.ISOLATED_MARGIN)&& StringUtils.isBlank(symbol)){
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        // check parent user status.
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(parentUserId);
        APIResponse<UserStatusEx> parentUserStatusResp = userApi.getUserStatusByUserId(APIRequest.instance(userIdRequest));

        checkResponse(parentUserStatusResp);
        UserStatusEx userStatusEx = parentUserStatusResp.getData();
        if (!userStatusEx.getIsSubUserFunctionEnabled()) {
            throw new BusinessException(GeneralCode.SUB_UER_FUNCTION_NOT_ENABLED);
        }

        CommonRet<List<UserAvailableBalanceListRet>> commonRet = new CommonRet<>();

        QueryUserAvailableBalanceListRequest req = new QueryUserAvailableBalanceListRequest();
        req.setEmail(arg.getEmail());
        req.setParentUserId(parentUserId);
        req.setType(accountType);
        req.setToAccountType(arg.getToAccountType());
        req.setSymbol(symbol);
        APIResponse<List<QueryUserAvailableBalanceResp>> response = subUserAssetApi.queryUserAvailableBalanceList(APIRequest.instance(req));
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

    @ApiOperation(value = "母账户获取子账户现货资产详情")
    @PostMapping(value = "/sub-user-spot/details")
    public CommonRet<List<SubUserAssetListRet>> getSubUserSpotAssetDetails(@Valid @RequestBody SubUserAssetDetailsArg userAssetArg) throws Exception {
        Long parentUserId = checkAndGetUserId();

        // 验一下登录账户是母账户，以及子母绑定关系即可
        BindingParentSubUserReq req = new BindingParentSubUserReq();
        req.setParentUserId(parentUserId);
        req.setSubUserId(Long.valueOf(userAssetArg.getSubUserId()));
        APIResponse<Boolean> apiResponse = subUserClient.checkRelationByParentSubUserIds(APIRequest.instance(req));
        checkResponse(apiResponse);
        if (!apiResponse.getData()) {
            log.info("checkRelationByParentSubUserIds result is false,parentUserId={},subUserId={}", parentUserId, userAssetArg.getSubUserId());
            throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
        }

        String subUserId = userAssetArg.getSubUserId();
        GetPrivateUserAssetRequest getPrivateUserAssetRequest = new GetPrivateUserAssetRequest();
        getPrivateUserAssetRequest.setAsset(userAssetArg.getAsset());
        getPrivateUserAssetRequest.setUserId(subUserId);

        List<UserAssetRet> userAssetRets = getUserSpotAsset(getPrivateUserAssetRequest);
        CommonRet<List<SubUserAssetListRet>> subUserAssetRetCmn = new CommonPageRet<>();
        List<SubUserAssetListRet> subUserAssetRets = new ArrayList<>();
        for (UserAssetRet userAssetRet : userAssetRets) {
            SubUserAssetListRet subUserAssetRet = new SubUserAssetListRet();
            BeanUtils.copyProperties(userAssetRet, subUserAssetRet);
            subUserAssetRets.add(subUserAssetRet);
        }
        UserAssetTransferBtcRequest request = new UserAssetTransferBtcRequest();
        request.setUserId(subUserId);
        APIResponse<UserAssetTransferBtcResponse> apiResp = userAssetApi.userAssetTransferBtc(getInstance(request));
        checkResponse(apiResp);
        UserAssetTransferBtcResponse apiData = apiResp.getData();
        if (apiData != null && CollectionUtils.isNotEmpty(apiData.getAssetTransferBtcList())) {
            List<UserAssetTransferBtcResponse.AssetTransferBtc> assetTransferBtcList = apiData.getAssetTransferBtcList();
            for (UserAssetTransferBtcResponse.AssetTransferBtc assetTransferBtc : assetTransferBtcList) {
                for (SubUserAssetListRet subUserAssetRet : subUserAssetRets) {
                    if (subUserAssetRet.getAsset().equalsIgnoreCase(assetTransferBtc.getAsset())) {
                        subUserAssetRet.setBtcValue(assetTransferBtc.getTransferBtc());
                    }
                }
            }
        }
        subUserAssetRetCmn.setData(subUserAssetRets);
        return subUserAssetRetCmn;
    }

    private List<UserAssetRet> getUserSpotAsset(GetPrivateUserAssetRequest getPrivateUserAssetRequest) throws Exception {
        APIResponse<UserAssetResponse> apiResponse = userAssetApi.getPrivateUserAsset(getInstance(getPrivateUserAssetRequest));
        checkResponseWithoutLog(apiResponse);
        List<UserAssetRet> userAssetRets = Lists.newArrayList();
        UserAssetResponse apiData = apiResponse.getData();
        if (apiData != null && CollectionUtils.isNotEmpty(apiData.getUserAssetList())) {
            String language = baseHelper.getLanguage();
            apiData.getUserAssetList().forEach(userAssetResponse -> {
                UserAssetRet userAssetRet = new UserAssetRet();
                BeanUtils.copyProperties(userAssetResponse, userAssetRet);
                if (userAssetResponse.getSameAddress()) {
                    userAssetRet.setAssetLabel("cn".equals(language) ? userAssetResponse.getAssetLabel() : userAssetResponse.getAssetLabelEn());
                }
                if (userAssetResponse.getDepositTipStatus()) {
                    userAssetRet.setDepositTip("cn".equals(language) ? userAssetResponse.getDepositTipCn() : userAssetResponse.getDepositTipEn());
                }
                if (userAssetResponse.getForceStatus()) {
                    userAssetRet.setAssetDetail("cn".equals(language) ? userAssetResponse.getChargeDescCn() : userAssetResponse.getChargeDescEn());
                }
                userAssetRets.add(userAssetRet);
            });
        }
        return userAssetRets;
    }
}

