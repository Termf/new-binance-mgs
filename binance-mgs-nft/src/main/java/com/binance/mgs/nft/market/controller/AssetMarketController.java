package com.binance.mgs.nft.market.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.mgs.nft.market.request.PropertySearchMgsRequest;
import com.binance.mgs.nft.market.vo.MgsSearchResult;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.nft.market.ifae.AssetMarketApi;
import com.binance.nft.market.request.AssetMarketRequest;
import com.binance.nft.market.request.AvailableTokenRequest;
import com.binance.nft.market.request.PropertySearchRequest;
import com.binance.nft.market.vo.AssetMarketVo;
import com.binance.nft.market.vo.AvailableTokenVo;
import com.binance.nft.market.vo.PropertyValueItem;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.tradeservice.enums.SourceTypeEnum;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class AssetMarketController {

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;
    private final MarketCacheProxy marketCacheProxy;

    private final BaseHelper baseHelper;

    private final ApproveHelper approveHelper;

    private final AssetMarketApi assetMarketApi;

    private final NftAssetHelper nftAssetHelper;

    @PostMapping(value = "/friendly/nft/asset/market/asset-list")
    CommonRet<SearchResult<AssetMarketVo>> assetList(@Valid @RequestBody AssetMarketRequest request) throws Exception {
        Long userId = baseHelper.getUserId();
        List<Integer> productSource = null;
        if(!marketCacheProxy.containsDex(userId)) {
            productSource = Arrays.asList(SourceTypeEnum.OPENSEA.getCode(), SourceTypeEnum.LOOKSRARE.getCode(), SourceTypeEnum.X2Y2.getCode());
        }
        request.setProductSource(productSource);
        SearchResult<AssetMarketVo> resp = marketCacheProxy.assetList(request, userId);
        return assetListPostProc(resp, request.getPage(), request.getRows());
    }

    @PostMapping(value = "/friendly/nft/asset/market/availabe-tokens")
    CommonRet<SearchResult<AvailableTokenVo>> availableTokenList(@Valid @RequestBody AvailableTokenRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SearchResult<AvailableTokenVo>> resp = assetMarketApi.availableTokenList(APIRequest.instance(request));
        com.binance.nftcore.utils.lambda.check.BaseHelper.checkResponse(resp);
        return new CommonRet<>(resp.getData());
    }

    private CommonRet<SearchResult<AssetMarketVo>> assetListPostProc(SearchResult<AssetMarketVo> resp, int page, int size) {
        if (CollectionUtils.isEmpty(resp.getRows())) {
            return new CommonRet<>(new MgsSearchResult<AssetMarketVo>(resp.getRows(), resp.getTotal(), false));
        }
        UserApproveInfo defaultInfo =UserApproveInfo.builder()
                .approve(false)
                .count(0L)
                .build();
        List<AssetMarketVo> list = resp.getRows().stream().filter(item -> {
            item.setApprove(defaultInfo);
            if(Objects.isNull(item.getProductSource()) ||!SourceTypeEnum.isExternal(item.getProductSource())) {
                return true;
            }
            return marketCacheProxy.checkStockAvailable(item.getProductId());
        }).collect(Collectors.toList());
        return new CommonRet<>(new MgsSearchResult<AssetMarketVo>(list, resp.getTotal(), page, size));
    }

    @PostMapping(value = "/friendly/nft/asset/market/property-search")
    CommonRet<Map<String, List<PropertyValueItem>>> propertySearch(@Valid @RequestBody PropertySearchMgsRequest request) {
        Long userId = baseHelper.getUserId();
        PropertySearchRequest req = new PropertySearchRequest();
        req.setUserId(userId);
        req.setKeyword(request.getKeyword());
        req.setCollectionId(request.getCollectionId());
        req.setSource(request.getSource());
        APIResponse<Map<String, List<PropertyValueItem>>> mapAPIResponse = assetMarketApi.propertySearch(APIRequest.instance(req));
        return new CommonRet(mapAPIResponse);
    }

}
