package com.binance.mgs.account.account.controller;

import com.binance.assetlog.api.IUserAssetLogApi;
import com.binance.assetlog.vo.PagingResult;
import com.binance.assetlog.vo.UserAssetLogRequest;
import com.binance.assetlog.vo.UserAssetLogResponse;
import com.binance.margin.api.bookkeeper.BookKeeperApi;
import com.binance.margin.api.bookkeeper.dto.MgsBorrowHistoryDto;
import com.binance.margin.api.bookkeeper.dto.MgsRepayHistoryDto;
import com.binance.margin.api.page.Results;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.account.vo.marginRelated.BorrowHistoryResponseRet;
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
import com.binance.mgs.account.account.vo.marginRelated.RepayHistoryResponseRet;
import com.binance.mgs.account.account.vo.marginRelated.TradeDetailResponseRet;
import com.binance.mgs.account.account.vo.marginRelated.TradeRet;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.service.SubUserMarginRelatedService;
import com.binance.platform.mgs.base.BaseAction;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sean w
 * @date 2021/9/26
 **/
@Slf4j
@RestController
@RequestMapping("/v1/private/account/subUser/margin")
public class SubUserMarginAllController extends BaseAction {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private BookKeeperApi bookKeeperApi;

    @Resource
    private TradeApi tradeApi;

    @Autowired
    private SubUserMarginRelatedService subUserMarginRelatedService;

    @Autowired
    private IUserAssetLogApi iUserAssetLogApi;

    @PostMapping("/open-orders")
    @ApiOperation(value = "全仓查询子账号的委托订单")
    public CommonPageRet<OpenOrderRet> queryOpenOrders(@RequestBody @Validated QuerySubUserOpenOrderRequest request) throws Exception {

        /*final Long parentUserId = getUserId();*/
        log.info("margin queryOpenOrders request: {}", JsonUtils.toJsonHasNullKey(request));
        List<Long> marginUserIds = new ArrayList<>();
        Long marginUserId = subUserMarginRelatedService.getMarginUserIdByEmail(request.getEmail());
        marginUserIds.add(marginUserId);
        CommonPageRet<OpenOrderRet> response = new CommonPageRet<OpenOrderRet>(Collections.EMPTY_LIST, 0);

        if (CollectionUtils.isNotEmpty(marginUserIds)) {
            QuerySubAccountOpenOrderRequest queryOpenOrderRequest = new QuerySubAccountOpenOrderRequest();
            BeanUtils.copyProperties(request, queryOpenOrderRequest);
            queryOpenOrderRequest.setUserIds(marginUserIds);
            /*queryOpenOrderRequest.getUserIds().add(parentUserId);*/
            log.info("orderApi querySubAccountOpenOrder request: {}", JsonUtils.toJsonHasNullKey(queryOpenOrderRequest));
            APIResponse<SearchResult<OpenOrderVo>> streamerApiResponse = orderApi.querySubAccountOpenOrder(getInstance(queryOpenOrderRequest));
            log.info("orderApi querySubAccountOpenOrder response: {}", JsonUtils.toJsonHasNullKey(streamerApiResponse));
            checkResponse(streamerApiResponse);
            SearchResult<OpenOrderVo> result = streamerApiResponse.getData();
            List<OpenOrderRet> openOrderRetList = null;

            if (!CollectionUtils.isEmpty(result.getRows())) {
                openOrderRetList = result.getRows().stream().map(OpenOrderRet::of).collect(Collectors.toList());
            }
            response = new CommonPageRet<>(openOrderRetList, result.getTotal());
        }

        return response;
    }

