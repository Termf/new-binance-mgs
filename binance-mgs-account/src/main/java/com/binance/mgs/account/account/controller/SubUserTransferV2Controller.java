package com.binance.mgs.account.account.controller;

import com.binance.account.vo.subuser.response.GetrSubUserBindingsResp;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountsubuser.api.SubUserSpotApi;
import com.binance.accountsubuser.core.annotation.RolePermissionCheck;
import com.binance.accountsubuser.core.helper.RolePermissionCheckHelper;
import com.binance.accountsubuser.vo.constants.Constant;
import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import com.binance.accountsubuser.vo.spot.GetSubUserTransferHistoryInfoRequestV2;
import com.binance.accountsubuser.vo.spot.GetSubUserTransferHistoryInfoResponseV2;
import com.binance.accountsubuser.vo.spot.MultiFunctionSubUserTransferRequest;
import com.binance.accountsubuser.vo.spot.MultiFunctionSubUserTransferResponse;
import com.binance.broker.api.BrokerSpotApi;
import com.binance.broker.vo.spot.request.BrokerMultiFunctionTransferRequest;
import com.binance.broker.vo.spot.request.GetBrokerSubUserTransferHistoryInfoRequestV2;
import com.binance.broker.vo.spot.response.BrokerMultiFunctionTransferResponse;
import com.binance.broker.vo.spot.response.GetBrokerSubUserTransferHistoryInfoResponseV2;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.subuser.SubUserInternalTransferArgV2;
import com.binance.mgs.account.account.vo.subuser.SubUserRoleInternalTransferArgV2;
import com.binance.mgs.account.account.vo.subuser.SubUserTransferLogArgV2;
import com.binance.mgs.account.account.vo.subuser.SubUserTransferLogRetV2;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Slf4j
@RestController
@RequestMapping(value = "/v2/private/account/subuser/asset")
public class SubUserTransferV2Controller extends AccountBaseAction {

    @Autowired
    private SubUserSpotApi subUserSpotApi;

    @Autowired
    private BrokerSpotApi brokerSpotApi;


    @ApiOperation(value = "角色账户代理母账户持仓划转")
    @PostMapping(value = "/transfer/internal/subUserInternalTransferByRole")
    @UserOperation(name = "角色账户代理母账户持仓划转", eventName = "subUserInternalTransferByRole", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"},
            requestKeys = {"senderUserId", "recipientUserId", "asset", "amount"},
            requestKeyDisplayNames = {"转出方userId", "转入方userId", "币种", "数量"})
    @DDoSPreMonitor(action = "subUserInternalTransferByRole")// todo ddos配置？
    @RolePermissionCheck(permissionNames = {Constant.EnterpriseBasePermission.ASSET_TRANSFER})
    public CommonRet<String> subUserInternalTransferByRole(@RequestBody @Validated SubUserRoleInternalTransferArgV2 arg, HttpServletResponse resp) throws Exception {
        // 当前登陆账户
        Long currentLoginUserId = checkAndGetUserId();
        Long parentUserId = RolePermissionCheckHelper.getEnterpriseId();

        //当前登陆账户为角色账户，代母账户划转
        Long operatedRoleUserId = null;
        if(!currentLoginUserId.equals(parentUserId)){
            operatedRoleUserId = currentLoginUserId;
        }
        return subUserInternalTransfer(arg, parentUserId, operatedRoleUserId);
    }


