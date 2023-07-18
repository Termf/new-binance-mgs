package com.binance.mgs.nft.withdraw;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.access.*;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.mgs.nft.sql.SqlInject;
import com.binance.mgs.nft.twofa.TwoFa;
import com.binance.mgs.nft.withdraw.vo.WithdrawBatchItemVo;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.bnbgtwservice.common.enums.BusinessSceneEnum;
import com.binance.nft.cex.wallet.api.withdraw.frontend.IWithdrawAPI;
import com.binance.nft.cex.wallet.api.withdraw.frontend.data.req.WithdrawFeeV2Request;
import com.binance.nft.cex.wallet.api.withdraw.frontend.data.req.WithdrawInfoRequest;
import com.binance.nft.cex.wallet.api.withdraw.frontend.data.req.WithdrawRequest;
import com.binance.nft.cex.wallet.api.withdraw.frontend.data.res.*;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: allen.f
 * @date: 2021/9/13
 **/
@Slf4j
@RequiredArgsConstructor
@RestController
public class WithdrawController {

    private final IWithdrawAPI withdrawAPI;

    private final BaseHelper baseHelper;

    private final NftInboxHelper nftInboxHelper;

    private final Config config = ConfigService.getAppConfig();

    @GetMapping("/v1/private/nft/asset-withdraw/check-withdraw-allow")
    CommonRet<Boolean> checkIfWithdrawAllowed() {

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>(false);
        }

        String withdrawStr = config.getProperty("nft.access.control.withdraw",null);

        if (StringUtils.isEmpty(withdrawStr)){
            return new CommonRet<>(false);
        }

        WhiteListDo whiteListDo = JsonUtils.toObj(withdrawStr,WhiteListDo.class);

