package com.binance.mgs.nft.trade.proxy;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.helper.ArtistHelper;
import com.binance.mgs.nft.nftasset.controller.helper.ApproveHelper;
import com.binance.mgs.nft.trade.request.BatchOnsaleFeeRequest;
import com.binance.mgs.nft.trade.response.ProductDetailExtendResponse;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.data.vo.NftInfoDto;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.mystery.api.vo.MysteryBoxDetailQueryType;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import com.binance.nft.mystery.api.vo.QueryMysteryBoxDetailRequest;
import com.binance.nft.tradeservice.api.IProductApi;
import com.binance.nft.tradeservice.api.ITradeConfApi;
import com.binance.nft.tradeservice.api.launchpad.ILaunchpadConfigApi;
import com.binance.nft.tradeservice.dto.BatchOnsaleFeeItemDto;
import com.binance.nft.tradeservice.dto.LaunchpadConfigDto;
import com.binance.nft.tradeservice.enums.NftTypeEnum;
import com.binance.nft.tradeservice.enums.ProductRemarkTypeEnum;
import com.binance.nft.tradeservice.enums.ProductStatusEnum;
import com.binance.nft.tradeservice.request.*;
import com.binance.nft.tradeservice.response.BatchNftInfoListConfig;
import com.binance.nft.tradeservice.response.ProductDetailResponse;
import com.binance.nft.tradeservice.response.ProductOnsaleConfResponse;
import com.binance.nft.tradeservice.response.launchpad.LaunchpadDetailResponse;
import com.binance.nft.tradeservice.vo.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TradeCacheProxy {

    public static ProductDetailExtendResponse productDetailExtendResponse = new ProductDetailExtendResponse();
    @Resource
    private IProductApi productApi;
    @Resource
    private ITradeConfApi tradeConfApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private NFTMysteryBoxApi mysteryBoxApi;
    @Resource
    private ApproveHelper approveHelper;
    @Resource
    private ArtistHelper artistHelper;
    @Resource
    private ILaunchpadConfigApi launchpadConfigApi;
    @Resource
    private CrowdinHelper crowdinHelper;

    @Resource
    private INftInfoApi nftInfoApi;

    /**
     * 缓存
     */
    private final LoadingCache<Long, List<LaunchpadConfigDto>> lpdConfigCache =
            CacheBuilder.newBuilder()
                    .initialCapacity(1)
                    .maximumSize(4)
                    .refreshAfterWrite(30, TimeUnit.SECONDS)
                    .expireAfterAccess(90, TimeUnit.SECONDS)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, List<LaunchpadConfigDto>>() {
                        @Override
                        public List<LaunchpadConfigDto> load(Long key) throws Exception {
                            APIResponse<List<LaunchpadConfigDto>> response = launchpadConfigApi.queryAllMysteryLpdConfig();
                            baseHelper.checkResponse(response);
                            return response.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public ProductDetailExtendResponse productDetail(ProductDetailRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<ProductDetailResponse> response = productApi.detail(APIRequest.instance(request));
        if(!baseHelper.isOk(response)) {
            return productDetailExtendResponse;
        }

        ProductDetailExtendResponse resp = CopyBeanUtils.fastCopy(response.getData(), ProductDetailExtendResponse.class);
        if(resp != null) {
            List<Integer> banList = Lists.newArrayList(ProductRemarkTypeEnum.BANNED_CONTENT.getType(), ProductRemarkTypeEnum.BANNED_REPLICATE.getType(), ProductRemarkTypeEnum.BANNED_COPYRIGHTS.getType(), ProductRemarkTypeEnum.BANNED_OTHERS.getType());

            if (resp.getProductDetail() != null && resp.getProductDetail().getRemarkType() != null && !ProductStatusEnum.ONSALE.statusEquals(resp.getProductDetail().getStatus()) && banList.contains(resp.getProductDetail().getRemarkType())) {
                return productDetailExtendResponse;
            }
            if (resp.getNftInfo() != null && !NftTypeEnum.NORMAL.typeEquals(resp.getProductDetail().getNftType())) {
                APIResponse<MysteryBoxProductDetailVo> productDetailVoAPIResponse = mysteryBoxApi.queryMysteryBoxDetail(APIRequest.instance(
                        QueryMysteryBoxDetailRequest.builder().nftInfoId(response.getData().getNftInfo().getNftId()).mysteryBoxDetailQueryType(MysteryBoxDetailQueryType.NFT_INFO_ID).build()));
                if (baseHelper.isOk(productDetailVoAPIResponse)) {
                    resp.setMysteryBoxProductDetailVo(productDetailVoAPIResponse.getData());
                }
            } else {
                UserApproveInfo approveInfo = approveHelper.queryApproveInfo(resp.getProductDetail().getId(), null);
                resp.setApprove(approveInfo);
                UserInfoVo creator = resp.getNftInfo().getCreator();
                if((creator != null) && (creator instanceof ArtistUserInfo)){
                    ArtistUserInfo artistUserInfo = (ArtistUserInfo) creator;
                    Long userId = artistUserInfo.getUserId();
                    boolean artist = artistHelper.checkUserArtist(userId);
                    if(artist){
                        artistUserInfo.setArtist(true);
                    }else{
                        artistUserInfo.setUserId(null);
                    }
                }
            }
        }
        return resp;
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public SearchResult<TradeHistoryVo> tradeHistory(TradeHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SearchResult<TradeHistoryVo>> response = productApi.tradeHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE, key = "'onsaleFee'+#request")
    public ProductFeeVo onsaleFee(ProductFeeRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<ProductFeeVo> response = tradeConfApi.fee(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }


    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE,key = "'batchOnsaleFee'+#nftIds")
    public Map<Long, BatchNftInfoListConfig> batchOnsaleConfig(BatchOnsaleFeeRequest request) {
        if (CollectionUtils.isEmpty(request.getNftIds())){
            return Maps.newHashMap();
        }
        APIResponse<List<NftInfoDto>> nftInfoListResult = nftInfoApi.getNFTInfoList(request.getNftIds());
        baseHelper.checkResponse(nftInfoListResult);
        List<BatchOnsaleFeeItemDto> feeItemDtos = nftInfoListResult.getData().stream().map(n -> {
            Long relateId;
            if (NftTypeEnum.UNOPENED_BOX.typeEquals(n.getNftType().intValue())) {
                relateId = n.getItemId();
            } else {
                relateId = n.getSerialsNo();
            }
            return BatchOnsaleFeeItemDto.builder().nftId(n.getId()).nftType(n.getNftType().intValue()).relateId(relateId).build();
        }).collect(Collectors.toList());
        BatchOnsaleFeeConfigRequest batchOnsaleFeeConfigRequest = BatchOnsaleFeeConfigRequest.builder().userId(baseHelper.getUserId()).tradeType(request.getTradeType()).items(feeItemDtos).build();
        APIResponse<Map<Long, BatchNftInfoListConfig>> mapAPIResponse = tradeConfApi.batchOnsaleConfigList(APIRequest.instance(batchOnsaleFeeConfigRequest));
        baseHelper.checkResponse(mapAPIResponse);
        return mapAPIResponse.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public ProductOnsaleConfResponse onsaleConfig() throws Exception {
        APIResponse<ProductOnsaleConfResponse> response = tradeConfApi.config();
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE,key = "#productOnsaleConfRequest")
    public ProductOnsaleConfResponse onsaleConfigV2(ProductOnsaleConfRequest productOnsaleConfRequest) throws Exception {
        productOnsaleConfRequest.setUserId(baseHelper.getUserId());
        APIResponse<ProductOnsaleConfResponse> response = tradeConfApi.configV2(APIRequest.instance(productOnsaleConfRequest));
        baseHelper.checkResponse(response);
        return response.getData();
    }



    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public LaunchpadDetailResponse launchpadDetail(String pageLink) throws Exception {
        APIResponse<LaunchpadDetailResponse> response = launchpadConfigApi.detail(pageLink);
        baseHelper.checkResponse(response);
        LaunchpadDetailResponse data = response.getData();
        return data;
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public Map<Integer, CollectionOlConfigVo> olOnSaleInfo(ProductOnsaleConfRequest request) throws Exception {
        APIResponse<Map<Integer, CollectionOlConfigVo>> response = tradeConfApi.olOnSaleInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL)
    public Map<Long, List<CollectionOlConfigVo>> olOnSaleInfoList(BatchOnsaleFeeConfigRequest request) throws Exception {
        APIResponse<Map<Long, List<CollectionOlConfigVo>>> response = tradeConfApi.olOnSaleInfoList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public List<LaunchpadConfigDto> queryAllMysteryLpdConfig() {
        try {
            return lpdConfigCache.get(0L);
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }
}
