package com.binance.mgs.nft.trade;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.mgs.nft.trade.response.BuyOrderItemVo;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.nft.tradeservice.api.IBuyOrderApi;
import com.binance.nft.tradeservice.api.IPreOrderApi;
import com.binance.nft.tradeservice.dto.BatchCreateOrderRequest;
import com.binance.nft.tradeservice.dto.BatchOrderStatusDto;
import com.binance.nft.tradeservice.request.*;
import com.binance.nft.tradeservice.request.preorder.PreOrderBatchCreateRequest;
import com.binance.nft.tradeservice.request.preorder.PreOrderConfirmRequest;
import com.binance.nft.tradeservice.request.preorder.PreOrderCreateRequest;
import com.binance.nft.tradeservice.request.preorder.PreOrderTradingRoutingRequest;
import com.binance.nft.tradeservice.response.*;
import com.binance.nft.tradeservice.response.preorder.PreOrderBatchCreateResponse;
import com.binance.nft.tradeservice.vo.*;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class BuyOrderController {

    @Resource
    private IBuyOrderApi buyOrderApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private IPreOrderApi preOrderApi;
    @Resource
    private NftInboxHelper nftInboxHelper;
    @Resource
    private CrowdinHelper crowdinHelper;

    /**
     * 下单
     *
     * @return
     */
    @GoogleRecaptha("/private/nft/nft-trade/order-create")
    @PostMapping("/private/nft/nft-trade/order-create")
    @UserOperation(eventName = "NFT_Market_Purchase", name = "NFT_Market_Purchase", sendToBigData = true, sendToDb = true,
            requestKeys = {"$.tradeType"}, requestKeyDisplayNames = {"tradeType(0:fixedprice,1:auction,2:makeoffer)"},
            responseKeys = {"$.code", "$.message", "$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "data", "errorMessage", "errorCode"})
    public CommonRet<OrderCreateResponse> create(@Valid @RequestBody OrderCreateRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        request.setSource(0);
        APIResponse<OrderCreateResponse> response = buyOrderApi.create(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * 交易历史
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/order-history")
    public CommonRet<SearchResult<BuyOrderItemVo>> orderHistory(@Valid @RequestBody OrderHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SearchResult<OrderItemVo>> response = buyOrderApi.history(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        if(response.getData() != null && CollectionUtils.isNotEmpty(response.getData().getRows())) {
            List<BizIdModel> bizIdList = Arrays.asList(new BizIdModel(HistoryType.PURCHASE.name(),response.getData().getRows().stream().map(b->b.getId()).collect(Collectors.toList())));
            SearchResult<BuyOrderItemVo> retResult = nftInboxHelper.searchResultWithFlag(response.getData(), BuyOrderItemVo.class,
                    BuyOrderItemVo::getId, bizIdList, BuyOrderItemVo::setUnreadFlag);
            retResult.getRows().stream().forEach(item -> {
                String errorMsg = StringUtils.isBlank(item.getErrorCode()) ? item.getErrorCode() :
                        crowdinHelper.getMessageByKey(item.getErrorCode(), baseHelper.getLanguage());
                item.setErrorMsg(errorMsg);
            });
            return new CommonRet<>(retResult);
        }
        return new CommonRet<>(new SearchResult<>());
    }


    /**
     * 交易历史
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/bidding-history")
    public CommonRet<SearchResult<BiddingOrderItemVo>> orderHistory(@Valid @RequestBody OrderBiddingHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SearchResult<BiddingOrderItemVo>> response = buyOrderApi.biddingHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * 订单查询
     *
     * @return
     */
    @UserOperation(eventName = "NFT_Order_Query", name = "NFT_Order_Query",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/order-query")
    public CommonRet<OrderQueryResponse> query(@Valid @RequestBody OrderQueryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<OrderQueryResponse> response = buyOrderApi.query(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * 单号生成
     *
     * @return
     */
    @PostMapping("/public/nft/nft-trade/order-no")
    public CommonRet<Long> genOrderNo() throws Exception {
        APIResponse<Long> response = buyOrderApi.genOrderNo();
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * cancel
     *
     * @return
     */
    @UserOperation(eventName = "NFT_Offer_Cancel", name = "NFT_Offer_Cancel",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/order-cancel")
    public CommonRet<Void> cancel(@Valid @RequestBody OrderCancelRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = buyOrderApi.cancel(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * accept
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/order-accept")
    @UserOperation(eventName = "NFT_Offer_Acept", name = "NFT_Offer_Acept", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code", "$.message", "$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "data", "errorMessage", "errorCode"})
    public CommonRet<OrderCreateResponse> accept(@Valid @RequestBody OrderOfferAcceptRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<OrderCreateResponse> response = buyOrderApi.accept(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @GetMapping("/private/nft/nft-trade/offer-history")
    public CommonRet<SearchResult<MakeOfferHistoryVo>> queryMadeOfferHistory(@RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size) throws Exception {
        APIResponse<SearchResult<MakeOfferHistoryVo>> response = buyOrderApi.queryMadeOfferHistory(APIRequest.instance(MakeOrderHistoryRequest.builder().page(page).size(size).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/nft-trade/offer-received")
    public CommonRet<SearchResult<ProductMadeOfferVo>> queryReceivedOfferHistory(@Valid @RequestParam(defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size) throws Exception {
        APIResponse<SearchResult<ProductMadeOfferVo>> response = buyOrderApi.queryReceivedOfferHistory(APIRequest.instance(MakeOrderHistoryRequest.builder().page(page).size(size).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping({"/friendly/nft/nft-trade/offer-received-by-nft", "/public/nft/nft-trade/offer-received-by-nft"})
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'offerReceivedByNft-'+#nftId+'-'+ #page+'-'+#size")
    public CommonRet<SearchResult<MakeOfferHistoryVo>> queryReceivedOfferHistoryByNftId(@Valid @RequestParam(defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, @RequestParam(value = "nftId") Long nftId, @RequestParam(value = "userId", required = false) Long userId) throws Exception {
        userId = Objects.nonNull(userId) ? userId : baseHelper.getUserId();
        APIResponse<SearchResult<MakeOfferHistoryVo>> response = buyOrderApi.queryReceivedOfferHistoryByNftId(APIRequest.instance(MakeOrderHistoryRequest.builder().page(page).size(size).userId(userId).nftId(nftId).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/nft-trade/offer-notice")
    public CommonRet<MakeOrderNewTipsResponse> getNewTips() throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        APIResponse<MakeOrderNewTipsResponse> response = buyOrderApi.getNewTips(APIRequest.instance(MakeOrderNewTipsRequest.builder().userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Offer_Confirm", name = "NFT_Offer_Confirm",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GetMapping("/private/nft/nft-trade/confirm")
    public CommonRet<MakeOfferConfirmResponse> queryConfirmButton(@RequestParam(value = "orderNo") Long orderNo, @RequestParam(value = "type") Integer type) throws Exception {
        APIResponse<MakeOfferConfirmResponse> response = buyOrderApi.queryConfirmButton(APIRequest.instance(MakeOfferConfirmRequest.builder().orderNo(orderNo).type(type).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @UserOperation(eventName = "NFT_Preorder_Create", name = "NFT_Preorder_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GoogleRecaptha(value = "/private/nft/nft-trade/preorder-create")
    @PostMapping("/private/nft/nft-trade/preorder-create")
    public CommonRet<PreOrderTradingRoutingResponse> getTradingrouting(@RequestBody PreOrderTradingRoutingRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<PreOrderTradingRoutingResponse> response = preOrderApi.gettradingrouting(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Preorder_Batch_Create", name = "NFT_Preorder_Batch_Preorder_Order_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GoogleRecaptha(value = "/private/nft/nft-trade/batch/pre-order")
    @PostMapping("/private/nft/nft-trade/batch/pre-order")
    public CommonRet<PreOrderBatchCreateResponse> preorderBatchCreate(@RequestBody PreOrderBatchCreateRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<PreOrderBatchCreateResponse> response = preOrderApi.getBatchTradingRouting(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @PostMapping("/private/nft/nft-trade/batch/buy-consult")
    public CommonRet<BuyConsultResponse> buyConsult(@RequestBody BuyConsultRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<BuyConsultResponse> response = preOrderApi.buyConsult(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GoogleRecaptha(value = "/private/nft/nft-trade/batch/pre-order-check")
    @PostMapping("/private/nft/nft-trade/batch/pre-order-check")
    public CommonRet<BatchPreOrderCheckResponse> preOrderCheck(@RequestBody BatchPreOrderCheckRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        APIResponse<BatchPreOrderCheckResponse> response = preOrderApi.preOrderCheck(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Preorder_Batch_Create", name = "NFT_Preorder_Batch_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GoogleRecaptha(value = "/private/nft/nft-trade/batch/create-order")
    @PostMapping("/private/nft/nft-trade/batch/create-order")
    public CommonRet<Boolean> createOrder(@RequestBody BatchCreateOrderRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<Boolean> response = buyOrderApi.createOrder(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/nft-trade/batch/get-sweep-num-limit")
    public CommonRet<Integer> getSweepNumLimit() {
        try {
            APIResponse<Integer> response = preOrderApi.getSweepNumLimit();
            return new CommonRet<>(response.getData());
        } catch (Exception e) {
            return new CommonRet<>(30);
        }
    }

    @GetMapping("/private/nft/nft-trade/batch/order-status")
    public CommonRet<BatchOrderStatusDto> orderStatus(@RequestParam("requestId") @NotNull Long requestId) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        APIResponse<BatchOrderStatusDto> response = buyOrderApi.orderStatus(APIRequest.instance(BatchCreateOrderRequest.builder().requestId(requestId).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Preorder_Checkrisk", name = "NFT_Preorder_Checkrisk",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/checkrisk")
    public CommonRet<PreOrderCreateResponse> checkrisk(@RequestBody PreOrderCreateRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<PreOrderCreateResponse> response = preOrderApi.checkrisk(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Preorder_CheckLimit", name = "NFT_Preorder_CheckLimit",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/check-limit")
    public CommonRet<CheckLimitResponse> checkLimit(@RequestBody CheckLimitRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<CheckLimitResponse> response = buyOrderApi.checkLimit(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @UserOperation(eventName = "NFT_Preorder_Confirm", name = "NFT_Preorder_Confirm",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/preorder-confirm")
    public CommonRet<Void> preOrderConfirm(@RequestBody PreOrderConfirmRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = preOrderApi.confirm(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }




    @PostMapping("/private/nft/nft-trade/preorder-info")
    public CommonRet<PreOrderInfoVo> preOrderInfo(@RequestBody PreOrderConfirmRequest req) throws Exception {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<PreOrderInfoVo> response = preOrderApi.info(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/preorder-detail")
    public CommonRet<PreOrderDetailResponse> preOrderDetail(@RequestBody PreOrderConfirmRequest req) throws Exception{
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        req.setUserId(baseHelper.getUserId());
        APIResponse<PreOrderDetailResponse> response = preOrderApi.detail(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
