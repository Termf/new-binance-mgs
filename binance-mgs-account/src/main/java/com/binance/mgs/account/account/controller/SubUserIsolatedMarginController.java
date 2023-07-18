package com.binance.mgs.account.account.controller;

import com.binance.assetlog.api.IUserAssetLogApi;
import com.binance.assetlog.vo.PagingResult;
import com.binance.assetlog.vo.UserAssetLogRequest;
import com.binance.assetlog.vo.UserAssetLogResponse;
import com.binance.margin.isolated.api.Results;
import com.binance.margin.isolated.api.borrow.BorrowApi;
import com.binance.margin.isolated.api.borrow.response.BorrowResponse;
import com.binance.margin.isolated.api.repay.RepayApi;
import com.binance.margin.isolated.api.repay.response.RepayResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.account.vo.marginRelated.MarginBaseAction;
import com.binance.mgs.account.account.vo.marginRelated.MarginCapitalFlowResponseRet;
import com.binance.mgs.account.account.vo.marginRelated.MarginHelper;
import com.binance.mgs.account.account.vo.marginRelated.OpenOrderRet;
import com.binance.mgs.account.account.vo.marginRelated.OrderHistoryRet;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserBorrowHistoryRequest;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserCapitalFlowRequest;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserOpenOrderRequest;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserOrderHistoryRequest;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserRepayHistoryRequest;
import com.binance.mgs.account.account.vo.marginRelated.QuerySubUserTradeHistoryRequest;
import com.binance.mgs.account.account.vo.marginRelated.QueryTradeDetailRequest;
import com.binance.mgs.account.account.vo.marginRelated.TradeDetailResponseRet;
import com.binance.mgs.account.account.vo.marginRelated.TradeRet;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.service.SubUserMarginRelatedService;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.utils.ListTransformUtil;
import com.binance.streamer.api.order.OrderApi;
import com.binance.streamer.api.request.order.QuerySubAccountOpenOrderRequest;
import com.binance.streamer.api.request.trade.QueryTradeDetailsRequest;
import com.binance.streamer.api.request.trade.QueryUserOrdersRequest;
import com.binance.streamer.api.request.trade.QueryUserTradeRequest;
import com.binance.streamer.api.response.SearchResult;
import com.binance.streamer.api.response.vo.OpenOrderVo;
import com.binance.streamer.api.response.vo.OrderVo;
import com.binance.streamer.api.response.vo.TradeDetailVo;
import com.binance.streamer.api.response.vo.TradeVo;
import com.binance.streamer.api.trade.TradeApi;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sean w
 * @date 2021/9/27
 **/
@Slf4j
@RestController
@RequestMapping("/v1/private/account/subUser/isolated-margin")
public class SubUserIsolatedMarginController extends MarginBaseAction {

    @Autowired
    private OrderApi orderApi;

    @Resource
    private TradeApi tradeApi;

    @Autowired
    private BorrowApi borrowApi;

    @Autowired
    private RepayApi repayApi;

    @Autowired
    private SubUserMarginRelatedService subUserMarginRelatedService;

    @Autowired
    private IUserAssetLogApi iUserAssetLogApi;

    @PostMapping("/open-orders")
    @ApiOperation(value = "逐仓查询子账号的委托订单")
    public CommonPageRet<OpenOrderRet> queryOpenOrders(@RequestBody @Validated QuerySubUserOpenOrderRequest request) throws Exception {

        /*final Long parentUserId = getUserId();*/
        log.info("isolated margin queryOpenOrders request: {}", JsonUtils.toJsonHasNullKey(request));
        List<Long> isolatedMarginUserIds = subUserMarginRelatedService.getIoslatedMarginUserIdByEmail(request.getEmail(), request.getSymbol());
        CommonPageRet<OpenOrderRet> response = new CommonPageRet<OpenOrderRet>(Collections.EMPTY_LIST, 0);
        if (CollectionUtils.isNotEmpty(isolatedMarginUserIds)) {
            QuerySubAccountOpenOrderRequest queryOpenOrderRequest = new QuerySubAccountOpenOrderRequest();
            BeanUtils.copyProperties(request, queryOpenOrderRequest);
            queryOpenOrderRequest.setUserIds(isolatedMarginUserIds);
            /*queryOpenOrderRequest.getUserIds().add(parentUserId);*/
            log.info("isolated margin orderApi querySubAccountOpenOrder request: {}", JsonUtils.toJsonHasNullKey(queryOpenOrderRequest));
            APIResponse<SearchResult<OpenOrderVo>> streamerApiResponse = orderApi.querySubAccountOpenOrder(getInstance(queryOpenOrderRequest));
            log.info("isolated margin orderApi querySubAccountOpenOrder response: {}", JsonUtils.toJsonHasNullKey(streamerApiResponse));
            checkResponse(streamerApiResponse);
            SearchResult<OpenOrderVo> result = streamerApiResponse.getData();
            List<OpenOrderRet> openOrderRets = null;
            if (!CollectionUtils.isEmpty(result.getRows())) {
                openOrderRets = result.getRows().stream().map(OpenOrderRet::of).collect(Collectors.toList());
            }
            response = new CommonPageRet<>(openOrderRets, result.getTotal());
        }
        return response;
    }

