package com.binance.mgs.nft.deposit;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.cex.wallet.api.deposit.frontend.IChainFrontendAPI;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.req.NftChainAssetQueryPageRequest;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.req.NftContractQueryPageRequest;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.access.AccessControl;
import com.binance.mgs.nft.access.AccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/private/nft/chain-info")
public class ChainInfoController {

    private final BaseHelper baseHelper;
    private final IChainFrontendAPI chainFrontendAPI;

    @AccessControl(event = AccessEvent.DEPOSIT, networkType = "#networkType")
    @GetMapping("/check-if-fetch-sync-able")
    CommonRet<Boolean> checkIfFetchSyncAble(@RequestParam(value = "networkType") String networkType,
                                            @RequestParam(value = "contractAddress") String contractAddress) {

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>();
        }

        APIResponse<Boolean> response = chainFrontendAPI.checkIfFetchAble(networkType, contractAddress);

        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT, networkType = "#request.networkType")
    @PostMapping("/fetch")
    CommonRet<String> fetchChainInfoAsync(@RequestBody FetchChainInfoRequest request) {

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>();
        }

        APIResponse<String> response = chainFrontendAPI.fetchChainInfosAsync(request.getNetworkType(), request.getContractAddress(), request.getTokenIds());

        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT)
    @GetMapping("/result/{id}")
    CommonRet<List<NftChainAssetInfo>> get(@PathVariable("id") String id){
        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>();
        }

        APIResponse<List<NftChainAssetInfo>> response = chainFrontendAPI.get(Long.parseLong(id));

        if (null != response && !CollectionUtils.isEmpty(response.getData())){
            return new CommonRet<>(response.getData());
        }

        return new CommonRet<>();
    }

    @AccessControl(event = AccessEvent.DEPOSIT, networkType = "#request.networkType")
    @PostMapping("/fetch-sync")
    CommonRet<List<NftChainAssetInfo>> fetchChainInfoSync(@RequestBody FetchChainInfoRequest request) {

        final Long userId = baseHelper.getUserId();

        if (null == userId){
            return new CommonRet<>();
        }

        APIResponse<List<NftChainAssetInfo>> response = chainFrontendAPI.fetchChainSync(request.getNetworkType(), request.getContractAddress(),request.getTokenIds());

        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT, networkType = "#networkType")
    @GetMapping("/fetch-contract-async")
    CommonRet<String> fetchContractInfosAsync(@RequestParam(value = "networkType") String networkType,
                                          @RequestParam(value = "walletAddress") String walletAddress) {
        APIResponse<String> response = chainFrontendAPI.fetchContractInfosAsync(networkType, walletAddress);
        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT, networkType = "#request.networkType")
    @PostMapping("/query-contract")
    CommonRet<NftContractResponse> queryContract(@RequestBody NftContractQueryPageRequest request) {
        APIResponse<NftContractResponse> response = chainFrontendAPI.queryContract(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT, networkType = "#request.networkType")
    @PostMapping("/query-asset")
    CommonRet<NftTokenResponse> queryAsset(@RequestBody NftChainAssetQueryPageRequest request) {
        APIResponse<NftTokenResponse> response = chainFrontendAPI.queryAsset(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

}
