package com.binance.mgs.nft.trade;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.mgs.nft.trade.response.NftSellOrderItemVo;
import com.binance.nft.market.ifae.SellHistoryApi;
import com.binance.nft.market.request.QuerySellHistoryRequest;
import com.binance.nft.market.vo.SellHistoryVo;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.nft.tradeservice.api.ISellOrderApi;
import com.binance.nft.tradeservice.request.SellHistoryRequest;
import com.binance.nft.tradeservice.vo.SellOrderItemVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class SellOrderController {

    @Resource
    private ISellOrderApi sellOrderApi;

    @Resource
    private BaseHelper baseHelper;
    @Resource
    private NftInboxHelper nftInboxHelper;

    @ApolloJsonValue("${mgs.nft.sellhistory.userid.whitelist:[]}")
    private List<Long> userIdWhiteList;


    @Value("${mgs.nft.userid.rate:40}")
    private Long rate;

    @Resource
    private SellHistoryApi sellHistoryApi;


    /**
     * 交易历史
     * @return
     */
    @PostMapping("/private/nft/nft-trade/sell-history")
    public CommonRet<SearchResult<NftSellOrderItemVo>> orderHistory(@Valid @RequestBody SellHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        if (CollectionUtils.contains(userIdWhiteList.iterator(),request.getUserId()) || request.getUserId()%100 <= rate){
            QuerySellHistoryRequest sellHistoryRequest = CopyBeanUtils.fastCopy(request,QuerySellHistoryRequest.class);
            APIResponse<SearchResult<SellHistoryVo>> sellHisResp = sellHistoryApi.sellHistroyList(APIRequest.instance(sellHistoryRequest));
            baseHelper.checkResponse(sellHisResp);
            List<BizIdModel> bizIds = Arrays.asList(new BizIdModel(HistoryType.SALES.name(), sellHisResp.getData().getRows().stream().map(s->s.getId()).collect(Collectors.toList())));
            SearchResult<NftSellOrderItemVo> retResult = nftInboxHelper.searchResultWithFlag(sellHisResp.getData(), NftSellOrderItemVo.class,
                    NftSellOrderItemVo::getId, bizIds, NftSellOrderItemVo::setUnreadFlag);
            return new CommonRet<>(retResult);
        }
        APIResponse<SearchResult<SellOrderItemVo>> response = sellOrderApi.sellHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        if(response.getData() != null && org.apache.commons.collections4.CollectionUtils.isNotEmpty(response.getData().getRows())) {
            List<BizIdModel> bizIdList = Arrays.asList(new BizIdModel(HistoryType.SALES.name(), response.getData().getRows().stream().map(s->s.getId()).collect(Collectors.toList())));
            SearchResult<NftSellOrderItemVo> retResult = nftInboxHelper.searchResultWithFlag(response.getData(), NftSellOrderItemVo.class,
                    NftSellOrderItemVo::getId, bizIdList, NftSellOrderItemVo::setUnreadFlag);
            return new CommonRet<>(retResult);
        }

        return new CommonRet<>(new SearchResult<>());
    }

}
