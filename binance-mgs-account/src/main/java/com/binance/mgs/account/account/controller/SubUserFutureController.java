package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.SubUserApi;
import com.binance.account.vo.subuser.request.BindingParentSubUserEmailReq;
import com.binance.account.vo.subuser.response.BindingParentSubUserEmailResp;
import com.binance.future.api.PositionLimitApi;
import com.binance.future.api.request.PLDecreaseReq;
import com.binance.future.api.request.PLGetConfigReq;
import com.binance.future.api.request.PLIncreaseReq;
import com.binance.future.api.request.PLMyAdjustmentReq;
import com.binance.future.api.vo.PLConfigRes;
import com.binance.future.api.vo.PLMyAdjustmentRes;
import com.binance.futurestreamer.api.balance.UserBalanceLogApi;
import com.binance.futurestreamer.api.order.OrderApi;
import com.binance.futurestreamer.api.request.balance.QueryUserBalanceLogRequest;
import com.binance.futurestreamer.api.request.order.QueryOpenOrdersRequest;
import com.binance.futurestreamer.api.request.order.QueryUserOrdersRequest;
import com.binance.futurestreamer.api.request.trade.QueryUserTradesRequest;
import com.binance.futurestreamer.api.response.balance.UserBalanceLogVo;
import com.binance.futurestreamer.api.response.order.OpenOrderVo;
import com.binance.futurestreamer.api.response.order.OrderVo;
import com.binance.futurestreamer.api.response.trade.TradeVo;
import com.binance.futurestreamer.api.trade.TradeApi;
import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.vo.future.*;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.api.client.util.Lists;
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

@Slf4j
@RestController
@RequestMapping("/v1/private/account/subUser/future")
public class SubUserFutureController extends BaseAction {


    @Autowired
    private OrderApi orderApi;

    @Autowired
    private VerifyRelationService verifyRelationService;

    @Autowired
    private TradeApi tradeApi;

    @Autowired
    private UserBalanceLogApi userBalanceLogApi;

    @Autowired
    private SubUserApi subUserApi;

    @Autowired
    private PositionLimitApi positionLimitApi;

    @PostMapping("/open-orders")
    @ApiOperation(value = "U本位下查询子账号的委托订单")
    public CommonRet<List<OpenOrderRet>> queryOpenOrders(@RequestBody @Validated QuerySubUserOpenOrderRequest req) {
        Long futureUserId = verifyRelationService.getFutureUserId(req.getEmail());
        QueryOpenOrdersRequest request = new QueryOpenOrdersRequest();
        request.setUserId(futureUserId);
        request.setSymbol(req.getSymbol());
        log.info("queryOpenOrder request:{}", JSONObject.toJSONString(request));
        APIResponse<List<OpenOrderVo>> openOrderResponse = orderApi.queryOpenOrder(getInstance(request));
        checkResponse(openOrderResponse);
        List<OpenOrderRet> data = null;
        if (!CollectionUtils.isEmpty(openOrderResponse.getData())) {
            data = openOrderResponse.getData().stream().map(OpenOrderRet::of).collect(Collectors.toList());
        }
        return ok(data);
    }

