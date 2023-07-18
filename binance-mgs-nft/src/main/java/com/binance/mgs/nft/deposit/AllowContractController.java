package com.binance.mgs.nft.deposit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.cex.wallet.api.deposit.frontend.IContractFrontendAPI;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.req.*;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.*;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.nft.mintservice.api.iface.NFTContractApi;
import com.binance.nft.mintservice.api.vo.CommonPageRequest;
import com.binance.nft.mintservice.api.vo.CommonPageResponse;
import com.binance.nft.mintservice.api.vo.NFTContractVo;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class AllowContractController {

    private final BaseHelper baseHelper;
    private final NFTContractApi nftContractApi;
    private final IContractFrontendAPI contractFrontendAPI;

    @GetMapping("/v1/public/nft/asset-deposit/allow-contract/{networkType}/{address}")
    @UserOperation(
            eventName = "NFT_Deposit_White",
            name = "NFT_Deposit_White",
            sendToBigData = true,
            sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"}
    )
    CommonRet<AllowContractInfoResponse> getExactAllowContract(@PathVariable("networkType") String networkType, @PathVariable("address") String address) {
        AllowContractQueryRequest request = AllowContractQueryRequest.builder().networkType(networkType).contractAddress(address).build();
        APIResponse<List<AllowContractInfoResponse>> response = contractFrontendAPI.getContractInfo(APIRequest.instance(request));

        if(null != response && !CollectionUtils.isEmpty(response.getData())) {
            return new CommonRet<>(response.getData().get(0));
        }

        NFTContractVo nftContractVo = NFTContractVo.builder().network(networkType).address(address).inUse(Boolean.TRUE).build();
        APIResponse<CommonPageResponse<NFTContractVo>> apiResponse = nftContractApi.searchContract(APIRequest.instance(CommonPageRequest.<NFTContractVo>builder().params(nftContractVo).build()));
        if(null != apiResponse.getData() && apiResponse.getData().getData().size() > 0) {
            NFTContractVo no = apiResponse.getData().getData().get(0);
            return new CommonRet<>(AllowContractInfoResponse.builder().contractName(no.getName()).networkType(no.getNetwork()).contractAddress(no.getAddress()).build());
        }

        return new CommonRet<>();
    }

    @GetMapping("/v1/public/nft/asset-deposit/allow-contract/query/{networkType}/{contractName}")
    @UserOperation(
            eventName = "NFT_Deposit_White",
            name = "NFT_Deposit_White",
            sendToBigData = true,
            sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"}
    )
    CommonRet<List<AllowContractInfoResponse>> getAllowContractWithMint(@PathVariable("networkType") String networkType,
                                                                     @PathVariable("contractName") String contractName) {
        AllowContractQueryRequest request = AllowContractQueryRequest.builder().networkType(networkType).contractName(contractName).build();
        APIResponse<List<AllowContractInfoResponse>> response = contractFrontendAPI.getContractInfo(APIRequest.instance(request));

        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/private/nft/asset-deposit/allow-contract/query-used")
    @UserOperation(
            eventName = "NFT_Deposit_White",
            name = "NFT_Deposit_White",
            sendToBigData = true,
            sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"}
    )
    CommonRet<List<AllowContractInfoResponse>> getUsedContract(@RequestParam("networkType") String networkType) {

        Long userId = baseHelper.getUserId();

        AllowContractQueryRequest request = AllowContractQueryRequest.builder().networkType(networkType).userId(userId).build();
        APIResponse<List<AllowContractInfoResponse>> response = contractFrontendAPI.getUsedContractInfo(APIRequest.instance(request));

        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v1/public/nft/asset-deposit/allow-contract/query-list")
    @UserOperation(
            eventName = "NFT_Deposit_White",
            name = "NFT_Deposit_White",
            sendToBigData = true,
            sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"}
    )
    CommonRet<List<AllowContractInfoResponse>> getOpenedContract(@RequestParam("networkType") String networkType,
                                                                 @RequestParam(required = false,value = "contractName") String contractName,
                                                                 @RequestParam(required = false,value = "contractAddress") String contractAddress) {
        AllowContractQueryRequest request = AllowContractQueryRequest.builder().networkType(networkType).contractName(contractName).contractAddress(contractAddress).build();
        APIResponse<List<AllowContractInfoResponse>> response = contractFrontendAPI.getContractInfo(APIRequest.instance(request));

        return new CommonRet<>(response.getData());
    }

    @GetMapping("/v2/public/nft/asset-deposit/allow-contract/query-list")
    @UserOperation(
            eventName = "NFT_Deposit_White",
            name = "NFT_Deposit_White",
            sendToBigData = true,
            sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"}
    )
    CommonRet<Page<AllowContractInfoResponse>> getOpenedContractV2(@RequestParam("networkType") String networkType,
                                                                   @RequestParam(required = false,value = "contractName") String contractName,
                                                                   @RequestParam(required = false,value = "contractAddress") String contractAddress,
                                                                   @RequestParam(value = "page") Integer page,
                                                                   @RequestParam(value = "pageSize") Integer pageSize) {
        AllowContractQueryV3Request request = AllowContractQueryV3Request.builder().networkType(networkType).contractName(contractName).contractAddress(contractAddress).page(page).pageSize(pageSize).build();
        APIResponse<Page<AllowContractInfoResponse>> response = contractFrontendAPI.getContractInfoV3(APIRequest.instance(request));

        return new CommonRet<>(response.getData());
    }
}
