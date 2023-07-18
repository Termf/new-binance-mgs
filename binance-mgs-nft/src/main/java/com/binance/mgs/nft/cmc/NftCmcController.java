package com.binance.mgs.nft.cmc;

import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.nft.market.vo.cmc.CmcCollectionVo;
import com.binance.nft.market.vo.cmc.CmcTransactionVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/v1/public/nft/cmc")
public class NftCmcController {

    private static final int DEFAULT_MAX_LIMIT = 100;
    private static final String ILLEGAL_PARAM = "Limit param cannot be greater than 100";

    @Resource
    private NftCmcCacheProxy nftCmcCacheProxy;

    @GetMapping("/collection-categories")
    public CommonRet<List<String>> getNFTCategories() throws Exception {
        List<String> res = nftCmcCacheProxy.getNFTCategories();
        return new CommonRet<>(res);
    }

    @GetMapping("/collection-data")
    public CommonRet<CmcCollectionVo> getNFTCollections(@RequestParam(value="start", defaultValue="0") int start,
                                                        @RequestParam(value="limit", defaultValue="50") int limit) {
        if (limit > DEFAULT_MAX_LIMIT){
            throw new IllegalArgumentException(ILLEGAL_PARAM);
        }
        CmcCollectionVo cmcCollectionVo = nftCmcCacheProxy.getNFTCollections(start+1, limit);
        return new CommonRet<>(cmcCollectionVo);
    }

    @GetMapping("/collection-transaction/{slug}")
    public CommonRet<CmcTransactionVo> getCollectionTransaction(@PathVariable(value = "slug") long slug,
                                                                @RequestParam(value = "start", defaultValue = "0") int start,
                                                                @RequestParam(value = "limit", defaultValue = "50") int limit) {
        if (limit > DEFAULT_MAX_LIMIT){
            throw new IllegalArgumentException(ILLEGAL_PARAM);
        }
        CmcTransactionVo cmcTransactionVo = nftCmcCacheProxy.getCollectionTransaction(slug, start+1, limit);
        return new CommonRet<>(cmcTransactionVo);
    }

    @PostMapping("/collection-transaction-by-date")
    public CommonRet<CmcTransactionVo> getCollectionTransactionByDate(@RequestBody @Valid CmcTrancsationByDateVo req) {
        CmcTransactionVo cmcTransactionVo = nftCmcCacheProxy.getCollectionTransaction(req.getSlug(),req.getStart()+1,
                req.getLimit(), req.getStartDate(), req.getEndDate());
        return new CommonRet<>(cmcTransactionVo);
    }

}
