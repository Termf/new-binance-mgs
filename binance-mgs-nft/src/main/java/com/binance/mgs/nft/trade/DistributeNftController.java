package com.binance.mgs.nft.trade;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.nft.tradeservice.api.INftDistributeApi;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import com.binance.nft.tradeservice.request.NftDistributeRequest;
import com.binance.nft.tradeservice.response.NftDistributeResponse;
import com.binance.nftcore.utils.Assert;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class DistributeNftController {

    @Resource
    private INftDistributeApi nftDistributeApi;
    @Resource
    private MgsNftProperties mgsNftProperties;
    @Resource
    private BaseHelper baseHelper;

    /**
     * nft分发
     * @return
     */
    @UserOperation(eventName = "NFT_Distribute_Nft", name = "NFT_Distribute_Nft",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/distribute-nft")
    public CommonRet<NftDistributeResponse> create(@Valid @RequestBody NftDistributeRequest request) throws Exception {
        Assert.isTrue(mgsNftProperties.getDistributeWhitelist().contains(request.getDistributeNo()),
                TradeErrorCode.PARAM_ERROR);
        request.setUserId(baseHelper.getUserId());
        request.setRequestId(baseHelper.getUserId());
        APIResponse<NftDistributeResponse> response = nftDistributeApi.distributeNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