    @PostMapping("/order-history")
    @ApiOperation(value = "逐仓查询子账户历史委托订单")
    public CommonPageRet<OrderHistoryRet> queryOrderHistory(@RequestBody @Validated QuerySubUserOrderHistoryRequest request) throws Exception {

        final Long parentUserId = getUserId();
        log.info("isolated margin queryOpenOrders request: {}", JsonUtils.toJsonHasNullKey(request));
        List<Long> isolatedMarginUserIds = subUserMarginRelatedService.getIoslatedMarginUserIdByEmail(request.getEmail(), request.getSymbol());
        CommonPageRet<OrderHistoryRet> response = new CommonPageRet<OrderHistoryRet>(Collections.EMPTY_LIST, 0);

        if (CollectionUtils.isNotEmpty(isolatedMarginUserIds)) {
            QueryUserOrdersRequest queryUserOrdersRequest = new QueryUserOrdersRequest();
            BeanUtils.copyProperties(request, queryUserOrdersRequest);
            if (StringUtils.isBlank(request.getEmail())) {
                queryUserOrdersRequest.setUserId(parentUserId);
            }
            queryUserOrdersRequest.setUserIds(isolatedMarginUserIds);
            log.info("isolated margin orderApi queryUserOrders request: {}", JsonUtils.toJsonHasNullKey(queryUserOrdersRequest));
            APIResponse<SearchResult<OrderVo>> streamerApiResponse = orderApi.queryUserOrders(getInstance(queryUserOrdersRequest));
            log.info("isolated margin orderApi queryUserOrders response: {}", JsonUtils.toJsonHasNullKey(streamerApiResponse));
            checkResponse(streamerApiResponse);
            SearchResult<OrderVo> orderVoSearchResult = streamerApiResponse.getData();
            List<OrderHistoryRet> orderHistoryRets = null;
            if (!CollectionUtils.isEmpty(orderVoSearchResult.getRows())) {
                orderHistoryRets = orderVoSearchResult.getRows().stream().map(OrderHistoryRet::of).collect(Collectors.toList());
            }
            response = new CommonPageRet<>(orderHistoryRets, orderVoSearchResult.getTotal());
        }

        return response;
    }

    @PostMapping("/trade-history")
    @ApiOperation(value = "逐仓查询子账户历史订单")
    public CommonPageRet<TradeRet> queryTradeHistory(@RequestBody @Validated QuerySubUserTradeHistoryRequest request) throws Exception {

        final Long parentUserId = getUserId();
        log.info("isolated margin queryTradeHistory request: {}", JsonUtils.toJsonHasNullKey(request));
        List<Long> isolatedMarginUserIds = subUserMarginRelatedService.getIoslatedMarginUserIdByEmail(request.getEmail(), request.getSymbol());
        CommonPageRet<TradeRet> response = new CommonPageRet<TradeRet>(Collections.EMPTY_LIST, 0);

        if (CollectionUtils.isNotEmpty(isolatedMarginUserIds)) {
            QueryUserTradeRequest queryUserTradeRequest = new QueryUserTradeRequest();
            BeanUtils.copyProperties(request, queryUserTradeRequest);
            if (StringUtils.isBlank(request.getEmail())) {
                queryUserTradeRequest.setUserId(parentUserId);
            }
            queryUserTradeRequest.setUserIds(isolatedMarginUserIds);
            log.info("isolated margin tradeApi getUserTrades is {}", JsonUtils.toJsonHasNullKey(queryUserTradeRequest));
            APIResponse<SearchResult<TradeVo>> apiResponse = tradeApi.getUserTrades(getInstance(queryUserTradeRequest));
            log.info("isolated margin tradeApi getUserTrades response {}", JsonUtils.toJsonHasNullKey(apiResponse));
            checkResponse(apiResponse);
            SearchResult<TradeVo> result = apiResponse.getData();
            List<TradeRet> retList = null;
            if (!CollectionUtils.isEmpty(result.getRows())) {
                retList = result.getRows().stream().map(TradeRet::of).collect(Collectors.toList());
            }
            response = new CommonPageRet<>(retList, result.getTotal());
        }

        return response;
    }