        return new CommonRet<>(whiteListDo.isGlobalSwitch() || (null != whiteListDo.getUids() && whiteListDo.getUids().contains(userId)));
    }

    @GetMapping("/v1/private/nft/asset-withdraw/address/{networkType}")
    CommonRet<List<WithdrawAddressVo>> getWithdrawAddresses(@PathVariable("networkType") String networkType){

        final Long userId = baseHelper.getUserId();

        APIResponse<List<WithdrawAddressVo>> response = withdrawAPI.getWithdrawAddresses(userId,networkType);

        return new CommonRet<>(response.getData());
    }



    @UserOperation(eventName = "NFT_Withdraw_Async", name = "NFT_Withdraw_Async",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @KycForDeposit(scene = BusinessSceneEnum.WITHDRAW)
    @TwoFa(scene = BizSceneEnum.BINANCENFT_WITHDRAW)
    @AccessControl(event = AccessEvent.WITHDRAW)
    @SqlInject(params = {"targetAddress","fee","feeAsset","mobileVerifyCode","emailVerifyCode","googleVerifyCode","yubikeyVerifyCode"})
    @PostMapping("/v3/private/nft/asset-withdraw/withdraw-async")
    CommonRet<WithdrawBatchWithRiskVo> withdrawAsyncV3(@RequestBody WithdrawRequest request){

        final Long userId = baseHelper.getUserId();

        request.setUserId(userId);



        log.warn("[withdraw]withdraw async v1(req),user id : {},request : {}.",userId,JsonUtils.toJsonHasNullKey(request));

        APIResponse<WithdrawBatchWithRiskVo> response = withdrawAPI.withdrawSync(APIRequest.instance(request));

        log.warn("[withdraw]withdraw async v1(res),user id : {},request : {},response : {}.",userId,JsonUtils.toJsonHasNullKey(request),JsonUtils.toJsonHasNullKey(response));

        if (APIResponse.Status.OK.equals(response.getStatus())){
            return new CommonRet<>(response.getData());
        }

        for (NftAssetErrorCode errorCode : NftAssetErrorCode.values()){
            if (errorCode.getCode().equals(response.getCode())){
                throw new BusinessException(errorCode);
            }
        }

        throw new BusinessException(NftAssetErrorCode.WITHDRAW_FAILED);
    }

    @AccessControl(event = AccessEvent.WITHDRAW)
    @GetMapping("/v1/private/nft/asset-withdraw/cancel-risk-challenge/{batchNumber}")
    CommonRet<Boolean> cancelRiskChallenge(@PathVariable("batchNumber") Long batchNumber){

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>(false);
        }

        log.warn("[withdraw]withdraw cancel risk challenge,user id : {},batchNumber : {}.",userId,batchNumber);

        withdrawAPI.cancelWithdrawChallengeFlow(userId, batchNumber);

        return new CommonRet<>(true);
    }

    @GetMapping("/v3/private/nft/asset-withdraw/withdraw/orders")
    CommonRet<Page<WithdrawBatchItemVo>> withdrawOrdersV3(@RequestParam("page") Long page,
                                                          @RequestParam("pageSize") Long pageSize,
                                                          @RequestParam(value = "status",required = false) Integer status,
                                                          @RequestParam(value = "createTimeStart",required = false) Long createTimeStart,
                                                          @RequestParam(value = "createTimeEnd",required = false) Long createTimeEnd){

        checkOrdersParamLegal(page,pageSize,createTimeStart,createTimeEnd);

        final Long userId = baseHelper.getUserId();

        APIResponse<Page<WithdrawBatchVo>> response = withdrawAPI.withdrawOrders(
                userId,
                status,
                createTimeStart,
                createTimeEnd,
                page,
                pageSize
        );
        List<BizIdModel> bizIds = Arrays.asList(new BizIdModel(HistoryType.WITHDRAWS.name(), response.getData().getRecords().stream().map(w->w.getBatchId()).collect(Collectors.toList())));
        Page<WithdrawBatchItemVo> retResult = nftInboxHelper.pageResultWithFlag(response.getData(), WithdrawBatchItemVo.class,
                WithdrawBatchItemVo::getBatchId, bizIds, WithdrawBatchItemVo::setUnreadFlag);
        return new CommonRet<>(retResult);
    }

    @AccessControl(event = AccessEvent.WITHDRAW)
    @GetMapping("/v1/private/nft/asset-withdraw/withdraw-details/{batchId}")
    CommonRet<List<WithdrawOrderVo>> withdrawDetails(@PathVariable("batchId") String batchId){

        final Long userId = baseHelper.getUserId();

        APIResponse<List<WithdrawOrderVo>> response = withdrawAPI.withdrawBatchDetails(userId,Long.parseLong(batchId));

        log.warn("[withdraw]withdraw details,user id : {},order id : {},response : {}.",userId,batchId,JsonUtils.toJsonHasNullKey(response));

        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/private/nft/asset-withdraw/withdraw/status")
    CommonRet<Integer> withdrawStatus(){

        final Long userId = baseHelper.getUserId();

        APIResponse<Integer> response = withdrawAPI.withdrawStatus(userId);

        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/public/nft/asset-withdraw/withdraw/fee/{networkType}")
    CommonRet<WithdrawFeeVo> withdrawFee(@PathVariable("networkType") String networkType){

        APIResponse<WithdrawFeeVo> response = withdrawAPI.withdrawFee(networkType);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/v2/public/nft/asset-withdraw/withdraw/fee")
    CommonRet<WithdrawFeeV2Vo> withdrawFee(@RequestBody WithdrawFeeV2Request request){

        APIResponse<WithdrawFeeV2Vo> response = withdrawAPI.withdrawFeeV2(APIRequest.instance(request));

        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/public/nft/asset-withdraw/withdraw/number")
    CommonRet<Integer> withdrawNumber(){

        APIResponse<Integer> response = withdrawAPI.withdrawBatchNumber();

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/v1/public/nft/nft-asset-withdraw/withdraw/all-nft/infos")
    CommonRet<List<WithdrawNftInfoVo>> withdrawAllNftInfos(@RequestBody WithdrawInfoRequest request){

        return new CommonRet<>(withdrawAPI.getAllNftInfos(APIRequest.instance(request)).getData());
    }

    private void checkOrdersParamLegal(Long page, Long pageSize, Long createTimeStart, Long createTimeEnd) {
        if (null == page || page <= 0){
            throw new BusinessException(NftAssetErrorCode.BAD_REQUEST);
        }

        if (null == pageSize || pageSize <= 0){
            throw new BusinessException(NftAssetErrorCode.BAD_REQUEST);
        }

        if (null != createTimeStart && createTimeStart <= 0){
            throw new BusinessException(NftAssetErrorCode.BAD_REQUEST);
        }

        if (null != createTimeEnd && createTimeEnd <= 0){
            throw new BusinessException(NftAssetErrorCode.BAD_REQUEST);
        }
    }
}
