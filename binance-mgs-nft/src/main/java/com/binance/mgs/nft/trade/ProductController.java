package com.binance.mgs.nft.trade;

import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.common.cache.CacheSeneEnum;
import com.binance.mgs.nft.common.cache.CacheUtils;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.mgs.nft.mysterybox.helper.MysteryBoxI18nHelper;
import com.binance.mgs.nft.mysterybox.vo.MysteryBoxProductDetailIncludeAssetVo;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.mgs.nft.nftasset.controller.helper.ReportHelper;
import com.binance.mgs.nft.nftasset.controller.helper.UserHelper;
import com.binance.mgs.nft.nftasset.vo.AssetDetailReq;
import com.binance.mgs.nft.reconciliation.helper.ReconciliationHelper;
import com.binance.mgs.nft.trade.convertor.ProductDetailExtendMgsResponseConvertor;
import com.binance.mgs.nft.trade.proxy.TradeCacheProxy;
import com.binance.mgs.nft.trade.request.BatchOffsaleRequest;
import com.binance.mgs.nft.trade.response.NftInfoMgsVo;
import com.binance.mgs.nft.trade.response.ProductDetailExtendMgsResponse;
import com.binance.mgs.nft.trade.response.ProductDetailExtendResponse;
import com.binance.mgs.nft.trade.service.TradeService;
import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.reconcilication.api.INftRoyaltyFeeApi;
import com.binance.nft.reconcilication.response.GetRoyaltyFeeResponse;
import com.binance.nft.tradeservice.api.IOpenListApi;
import com.binance.nft.tradeservice.api.IProductApi;
import com.binance.nft.tradeservice.api.ITradeConfApi;
import com.binance.nft.tradeservice.constant.Constants;
import com.binance.nft.tradeservice.enums.*;
import com.binance.nft.tradeservice.request.*;
import com.binance.nft.tradeservice.response.*;
import com.binance.nft.tradeservice.utils.Switcher;
import com.binance.nft.tradeservice.vo.CategoryVo;
import com.binance.nft.tradeservice.vo.CollectionInfoVo;
import com.binance.nft.tradeservice.vo.ProductDetailVo;
import com.binance.nft.tradeservice.vo.TradeHistoryVo;
import com.binance.nftcore.utils.Assert;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class ProductController {

    @Resource
    private IProductApi productApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private TradeCacheProxy tradeCacheProxy;
    @Resource
    private TradeService tradeService;
    @Resource
    private CrowdinHelper crowdinHelper;
    @Resource
    private MysteryBoxI18nHelper mysteryBoxI18nHelper;
    @Resource
    private ApproveHelper approveHelper;
    @Resource
    private ReportHelper reportHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;
    @Resource
    private UserHelper userHelper;
    @Resource
    private NftAssetHelper nftAssetHelper;
    @Resource
    private ReconciliationHelper reconciliationHelper;
    @Resource
    private INftRoyaltyFeeApi iNftRoyaltyFeeApi;

    @Resource
    private ITradeConfApi confApi;
    @Resource
    private IOpenListApi openListApi;
    @Resource
    private RedissonClient redissonClient;


    private GetRoyaltyFeeResponse paddingFeesInfo(Long collectionId, boolean premium) {
        APIResponse<GetRoyaltyFeeResponse> getFeesResponse = iNftRoyaltyFeeApi.getRoyaltyFee(collectionId,
                null, premium);

        if(!baseHelper.isOk(getFeesResponse)) {
            throw new BusinessException("fees information error");
        }
        return getFeesResponse.getData();
    }

    /**
     * 上架
     *
     * @return
     */
    @GoogleRecaptha("/private/nft/nft-trade/product-onsale")
    @PostMapping("/private/nft/nft-trade/product-onsale")
    @UserOperation(eventName = "NFT_Market_Listing", name = "NFT_Market_Listing", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<ProductCreateResponse> productOnsale(@Valid @RequestBody ProductCreateRequest request) throws Exception {

        request.setUserId(baseHelper.getUserId());
        request.setSellerId(baseHelper.getUserId());
        request.setSource(SourceTypeEnum.WEB.getCode());
        Long leftTime = tradeService.canOnsale(request.getRelateId(), request.getNftType());
        //check auction step
        Assert.isTrue(request.getTradeType() != 1 || request.getAmount().compareTo(request.getStepAmount() != null ? request.getStepAmount() : BigDecimal.ZERO) > 0, TradeErrorCode.PARAM_ERROR);
        Assert.isTrue(leftTime <= 0L, TradeErrorCode.MYSTERY_ONSALE_LIMIT, Math.max(leftTime / (1000 * 60 * 60), 1));
        // nft type 不等于2或者不等于3的话，category 如果不为0的话，就说明有问题。
        boolean isMystery = NftTypeEnum.OPENED_BOX.getCode().equals(request.getNftType()) ||
                NftTypeEnum.UNOPENED_BOX.getCode().equals(request.getNftType());
        Assert.isTrue(!isMystery || StringUtils.isEmpty(request.getSetStartTime()), TradeErrorCode.PARAM_ERROR);
        boolean isMysteryCategoryCheck = isMystery && !ProductCategoryEnum.MysteryBox.getCode().equals(request.getCategory());
        Assert.isTrue(!isMysteryCategoryCheck, TradeErrorCode.PARAM_ERROR);

        boolean needLimitOnSale =  tradeService.needLimitOnSale(request,request.getNftIds());
        Integer onSaleCount = 0;
        if(needLimitOnSale) {
            onSaleCount = userHelper.checkOnSaleCount(baseHelper.getUserId(),request.getNftIds());
        }
        APIResponse<ProductCreateResponse> response = productApi.onsale(APIRequest.instance(request));
        if(needLimitOnSale) {
            cacheNft(baseHelper.isOk(response),onSaleCount,request.getNftIds());
        }
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    private void cacheNft(boolean response, Integer onSaleCount, List<Long> nftIds) {
        if(response) {
            userHelper.cacheNftIds(baseHelper.getUserId(),nftIds,onSaleCount);
            return;
        }
        userHelper.decrOnSaleCount(baseHelper.getUserId(),onSaleCount);
    }


    /**
     * 上架撤销
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/product-onsale-revoke")
    @UserOperation(eventName = "NFT_Retractlisting", name = "NFT_Retractlisting", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<Void> productOnsaleRevoke(@Valid @RequestBody ProductOffsaleRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<List<Long>> response = productApi.onsaleRevoke(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        if(baseHelper.isOk(response)) {
            log.error("onsaleRevoke nft = " + JsonUtils.toJsonHasNullKey(response.getData()));
            userHelper.checkNftIdExist(baseHelper.getUserId(),response.getData().get(0));
        }
        return new CommonRet<>();
    }


    /**
     * 下架
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/product-offsale")
    @UserOperation(eventName = "NFT_Delisting", name = "NFT_Delisting", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<ProductOffsaleResponse> productOffsale(@Valid @RequestBody ProductOffsaleRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        request.setSource(SourceTypeEnum.WEB.getCode());
        APIResponse<ProductOffsaleResponse> response = productApi.offsale(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * 下架check
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/product-offsale-check")
    public CommonRet<ProductOffsaleCheckResponse> productOffsaleCheck(@Valid @RequestBody ProductOffsaleCheckRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<ProductOffsaleCheckResponse> response = productApi.offsaleCheck(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * 商品详情页
     * 商品详情
     *
     * @return
     */
    @PostMapping("/friendly/nft/nft-trade/product-detail")
    public CommonRet<ProductDetailExtendMgsResponse> productDetail(@Valid @RequestBody ProductDetailRequest request) throws Exception {

        Long userId = baseHelper.getUserId();
        ProductDetailExtendResponse response = tradeCacheProxy.productDetail(request);
        if(Objects.equals(response, tradeCacheProxy.productDetailExtendResponse)) {
            return new CommonRet();
        }
        ProductDetailExtendResponse resp = CopyBeanUtils.fastCopy(response, ProductDetailExtendResponse.class);
        resp.setTimestamp(DateUtils.getNewUTCTimeMillis());

        Optional.ofNullable(resp.getProductDetail()).ifPresent(d -> {
            boolean onsaleApply = ProductStatusEnum.ONSALE_APPLY.statusEquals(d.getStatus());
            Assert.isTrue(!onsaleApply ||Objects.equals(userId, d.getListerId()), TradeErrorCode.PRODUCT_NOT_EXISTS);
            ProductDetailVo vo = CopyBeanUtils.fastCopy(d, ProductDetailVo.class);
            resp.setProductDetail(vo);

            boolean isOwner = Objects.equals(d.getListerId(), userId);
            resp.setIsOwner(BooleanUtils.toInteger(isOwner));
            vo.setListerId(null);
            vo.setCreatorId(null);

            if (Objects.nonNull(d.getCategoryVo())) {
                CategoryVo category = CopyBeanUtils.fastCopy(d.getCategoryVo(), CategoryVo.class);
                vo.setCategoryVo(category);
                String json = crowdinHelper.getMessageByKey(Constants.TRADE_MARKET_CATEGORYS_KEY, baseHelper.getLanguage());
                Switcher.<List<CategoryVo>>Case()
                        .when(!Objects.equals(json, Constants.TRADE_MARKET_CATEGORYS_KEY))
                        .then(() -> {
                            List<CategoryVo> categoryList = JsonUtils.toObjList(json, CategoryVo.class);
                            if (CollectionUtils.isNotEmpty(categoryList)) {
                                categoryList.stream().filter(c -> Objects.equals(c.getCode(), d.getCategory()))
                                        .findAny().ifPresent(c -> category.setName(c.getName()));
                            }
                        });
            }
        });

        try {
            // mystrybox detail i18n
            if (resp.getMysteryBoxProductDetailVo() != null) {
                MysteryBoxProductDetailIncludeAssetVo mysteryBoxProductDetailIncludeAssetVo =
                        CopyBeanUtils.fastCopy(resp.getMysteryBoxProductDetailVo(),
                                MysteryBoxProductDetailIncludeAssetVo.class);
                tradeService.checkAndSetSecondDelay(mysteryBoxProductDetailIncludeAssetVo);
                mysteryBoxI18nHelper.doI18n(mysteryBoxProductDetailIncludeAssetVo);
                resp.setMysteryBoxProductDetailVo(mysteryBoxProductDetailIncludeAssetVo);
            } else if (Objects.nonNull(userId)) {
                UserApproveInfo approveInfo = approveHelper.queryApproveInfo(resp.getProductDetail().getId(), userId);
                if (approveInfo != null) {
                    resp.setApprove(approveInfo);
                    response.setApprove(UserApproveInfo.builder().count(approveInfo.getCount()).build());
                }
                Long nftInfoId = Optional.ofNullable(resp.getNftInfo()).map(item -> item.getNftId()).orElse(0L);
                if(nftInfoId != null && nftInfoId > 0){
                    ReportVo reportVo = reportHelper.reportVo(resp.getNftInfo().getNftId(), userId);
                    resp.setReportVo(reportVo);
                }
            }

            if (!Objects.equals(userId, resp.getMaxAmountUserId())) {
                resp.setMaxAmountUserId(null);
            }

            ProductDetailExtendMgsResponse result = ProductDetailExtendMgsResponseConvertor.convert(resp, AES_PASSWORD);

            NftInfoMgsVo nftInfoMgsVo = result.getNftInfo();
            CollectionInfoVo collectionInfoVo = response.getProductDetail().getCollection();
            if(Objects.nonNull(nftInfoMgsVo) && Objects.nonNull(collectionInfoVo)){
                nftAssetHelper.fillPropertiesByAddress(collectionInfoVo.getCollectionId(),nftInfoMgsVo.getProperties());
            }

            if(resp.getProductDetail() != null) {
                boolean premium = resp.getProductDetail().getListType() != null && resp.getProductDetail().getListType() == 0;
                GetRoyaltyFeeResponse feesResponse = paddingFeesInfo(collectionInfoVo.getCollectionId(),
                        premium);
                if(Objects.nonNull(feesResponse)) {
                    result.setRoyaltyFee(feesResponse.getRoyaltyFee());
                    result.setPlatformFee(feesResponse.getPlatformFee());
                }
            }
            return new CommonRet<>(result);
        } catch (Exception e) {
            log.error("productDetail error", e);
        }
        return new CommonRet<>();
    }

    /**
     * 竞拍历史
     *
     * @return
     */
    @PostMapping("/public/nft/nft-trade/trade-history")
    public CommonRet<SearchResult<TradeHistoryVo>> tradeHistory(@Valid @RequestBody TradeHistoryRequest request) throws Exception {
        return this.tradeHistoryV2(request);
    }

    /**
     * 竞拍历史
     *
     * @return
     */
    @PostMapping("/friendly/nft/nft-trade/trade-history")
    public CommonRet<SearchResult<TradeHistoryVo>> tradeHistoryV2(@Valid @RequestBody TradeHistoryRequest request) throws Exception {
        SearchResult<TradeHistoryVo> result = tradeCacheProxy.tradeHistory(request);
        Long timestamp = DateUtils.getNewUTCTimeMillis();
        Long userId = baseHelper.getUserId();
        if (CollectionUtils.isNotEmpty(result.getRows())) {
            List<TradeHistoryVo> list = result.getRows().stream()
                    .map(r -> {
                        TradeHistoryVo vo = CopyBeanUtils.fastCopy(r, TradeHistoryVo.class);
                        vo.setTimestamp(timestamp);
                        if (!Objects.equals(userId, vo.getUserId())) {
                            vo.setUserId(null);
                        }
                        return vo;
                    }).collect(Collectors.toList());
            result = new SearchResult<TradeHistoryVo>(list, result.getTotal());
        }
        return new CommonRet<>(result);
    }

    @PostMapping("/private/nft/nft-trade/batch-list")
    public CommonRet<String> batchList(@Valid @RequestBody BatchProductOnsaleRequest request) {
        request.setUserId(baseHelper.getUserId());
        if(request.getTradeType().equals(TradeTypeEnum.AUCTION.getCode())) {
            //check auction step
            request.getProductOnsaleItems().forEach(it-> Assert.isTrue(it.getAmount().compareTo(it.getStepAmount() != null ? it.getStepAmount() : BigDecimal.ZERO) > 0, TradeErrorCode.PARAM_ERROR));
        }
        APIResponse<String> response = productApi.batchOnsale(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/batch-onsale-result")
    public CommonRet<BatchTaskResponse> batchOnsaleCheckResult(@RequestBody BatchActionCheckResultRequest request){
        APIResponse<BatchTaskResponse> response = productApi.batchOnsaleCheckResult(APIRequest.instance(request.getTaskId()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/batch-offsale")
    public CommonRet<String> batchOffsale(@RequestBody BatchOffsaleRequest request){
        APIResponse<String> response = productApi.batchOffsale(APIRequest.instance(BatchOffsaleTaskRequest.builder().productIds(request.getProductIds()).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/batch-offsale-result")
    public CommonRet<BatchTaskResponse> batchOffsaleCheckResult(@RequestBody BatchActionCheckResultRequest request){
        APIResponse<BatchTaskResponse> response = productApi.batchOffsaleCheckResult(APIRequest.instance(request.getTaskId()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/public/nft/nft-trade/batch-offsale-limit")
    public CommonRet<Integer> batchOffsaleList(){
        APIResponse<Integer> response = confApi.batchOffsaleLimit();
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping(value = "/private/nft/nft-trade/onsale-config-list")
    public CommonRet<Map<Long, BatchNftInfoListConfig>> batchOnsaleConfigList(@RequestBody BatchOnsaleFeeConfigRequest request){
        request.setUserId(baseHelper.getUserId());
        APIResponse<Map<Long, BatchNftInfoListConfig>> response = confApi.batchOnsaleConfigList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/offsale/apply")
    public CommonRet<OpenListOffSaleResponse> offSaleApply(@RequestBody OpenListOffSaleRequest request){
        request.setUserId(baseHelper.getUserId());
        APIResponse<OpenListOffSaleResponse> response = openListApi.offsaleApply(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/offsale/progress")
    public CommonRet<OpenListOffSaleProgressResponse> offSaleProgess(@RequestBody OpenListOffSaleProgressRequest request){
        request.setUserId(baseHelper.getUserId());

        APIResponse<OpenListOffSaleProgressResponse> response = openListApi.offSaleProgress(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-trade/batch-offsale/apply")
    public CommonRet<OpenListBatchOffSaleResponse> batchOffSaleApply(@RequestBody OpenListBatchOffSaleRequest request){
        request.setUserId(baseHelper.getUserId());
        APIResponse<OpenListBatchOffSaleResponse> response = openListApi.batchOffsaleApply(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
    @PostMapping("/private/nft/nft-trade/batch-offsale/progress")
    public CommonRet<OpenListOffSaleProgressResponse> batchOffSaleProgess(@RequestBody OpenListOffSaleProgressRequest request){
        request.setUserId(baseHelper.getUserId());
        APIResponse<OpenListOffSaleProgressResponse> response = openListApi.batchOffsaleProgress(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }



    @PostMapping("/private/nft/nft-trade/onsale-edit")
    public CommonRet<Void> onsaleEdit(@RequestBody ProductEditRequest request){
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = productApi.onsaleEdit(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        AssetDetailReq assetDetailReq = AssetDetailReq.builder()
                .nftInfoId(request.getNftInfoId())
                .build();
        CacheUtils.invalidCacheData(assetDetailReq, CacheSeneEnum.ASSET_DETAIL, redissonClient);
        return new CommonRet<>();
    }
}
