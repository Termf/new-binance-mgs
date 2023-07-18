package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.binance.deliverystreamer.api.balance.UserBalanceLogApi;
import com.binance.deliverystreamer.api.order.OrderApi;
import com.binance.deliverystreamer.api.request.balance.QueryUserBalanceLogRequest;
import com.binance.deliverystreamer.api.request.order.QueryOpenOrdersRequest;
import com.binance.deliverystreamer.api.request.order.QueryUserOrdersRequest;
import com.binance.deliverystreamer.api.request.trade.QueryUserTradesRequest;
import com.binance.deliverystreamer.api.response.balance.UserBalanceLogVo;
import com.binance.deliverystreamer.api.response.order.OpenOrderVo;
import com.binance.deliverystreamer.api.response.order.OrderVo;
import com.binance.deliverystreamer.api.response.trade.TradeVo;
import com.binance.deliverystreamer.api.trade.TradeApi;
import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.vo.delivery.*;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/private/account/subUser/delivery")
@Slf4j
public class SubUserDeliveryController extends BaseAction {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private VerifyRelationService verifyRelationService;


    @Autowired
    private TradeApi tradeApi;

    @Autowired
    private UserBalanceLogApi userBalanceLogApi;

    @PostMapping("/open-orders")
    @ApiOperation(value = "币本位下查询子账号的委托订单")
    public CommonRet<List<OpenOrderRet>> queryOpenOrders(@RequestBody @Validated QuerySubUserOpenOrderRequest request) {
        Long futureUserId = verifyRelationService.getFutureUserId(request.getEmail());
        QueryOpenOrdersRequest queryOpenOrdersRequest = new QueryOpenOrdersRequest();
        queryOpenOrdersRequest.setUserId(futureUserId);
        if (Objects.nonNull(request)) {
            BeanUtils.copyProperties(request, queryOpenOrdersRequest);
        }
        log.info("queryOpenOrder request:{}", JSONObject.toJSONString(queryOpenOrdersRequest));
        APIResponse<List<OpenOrderVo>> openOrderResponse = orderApi.queryOpenOrder(getInstance(queryOpenOrdersRequest));
        checkResponse(openOrderResponse);
        List<OpenOrderRet> data = null;
        if (!CollectionUtils.isEmpty(openOrderResponse.getData())) {
            data = openOrderResponse.getData().stream().map(OpenOrderRet::of).collect(Collectors.toList());
        }
        return ok(data);
    }

    @PostMapping("/order-history")
    @ApiOperation(value = "币本位下查询子账户历史委托订单")
    public CommonPageRet<OrderHistoryRet> queryOrderHistory(
            @RequestBody @Validated QuerySubUserOrderHistoryRequest request) {
        Long futureUserId = verifyRelationService.getFutureUserId(request.getEmail());
        QueryUserOrdersRequest queryUserOrdersRequest = new QueryUserOrdersRequest();
        queryUserOrdersRequest.setUserId(futureUserId);
        if (Objects.nonNull(request)) {
            BeanUtils.copyProperties(request, queryUserOrdersRequest);
        }
        log.info("queryUserOrders request:{}", JSONObject.toJSONString(queryUserOrdersRequest));
        APIResponse<SearchResult<OrderVo>> response =
                orderApi.queryUserOrders(getInstance(queryUserOrdersRequest));
        checkResponse(response);
        SearchResult<OrderVo> result = response.getData();
        List<OrderHistoryRet> data = null;
        if (!CollectionUtils.isEmpty(result.getRows())) {
            data = result.getRows().stream().map(OrderHistoryRet::of).collect(Collectors.toList());
        }
        return new CommonPageRet<>(data, result.getTotal());
    }

    @PostMapping("/transaction-history")
    @ApiOperation(value = "币本位下查询子账号下的资金流水")
    public CommonPageRet<UserBalanceLogRet> queryTransactionHistory(@RequestBody @Validated QuerySubUserTransactionHistoryRequest request) {
        Long futureUserId = verifyRelationService.getFutureUserId(request.getEmail());

        QueryUserBalanceLogRequest queryUserBalanceLogRequest = new QueryUserBalanceLogRequest();
        queryUserBalanceLogRequest.setUserId(futureUserId);
        if (Objects.nonNull(request)) {
            BeanUtils.copyProperties(request, queryUserBalanceLogRequest);
        }
        log.info("queryUserBalanceLog request:{}", JSONObject.toJSONString(queryUserBalanceLogRequest));
        APIResponse<SearchResult<UserBalanceLogVo>> response = userBalanceLogApi.queryUserBalanceLog(APIRequest.instance(queryUserBalanceLogRequest));
        checkResponse(response);
        SearchResult<UserBalanceLogVo> data = response.getData();
        List<UserBalanceLogRet> result = null;
        if (!CollectionUtils.isEmpty(data.getRows())){
            result = data.getRows().stream()
                    .map(UserBalanceLogRet::of).collect(Collectors.toList());
        }
        return new CommonPageRet<>(result, data.getTotal());
    }

    @PostMapping("/trade-history")
    @ApiOperation(value = "币本位下查询子账户历史订单")
    public CommonPageRet<TradeRet> queryTradeHistory(@RequestBody @Validated QuerySubUserTradeHistoryRequest request) {
        Long futureUserId = verifyRelationService.getFutureUserId(request.getEmail());

        QueryUserTradesRequest queryUserTradesRequest = new QueryUserTradesRequest();
        queryUserTradesRequest.setUserId(futureUserId);
        if (Objects.nonNull(request)) {
            BeanUtils.copyProperties(request, queryUserTradesRequest);
        }
        log.info("queryUserTrades request:{}", JSONObject.toJSONString(queryUserTradesRequest));
        APIResponse<SearchResult<TradeVo>> response = tradeApi.queryUserTrades(APIRequest.instance(queryUserTradesRequest));
        checkResponse(response);
        SearchResult<TradeVo> data = response.getData();
        List<TradeRet> result = null;
        if (!CollectionUtils.isEmpty( data.getRows())){
            result = data.getRows().stream().map(TradeRet::of).collect(Collectors.toList());
        }
        return new CommonPageRet<>(result, data.getTotal());
    }


}
