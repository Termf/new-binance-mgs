package com.binance.mgs.nft.market.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.mgs.nft.market.converter.MarketConvert;
import com.binance.mgs.nft.market.proxy.MarketCacheProxy;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.MarketProductMgsItem;
import com.binance.mgs.nft.market.vo.MarketProductReq;
import com.binance.mgs.nft.market.vo.UserInfoMgsVo;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.nft.assetservice.api.data.vo.ItemsApproveInfo;
import com.binance.nft.common.utils.ObjectUtils;
import com.binance.nft.market.ifae.NftMarketApi;
import com.binance.nft.market.request.MarketProductRequest;
import com.binance.nft.market.vo.MarketProductItem;
import com.binance.nft.market.vo.ProductTagInfoVo;
import com.binance.nft.market.vo.ProductTagVo;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.market.vo.homepage.Banner;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class MarketProductController {

    private final BaseHelper baseHelper;

    private final NftMarketApi marketApi;

    private final MarketCacheProxy marketCacheProxy;

    private final ApproveHelper approveHelper;

    private final CrowdinHelper crowdinHelper;

    private final MarketConvert marketConvert;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @PostMapping({"/friendly/nft/product-list"})
    public CommonRet<SearchResult<MarketProductMgsItem>> productList(@Valid @RequestBody MarketProductReq request) throws Exception {
        if (request.getRows() == null || request.getRows() != 16){
            return new CommonRet<>();
        }
        MarketProductRequest marketProductRequest = marketConvert.convertProductList(request);
        SearchResult<MarketProductItem> resp = marketCacheProxy.productList(marketProductRequest);
        return productListPostProc(resp);
    }

    @PostMapping({"/friendly/nft/mgs/product-list"})
    public CommonRet<SearchResult<MarketProductMgsItem>> productList(@Valid @RequestBody MarketProductRequest request) throws Exception {
        if (request.getRows() == null || request.getRows() != 16){
            return new CommonRet<>();
        }
        SearchResult<MarketProductItem> resp = marketCacheProxy.productList(request);
        return productListPostProc(resp);
    }

    @PostMapping("/public/nft/banner-list")
    public CommonRet<List<Banner>> bannerList(HttpServletRequest request) {
        APIResponse<List<Banner>> listAPIResponse = marketApi.marketplaceBanners(APIRequest.instance(isGray(request)));
        baseHelper.checkResponse(listAPIResponse);
        return new CommonRet<>(listAPIResponse.getData());
    }

    @GetMapping("/public/nft/product-tag-list")
    public CommonRet<List<ProductTagInfoVo>> productTagList(HttpServletRequest request) {
        List<ProductTagInfoVo> resultList = marketCacheProxy.productTagList(BooleanUtils.toInteger(!isGray(request)));

        if (CollectionUtils.isNotEmpty(resultList)) {
            resultList = resultList.stream()
                    .map(pt -> {
                        ProductTagInfoVo res = CopyBeanUtils.fastCopy(pt, ProductTagInfoVo.class);
                        ProductTagVo tag = CopyBeanUtils.fastCopy(res.getProductTag(), ProductTagVo.class);
                        String message = crowdinHelper.getMessageByKey(tag.getTagKey(), baseHelper.getLanguage());
                        message = StringUtils.equals(message, tag.getTagKey()) ? tag.getTagName() : message;
                        tag.setTagName(message);
                        res.setProductTag(tag);
                        return res;
                    })
                    .collect(Collectors.toList());

        }

        return new CommonRet<>(resultList);
    }

    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);

    }

    private CommonRet<SearchResult<MarketProductMgsItem>> productListPostProc(SearchResult<MarketProductItem> resp) {
        Long userId = baseHelper.getUserId();
        SearchResult<MarketProductMgsItem> result = new SearchResult<>();
        Long timestamp = DateUtils.getNewUTCTimeMillis();
        if (CollectionUtils.isNotEmpty(resp.getRows())) {
            List<MarketProductMgsItem> itemList = new ArrayList<>(resp.getRows().size());
            resp.getRows().forEach(item -> {
                item.setTimestamp(timestamp);
                MarketProductMgsItem marketProductMgsItem = CopyBeanUtils.fastCopy(item, MarketProductMgsItem.class);
                if (!ObjectUtils.isEmpty(item.getOwner())){
                    UserInfoMgsVo owner = CopyBeanUtils.fastCopy(item.getOwner(), UserInfoMgsVo.class);

                    if (!ObjectUtils.isEmpty(item.getOwner().getUserId())){
                        owner.setUserId(AesUtil.encrypt(item.getOwner().getUserId().toString(), AES_PASSWORD));
                    }
                    marketProductMgsItem.setOwner(owner);
                }
                if (!ObjectUtils.isEmpty(item.getCreator())){
                    UserInfoMgsVo creator = CopyBeanUtils.fastCopy(item.getCreator(), UserInfoMgsVo.class);
                    if (!ObjectUtils.isEmpty(item.getCreator().getUserId())){
                        creator.setUserId(AesUtil.encrypt(item.getCreator().getUserId().toString(), AES_PASSWORD));
                    }
                    marketProductMgsItem.setCreator(creator);
                }
                itemList.add(marketProductMgsItem);
            });
            result.setRows(itemList);
            result.setTotal(resp.getTotal());

            if (CollectionUtils.isNotEmpty(result.getRows())) {
                List<Long> productIdList = result.getRows().stream()
                        .filter(p -> Objects.equals(p.getNftType(), 1))
                        .map(MarketProductMgsItem::getProductId)
                        .collect(Collectors.toList());
                Map<Long, ItemsApproveInfo> approveInfoMap = approveHelper.queryApproveInfoMap(productIdList, userId);
                result.getRows().forEach(item -> {
                    item.setTimestamp(timestamp);
                    ItemsApproveInfo approveInfo = approveInfoMap.get(item.getProductId());
                    if (approveInfo != null) {
                        item.setApprove(UserApproveInfo.builder()
                                .approve(approveInfo.isApprove())
                                .count(approveInfo.getCount())
                                .build());
                    }
                });
            }
        }
        return new CommonRet<>(result);
    }

}