    @PostMapping("/transaction-history")
    @ApiOperation(value = "逐仓查询子账号下的资金流水")
    public CommonPageRet<MarginCapitalFlowResponseRet> queryTransactionHistory(@RequestBody @Validated QuerySubUserCapitalFlowRequest request) throws Exception {

        if (subUserMarginRelatedService.getMonthSpace(request.getStartTime(), request.getEndTime())) {
            throw new BusinessException(AccountMgsErrorCode.SEARCH_TIME_GREATER_THAN_THREE_MONTH);
        }
        Long uid = subUserMarginRelatedService.getUid(request.getEmail(), request.getSymbol());
        UserAssetLogRequest assetLogRequest = subUserMarginRelatedService.buildRequest(request, uid);
        log.info("isolated margin iUserAssetLogApi queryUserAssetLog email:{} request:{}", request.getEmail(), JsonUtils.toJsonHasNullKey(assetLogRequest));
        APIResponse<PagingResult<UserAssetLogResponse>> apiResponse = iUserAssetLogApi.queryUserAssetLog(APIRequest.instance(assetLogRequest));
        log.info("isolated margin iUserAssetLogApi queryUserAssetLog response: {}", JsonUtils.toJsonHasNullKey(apiResponse));
        boolean notOpenAccount = MarginHelper.isNotOpenAccount(apiResponse);
        if (notOpenAccount) {
            return new CommonPageRet<MarginCapitalFlowResponseRet>(Collections.EMPTY_LIST, 0);
        }
        checkResponse(apiResponse);
        PagingResult<UserAssetLogResponse> data = apiResponse.getData();

        return new CommonPageRet<>(subUserMarginRelatedService.processData(data.getRows(), request.getSymbol()), data.getTotal());
    }

    @PostMapping("/borrow-history")
    @ApiOperation(value = "逐仓查询借款历史")
    public CommonPageRet<BorrowResponse> queryBorrowHistory(@RequestBody @Validated QuerySubUserBorrowHistoryRequest request) throws Exception{

        Long subUserId = subUserMarginRelatedService.getSubUserId(request.getEmail());
        log.info("isolated margin borrowApi borrowHistories request: subUserId:{}, param:{}", subUserId, JsonUtils.toJsonHasNullKey(request));
        APIResponse<Results<BorrowResponse>> response = borrowApi.borrowHistories(subUserId, request.getSymbol(), request.getAsset(), request.getTxId(),
                request.getStatus(), request.getStartTime(), request.getEndTime(), request.getCurrent(), request.getSize(), request.isArchived(), request.isNeedBorrowType());
        if (MarginHelper.isNotOpenAccount(response)) {
            return MarginHelper.emptyResponse();
        }
        log.info("isolated margin borrowApi borrowHistories response: {}", JsonUtils.toJsonHasNullKey(response));
        checkResponse(response);
        return ok(response.getData().getRows(), response.getData().getTotal());
    }

    @PostMapping("/repay-history")
    @ApiOperation(value = "逐仓查询还款历史")
    public CommonPageRet<RepayResponse> queryRepayHistory(@RequestBody @Validated QuerySubUserRepayHistoryRequest request) throws Exception{

        Long subUserId = subUserMarginRelatedService.getSubUserId(request.getEmail());
        log.info("isolated margin repayApi repayHistories request: subUserId:{}, param:{}", subUserId, JsonUtils.toJsonHasNullKey(request));
        APIResponse<Results<RepayResponse>> response = repayApi.repayHistories(subUserId, request.getSymbol(), request.getAsset(), request.getStatus(), request.getTxId(),
                request.getStartTime(), request.getEndTime(), request.getCurrent(), request.getSize(), request.isArchived(), request.isNeedRepayType());
        if (MarginHelper.isNotOpenAccount(response)) {
            return MarginHelper.emptyResponse();
        }
        log.info("isolated margin repayApi repayHistories response: {}", JsonUtils.toJsonHasNullKey(response));
        checkResponse(response);
        return ok(response.getData().getRows(), response.getData().getTotal());
    }

    @PostMapping("/trade-detail")
    @ApiOperation(value = "逐仓交易详情信息")
    public CommonPageRet<TradeDetailResponseRet> queryTradeDetail(@RequestBody @Validated QueryTradeDetailRequest request) throws Exception {
        log.info("SubUserIsolatedMarginController queryTradeDetail request: {}", JsonUtils.toJsonHasNullKey(request));
        QueryTradeDetailsRequest queryTradeDetailsRequest = subUserMarginRelatedService.buildIsolatedTradeDetailRequest(request);
        log.info("tradeApi fetchUserIsolatedTradeDetails request:{}", JsonUtils.toJsonHasNullKey(queryTradeDetailsRequest));
        APIResponse<SearchResult<TradeDetailVo>> apiResponse = tradeApi.fetchUserIsolatedTradeDetails(getInstance(queryTradeDetailsRequest));
        log.info("tradeApi fetchUserIsolatedTradeDetails response:{}", JsonUtils.toJsonHasNullKey(apiResponse));
        checkResponse(apiResponse);
        SearchResult<TradeDetailVo> result = apiResponse.getData();
        CommonPageRet<TradeDetailResponseRet> response = new CommonPageRet<>();
        response.setTotal(result.getTotal());
        response.setData(ListTransformUtil.transform(result.getRows(), TradeDetailResponseRet.class));
        return response;
    }
}

