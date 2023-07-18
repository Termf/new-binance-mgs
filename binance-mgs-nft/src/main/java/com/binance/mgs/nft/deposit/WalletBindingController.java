package com.binance.mgs.nft.deposit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.access.AccessControl;
import com.binance.mgs.nft.access.AccessEvent;
import com.binance.mgs.nft.access.KycForDeposit;
import com.binance.mgs.nft.sql.SqlInject;
import com.binance.mgs.nft.twofa.TwoFa;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.bnbgtwservice.common.enums.BusinessSceneEnum;
import com.binance.nft.cex.wallet.api.deposit.frontend.IWalletBindingFrontendAPI;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.req.*;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.WalletBindingInfoResponse;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
public class WalletBindingController {

    private final BaseHelper baseHelper;
    private final IWalletBindingFrontendAPI walletBindingFrontendAPI;

    @AccessControl(event = AccessEvent.DEPOSIT)
    @SqlInject(params = {"walletAddress","nickName"})
    @PostMapping("/private/nft/asset-deposit/wallet-binding/nick-name")
    CommonRet<String> modifyWalletBindingNickName(@RequestBody WalletBindingModifyRequest request) {
        final Long userId = baseHelper.getUserId();

        request.setUserId(userId);

        APIResponse<String> response = walletBindingFrontendAPI.modifyWalletBinding(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT)
    @SqlInject(params = {"walletAddress"})
    @PostMapping("/private/nft/asset-deposit/wallet-binding/deletion")
    CommonRet<String> delWalletBinding(@RequestBody WalletBindingDeleteRequest request) {
        final Long userId = baseHelper.getUserId();

        request.setUserId(userId);

        APIResponse<String> response = walletBindingFrontendAPI.delWalletBinding(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/asset-deposit/wallet-binding/is-binding/wallet-address/{networkType}/{walletAddress}")
    CommonRet<String> getUserIdByWalletAddress(@PathVariable("networkType") String networkType,@PathVariable("walletAddress") String walletAddress) {
        final Long userId = baseHelper.getUserId();

        WalletBindingCheckRequest request = WalletBindingCheckRequest.builder().userId(userId).networkType(networkType).walletAddress(walletAddress).build();
        APIResponse<String> response = walletBindingFrontendAPI.checkWalletBinding(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }


    @AccessControl(event = AccessEvent.DEPOSIT)
    @GetMapping("/private/nft/asset-deposit/wallet-binding/page")
    CommonRet<Page<WalletBindingInfoResponse>> getWalletBindings(@RequestParam(value = "walletAddress", required = false) String walletAddress, @RequestParam("page") Integer page, @RequestParam("pageSize") Integer pageSize) {
        final Long userId = baseHelper.getUserId();

        WalletBindingQueryRequest request = WalletBindingQueryRequest.builder().userId(userId).page(page).pageSize(pageSize).build();
        APIResponse<Page<WalletBindingInfoResponse>> response = walletBindingFrontendAPI.queryWalletBinding(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @AccessControl(event = AccessEvent.DEPOSIT)
    @SqlInject(params = {"message","address"})
    @PostMapping("/private/nft/asset-deposit/deposit/sign-data")
    @UserOperation(
            eventName = "NFT_BindWallet",
            name = "NFT_BindWallet",
            sendToBigData = true,
            sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"}
    )
    @KycForDeposit(scene = BusinessSceneEnum.WITHDRAW)
    @TwoFa(scene = BizSceneEnum.BINANCENFT_WALLET_BINDING)
    CommonRet<String> receiveSignedData(@RequestBody WalletBindingAddRequest request) {
        final Long userId = baseHelper.getUserId();

        request.setUserId(userId);

        APIResponse<String> response = walletBindingFrontendAPI.addWalletBinding(APIRequest.instance(request));

        if (APIResponse.Status.OK.equals(response.getStatus())){
            return new CommonRet<>(response.getData());
        }

        for (NftAssetErrorCode errorCode : NftAssetErrorCode.values()){
            if (errorCode.getCode().equals(response.getCode())){
                throw new BusinessException(errorCode);
            }
        }

        throw new BusinessException(NftAssetErrorCode.BINGING_FAILED);
    }

    @GetMapping("/private/nft/asset-deposit/deposit/message")
    CommonRet<String> getMessage(@RequestParam String walletAddress) {
        final Long userId = baseHelper.getUserId();

        WalletBindingMessageRequest request = WalletBindingMessageRequest.builder().userId(userId).address(walletAddress).build();
        APIResponse<String> message = walletBindingFrontendAPI.getWalletBindingMessage(APIRequest.instance(request));

        return new CommonRet<>(message.getData());
    }
}
