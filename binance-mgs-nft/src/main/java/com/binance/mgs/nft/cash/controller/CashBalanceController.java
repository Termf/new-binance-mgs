package com.binance.mgs.nft.cash.controller;

import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.nftasset.vo.UserAssetBalanceArg;
import com.binance.mgs.nft.nftasset.vo.UserCashBalanceRet;
import com.binance.mgs.nft.reconciliation.helper.ReconciliationHelper;
import com.binance.nft.assetservice.api.ICashBalanceApi;
import com.binance.nft.assetservice.api.data.request.UserCashBalanceRequest;
import com.binance.nft.assetservice.api.data.vo.UserCashBalanceVo;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Api
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
public class CashBalanceController {

    private final BaseHelper baseHelper;
    private final ICashBalanceApi cashBalanceApi;

    private final ReconciliationHelper reconciliationHelper;

    @Value("${nft.mint.activity.fee:0}")
    private BigDecimal ACTIVITY_FEE;

    @PostMapping("/user-asset/balance")
    public CommonRet<UserCashBalanceRet> getUserAssetForMint(@RequestBody UserAssetBalanceArg request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        ReconciliationHelper.ReconciliationDto reconciliation = null;
        if (Objects.nonNull(request.getActionType()) && 0 != request.getActionType()){
            reconciliation = reconciliationHelper.getReconciliationDtoByFeeType(request.getActionType());
        }else {
            reconciliation = reconciliationHelper.getReconciliationDto();
        }
        UserCashBalanceRequest userCashBalanceRequest = CopyBeanUtils.fastCopy(request, UserCashBalanceRequest.class);
        userCashBalanceRequest.setUserId(userId);
        userCashBalanceRequest.setAssetList(
                Collections.singletonList(reconciliation.getCurrency()));
        APIResponse<UserCashBalanceVo> apiResponse = cashBalanceApi.getUserCashBalance(baseHelper.getInstance(userCashBalanceRequest));
        baseHelper.checkResponse(apiResponse);
        UserCashBalanceRet userCashBalanceRet = new UserCashBalanceRet();
        userCashBalanceRet.setFiatName(request.getFiatName());

        userCashBalanceRet.setAsset(reconciliation.getCurrency());

        userCashBalanceRet.setOriginalFee(reconciliation.getAmount().stripTrailingZeros().toPlainString());
        userCashBalanceRet.setActualFee(reconciliation.getAmount()
                .subtract(ACTIVITY_FEE).stripTrailingZeros().toPlainString());
        UserCashBalanceVo balanceVo = apiResponse.getData();
        List<UserCashBalanceVo.AssetBalance> assetBalances = Optional.of(balanceVo)
                .map(item -> item.getAssetBalanceList()).orElse(null);
        if (CollectionUtils.isNotEmpty(assetBalances)) {
            UserCashBalanceVo.AssetBalance assetBalance = assetBalances.get(0);
            userCashBalanceRet.setFree(assetBalance.getFree().stripTrailingZeros().toPlainString());
            userCashBalanceRet.setLogoUrl(assetBalance.getLogoUrl());
        }
        return new CommonRet<>(userCashBalanceRet);
    }

}