    @PostMapping("/order-history")
    @ApiOperation(value = "U本位下查询子账户历史委托订单")
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
    @ApiOperation(value = "U本位下查询子账号下的资金流水")
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
        List<UserBalanceLogRet> result = data.getRows().stream()
                .map(UserBalanceLogRet::of).collect(Collectors.toList());
        return new CommonPageRet<>(result, data.getTotal());
    }

    @PostMapping("/trade-history")
    @ApiOperation(value = "U本位下查询子账户历史订单")
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
        if (!CollectionUtils.isEmpty(data.getRows())){
            result = data.getRows().stream().map(TradeRet::of).collect(Collectors.toList());
        }
        return new CommonPageRet<>(result, data.getTotal());
    }

    @PostMapping("/position-limit/update")
    @ApiOperation(value = "调高/调低 FUTURE的position-limit")
    public CommonRet<String> updatePositionLimit(@RequestBody @Validated UpdatePositionLimitArg arg) throws Exception {

        // 校验母子关系
        BindingParentSubUserEmailReq bindingParentSubUserEmailReq = new BindingParentSubUserEmailReq();
        bindingParentSubUserEmailReq.setParentUserId(getUserId());
        bindingParentSubUserEmailReq.setSubUserEmail(arg.getSubUserEmail());
        APIResponse<BindingParentSubUserEmailResp> bindResp = subUserApi
                .checkRelationByParentSubUserEmail(APIRequest.instance(bindingParentSubUserEmailReq));
        checkResponse(bindResp);

        Long futureUserId = verifyRelationService.getFutureUserId(arg.getSubUserEmail());
        APIResponse<Void> resp;
        if (arg.isIncrease()) {
            PLIncreaseReq req = new PLIncreaseReq();
            req.setSymbol(arg.getSymbol());
            log.info("SubUserFutureController increasePositionLimit req = {}", req);
            resp = positionLimitApi.increase(futureUserId, getInstance(req));
        } else {
            PLDecreaseReq req = new PLDecreaseReq();
            req.setSymbol(arg.getSymbol());
            log.info("SubUserFutureController decreasePositionLimit req = {}", req);
            resp = positionLimitApi.decrease(futureUserId, getInstance(req));
        }

        checkResponse(resp);
        return new CommonRet<>();
    }

    @PostMapping("/position-limit/myAdjustment")
    @ApiOperation(value = "获取子账户position-limit的调整信息")
    public CommonRet<List<QueryPositionLimitAdjustmentRet>> queryPositionLimitAdjustment(@RequestBody @Validated QueryPositionLimitAdjustmentArg arg) throws Exception {

        // 校验母子关系
        BindingParentSubUserEmailReq bindingParentSubUserEmailReq = new BindingParentSubUserEmailReq();
        bindingParentSubUserEmailReq.setParentUserId(getUserId());
        bindingParentSubUserEmailReq.setSubUserEmail(arg.getSubUserEmail());
        APIResponse<BindingParentSubUserEmailResp> bindResp = subUserApi
                .checkRelationByParentSubUserEmail(APIRequest.instance(bindingParentSubUserEmailReq));
        checkResponse(bindResp);

        Long futureUserId = verifyRelationService.getFutureUserId(arg.getSubUserEmail());
        log.info("SubUserFutureController queryPositionLimitAdjustment futureUserId = {}", futureUserId);
        APIResponse<List<PLMyAdjustmentRes>> resp = positionLimitApi.myAdjustment(futureUserId, getInstance(new PLMyAdjustmentReq()));
        log.info("SubUserFutureController queryPositionLimitAdjustment resp = {}", resp);
        checkResponse(resp);

        List<QueryPositionLimitAdjustmentRet> ret = Lists.newArrayList();
        for (PLMyAdjustmentRes plMyAdjustmentRes : resp.getData()) {
            QueryPositionLimitAdjustmentRet queryPositionLimitAdjustmentRet = new QueryPositionLimitAdjustmentRet();
            BeanUtils.copyProperties(plMyAdjustmentRes, queryPositionLimitAdjustmentRet);
            ret.add(queryPositionLimitAdjustmentRet);
        }
        return new CommonRet<>(ret);
    }

    @PostMapping("/position-limit/config")
    @ApiOperation(value = "获取子账户position-limit的配置信息")
    public CommonRet<QueryPositionLimitConfigRet> queryPositionLimitConfig(@RequestBody @Validated QueryPositionLimitConfigArg arg) throws Exception {

        // 校验母子关系
        BindingParentSubUserEmailReq bindingParentSubUserEmailReq = new BindingParentSubUserEmailReq();
        bindingParentSubUserEmailReq.setParentUserId(getUserId());
        bindingParentSubUserEmailReq.setSubUserEmail(arg.getSubUserEmail());
        APIResponse<BindingParentSubUserEmailResp> bindResp = subUserApi
                .checkRelationByParentSubUserEmail(APIRequest.instance(bindingParentSubUserEmailReq));
        checkResponse(bindResp);

        Long futureUserId = verifyRelationService.getFutureUserId(arg.getSubUserEmail());
        log.info("SubUserFutureController queryPositionLimitConfig futureUserId = {}", futureUserId);
        APIResponse<PLConfigRes> resp = positionLimitApi.getConfig(futureUserId, APIRequest.instance(new PLGetConfigReq()));
        log.info("SubUserFutureController queryPositionLimitConfig resp = {}", resp);
        checkResponse(resp);

        QueryPositionLimitConfigRet ret = new QueryPositionLimitConfigRet();
        BeanUtils.copyProperties(resp.getData(), ret);
        return new CommonRet<>(ret);
    }
}
