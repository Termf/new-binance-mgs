package com.binance.mgs.nft.trade;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.nft.paymentservice.api.iface.IPaymentChannelApi;
import com.binance.nft.paymentservice.api.request.BatchTradeEncryptionRequest;
import com.binance.nft.paymentservice.api.request.EncryptionRequest;
import com.binance.nft.paymentservice.api.response.EncryptionResponse;
import com.binance.nft.tradeservice.api.IOpenListOnSaleApi;
import com.binance.nft.tradeservice.request.openlist.*;
import com.binance.nft.tradeservice.response.openlist.OnListProgressResponse;
import com.binance.nft.tradeservice.response.openlist.OpenListOnSaleBatchResponse;
import com.binance.nft.tradeservice.response.openlist.OpenListOnSaleSingleResponse;
import com.binance.nft.tradeservice.response.openlist.PreListRiskConsultResponse;
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
public class OpenListOnSaleController {
    @Resource
    private BaseHelper baseHelper;

    @Resource
    private IOpenListOnSaleApi openListOnSaleApi;

    @Resource
    private IPaymentChannelApi paymentChannelApi;

    /**
     * pre list
     *
     * @return
     */
    @GoogleRecaptha("private/nft/nft-trade/pre-list")
    @PostMapping("/private/nft/nft-trade/pre-list")
    @UserOperation(eventName = "open_list_risk_consult", name = "open_list_risk_consult", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code", "$.message", "$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "data", "errorMessage", "errorCode"})
    public CommonRet<PreListRiskConsultResponse> preListRiskConsult(@Valid @RequestBody PreListRiskConsultRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<PreListRiskConsultResponse> response = openListOnSaleApi.onSaleRiskConsult(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * single list
     *
     * @return
     */
    @GoogleRecaptha("private/nft/nft-trade/onsale")
    @PostMapping("/private/nft/nft-trade/onsale")
    @UserOperation(eventName = "open_list_single_list", name = "open_list_single_list", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code", "$.message", "$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "data", "errorMessage", "errorCode"})
    public CommonRet<OpenListOnSaleSingleResponse> singleList(@Valid @RequestBody SingleProductOnListRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<OpenListOnSaleSingleResponse> response = openListOnSaleApi.onSaleSingleApply(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * batch list
     *
     * @return
     */
    @GoogleRecaptha("private/nft/nft-trade/onsale-batch-list")
    @PostMapping("/private/nft/nft-trade/onsale-batch-list")
    @UserOperation(eventName = "open_list_batch_list", name = "open_list_batch_list", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code", "$.message", "$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "data", "errorMessage", "errorCode"})
    public CommonRet<OpenListOnSaleBatchResponse> batchList(@Valid @RequestBody BatchProductOnListRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<OpenListOnSaleBatchResponse> response = openListOnSaleApi.onSaleBatchApply(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * batch list
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/onsale-retry")
    public CommonRet<Void> onsaleRetry(@Valid @RequestBody OnSaleRetryRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = openListOnSaleApi.onSaleRetry(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    /**
     * on sale progress
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/onsale-progress")
    public CommonRet<OnListProgressResponse> onSaleProgress(@Valid @RequestBody OnSaleProgressRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<OnListProgressResponse> response = openListOnSaleApi.onSaleProgress(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * list encryption
     *
     * @return
     */
    @PostMapping({"/private/nft/nft-trade/list-encryption"})
    public CommonRet<EncryptionResponse> listEncryption(@RequestBody EncryptionRequest req) {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }

        long expireAfterMs = 8 * 60 * 1000L;
        req.setExpireTime(System.currentTimeMillis() + expireAfterMs);
        APIResponse<EncryptionResponse> response = paymentChannelApi.encryption(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

}
