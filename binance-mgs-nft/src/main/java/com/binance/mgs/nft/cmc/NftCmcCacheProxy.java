package com.binance.mgs.nft.cmc;

import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.nft.market.ifae.NftCmcApi;
import com.binance.nft.market.request.CmcQueryRequest;
import com.binance.nft.market.vo.CommonPageRequest;
import com.binance.nft.market.vo.cmc.CmcCollectionVo;
import com.binance.nft.market.vo.cmc.CmcTransactionVo;
import com.binance.nft.tradeservice.api.ITradeConfApi;
import com.binance.nft.tradeservice.response.ProductOnsaleConfResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class NftCmcCacheProxy {

    @Resource
    BaseHelper baseHelper;

    @Resource
    private ITradeConfApi tradeConfApi;

    @Resource
    private NftCmcApi nftCmcApi;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_BIG, key = "'cmc-category'")
    public List<String> getNFTCategories() throws Exception {
        APIResponse<ProductOnsaleConfResponse> categories = tradeConfApi.config();
        baseHelper.checkResponse(categories);
        List<String> res = new ArrayList<>();
        if (categories.getData() != null &&  categories.getData().getCategoryList() != null){
            res = categories.getData().getCategoryList().stream().map(c -> c.getName()).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return res;
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'getNFTCollections-'+#start+'-'+#limit")
    public CmcCollectionVo getNFTCollections(int start, int limit){
        CommonPageRequest<CmcQueryRequest> request = CommonPageRequest.<CmcQueryRequest>builder().page(start).size(limit).params(CmcQueryRequest.builder().build()).build();
        APIResponse<CmcCollectionVo> response = nftCmcApi.getNFTCollections(request);
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'getCollectionTransaction-'+#slug+'-'+#start+'-'+#limit")
    public CmcTransactionVo getCollectionTransaction(long slug, int start, int limit){
        CommonPageRequest<CmcQueryRequest> request = CommonPageRequest.<CmcQueryRequest>builder().page(start)
                .size(limit).params(CmcQueryRequest.builder().slug(slug).build()).build();
        return executeCollectionTransaction(request);
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'getCollectionTransaction-'+#slug+'-'+#start+'-'+#limit+#startDate+'-'+#endDate")
    public CmcTransactionVo getCollectionTransaction(long slug, int start, int limit, Date startDate, Date endDate){
        CmcQueryRequest cmcQueryRequest = CmcQueryRequest.builder().slug(slug).startDate(startDate).endDate(endDate).build();
        CommonPageRequest<CmcQueryRequest> request = CommonPageRequest.<CmcQueryRequest>builder()
                .page(start == 0? 1 : start)
                .size(limit == 0 ? 50 : limit)
                .params(cmcQueryRequest)
                .build();
        return executeCollectionTransaction(request);
    }

    private CmcTransactionVo executeCollectionTransaction(CommonPageRequest<CmcQueryRequest> request) {
        APIResponse<CmcTransactionVo> response = nftCmcApi.getCollectionTransactionByDate(request);
        baseHelper.checkResponse(response);
        return response.getData();
    }

}

