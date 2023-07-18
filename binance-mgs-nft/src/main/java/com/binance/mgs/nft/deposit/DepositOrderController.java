package com.binance.mgs.nft.deposit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.access.WhiteListDo;
import com.binance.mgs.nft.deposit.response.DepositBatchResponseVo;
import com.binance.nft.cex.wallet.api.NftCexWalletErrorCode;
import com.binance.mgs.nft.deposit.response.DepositOrderVo;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.DepositCheckResponse;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.DepositOrderResponse;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import org.springframework.util.CollectionUtils;

import com.binance.nft.cex.wallet.api.deposit.frontend.IDepositFrontendAPI;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.req.*;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class DepositOrderController {

    private final BaseHelper baseHelper;
    private final NftInboxHelper nftInboxHelper;
    private final IDepositFrontendAPI depositFrontendAPI;
    private final Config config = ConfigService.getAppConfig();

    @GetMapping("/v1/private/nft/asset-deposit/is-new-deposit-mode-enable")
    CommonRet<Boolean> isNewDepositModeEnable() {
        final Long userId = baseHelper.getUserId();
        APIResponse<Boolean> response = depositFrontendAPI.isNewDepositModeEnable(userId);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/private/nft/asset-deposit/get-num-limit")
    CommonRet<DepositNumLimitResponse> getDepositNumLimit() {
        final Long userId = baseHelper.getUserId();
        DepositNumLimitRequest request = DepositNumLimitRequest.builder().uid(userId).build();
        APIResponse<DepositNumLimitResponse> response = depositFrontendAPI.getDepositNumLimit(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v2/private/nft/asset-deposit/deposit-order")
    CommonRet<Page<DepositOrderVo>> getDepositOrderV2(@RequestParam(value = "status",required = false) Integer status,
                                                      @RequestParam(value = "batchId",required = false) String batchId,
                                                      @RequestParam(value = "createTimeStart",required = false) Long createTimeStart,
                                                      @RequestParam(value = "createTimeEnd",required = false) Long createTimeEnd,
                                                      @RequestParam("page") Integer page,
                                                      @RequestParam("pageSize") Integer pageSize) {
        final Long userId = baseHelper.getUserId();

        DepositOrderQueryRequest request = DepositOrderQueryRequest.builder().userId(userId).batchId(batchId).status(status).createTimeStart(createTimeStart).createTimeEnd(createTimeEnd).page(page).pageSize(pageSize).build();
        APIResponse<Page<DepositOrderResponse>> response = depositFrontendAPI.queryDepositOrder(APIRequest.instance(request));

        Page<DepositOrderVo> retResult = nftInboxHelper.pageResultWithFlag(response.getData(), DepositOrderVo.class,
                DepositOrderVo::getId, Arrays.asList(new BizIdModel(HistoryType.DEPOSITS.name(),response.getData().getRecords().stream().map(d->d.getId()).collect(Collectors.toList()))), DepositOrderVo::setUnreadFlag);
        return new CommonRet<>(retResult);
    }

    @GetMapping("/v1/private/nft/asset-deposit/deposit-batch")
    CommonRet<Page<DepositBatchResponseVo>> getDepositBatch(@RequestParam(value = "status", required = false) Integer status,
                                                            @RequestParam(value = "networkType", required = false) String networkType,
                                                            @RequestParam(value = "createTimeStart", required = false) Long createTimeStart,
                                                            @RequestParam(value = "createTimeEnd", required = false) Long createTimeEnd,
                                                            @RequestParam("page") Integer page,
                                                            @RequestParam("pageSize") Integer pageSize) {
        final Long userId = baseHelper.getUserId();

        DepositBatchQueryRequest request = DepositBatchQueryRequest.builder().userId(userId).status(status).networkType(networkType).createTimeStart(createTimeStart).createTimeEnd(createTimeEnd).page(page).pageSize(pageSize).build();
        APIResponse<Page<DepositBatchResponse>> response = depositFrontendAPI.queryDepositBatch(APIRequest.instance(request));
        Page<DepositBatchResponseVo> result = nftInboxHelper.pageResultWithFlag(response.getData(),DepositBatchResponseVo.class,
                DepositBatchResponseVo::getId, Arrays.asList(new BizIdModel(HistoryType.DEPOSITS.name(),response.getData().getRecords().stream().map(d->d.getId()).collect(Collectors.toList()))), DepositBatchResponseVo::setUnreadFlag);
        return new CommonRet<>(result);
    }

    @PostMapping("/v2/private/nft/asset-deposit/check-if-deposit-able")
    CommonRet<DepositCheckResponse> checkDepositAble(@RequestBody DepositCheckRequest request) {
        final Long userId = baseHelper.getUserId();
        request.setUid(userId);

        APIResponse<DepositCheckResponse> response = depositFrontendAPI.checkIfDepositAble(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/v2/private/nft/asset-deposit/confirm-deposit")
    CommonRet<Boolean> confirmDeposit(@RequestBody DepositConfirmRequest request) {
        final Long userId = baseHelper.getUserId();

        request.setUid(userId);

        APIResponse<Boolean> response = depositFrontendAPI.confirmDeposit(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/v2/private/nft/asset-deposit/check-if-deposit-able-v2")
    CommonRet<DepositCheckV2Response> checkDepositAbleV3(@RequestBody DepositCheckV2Request request) {
        final Long userId = baseHelper.getUserId();
        request.setUid(userId);

        APIResponse<DepositCheckV2Response> response = depositFrontendAPI.checkIfDepositAbleV2(APIRequest.instance(request));

        if (APIResponse.Status.OK.equals(response.getStatus())){
            return new CommonRet<>(response.getData());
        }

        for (NftCexWalletErrorCode errorCode : NftCexWalletErrorCode.values()){
            if (errorCode.getCode().equals(response.getCode())){
                throw new BusinessException(errorCode);
            }
        }

        throw new BusinessException(NftCexWalletErrorCode.SYSTEM_INTERNAL_ERROR);
    }

    @PostMapping("/v2/private/nft/asset-deposit/confirm-deposit-v2")
    CommonRet<Boolean> confirmDepositV3(@RequestBody DepositConfirmV2Request request) {
        final Long userId = baseHelper.getUserId();

        request.setUid(userId);

        APIResponse<Boolean> response = depositFrontendAPI.confirmDepositV2(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/private/nft/asset-deposit/check-deposit-allow")
    CommonRet<Boolean> checkIfDepositAllowed() {

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>(false);
        }

        String depositStr = config.getProperty("nft.access.control.deposit",null);

        if (StringUtils.isEmpty(depositStr)){
            return new CommonRet<>(false);
        }

        WhiteListDo whiteListDo = JsonUtils.toObj(depositStr,WhiteListDo.class);

        return new CommonRet<>(whiteListDo.isGlobalSwitch() || (null != whiteListDo.getUids() && whiteListDo.getUids().contains(userId)));
    }

    @GetMapping("/v2/private/nft/asset-deposit/check-deposit-allow")
    CommonRet<Integer> checkIfDepositAllowedV2() {

        // 1、表示deposit维护; 2、表示充值不可用; 3、表示可以充值
        String depositStr = config.getProperty("nft.access.control.deposit",null);

        if (StringUtils.isEmpty(depositStr)){
            return new CommonRet<>(1);
        }

        WhiteListDo whiteListDo = JsonUtils.toObj(depositStr,WhiteListDo.class);

        if (whiteListDo.isGlobalForbidden()) {
            return new CommonRet<>(1);
        }

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>(2);
        }

        if (whiteListDo.isGlobalSwitch() || (null != whiteListDo.getUids() && whiteListDo.getUids().contains(userId))) {
            return new CommonRet<>(3);
        }

        return new CommonRet<>(2);
    }

    @GetMapping("/v1/private/nft/asset-deposit/get-network-status")
    CommonRet<Map<String, Boolean>> queryNetworkStatus() {

        Map<String, Boolean> networkMap = new HashMap<>();

        String depositStr = config.getProperty("nft.access.control.deposit",null);

        if (StringUtils.isEmpty(depositStr)){
            networkMap.put("ETH", true);
            networkMap.put("BSC", true);
            networkMap.put("Polygon", true);
            return new CommonRet<>(networkMap);
        }

        WhiteListDo whiteListDo = JsonUtils.toObj(depositStr,WhiteListDo.class);

        if (whiteListDo.isGlobalForbidden()) {
            networkMap.put("ETH", true);
            networkMap.put("BSC", true);
            networkMap.put("Polygon", true);
            return new CommonRet<>(networkMap);
        }

        if (!CollectionUtils.isEmpty(whiteListDo.getSuspendedNetwork())) {
            networkMap.put("ETH", false);
            networkMap.put("BSC", false);
            networkMap.put("Polygon", false);
            for (String network : whiteListDo.getSuspendedNetwork()) {
                networkMap.put(network, true);
            }
            return new CommonRet<>(networkMap);
        } else {
            networkMap.put("ETH", false);
            networkMap.put("BSC", false);
            networkMap.put("Polygon", false);
            return new CommonRet<>(networkMap);
        }
    }

    @GetMapping("/v1/public/nft/asset-deposit/target-address/{networkType}")
    CommonRet<String> getTargetAddress(@PathVariable("networkType") String networkType) {
        APIResponse<String> response = depositFrontendAPI.getDepositAddress(networkType);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/public/nft/asset-deposit/contract-address/{networkType}")
    CommonRet<String> getContractAddress(@PathVariable("networkType") String networkType) {
        APIResponse<String> response = depositFrontendAPI.getDepositContractAddress(networkType);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/public/nft/asset-deposit/blockchain/node")
    CommonRet<List<BlockChainNodeResponse>> getBlockChainNode(@RequestParam(value = "networkType", required = false) String networkType){

        return new CommonRet<>(depositFrontendAPI.getBlockChainNode(networkType).getData());
    }

    @GetMapping("/v1/private/nft/asset-deposit/user-deposit-address/get")
    CommonRet<String> getUserDepositAddress() {
        APIResponse<String> response = depositFrontendAPI.getUserDepositAddress(baseHelper.getUserIdStr());
        return new CommonRet<>(response.getData());
    }
}
