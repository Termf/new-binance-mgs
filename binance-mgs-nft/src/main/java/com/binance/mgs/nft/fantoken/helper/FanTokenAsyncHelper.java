package com.binance.mgs.nft.fantoken.helper;

import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.nft.fantoken.request.QueryNftMarketRequest;
import com.binance.nft.fantoken.response.QueryNftMarketResponse;
import com.binance.nft.market.request.MysteryProductQueryArg;
import com.binance.nft.market.vo.MysteryProductItemVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h1>FanToken 一些异步执行的任务</h1>
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class FanTokenAsyncHelper {

    /** 用于查询 NFT Market 的线程池 */
    private static final ExecutorService nftMarketExecutors = Executors.newFixedThreadPool(6);
    private final MarketCacheProxy marketCacheProxy;

    /**
     * <h2>查询 NFT 市场的售卖信息</h2>
     * */
    public QueryNftMarketResponse queryNftMarket(QueryNftMarketRequest request) {

        CompletionService<QueryNftMarketResponse.KeywordSize> completionService =
                new ExecutorCompletionService<>(nftMarketExecutors);

        request.getKeywords().forEach(k -> completionService.submit(() -> {

            // 参考 MarketMysteryController.mysteryList 方法
            MysteryProductQueryArg productQueryArg = new MysteryProductQueryArg();
            productQueryArg.setKeyword(k);
            productQueryArg.setTradeType(0);
            productQueryArg.setSerialNo(Collections.singletonList(Long.parseLong(request.getSerialsNo())));

            com.binance.nft.market.vo.CommonPageRequest<MysteryProductQueryArg> arg =
                    new com.binance.nft.market.vo.CommonPageRequest<>();
            arg.setPage(1);
            arg.setSize(1);
            arg.setParams(productQueryArg);

            List<MysteryProductItemVo> productItemVos = marketCacheProxy.mysteryList(arg).getData();
            return new QueryNftMarketResponse.KeywordSize(
                    k, CollectionUtils.isNotEmpty(productItemVos) ? productItemVos.size() : 0
            );
        }));

        List<QueryNftMarketResponse.KeywordSize> keywordSizes = new ArrayList<>(request.getKeywords().size());

        for (int i = 0; i != request.getKeywords().size(); ++i) {

            try {
                QueryNftMarketResponse.KeywordSize keywordSize = completionService.take().get();
                if (null != keywordSize) {
                    keywordSizes.add(keywordSize);
                }
            } catch (InterruptedException | ExecutionException ex) {
                log.error("async get nft market sale info error: [{}]", ex.getMessage(), ex);
            }
        }

        return new QueryNftMarketResponse(request.getSerialsNo(), keywordSizes);
    }
}
