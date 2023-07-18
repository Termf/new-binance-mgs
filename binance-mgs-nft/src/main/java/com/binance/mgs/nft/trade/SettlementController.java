package com.binance.mgs.nft.trade;

import com.binance.master.models.APIResponse;
import com.binance.nft.tradeservice.api.ISettlementApi;
import com.binance.nft.tradeservice.response.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class SettlementController {

    @Resource
    private ISettlementApi iSettlementApi;

    @Resource
    private BaseHelper baseHelper;

    @GetMapping("/private/nft/nft-trade/seller-total-fee")
    APIResponse<SellerTotalFeeResponse> sellerTotalFee() {
        Long userAccount = baseHelper.getUserId();
        APIResponse<SellerTotalFeeResponse> response = iSettlementApi.sellerTotalFee(userAccount);
        baseHelper.checkResponse(response);

        return response;
    }
}
