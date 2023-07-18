package com.binance.mgs.account.account.controller;

import com.binance.futurestreamer.api.request.trade.QueryTradeDetailsRequest;
import com.binance.futurestreamer.api.response.trade.TradeDetailVo;
import com.binance.futurestreamer.api.trade.TradeApi;
import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.vo.future.QueryTradeDetailRequest;
import com.binance.mgs.account.account.vo.future.TradeRet;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/private/account/future/user-data")
@Log4j2
@Api
public class FutureUserDataController extends BaseAction {

    @Autowired
    private VerifyRelationService verifyRelationService;

    @Autowired
    private TradeApi tradeApi;

    @PostMapping("/trade-detail")
    public CommonPageRet<TradeRet> queryTradeDetailHistory(@RequestBody @Validated QueryTradeDetailRequest request) {
        QueryTradeDetailsRequest queryUserTradesRequest = new QueryTradeDetailsRequest();
        BeanUtils.copyProperties(request, queryUserTradesRequest);
        queryUserTradesRequest.setUserId(verifyRelationService.checkRelationAndFetchFutureUserId(request.getUserId()));
        APIResponse<SearchResult<TradeDetailVo>> response = tradeApi.queryUserTradeDetails(APIRequest.instance(queryUserTradesRequest));
        checkResponse(response);
        SearchResult<TradeDetailVo> data = response.getData();
        List<TradeRet> result = data.getRows().stream().map(TradeRet::of).collect(Collectors.toList());
        return new CommonPageRet<>(result, data.getTotal());
    }

}