    @ApiOperation(value = "母账户持仓划转")
    @PostMapping(value = "/transfer/internal")
    @UserOperation(name = "母账户持仓划转", eventName = "subUserInternalTransfer", responseKeys = {"$.success",},
            responseKeyDisplayNames = {"success"},
            requestKeys = {"senderUserId", "recipientUserId", "asset", "amount"},
            requestKeyDisplayNames = {"转出方userId", "转入方userId", "币种", "数量"})
    @DDoSPreMonitor(action = "subUserInternalTransfer")
    public CommonRet<String> subUserInternalTransfer(@RequestBody @Validated SubUserInternalTransferArgV2 arg, HttpServletResponse resp) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        return subUserInternalTransfer(arg,parentUserId,null);
    }

    private CommonRet<String> subUserInternalTransfer(SubUserInternalTransferArgV2 arg, Long parentUserId, Long operatedRoleUserId) throws Exception {

        if (arg.getRecipientFunctionAccountType() == FunctionAccountType.ISOLATED_MARGIN || arg.getSenderFunctionAccountType() == FunctionAccountType.ISOLATED_MARGIN) {
            if (org.apache.commons.lang3.StringUtils.isBlank(arg.getSymbol())){
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
        }

        // 非空校验
        if (!NumberUtils.isParsable(arg.getSenderUserId()) || !NumberUtils.isParsable(arg.getRecipientUserId()) ||
                StringUtils.isBlank(arg.getAsset()) || !NumberUtils.isParsable(arg.getAmount())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        UserStatusEx userStatusEx= getUserStatusByUserId(parentUserId);
        //如果是broker母账号，那么走broker的接口
        if(userStatusEx.getIsBrokerSubUserFunctionEnabled().booleanValue()){
            BrokerMultiFunctionTransferRequest request = new BrokerMultiFunctionTransferRequest();
            request.setParentUserId(parentUserId);
            if(parentUserId.toString().equalsIgnoreCase(arg.getSenderUserId())){
                request.setFromId(null);
            }else{
                GetrSubUserBindingsResp getrSubUserBindingsResp= getSubBindInfoByParentUserIdAndSubUserId(parentUserId,Long.valueOf(arg.getSenderUserId()));
                request.setFromId(Long.valueOf(getrSubUserBindingsResp.getBrokerSubAccountId()));
            }
            if(parentUserId.toString().equalsIgnoreCase(arg.getRecipientUserId())){
                request.setToId(null);
            }else{
                GetrSubUserBindingsResp getrSubUserBindingsResp= getSubBindInfoByParentUserIdAndSubUserId(parentUserId,Long.valueOf(arg.getRecipientUserId()));
                request.setToId(Long.valueOf(getrSubUserBindingsResp.getBrokerSubAccountId()));
            }
            request.setAsset(arg.getAsset().toUpperCase());
            request.setFromIsolatedMarginSymbol(arg.getSymbol());
            request.setToIsolatedMarginSymbol(arg.getSymbol());
            request.setAmount(new BigDecimal(arg.getAmount()));
            request.setFromFunctionAccountType(com.binance.broker.vo.spot.enums.FunctionAccountType.getByCode(arg.getSenderFunctionAccountType().getAccountType()));
            request.setToFunctionAccountType(com.binance.broker.vo.spot.enums.FunctionAccountType.getByCode(arg.getRecipientFunctionAccountType().getAccountType()));
            request.setOperatedRoleUserId(operatedRoleUserId);
            APIResponse<BrokerMultiFunctionTransferResponse> apiResponse = brokerSpotApi.brokerMultiFunctionAccountTransfer(getInstance(request));
            checkResponse(apiResponse);
            CommonRet<String> commonRet = new CommonRet<String>();
            commonRet.setData(apiResponse.getData().getTxnId().toString());
            //给风控打数据   todo 角色账户
            UserOperationHelper.log("parentUserId", request.getParentUserId());
            UserOperationHelper.log("senderUserId", Long.valueOf(arg.getSenderUserId()));
            UserOperationHelper.log("recipientUserId", Long.valueOf(arg.getRecipientUserId()));
            UserOperationHelper.log("asset", request.getAsset());
            UserOperationHelper.log("amount", request.getAmount());
            if(operatedRoleUserId != null){
                UserOperationHelper.log("operatedRoleUserId", request.getOperatedRoleUserId());
            }
            return commonRet;
        }else{
            MultiFunctionSubUserTransferRequest request = new MultiFunctionSubUserTransferRequest();
            request.setParentUserId(parentUserId);
            request.setSenderUserId(Long.valueOf(arg.getSenderUserId()));
            request.setRecipientUserId(Long.valueOf(arg.getRecipientUserId()));
            request.setAsset(arg.getAsset().toUpperCase());
            request.setSenderIsolatedMarginSymbol(arg.getSymbol());
            request.setRecipientIsolatedMarginSymbol(arg.getSymbol());
            request.setAmount(new BigDecimal(arg.getAmount()));
            request.setSenderFunctionAccountType(arg.getSenderFunctionAccountType());
            request.setRecipientFunctionAccountType(arg.getRecipientFunctionAccountType());
            request.setOperatedRoleUserId(operatedRoleUserId);
            APIResponse<MultiFunctionSubUserTransferResponse> apiResponse = subUserSpotApi. multiFunctionAccountTransfer(getInstance(request));
            checkResponse(apiResponse);
            CommonRet<String> commonRet = new CommonRet<String>();
            commonRet.setData(apiResponse.getData().getTransactionId().toString());
            //给风控打数据
            UserOperationHelper.log("parentUserId", request.getParentUserId());
            UserOperationHelper.log("senderUserId", request.getSenderUserId());
            UserOperationHelper.log("recipientUserId", request.getRecipientUserId());
            UserOperationHelper.log("asset", request.getAsset());
            UserOperationHelper.log("amount", request.getAmount());
            if(operatedRoleUserId != null){
                UserOperationHelper.log("operatedRoleUserId", request.getOperatedRoleUserId());
            }
            return commonRet;
        }
    }


    @ApiOperation(value = "子母账户划转历史")
    @PostMapping(value = "/transfer/log/list")
    public CommonPageRet<SubUserTransferLogRetV2> getSubUsertransferLogList(@RequestBody @Validated SubUserTransferLogArgV2 arg, HttpServletResponse resp) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        Long startTime = arg.getStartTime();
        Long endTime = arg.getEndTime();

        UserStatusEx userStatusEx= getUserStatusByUserId(parentUserId);
        //如果是broker母账号，那么走broker的接口
        if(userStatusEx.getIsBrokerSubUserFunctionEnabled().booleanValue()){
            GetBrokerSubUserTransferHistoryInfoRequestV2 request = new GetBrokerSubUserTransferHistoryInfoRequestV2();
            request.setParentUserId(parentUserId);
            if (StringUtils.isNotBlank(arg.getUserId())) {
                request.setUserId(Long.valueOf(arg.getUserId()));
            }
            request.setTransfers(arg.getTransfers());
            request.setPage(arg.getPage());
            request.setLimit(arg.getRows());
            // 检查开始时间、结束时间
            request.setStartTime(checkAndGetStartTime(startTime, endTime).getTime());
            request.setEndTime(checkAndGetEndTime(endTime).getTime());

            APIResponse<GetBrokerSubUserTransferHistoryInfoResponseV2> apiResponse = brokerSpotApi.getBrokerSubUserTransferHistoryInfoV2(getInstance(request));
            checkResponse(apiResponse);
            CommonPageRet<SubUserTransferLogRetV2> commonPageRet = new CommonPageRet<>();
            List<SubUserTransferLogRetV2> resultList = apiResponse.getData().getResult().stream().map(x -> {
                SubUserTransferLogRetV2 ret = new SubUserTransferLogRetV2();
                BeanUtils.copyProperties(x, ret);
                //新的接口是不存在id的
                if(null!=x.getId()){
                    ret.setId(x.getId().toString());
                }
                ret.setAmount(x.getAmount().toPlainString());
                ret.setTransactionId(x.getTransactionId().toString());
                return ret;
            }).collect(Collectors.toList());
            commonPageRet.setData(resultList);
            commonPageRet.setTotal(apiResponse.getData().getCount());
            return commonPageRet;
        }else{
            GetSubUserTransferHistoryInfoRequestV2 request = new GetSubUserTransferHistoryInfoRequestV2();
            request.setParentUserId(parentUserId);
            if (StringUtils.isNotBlank(arg.getUserId())) {
                request.setUserId(Long.valueOf(arg.getUserId()));
            }
            request.setTransfers(arg.getTransfers());
            request.setPage(arg.getPage());
            request.setLimit(arg.getRows());
            // 检查开始时间、结束时间
            request.setStartTime(startTime);
            request.setEndTime(endTime);
            APIResponse<GetSubUserTransferHistoryInfoResponseV2> apiResponse = subUserSpotApi.getSubUserTransferHistoryInfoV2(getInstance(request));
            checkResponse(apiResponse);
            CommonPageRet<SubUserTransferLogRetV2> commonPageRet = new CommonPageRet<>();
            List<SubUserTransferLogRetV2> resultList = apiResponse.getData().getResult().stream().map(x -> {
                SubUserTransferLogRetV2 ret = new SubUserTransferLogRetV2();
                BeanUtils.copyProperties(x, ret);
                //新的接口是不存在id的
                if(null!=x.getId()){
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


}