    @PostMapping("/order-history")
    @ApiOperation(value = "全仓查询子账户历史委托订单")
    public CommonPageRet<OrderHistoryRet> queryOrderHistory(@RequestBody @Validated QuerySubUserOrderHistoryRequest request) throws Exception {

        log.info("margin queryOrderHistory request: {}", JsonUtils.toJsonHasNullKey(request));
        List<Long> marginUserIds = new ArrayList<>();
        Long marginUserId = subUserMarginRelatedService.getMarginUserIdByEmail(request.getEmail());
        marginUserIds.add(marginUserId);
        CommonPageRet<OrderHistoryRet> response = new CommonPageRet<OrderHistoryRet>(Collections.EMPTY_LIST, 0);

        if (CollectionUtils.isNotEmpty(marginUserIds)) {
            QueryUserOrdersRequest queryUserOrdersRequest = new QueryUserOrdersRequest();
            BeanUtils.copyProperties(request, queryUserOrdersRequest);
            queryUserOrdersRequest.setUserIds(marginUserIds);
            log.info("orderApi queryUserOrders request: {}", JsonUtils.toJsonHasNullKey(queryUserOrdersRequest));
            APIResponse<SearchResult<OrderVo>> streamerApiResponse = orderApi.queryUserOrders(getInstance(queryUserOrdersRequest));
            log.info("orderApi queryUserOrders response: {}", JsonUtils.toJsonHasNullKey(streamerApiResponse));
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
    @ApiOperation(value = "全仓查询子账户历史订单")
    public CommonPageRet<TradeRet> queryTradeHistory(@RequestBody @Validated QuerySubUserTradeHistoryRequest request) throws Exception {

        /*final Long parentUserId = getUserId();*/
        log.info("margin queryTradeHistory request: {}", JsonUtils.toJsonHasNullKey(request));
        List<Long> marginUserIds = new ArrayList<>();
        Long marginUserId = subUserMarginRelatedService.getMarginUserIdByEmail(request.getEmail());
        marginUserIds.add(marginUserId);
        CommonPageRet<TradeRet> response = new CommonPageRet<TradeRet>(Collections.EMPTY_LIST, 0);

        if (CollectionUtils.isNotEmpty(marginUserIds)) {
            QueryUserTradeRequest queryUserTradeRequest = new QueryUserTradeRequest();
            BeanUtils.copyProperties(request, queryUserTradeRequest);
            /*queryUserTradeRequest.setUserId(parentUserId);*/
            queryUserTradeRequest.setUserIds(marginUserIds);
            log.info("tradeApi getUserTrades request: {}", JsonUtils.toJsonHasNullKey(queryUserTradeRequest));
            APIResponse<SearchResult<TradeVo>> apiResponse = tradeApi.getUserTrades(getInstance(queryUserTradeRequest));
            log.info("tradeApi getUserTrades response: {}", JsonUtils.toJsonHasNullKey(apiResponse));
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
    @ApiOperation(value = "全仓查询子账号下的资金流水")
    public CommonPageRet<MarginCapitalFlowResponseRet> queryTransactionHistory(@RequestBody @Validated QuerySubUserCapitalFlowRequest request) throws Exception {

        if (subUserMarginRelatedService.getMonthSpace(request.getStartTime(), request.getEndTime())) {
            throw new BusinessException(AccountMgsErrorCode.SEARCH_TIME_GREATER_THAN_THREE_MONTH);
        }
        Long uid = subUserMarginRelatedService.getUid(request.getEmail(), request.getSymbol());
        UserAssetLogRequest assetLogRequest = subUserMarginRelatedService.buildRequest(request, uid);
        log.info("iUserAssetLogApi queryUserAssetLog request: email:{}, param:{}", request.getEmail(), JsonUtils.toJsonHasNullKey(assetLogRequest));
        APIResponse<PagingResult<UserAssetLogResponse>> apiResponse = iUserAssetLogApi.queryUserAssetLog(APIRequest.instance(assetLogRequest));
        log.info("iUserAssetLogApi queryUserAssetLog response: {}", JsonUtils.toJsonHasNullKey(apiResponse));
        boolean notOpenAccount = MarginHelper.isNotOpenAccount(apiResponse);
        if (notOpenAccount) {
            return new CommonPageRet<MarginCapitalFlowResponseRet>(Collections.EMPTY_LIST, 0);
        }
        checkResponse(apiResponse);
        PagingResult<UserAssetLogResponse> data = apiResponse.getData();
        return new CommonPageRet<>(subUserMarginRelatedService.processData(data.getRows(), request.getSymbol()), data.getTotal());
    }

    @PostMapping("/borrow-history")
    @ApiOperation(value = "全仓查询借款历史")
    public CommonPageRet<BorrowHistoryResponseRet> queryBorrowHistory(@RequestBody @Validated QuerySubUserBorrowHistoryRequest request) {

        Long subUserId = subUserMarginRelatedService.getSubUserId(request.getEmail());
        log.info("bookKeeperApi findBorrowHistories request: subUserId:{}, param:{}", subUserId, JsonUtils.toJsonHasNullKey(request));
        final APIResponse<Results<MgsBorrowHistoryDto>> borrowHistories = bookKeeperApi.findBorrowHistories(subUserId, request.getAsset(),
                request.getTxId(), request.getStartTime(), request.getEndTime(), request.getCurrent(), request.getSize(), request.isArchived(), request.isNeedBorrowType());
        log.info("bookKeeperApi findBorrowHistories response: {}", JsonUtils.toJsonHasNullKey(borrowHistories));
        if (MarginHelper.isNotOpenAccount(borrowHistories)) {
            return MarginHelper.emptyResponse();
        }
        checkResponse(borrowHistories);
        final Results<MgsBorrowHistoryDto> data = borrowHistories.getData();
        /*return new CommonPageRet<>(borrowHistoryBeanMapper.read(data.getRows()), data.getTotal());*/
        List<BorrowHistoryResponseRet> retList = null;
        if (!CollectionUtils.isEmpty(data.getRows())) {
            retList = data.getRows().stream().map(BorrowHistoryResponseRet::of).collect(Collectors.toList());
        }
        return new CommonPageRet<>(retList, data.getTotal());
    }

    @PostMapping("/repay-history")
    @ApiOperation(value = "全仓查询还款历史")
    public CommonPageRet<RepayHistoryResponseRet> queryRepayHistory(@RequestBody @Validated QuerySubUserRepayHistoryRequest request) {

        Long subUserId = subUserMarginRelatedService.getSubUserId(request.getEmail());
        log.info("bookKeeperApi repayHistory request: subUserId:{}, param:{}", subUserId, JsonUtils.toJsonHasNullKey(request));
        final APIResponse<Results<MgsRepayHistoryDto>> resultsApiResponse = bookKeeperApi.repayHistory(subUserId, request.getAsset(), request.getTxId(), request.getStartTime(), request.getEndTime(), request.getCurrent(), request.getSize(), request.isArchived(), request.isNeedRepayType());
        log.info("bookKeeperApi repayHistory response: {}", JsonUtils.toJsonHasNullKey(resultsApiResponse));
        if (MarginHelper.isNotOpenAccount(resultsApiResponse)) {
            return MarginHelper.emptyResponse();
        }
        checkResponse(resultsApiResponse);
        final Results<MgsRepayHistoryDto> data = resultsApiResponse.getData();
        /*return new CommonPageRet<>(repayHistoryBeanMapper.read(data.getRows()), data.getTotal());*/
        List<RepayHistoryResponseRet> retList = null;
        if (!CollectionUtils.isEmpty(data.getRows())) {
            retList = data.getRows().stream().map(RepayHistoryResponseRet::of).collect(Collectors.toList());
        }
        return new CommonPageRet<>(retList, data.getTotal());
    }

    @PostMapping("/trade-detail")
    @ApiOperation(value = "全仓交易详情信息")
    public CommonPageRet<TradeDetailResponseRet> queryTradeDetail(@RequestBody @Validated QueryTradeDetailRequest request) throws Exception {
        log.info("SubUserMarginAllController queryTradeDetail request: {}", JsonUtils.toJsonHasNullKey(request));
        QueryTradeDetailsRequest queryTradeDetailsRequest = subUserMarginRelatedService.buildTradeDetailRequest(request);
        log.info("tradeApi fetchUserTradeDetails request: {}", JsonUtils.toJsonHasNullKey(queryTradeDetailsRequest));
        APIResponse<SearchResult<TradeDetailVo>> apiResponse = tradeApi.fetchUserTradeDetails(getInstance(queryTradeDetailsRequest));
        log.info("tradeApi fetchUserTradeDetails response: {}", JsonUtils.toJsonHasNullKey(apiResponse));
        checkResponse(apiResponse);
        SearchResult<TradeDetailVo> result = apiResponse.getData();
        CommonPageRet<TradeDetailResponseRet> response = new CommonPageRet<>();
        response.setTotal(result.getTotal());
        response.setData(ListTransformUtil.transform(result.getRows(), TradeDetailResponseRet.class));
        return response;
    }
}
