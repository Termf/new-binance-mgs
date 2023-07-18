package com.binance.mgs.nft.nftasset.controller.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.common.cache.CacheSeneEnum;
import com.binance.mgs.nft.common.cache.CacheUtils;
import com.binance.mgs.nft.common.constant.MgsNftConstants;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.core.redis.RedisCommonConfig;
import com.binance.mgs.nft.market.helper.ArtistHelper;
import com.binance.mgs.nft.market.proxy.CollectionCacheProxy;
import com.binance.mgs.nft.market.request.ProfileAssetFilterRequest;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.response.NftAssetDetailResponse;
import com.binance.mgs.nft.nftasset.response.PreMintCheckResponse;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.nft.aggregator.proxy.api.CollectionApi;
import com.binance.nft.aggregator.proxy.response.CollectionFeeResponse;
import com.binance.nft.assetservice.api.INftAssetApi;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.IUserInfoApi;
import com.binance.nft.assetservice.api.data.dto.AvailableStakeNftDto;
import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesDto;
import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesInfoDto;
import com.binance.nft.assetservice.api.data.dto.SpecificationDto;
import com.binance.nft.assetservice.api.data.request.GetUserCollectionsRequest;
import com.binance.nft.assetservice.api.data.request.NftInfoDetailRequest;
import com.binance.nft.assetservice.api.data.request.UserProfileAssetRequest;
import com.binance.nft.assetservice.api.data.request.UserProfileRequest;
import com.binance.nft.assetservice.api.data.response.NftBlockChainRefDto;
import com.binance.nft.assetservice.api.data.vo.*;
import com.binance.nft.assetservice.api.data.vo.detail.NftDetailDto;
import com.binance.nft.assetservice.api.data.vo.detail.NftDetailWithUserDto;
import com.binance.nft.assetservice.api.data.vo.function.NftLayerConfigVo;
import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
import com.binance.nft.assetservice.api.function.ILayerInfoApi;
import com.binance.nft.assetservice.api.mintmanager.IMintManagerApi;
import com.binance.nft.assetservice.constant.Constants;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.assetservice.dto.MediaInfoDto;
import com.binance.nft.assetservice.dto.NftUrlDto;
import com.binance.nft.assetservice.enums.NftAssetStatusEnum;
import com.binance.nft.assetservice.enums.NftInfoStatusEnum;
import com.binance.nft.assetservice.enums.ProfileViewModeEnum;
import com.binance.nft.market.ifae.NftMarketCollectionApi;
import com.binance.nft.market.ifae.NftProfileApi;
import com.binance.nft.market.request.CollectionDetailRequest;
import com.binance.nft.market.request.CollectionOnsaleCountRequest;
import com.binance.nft.market.request.GetBestOffersRequest;
import com.binance.nft.market.request.GetSimpleFloorPriceListRequest;
import com.binance.nft.market.vo.MarketBestOfferVO;
import com.binance.nft.market.vo.collection.*;
import com.binance.nft.market.vo.ranking.FloorPriceVo;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.mystery.api.vo.Creator;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import com.binance.nft.mystery.api.vo.QueryMysteryBoxDetailForMetaRequest;
import com.binance.nft.mystery.api.vo.Series;
import com.binance.nft.reconcilication.api.INftRoyaltyFeeApi;
import com.binance.nft.reconcilication.response.GetRoyaltyFeeResponse;
import com.binance.nft.tradeservice.api.IProductApi;
import com.binance.nft.tradeservice.api.IProductV2Api;
import com.binance.nft.tradeservice.api.ITradeConfApi;
import com.binance.nft.tradeservice.enums.NftTypeEnum;
import com.binance.nft.tradeservice.enums.ProductStatusEnum;
import com.binance.nft.tradeservice.enums.SourceTypeEnum;
import com.binance.nft.tradeservice.request.ProductDetailRequest;
import com.binance.nft.tradeservice.response.ProductDetailV2Response;
import com.binance.nft.tradeservice.vo.NftProductInfoVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.binance.nft.assetservice.constant.NftAssetErrorCode.NFT_ASSET_FOLLOW_COUNT_ERROR;
import static com.binance.nft.reconciliaction.constant.NftReconciliactionErrorCode.RECON_USER_GRAY_NUMBER;
import static com.binance.nftcore.utils.lambda.check.BaseHelper.checkResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class NftAssetHelper {

    private final INftAssetApi nftAssetApi;
    private final INftInfoApi iNftInfoApi;
    private final NftMarketCollectionApi nftMarketCollectionApi;

    private final CollectionApi collectionApi;
    private final NftProfileApi nftProfileApi;
    private final CollectionCacheProxy collectionCacheProxy;
    private final ArtistHelper artistHelper;
    private final IProductApi iProductApi;
    private final IProductV2Api iProductV2Api;
    private final NFTMysteryBoxApi mysteryBoxApi;
    private final NftChainAssetHelper nftChainAssetHelper;
    private final BaseHelper baseHelper;
    private final ProductHelper productHelper;

    private final IMintManagerApi mintManagerApi;

    private final INftRoyaltyFeeApi iNftRoyaltyFeeApi;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private final ILayerInfoApi layerInfoApi;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @Resource
    private NftAssetHelper nftAssetHelper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private FreezeReasonHelper freezeReasonHelper;
    @Resource
    private MgsNftProperties mgsNftProperties;
    @Resource
    private ActivityCR7Helper activityCR7Helper;
    @Resource
    private CrowdinHelper crowdinHelper;

    @Resource
    private ITradeConfApi iTradeConfApi;

    @Resource
    private IUserInfoApi iUserInfoApi;

    @ApolloJsonValue("${ape.allow.stake.collection:{\"BAYC\":639681371319926784,\"bored_ape_mayc_collection\":640543913231953920,\"bored_ape_bakc_collection\":640547745089368064}}")
    private Map<String, Long> apeAllowStakeCollectionMap;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public List<NftProductVo> getUserNftAssetByInfoId(Long userId, Long nftInfoId) {

        APIResponse<List<NftProductVo>> apiResponse = nftAssetApi.
                fetchUserNftAssetByInfoId(userId, Arrays.asList(nftInfoId), Boolean.TRUE);
        baseHelper.checkResponse(apiResponse);
        return apiResponse.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public List<NftSimpleInfoVo> getWalletUserNftAsset(Long userId) {

        APIResponse<List<NftSimpleInfoVo>> apiResponse = nftAssetApi.
                fetchWalletUserAsset(userId, null, 1, 5);
        baseHelper.checkResponse(apiResponse);
        return apiResponse.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public NftLayerVo getLayerInfo(Long layerId) {
        APIResponse<NftLayerVo> apiResponse = layerInfoApi.findLayerDetailById(APIRequest.instance(layerId));
        // todo debug
        log.debug("apiResponse :: {}", JsonUtils.toJsonHasNullKey(apiResponse));
        baseHelper.checkResponse(apiResponse);

        return apiResponse.getData();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonPageRet<NftSimpleInfoVo> getAvatarUserNftAsset(Long userId, int page, int size) {

        APIResponse<Page<NftSimpleInfoVo>> apiResponse = nftAssetApi.
                fetchAvailableUserAssetPic(userId, NftAssetStatusEnum.MARKET_READY.getCode(), page, size);
        baseHelper.checkResponse(apiResponse);

        final Page<NftSimpleInfoVo> data = apiResponse.getData();
        final CommonPageRet<NftSimpleInfoVo> commonPageRet = new CommonPageRet<>();
        commonPageRet.setTotal(data.getTotal());
        commonPageRet.setData(data.getRecords());
        return commonPageRet;
    }

    public void fillPropertiesByAddress(Long collectionId, List<NftAssetPropertiesInfoDto> tokenAttributes) {
        if (collectionId == null || CollectionUtils.isEmpty(tokenAttributes)) {
            return;
        }
        if (mgsNftProperties.getCollectionBlackList().contains(collectionId)) {
            return;
        }

        List<NftAssetPropertiesInfoDto> nftAssetPropertiesInfoDtos = nftChainAssetHelper.proxyFetchAttributesByCollectionId(collectionId);

        if (CollectionUtils.isNotEmpty(nftAssetPropertiesInfoDtos)) {

            Map<String, NftAssetPropertiesInfoDto> allMap = new HashMap<>();
            nftAssetPropertiesInfoDtos.forEach(
                    nftAssetPropertiesInfoDto -> {
                        allMap.put(
                                nftAssetPropertiesInfoDto.getPropertyType().concat(nftAssetPropertiesInfoDto.getPropertyName()),
                                nftAssetPropertiesInfoDto);
                    }
            );

            tokenAttributes.forEach(s -> {
                NftAssetPropertiesInfoDto dto = allMap.get(s.getPropertyType().concat(s.getPropertyName()));
                if (Objects.nonNull(dto)) {
                    s.setTotal(dto.getTotal());
                    s.setRate(dto.getRate());
                }
            });

        }
    }


    /**
     * 整理response为k:v格式
     * "background":[
     * <p>
     * {
     * <p>
     * “value”:"background:Candy",
     * <p>
     * "name":"Candy",
     * <p>
     * "count": 2
     * },
     * {
     * <p>
     * “value”:"background:Moso",
     * <p>
     * "name":"Moso",
     * <p>
     * "count": 3
     * <p>
     * }
     * <p>
     * ]
     * }
     */
    public Map<String, List> getPropertiesByCollectionId(Long collectionId) {
        if (collectionId == null) {
            return null;
        }
        List<NftAssetPropertiesInfoDto> nftAssetPropertiesInfoDtos = nftChainAssetHelper.proxyFetchAttributesByCollectionId(collectionId);
        if (CollectionUtils.isNotEmpty(nftAssetPropertiesInfoDtos)) {
            Map<String, List> map = new TreeMap<>();
            nftAssetPropertiesInfoDtos.stream()
                    .sorted(Comparator.comparing(NftAssetPropertiesInfoDto::getTotal).reversed())
                    .collect(Collectors.toList()).forEach(dto -> {
                        Map<String, Object> attrMap = new HashMap<>();
                        attrMap.put("value", dto.getPropertyType().concat("->value=").concat(dto.getPropertyName()));
                        attrMap.put("name", dto.getPropertyName());
                        attrMap.put("count", dto.getTotal());
                        if (!map.containsKey(dto.getPropertyType())) {
                            map.put(dto.getPropertyType(), new ArrayList());
                        }
                        map.get(dto.getPropertyType()).add(attrMap);
                    });
            return map;
        } else {
            return null;
        }
    }

    //    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public NftAssetDetailResponse queryNftAssetDetailByNftInfoId(Long userId, Long nftInfoId) throws Exception {
        if (Objects.isNull(nftInfoId)) {
            log.error("[queryNftAssetDetailByNftInfoId] userId or nftInfoId is null");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        AssetDetailReq assetDetailReq = AssetDetailReq.builder()
                .nftInfoId(nftInfoId)
                .build();
        TypeReference typeReference = new TypeReference<NftAssetDetailResponse>() {
        };
        Function<AssetDetailReq, NftAssetDetailResponse> function = req -> {
            ProductDetailMgsVo productDetailMgsVo = getProductDetailMgsVo(req.getProductId(), req.getNftInfoId());
//            if (Objects.isNull(productDetailMgsVo) && isExternal(nftInfoId)) {
//                return null;
//            }
            Long ownerId = null;
            if (Objects.nonNull(productDetailMgsVo)) {
                ownerId = ProductStatusEnum.ONSALE.statusEquals(productDetailMgsVo.getStatus())
                        ? productDetailMgsVo.getOwnerId() : null;
                productDetailMgsVo.setOwnerId(null);
            }

            NftInfoDetailMgsVo nftInfoDetailMgsVo = getNftInfoDetailMgsVoV2(ownerId, req.getNftInfoId(),
                    Optional.ofNullable(productDetailMgsVo).map(ProductDetailMgsVo::getSource).orElse(null));
            if (Objects.isNull(nftInfoDetailMgsVo) || Objects.isNull(nftInfoDetailMgsVo.getNftType())) {
                log.warn("can't find nft:{}", req.getNftInfoId());
                return null;
            }

            // dex asset expose
            if (Objects.isNull(productDetailMgsVo) && !canAssetExpose(nftInfoDetailMgsVo.getCollectionId(), nftInfoId)) {
                return null;
            }

            MysteryBoxMgsVo mysteryBoxMgsVo = null;
            if (!NftTypeEnum.NORMAL.typeEquals(nftInfoDetailMgsVo.getNftType().intValue())) {
                mysteryBoxMgsVo = getMysteryBoxMgsVo(nftInfoDetailMgsVo.getSerialsNo());
            }

            NftAssetDetailResponse nftAssetDetailResponse = NftAssetDetailResponse
                    .builder()
                    .nftInfoDetailMgsVo(nftInfoDetailMgsVo)
                    .productDetailMgsVo(productDetailMgsVo)
                    .mysteryBoxMgsVo(mysteryBoxMgsVo)
                    .timestamp(DateUtils.getNewUTCTimeMillis())
                    .isAdminOwner(nftInfoDetailMgsVo.isAdminOwner())
                    .build();
            BigDecimal discount = null;
            if(userId !=null){
                Map<Integer,BigDecimal> discountMap = iTradeConfApi.vipFeeDsicount().getData();
                discount = discountMap.get(iUserInfoApi.fetchUserSimpleInfo(userId).getData().getNftVipLevel());
            }
            //royalty fee
            if (Objects.nonNull(productDetailMgsVo) && Objects.nonNull(nftInfoDetailMgsVo)) {
                boolean premium = productDetailMgsVo.getListType() != null
                        && productDetailMgsVo.getListType() == 0;



                if (!SourceTypeEnum.isExternal(productDetailMgsVo.getSource())) {
                    GetRoyaltyFeeResponse feesResponse = paddingFeesInfo(nftInfoDetailMgsVo.getSerialsNo()
                            , null, premium);
                    if (Objects.nonNull(feesResponse)) {
                        nftAssetDetailResponse.setRoyaltyFees(Arrays.asList(new NftAssetDetailResponse.FeeVO(1,feesResponse.getRoyaltyFee(),null)));
                        BigDecimal vipFeeRate = discount !=null && discount.compareTo(BigDecimal.ONE)<0?feesResponse.getPlatformFee().multiply(discount):null;
                        nftAssetDetailResponse.setPlatformFees(Arrays.asList(new NftAssetDetailResponse.FeeVO(1,feesResponse.getPlatformFee(),vipFeeRate)));
                    }
                } else {

                    List<NftAssetDetailResponse.FeeVO> royaltyFee = Lists.newArrayList();
                    List<NftAssetDetailResponse.FeeVO> platformFee = Lists.newArrayList();

                    GetRoyaltyFeeResponse feesResponse = paddingFeesInfo(nftInfoDetailMgsVo.getSerialsNo()
                            , null, premium);
                    if (Objects.nonNull(feesResponse)) {
                        royaltyFee.add(new NftAssetDetailResponse.FeeVO(1,feesResponse.getRoyaltyFee(),null));
                        BigDecimal vipFeeRate = discount !=null && discount.compareTo(BigDecimal.ONE)<0?feesResponse.getPlatformFee().multiply(discount):null;
                        platformFee.add(new NftAssetDetailResponse.FeeVO(1,feesResponse.getPlatformFee(),vipFeeRate));
                    }
                    CollectionFeeResponse  feeResponse = collectionApi.queryFee(nftInfoDetailMgsVo.getContractAddress()).getData();
                    if(Objects.nonNull(feeResponse)){
                        royaltyFee.add(new NftAssetDetailResponse.FeeVO(SourceTypeEnum.getByName(feeResponse.getPlatForm()).getCode(),feeResponse.getRoyaltyFeeRate(),null));
                        BigDecimal vipFeeRate = discount !=null && discount.compareTo(BigDecimal.ONE)<0?feeResponse.getPlatformFeeRate().multiply(discount):null;
                        platformFee.add(new NftAssetDetailResponse.FeeVO(SourceTypeEnum.getByName(feeResponse.getPlatForm()).getCode(),feeResponse.getPlatformFeeRate(),vipFeeRate));
                    }
                    nftAssetDetailResponse.setRoyaltyFees(royaltyFee);
                    nftAssetDetailResponse.setPlatformFees(platformFee);
                    nftInfoDetailMgsVo.setCoverUrl(productDetailMgsVo.getCoverUrl());
                }
            }
            // todo debug
            log.debug("nftAssetDetailResponse :: {}", JsonUtils.toJsonHasNullKey(nftAssetDetailResponse));
            return nftAssetDetailResponse;
        };

        NftAssetDetailResponse nftAssetDetailResponse = CacheUtils
                .<AssetDetailReq, NftAssetDetailResponse>getData(redissonClient, assetDetailReq, CacheSeneEnum.ASSET_DETAIL,
                        true, typeReference, function);

        if (Objects.isNull(nftAssetDetailResponse)) {
            return nftAssetDetailResponse;
        }
        if (Objects.nonNull(nftAssetDetailResponse.getProductDetailMgsVo())) {
            ProductDetailMgsVo productDetail = nftAssetDetailResponse.getProductDetailMgsVo();
            if (Objects.nonNull(productDetail.getMaxAmountUserId()) && !Objects.equals(userId, productDetail.getMaxAmountUserId())) {
                productDetail = CopyBeanUtils.fastCopy(productDetail, ProductDetailMgsVo.class);
                productDetail.setMaxAmountUserId(null);
                nftAssetDetailResponse.setProductDetailMgsVo(productDetail);
            }
        }
        //fill user extend info
        if (Objects.nonNull(nftAssetDetailResponse.getNftInfoDetailMgsVo())) {
            NftInfoDetailMgsVo mgsVo = fillDetailUserExtendInfo(userId, nftInfoId, nftAssetDetailResponse.getNftInfoDetailMgsVo());
            if (Objects.isNull(mgsVo)) return null;
        }
        //mysterybox i18n
        if (Objects.nonNull(nftAssetDetailResponse.getMysteryBoxMgsVo()) && StringUtils.isNotBlank(nftAssetDetailResponse.getMysteryBoxMgsVo().getArtist())) {
            nftAssetDetailResponse.getMysteryBoxMgsVo().setArtist(crowdinHelper.getMessageByKey(nftAssetDetailResponse.getMysteryBoxMgsVo().getArtist(),
                    baseHelper.getLanguage()));
        }
        nftAssetDetailResponse.setTimestamp(DateUtils.getNewUTCTimeMillis());
        return nftAssetDetailResponse;
    }

    //    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public NftAssetDetailResponse queryNftAssetDetailByProductId(Long userId, Long productId) throws Exception {
        if (Objects.isNull(productId)) {
            log.error("[queryNftAssetDetailByProductId] userId or productId is null");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        AssetDetailReq assetDetailReq = AssetDetailReq.builder()
                .productId(productId)
                .build();
        TypeReference typeReference = new TypeReference<NftAssetDetailResponse>() {
        };
        Function<AssetDetailReq, NftAssetDetailResponse> function = req -> {

            ProductDetailMgsVo productDetailMgsVo = getProductDetailMgsVo(assetDetailReq.getProductId(), null);
            if (Objects.isNull(productDetailMgsVo)) {
                log.warn("can't find productId:{}", assetDetailReq.getProductId());
                return null;
            }
            Long nftInfoId = productDetailMgsVo.getNftId();
            Long ownerId = ProductStatusEnum.ONSALE.statusEquals(productDetailMgsVo.getStatus()) ? productDetailMgsVo.getOwnerId() : null;
            productDetailMgsVo.setOwnerId(null);

            NftInfoDetailMgsVo nftInfoDetailMgsVo = getNftInfoDetailMgsVoV2(ownerId, nftInfoId, productDetailMgsVo.getSource());
            if (Objects.isNull(nftInfoDetailMgsVo) || Objects.isNull(nftInfoDetailMgsVo.getNftType())) {
                log.warn("can't find nft :{}", Objects.isNull(nftInfoDetailMgsVo));
                return null;
            }
            MysteryBoxMgsVo mysteryBoxMgsVo = null;
            if (!NftTypeEnum.NORMAL.typeEquals(nftInfoDetailMgsVo.getNftType().intValue())) {
                mysteryBoxMgsVo = getMysteryBoxMgsVo(nftInfoDetailMgsVo.getSerialsNo());
            }

            NftAssetDetailResponse nftAssetDetailResponse = NftAssetDetailResponse
                    .builder()
                    .nftInfoDetailMgsVo(nftInfoDetailMgsVo)
                    .productDetailMgsVo(productDetailMgsVo)
                    .mysteryBoxMgsVo(mysteryBoxMgsVo)
                    .timestamp(DateUtils.getNewUTCTimeMillis())
                    .build();
            BigDecimal discount = null;
            if(userId !=null){
                Map<Integer,BigDecimal> discountMap = iTradeConfApi.vipFeeDsicount().getData();
                discount = discountMap.get(iUserInfoApi.fetchUserSimpleInfo(userId).getData().getNftVipLevel());
            }
            //royalty fee
            if (Objects.nonNull(productDetailMgsVo) && Objects.nonNull(nftInfoDetailMgsVo)) {
                boolean premium = productDetailMgsVo.getListType() != null
                        && productDetailMgsVo.getListType() == 0;

                if (!SourceTypeEnum.isExternal(productDetailMgsVo.getSource())) {
                    GetRoyaltyFeeResponse feesResponse = paddingFeesInfo(nftInfoDetailMgsVo.getSerialsNo()
                            , null, premium);
                    if (Objects.nonNull(feesResponse)) {
                        nftAssetDetailResponse.setRoyaltyFees(Arrays.asList(new NftAssetDetailResponse.FeeVO(1,feesResponse.getRoyaltyFee(),null)));
                        BigDecimal vipFeeRate = discount !=null && discount.compareTo(BigDecimal.ONE)<0?feesResponse.getPlatformFee().multiply(discount):null;
                        nftAssetDetailResponse.setPlatformFees(Arrays.asList(new NftAssetDetailResponse.FeeVO(1,feesResponse.getPlatformFee(),vipFeeRate)));
                    }
                } else {
                    GetRoyaltyFeeResponse feesResponse = paddingFeesInfo(nftInfoDetailMgsVo.getSerialsNo()
                            , null, premium);
                    List<NftAssetDetailResponse.FeeVO> royaltyFee = Lists.newArrayList();
                    List<NftAssetDetailResponse.FeeVO> platformFee = Lists.newArrayList();
                    if (Objects.nonNull(feesResponse)) {
                        royaltyFee.add(new NftAssetDetailResponse.FeeVO(1,feesResponse.getRoyaltyFee(),null));
                        BigDecimal vipFeeRate = discount !=null && discount.compareTo(BigDecimal.ONE)<0?feesResponse.getPlatformFee().multiply(discount):null;
                        platformFee.add(new NftAssetDetailResponse.FeeVO(1,feesResponse.getPlatformFee(),vipFeeRate));
                    }
                    CollectionFeeResponse  feeResponse = collectionApi.queryFee(nftInfoDetailMgsVo.getContractAddress()).getData();
                    if(Objects.nonNull(feeResponse)){
                        royaltyFee.add(new NftAssetDetailResponse.FeeVO(SourceTypeEnum.getByName(feeResponse.getPlatForm()).getCode(),feeResponse.getRoyaltyFeeRate(),null));
                        BigDecimal vipFeeRate = discount !=null && discount.compareTo(BigDecimal.ONE)<0?feeResponse.getPlatformFeeRate().multiply(discount):null;
                        platformFee.add(new NftAssetDetailResponse.FeeVO(SourceTypeEnum.getByName(feeResponse.getPlatForm()).getCode(),feeResponse.getPlatformFeeRate(),vipFeeRate));
                    }
                    nftAssetDetailResponse.setRoyaltyFees(royaltyFee);
                    nftAssetDetailResponse.setPlatformFees(platformFee);
                    nftInfoDetailMgsVo.setCoverUrl(productDetailMgsVo.getCoverUrl());
                }
            }
            // todo debug
            log.debug("nftAssetDetailResponse :: {}", JsonUtils.toJsonHasNullKey(nftAssetDetailResponse));
            return nftAssetDetailResponse;
        };

        NftAssetDetailResponse nftAssetDetailResponse = CacheUtils
                .<AssetDetailReq, NftAssetDetailResponse>getData(redissonClient, assetDetailReq, CacheSeneEnum.ASSET_DETAIL,
                        true, typeReference, function);

        if (Objects.isNull(nftAssetDetailResponse)) {
            return nftAssetDetailResponse;
        }
        if (Objects.nonNull(nftAssetDetailResponse.getProductDetailMgsVo())) {
            ProductDetailMgsVo productDetail = nftAssetDetailResponse.getProductDetailMgsVo();
            if (Objects.nonNull(productDetail.getMaxAmountUserId()) && !Objects.equals(userId, productDetail.getMaxAmountUserId())) {
                productDetail = CopyBeanUtils.fastCopy(productDetail, ProductDetailMgsVo.class);
                productDetail.setMaxAmountUserId(null);
                nftAssetDetailResponse.setProductDetailMgsVo(productDetail);
            }
        }
        //fill user extend info
        if (Objects.nonNull(nftAssetDetailResponse.getNftInfoDetailMgsVo())) {
            NftInfoDetailMgsVo mgsVo = fillDetailUserExtendInfo(userId, nftAssetDetailResponse.getNftInfoDetailMgsVo().getNftId(), nftAssetDetailResponse.getNftInfoDetailMgsVo());
            if (Objects.isNull(mgsVo)) return null;
        }

        nftAssetDetailResponse.setTimestamp(DateUtils.getNewUTCTimeMillis());
        return nftAssetDetailResponse;
    }

    private boolean isExternal(Long nftInfoId) {
        return nftInfoId != null && nftInfoId >= Constants.DEX_ASSET_ID;
    }


    private GetRoyaltyFeeResponse paddingFeesInfo(Long collectionId, Date activeTime, boolean premium) {
        String activeTimeStr = activeTime == null ? null : DateUtils.formatter(activeTime, DateUtils.DETAILED_NUMBER_PATTERN);

        APIResponse<GetRoyaltyFeeResponse> getFeesResponse = iNftRoyaltyFeeApi.getRoyaltyFee(collectionId,
                activeTimeStr, premium);


        if (!baseHelper.isOk(getFeesResponse)) {
            throw new BusinessException("fees information error");
        }
        return getFeesResponse.getData();
    }

    public Page<NftProfileCollectionVo> fetchUserProfileCollections(UserProfileRequest request, boolean isGray) throws Exception {
        Page<NftProfileCollectionVo> nftProfileCollectionVoPage = new Page<>();
        APIResponse<Page<NftProfileCollectionDto>> pageAPIResponse = iNftInfoApi
                .getUserProfileCollections(APIRequest.instance(request));
        baseHelper.checkResponse(pageAPIResponse);
        Page<NftProfileCollectionDto> profileCollectionDtoPage = pageAPIResponse.getData();
        if (Objects.isNull(profileCollectionDtoPage.getRecords()) || profileCollectionDtoPage.getRecords().size() == 0) {
            return nftProfileCollectionVoPage;
        }
        Map<Long, CollectionPriceVo> collectionPriceVoMap = new HashMap<>();
        List<Long> regularLayerIds = new ArrayList<>();
        List<Long> mysteryLayerIds = new ArrayList<>();
        for (NftProfileCollectionDto nftProfileCollectionDto : profileCollectionDtoPage.getRecords()) {
            if (Objects.nonNull(nftProfileCollectionDto.getCollectionType()) && (nftProfileCollectionDto.getCollectionType() == 1)) {
                regularLayerIds.add(nftProfileCollectionDto.getLayerId());
            } else {
                mysteryLayerIds.add(nftProfileCollectionDto.getLayerId());
            }
        }
        if (regularLayerIds.size() > 0) {
            APIResponse<List<CollectionPriceVo>> regularCollectionPriceVoListResponse = nftMarketCollectionApi.findCollectionPriceInfoByIdList(APIRequest.instance(regularLayerIds));
            baseHelper.checkResponse(regularCollectionPriceVoListResponse);
            if (Objects.nonNull(regularCollectionPriceVoListResponse.getData()) && regularCollectionPriceVoListResponse.getData().size() > 0) {
                for (CollectionPriceVo collectionPriceVo : regularCollectionPriceVoListResponse.getData()) {
                    collectionPriceVoMap.put(collectionPriceVo.getLayerId(), collectionPriceVo);
                }
            }
        }
        if (mysteryLayerIds.size() > 0) {
            for (Long layerId : mysteryLayerIds) {
                CollectionDetailRequest collectionDetailRequest = CollectionDetailRequest.builder()
                        .layerId(layerId).gray(isGray)
                        .build();
                MysteryBoxCollectionPriceVo mysteryBoxCollectionPriceVo = collectionCacheProxy.mysteryLayerPrice(collectionDetailRequest);
                if (Objects.nonNull(mysteryBoxCollectionPriceVo)) {
                    DailyTradePriceVo dailyTradePriceVo = DailyTradePriceVo.builder()
                            .amount(mysteryBoxCollectionPriceVo.getDailyTradePriceVo().getAmount())
                            .currency(mysteryBoxCollectionPriceVo.getDailyTradePriceVo().getCurrency())
                            .build();
                    CollectionPriceVo collectionPriceVo = CollectionPriceVo.builder()
                            .floorPrice(mysteryBoxCollectionPriceVo.getFloorPriceDetailVo())
                            .dailyTradePrice(dailyTradePriceVo)
                            .build();
                    collectionPriceVoMap.put(layerId, collectionPriceVo);
                }
            }
        }
        if (request.getType() == 2) {
            List<Long> regularSerialsNoList = new ArrayList<>();
            List<Long> mysterySerialsNoList = new ArrayList<>();
            for (NftProfileCollectionDto nftProfileCollectionDto : profileCollectionDtoPage.getRecords()) {
                if (nftProfileCollectionDto.getCollectionType() == 1) {
                    regularSerialsNoList.add(nftProfileCollectionDto.getLayerId());
                } else {
                    mysterySerialsNoList.add(nftProfileCollectionDto.getLayerId());
                }
            }
            CollectionOnsaleCountRequest regularOnsaleCountRequest = new CollectionOnsaleCountRequest(1, regularSerialsNoList);
            CollectionOnsaleCountRequest mysteryOnsaleCountRequest = new CollectionOnsaleCountRequest(2, mysterySerialsNoList);
            APIResponse<Map<Long, Long>> regularCollectionResponse = null;
            APIResponse<Map<Long, Long>> mysteryCollectionResponse = null;
            Map<Long, Long> serialsOnSaleMap = new HashMap<>();
            if (regularSerialsNoList.size() > 0) {
                regularCollectionResponse = nftMarketCollectionApi.getCollectionOnsaleCount(APIRequest.instance(regularOnsaleCountRequest));
                baseHelper.checkResponse(regularCollectionResponse);
                serialsOnSaleMap.putAll(regularCollectionResponse.getData());

            }
            if (mysterySerialsNoList.size() > 0) {
                mysteryCollectionResponse = nftMarketCollectionApi.getCollectionOnsaleCount(APIRequest.instance(mysteryOnsaleCountRequest));
                baseHelper.checkResponse(mysteryCollectionResponse);
                serialsOnSaleMap.putAll(mysteryCollectionResponse.getData());
            }
            for (NftProfileCollectionDto nftProfileCollectionDto : profileCollectionDtoPage.getRecords()) {
                Long onSale = serialsOnSaleMap.get(nftProfileCollectionDto.getLayerId());
                if (Objects.nonNull(onSale)) {
                    nftProfileCollectionDto.setForSale(Integer.valueOf(onSale.toString()));
                } else {
                    nftProfileCollectionDto.setForSale(0);
                }
            }
        }
        List<NftProfileCollectionVo> nftProfileCollectionVoList = profileCollectionDtoPage.getRecords().stream().map(nftProfileCollectionDto -> {
            CollectionPriceVo collectionPriceVo = collectionPriceVoMap.get(nftProfileCollectionDto.getLayerId());
            NftProfileCollectionVo nftProfileCollectionVo = NftProfileCollectionVo
                    .builder()
                    .layerId(nftProfileCollectionDto.getLayerId())
                    .layerName(nftProfileCollectionDto.getLayerName())
                    .layerStatus(nftProfileCollectionDto.getLayerStatus())
                    .description(nftProfileCollectionDto.getDescription())
                    .avatarUrl(nftProfileCollectionDto.getAvatarUrl())
                    .bannerUrl(nftProfileCollectionDto.getBannerUrl())
                    .verifyType(nftProfileCollectionDto.getVerifyType())
                    .banned(nftProfileCollectionDto.getBanned())
                    .collectionType(nftProfileCollectionDto.getCollectionType())
                    .freezeReason(freezeReasonHelper.getViewTextByReason(nftProfileCollectionDto.getFreezeReason()))
                    .bannedReason(freezeReasonHelper.getViewTextByReason(nftProfileCollectionDto.getBannedReason()))
                    .isCreator(nftProfileCollectionDto.isCreator())
                    .forSale(nftProfileCollectionDto.getForSale())
                    .total(nftProfileCollectionDto.getTotal())
                    .build();
            if (Objects.nonNull(collectionPriceVo)) {
                nftProfileCollectionVo.setFloorPrice(collectionPriceVo.getFloorPrice());
                nftProfileCollectionVo.setDailyTradePrice(collectionPriceVo.getDailyTradePrice());
            }
            return nftProfileCollectionVo;
        }).collect(Collectors.toList());
        nftProfileCollectionVoPage.setCurrent(profileCollectionDtoPage.getCurrent());
        nftProfileCollectionVoPage.setSize(profileCollectionDtoPage.getSize());
        nftProfileCollectionVoPage.setTotal(profileCollectionDtoPage.getTotal());
        nftProfileCollectionVoPage.setPages(profileCollectionDtoPage.getPages());
        nftProfileCollectionVoPage.setRecords(nftProfileCollectionVoList);
        return nftProfileCollectionVoPage;
    }

    public Page<NftProfileAssetVo> fetchUserProfileAssets(UserProfileRequest request) throws Exception {
        APIResponse<Page<NftProfileAssetDto>> nftProfileAssetDtoAPIResponse = iNftInfoApi.getUserProfileAssets(APIRequest.instance(request));
        baseHelper.checkResponse(nftProfileAssetDtoAPIResponse);
        Page<NftProfileAssetDto> nftProfileAssetDtoPage = nftProfileAssetDtoAPIResponse.getData();
        AtomicReference<Boolean> modifyTotalFlag = new AtomicReference<>(Boolean.FALSE);
        Page<NftProfileAssetVo> resultPage = new Page<>();
        resultPage.setCurrent(nftProfileAssetDtoPage.getCurrent());
        resultPage.setSize(nftProfileAssetDtoPage.getSize());
        resultPage.setPages(nftProfileAssetDtoPage.getPages());
        List<NftProfileAssetVo> retList = new ArrayList<>();
        List<NftProductInfoVo> nftProductInfoVoList = productHelper.getUserProfileProductInfo(nftProfileAssetDtoPage, modifyTotalFlag);
        retList = productHelper.convertNftProfileAssetVoList(nftProductInfoVoList, nftProfileAssetDtoPage);
        retList.forEach(it -> activityCR7Helper.pendingCR7Info(it));
        resultPage.setTotal(modifyTotalFlag.get() ? nftProfileAssetDtoPage.getTotal() - NumberUtils.LONG_ONE : nftProfileAssetDtoPage.getTotal());
        resultPage.setRecords(retList);
        return resultPage;
    }

    public NftDetailDto getNFTInfoDetailByNft(Long ownerId, Long nftInfoId) {
        APIResponse<NftDetailDto> nftInfoDetailDtoAPIResponse = iNftInfoApi.getNFTInfoDetailByNft(APIRequest.instance(new NftInfoDetailRequest(ownerId, nftInfoId)));
        if (!baseHelper.isOk(nftInfoDetailDtoAPIResponse) || Objects.isNull(nftInfoDetailDtoAPIResponse.getData())) {
            return null;
        }
        return nftInfoDetailDtoAPIResponse.getData();
    }

    @SneakyThrows
    private ProductDetailMgsVo getProductDetailMgsVo(Long productId, Long nftInfoId) {
        ProductDetailRequest request = new ProductDetailRequest();
        request.setProductId(productId);
        request.setNftId(nftInfoId);
        APIResponse<ProductDetailV2Response> productDetailV2ResponseAPIResponse = iProductV2Api.detail(APIRequest.instance(request));
        if (!baseHelper.isOk(productDetailV2ResponseAPIResponse) || Objects.isNull(productDetailV2ResponseAPIResponse.getData())) {
            return null;
        }
        ProductDetailV2Response.ProductDetailV2Vo productDetailV2Vo = productDetailV2ResponseAPIResponse.getData().getProductDetail();
        ProductDetailMgsVo productDetailMgsVo = CopyBeanUtils.fastCopy(productDetailV2Vo, ProductDetailMgsVo.class);
        if (CollectionUtils.isNotEmpty(productDetailV2Vo.getTokenList())) {
            productDetailMgsVo.setNftId(productDetailV2Vo.getTokenList().get(0).getNftId());
        }
        productDetailMgsVo.setTokenList(getProductTokenList(productDetailV2Vo.getTokenList()));
        if (Objects.nonNull(productDetailV2Vo.getCategoryVo())) {
            productDetailMgsVo.setCategoryVo(CopyBeanUtils.fastCopy(productDetailV2Vo.getCategoryVo(), CategoryMgsVo.class));
        }
        productDetailMgsVo.setCreateTime(productDetailV2Vo.getCreateTime());
        productDetailMgsVo.setOpenListPlatforms(Optional.ofNullable(productDetailV2Vo.getOpenListPlatforms()).orElse(Collections.emptyList()));
        return productDetailMgsVo;
    }

    private List<NftBlockChainRefVo> getProductTokenList(List<ProductDetailV2Response.TokenInfoV2Vo> tokenList) {
        if (CollectionUtils.isEmpty(tokenList)) {
            return null;
        }
        List<NftBlockChainRefVo> nftBlockChainRefVos = new ArrayList<>();
        for (ProductDetailV2Response.TokenInfoV2Vo tokenInfoV2Vo : tokenList) {
            NftBlockChainRefVo nftBlockChainRefVo = CopyBeanUtils.fastCopy(tokenInfoV2Vo, NftBlockChainRefVo.class);
            nftBlockChainRefVo.setNftInfoId(tokenInfoV2Vo.getNftId());
            nftBlockChainRefVos.add(nftBlockChainRefVo);
        }
        return nftBlockChainRefVos;
    }


    private NftInfoDetailMgsVo getNftInfoDetailMgsVoV2(Long ownerId, Long nftInfoId, Integer source) {
        NftDetailDto nftInfoDetailDto = getNftDetailDto(ownerId, nftInfoId, source);
        if (Objects.isNull(nftInfoDetailDto)) {
            return null;
        }

        NftInfoDetailMgsVo nftInfoDetailMgsVo = CopyBeanUtils.fastCopy(nftInfoDetailDto, NftInfoDetailMgsVo.class);
        nftInfoDetailMgsVo.setContractAddress(nftInfoDetailDto.getContractAddress());

        nftInfoDetailMgsVo.setTokenId(nftInfoDetailDto.getTokenId());
        nftInfoDetailMgsVo.setRarity(nftInfoDetailDto.getRarity());
        nftInfoDetailMgsVo.setNftId(nftInfoDetailDto.getId());
        //上链的nft，不能burn，所以加入这个标识
        nftInfoDetailMgsVo.setOnChain(nftInfoDetailDto.isOnChain());
        nftInfoDetailMgsVo.setNetwork(nftInfoDetailDto.getNetwork());
        nftInfoDetailMgsVo.setForbiddenWithdraw(Optional.ofNullable(nftInfoDetailDto.getSpecificationDto()).map(SpecificationDto::isForbiddenWithdraw).orElse(false));
        nftInfoDetailMgsVo.setForbiddenTrade(Optional.ofNullable(nftInfoDetailDto.getSpecificationDto()).map(SpecificationDto::isForbiddenTrade).orElse(false));
        MediaInfoDto mediaInfoDto = JsonUtils.toObj(nftInfoDetailDto.getMediaInfo(), MediaInfoDto.class);
        nftInfoDetailMgsVo.setSpecification(mediaInfoDto.getSpecification());
        nftInfoDetailMgsVo.setMediaType(mediaInfoDto.getMediaType());
        nftInfoDetailMgsVo.setRawSize(mediaInfoDto.getRawSize());
        nftInfoDetailMgsVo.setDuration(mediaInfoDto.getDuration());
        NftUrlDto nftUrlDto = JsonUtils.toObj(nftInfoDetailDto.getNftUrl(), NftUrlDto.class);

        if (StringUtils.isBlank(nftUrlDto.getCoverUrl())) {
            boolean openMysteryBox = Optional.ofNullable(nftInfoDetailDto)
                    .map(item -> item.getNftType()).map(item -> item.intValue() == NftTypeEnum.OPENED_BOX.getCode().intValue())
                    .orElse(false);
            if (openMysteryBox) {
                nftInfoDetailMgsVo.setCoverUrl(nftUrlDto.getRawUrl());
            }
        } else {
            nftInfoDetailMgsVo.setCoverUrl(nftUrlDto.getCoverUrl());
        }
        nftInfoDetailMgsVo.setRawUrl(nftUrlDto.getRawUrl());

        CollectionInfoVo collectionInfoVo = nftInfoDetailDto.getCollection();
        if (Objects.nonNull(collectionInfoVo)) {
            NftLayerConfigVo nftLayerConfigVo = getNftLayerConfigVo(nftInfoDetailDto.getSerialsNo());
            int verified = 0;
            if (nftLayerConfigVo != null && nftLayerConfigVo.getVerifyType() != null) {
                verified = nftLayerConfigVo.getVerifyType();
            }
            nftInfoDetailMgsVo.setVerified(verified);
            nftInfoDetailMgsVo.setCollectionId(nftInfoDetailDto.getSerialsNo());
            NftLayerVo layerInfo = new NftLayerVo();
            if (nftInfoDetailDto.getSerialsNo() != null) {
                layerInfo = nftAssetHelper.getLayerInfo(nftInfoDetailDto.getSerialsNo());
            }
            layerInfo = Optional.ofNullable(layerInfo).orElse(new NftLayerVo());
            nftInfoDetailMgsVo.setCollectionName(layerInfo.getLayerName());
            nftInfoDetailMgsVo.setCanView(collectionInfoVo.getCanView());
            nftInfoDetailMgsVo.setAvatarUrl(collectionInfoVo.getAvatarUrl());
        }
        if (Objects.nonNull(nftInfoDetailDto.getCreator())) {
            UserInfoArtistMgsVo creator = CopyBeanUtils.fastCopy(nftInfoDetailDto.getCreator(), UserInfoArtistMgsVo.class);
            if (Objects.nonNull(nftInfoDetailDto.getCreator().getUserId())) {
                creator.setUserId(AesUtil.encrypt(nftInfoDetailDto.getCreator().getUserId().toString(), AES_PASSWORD));
                creator.setArtist(true);
            }
            nftInfoDetailMgsVo.setCreator(creator);
        }

        if (CollectionUtils.isNotEmpty(nftInfoDetailDto.getProperties())) {
            List<NftAssetPropertiesInfoVo> properties = new ArrayList<>();
            for (NftAssetPropertiesDto nftAssetPropertiesDto : nftInfoDetailDto.getProperties()) {
                properties.add(CopyBeanUtils.fastCopy(nftAssetPropertiesDto, NftAssetPropertiesInfoVo.class));
            }
            nftInfoDetailMgsVo.setProperties(properties);
            fillPropertiesByAddressV2(nftInfoDetailMgsVo.getCollectionId(), nftInfoDetailMgsVo.getProperties());
        }

        if (Objects.isNull(nftInfoDetailDto.getApproveCount())) {
            nftInfoDetailMgsVo.setApproveCount(0L);
        } else {
            nftInfoDetailMgsVo.setApproveCount(nftInfoDetailDto.getApproveCount());
        }
        if (Objects.nonNull(nftInfoDetailDto.getOwner())) {
            UserInfoArtistMgsVo owner = CopyBeanUtils.fastCopy(nftInfoDetailDto.getOwner(), UserInfoArtistMgsVo.class);
            owner.setUserId(AesUtil.encrypt(nftInfoDetailDto.getOwner().getUserId().toString(), AES_PASSWORD));
            nftInfoDetailMgsVo.setOwner(owner);
        }

        // todo debug
        log.debug("nftInfoDetailMgsVo :: {}", JsonUtils.toJsonHasNullKey(nftInfoDetailMgsVo));

        return nftInfoDetailMgsVo;
    }

    private NftDetailDto getNftDetailDto(Long ownerId, Long nftInfoId, Integer source) {
        boolean internal = true;
        if (isExternal(nftInfoId) || SourceTypeEnum.isExternal(source)) {
            internal = false;
        }
        //boolean internal = Objects.isNull(source) || !SourceTypeEnum.isExternal(source);
        NftDetailDto nftInfoDetailDto = null;
        APIResponse<NftDetailDto> nftInfoDetailDtoAPIResponse = internal ? iNftInfoApi.getNFTInfoDetailByNft(APIRequest.instance(new NftInfoDetailRequest(ownerId, nftInfoId)))
                : iNftInfoApi.getDexNFTInfoDetailByNft(APIRequest.instance(new NftInfoDetailRequest(ownerId, nftInfoId)));

        // todo debug
        log.debug("nftInfoDetailDtoAPIResponse :: {}", JsonUtils.toJsonHasNullKey(nftInfoDetailDtoAPIResponse));

        nftInfoDetailDto = nftInfoDetailDtoAPIResponse.getData();
        if (!baseHelper.isOk(nftInfoDetailDtoAPIResponse) || Objects.isNull(nftInfoDetailDto) || Objects.isNull(nftInfoDetailDto.getId())) {
            return null;
        }
        return nftInfoDetailDto;
    }

    private NftInfoDetailMgsVo fillDetailUserExtendInfo(Long userId, Long nftInfoId, NftInfoDetailMgsVo nftInfoDetailMgsVo) {
        boolean frozen = Objects.equals(NftInfoStatusEnum.FROZEN.getCode(), nftInfoDetailMgsVo.getNftStatus()) ||
                Objects.equals(NftInfoStatusEnum.ADMIN_FROZEN.getCode(), nftInfoDetailMgsVo.getNftStatus()) ||
                Objects.equals((byte) 1, nftInfoDetailMgsVo.getBanned());
        if (Objects.isNull(userId) && frozen) {
            return null;
        } else if (Objects.isNull(userId)) {
            nftInfoDetailMgsVo.setIsOwner(0);
            nftInfoDetailMgsVo.setApprove(false);
            nftInfoDetailMgsVo.setMysteryQuantity(0);
            nftInfoDetailMgsVo.setChainRefDtoList(null);
            return nftInfoDetailMgsVo;
        }

        Integer isOwner = 0;
        String userIdStr = AesUtil.encrypt(userId.toString(), AES_PASSWORD);
        if (Objects.nonNull(nftInfoDetailMgsVo.getOwner())) {
            isOwner = BooleanUtils.toInteger(Objects.equals(nftInfoDetailMgsVo.getOwner().getUserId(), userIdStr));
        }


        if (isOwner == 0 && frozen) {
            return null;
        }
        nftInfoDetailMgsVo.setIsOwner(isOwner);

        NftInfoDetailRequest request = new NftInfoDetailRequest(userId, nftInfoId);
        NftInfoDetailUserVo nftInfoDetailUserVo = getDetailUserExtendInfo(request);

        nftInfoDetailMgsVo.setApprove(nftInfoDetailUserVo.isApprove());
        nftInfoDetailMgsVo.setMysteryQuantity(nftInfoDetailUserVo.getMysteryQuantity());
        nftInfoDetailMgsVo.setChainRefDtoList(nftInfoDetailUserVo.getChainRefDtoList());
        nftInfoDetailMgsVo.setReportVo(nftInfoDetailUserVo.getReportVo());
        Pair<List<Long>, List<Long>> responseAPIResponse = checkForbiddenForBurnByDetailInfo(
                userIdStr,
                NftInfoCheckBurnReq.builder()
                        .isOwner(nftInfoDetailMgsVo.getIsOwner())
                        .creatorId(nftInfoDetailMgsVo.getCreator().getUserId())
                        .isOnChain(nftInfoDetailMgsVo.isOnChain())
                        .nftInfoId(nftInfoId)
                        .marketStatus(nftInfoDetailMgsVo.getMarketStatus())
                        .build()
        );
        if (Objects.nonNull(responseAPIResponse) && Objects.nonNull(responseAPIResponse.getLeft()) &&
                responseAPIResponse.getLeft().contains(nftInfoId)) {
            nftInfoDetailMgsVo.setCanBurn(Boolean.TRUE);
        } else {
            nftInfoDetailMgsVo.setCanBurn(Boolean.FALSE);
        }

        return nftInfoDetailMgsVo;
    }

    public Pair<List<Long>, List<Long>> checkForbiddenForBurnByDetailInfo(String userId, NftInfoCheckBurnReq nftInfoCheckBurnDtos) {
        //根据isOwner判断是否属于自己（isOwner=1），
        // 然后根据assetStatus判断是否是未上架（marketStatus=0\1,NftAssetStatusEnum），
        //然后判断creator是否是自己（creatorId=自己），
        // 然后判断未上过链（isOnChain=false）
        List<Long> couldBurnNftIds = Arrays.asList(nftInfoCheckBurnDtos).stream().filter(nftInfoCheckBurnDto ->
                nftInfoCheckBurnDto.getIsOwner() == 1
                        && nftInfoCheckBurnDto.getMarketStatus() <= NftAssetStatusEnum.MARKET_READY.getCode()
                        && userId.equals(nftInfoCheckBurnDto.getCreatorId())
                        && !nftInfoCheckBurnDto.isOnChain()
        ).map(nftInfoCheckBurnDto -> nftInfoCheckBurnDto.getNftInfoId()).collect(Collectors.toList());
        List<Long> forbidBurnNftIds = Arrays.asList(nftInfoCheckBurnDtos).stream()
                .filter(nftInfoCheckBurnDto -> !couldBurnNftIds.contains(nftInfoCheckBurnDto.getNftInfoId()))
                .map(nftInfoCheckBurnDto -> nftInfoCheckBurnDto.getNftInfoId())
                .collect(Collectors.toList());
        return Pair.of(couldBurnNftIds, forbidBurnNftIds);
    }

    private NftInfoDetailUserVo getDetailUserExtendInfo(NftInfoDetailRequest request) {
        String key = String.format(MgsNftConstants.NFT_REMOTE_CACHE_KEY, CacheSeneEnum.ASSET_USER_DETAIL.name(), CacheUtils.generateKey(request));

        RBucket<String> rBucket = redissonClient.getBucket(key);
        String data = rBucket.get();
        if (Objects.nonNull(data)) {
            return JsonUtils.parse(data, new TypeReference<NftInfoDetailUserVo>() {
            });
        }

        APIResponse<NftDetailWithUserDto> nftDetailWithUserResponse = iNftInfoApi.getNFTInfoDetailWithUser(APIRequest.instance(request));
        if (!baseHelper.isOk(nftDetailWithUserResponse) || Objects.isNull(nftDetailWithUserResponse.getData())) {
            NftInfoDetailUserVo nftInfoDetailUserVo = new NftInfoDetailUserVo();
            nftInfoDetailUserVo.setApprove(false);
            nftInfoDetailUserVo.setMysteryQuantity(0);
            nftInfoDetailUserVo.setChainRefDtoList(null);
            return nftInfoDetailUserVo;
        }
        NftDetailWithUserDto nftDetailWithUser = nftDetailWithUserResponse.getData();
        NftInfoDetailUserVo nftInfoDetailUserVo = new NftInfoDetailUserVo();

        nftInfoDetailUserVo.setApprove(nftDetailWithUser.isApprove());
        nftInfoDetailUserVo.setMysteryQuantity(nftDetailWithUser.getMysteryQuantity());
        nftInfoDetailUserVo.setChainRefDtoList(getNftBlockChainRefList(nftDetailWithUser.getChainRefDtoList()));
        nftInfoDetailUserVo.setReportVo(nftDetailWithUser.getReportVo() != null ?
                nftDetailWithUser.getReportVo() :
                ReportVo.builder().canAdd(false).leftCount(0).build());

        rBucket.setAsync(JsonUtils.toJsonNotNullKey(nftDetailWithUser), 5L, TimeUnit.SECONDS);
        return nftInfoDetailUserVo;
    }

    private List<NftBlockChainRefVo> getNftBlockChainRefList(List<NftBlockChainRefDto> nftBlockChainRefDtoList) {
        if (CollectionUtils.isEmpty(nftBlockChainRefDtoList)) {
            return null;
        }
        List<NftBlockChainRefVo> nftBlockChainRefVos = new ArrayList<>();
        for (NftBlockChainRefDto nftBlockChainRefDto : nftBlockChainRefDtoList) {
            nftBlockChainRefVos.add(CopyBeanUtils.fastCopy(nftBlockChainRefDto, NftBlockChainRefVo.class));
        }
        return nftBlockChainRefVos;
    }

    private MysteryBoxMgsVo getMysteryBoxMgsVo(Long serialsNo) {
        if (Objects.isNull(serialsNo)) {
            return null;
        }
        APIResponse<MysteryBoxProductDetailVo> productDetailVoAPIResponse = mysteryBoxApi.queryMysteryBoxDetailForMeta(APIRequest.instance(
                QueryMysteryBoxDetailForMetaRequest.builder()
                        .serialNo(serialsNo)
                        .build()
        ));
        if (!baseHelper.isOk(productDetailVoAPIResponse)) {
            return null;
        }
        MysteryBoxProductDetailVo mysteryBoxProductDetailVo = productDetailVoAPIResponse.getData();
        MysteryBoxMgsVo mysteryBoxMgsVo = CopyBeanUtils.fastCopy(mysteryBoxProductDetailVo, MysteryBoxMgsVo.class);
        mysteryBoxMgsVo.setSeries(getSeriesMgsVo(mysteryBoxProductDetailVo.getSeries()));
        Creator creator = mysteryBoxProductDetailVo.getCreator();
        if (Objects.nonNull(creator)) {
            mysteryBoxMgsVo.setCreator(UserInfoMgsVo.builder().avatarUrl(creator.getAvatarUrl()).nickName(creator.getNickName()).build());
        }
        return mysteryBoxMgsVo;
    }

    private List<SeriesMgsVo> getSeriesMgsVo(List<Series> seriesList) {
        if (CollectionUtils.isEmpty(seriesList)) {
            return null;
        }
        List<SeriesMgsVo> seriesMgsVoList = new ArrayList<>();
        for (Series series : seriesList) {
            seriesMgsVoList.add(CopyBeanUtils.fastCopy(series, SeriesMgsVo.class));
        }
        return seriesMgsVoList;
    }

    public CommonRet<HashMap> getMintConfig() {
        APIResponse<String> apiResponse = nftAssetApi.getMintConfig();
        baseHelper.checkResponse(apiResponse);
        String mintConfig = apiResponse.getData();
        return new CommonRet(JSON.parseObject(mintConfig, HashMap.class));
    }

    private LoadingCache<Long, NftLayerConfigVo> NFT_LAYER_CONFIG_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(1000)
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build(CacheLoader.asyncReloading(new CacheLoader<Long, NftLayerConfigVo>() {
                @Override
                public NftLayerConfigVo load(Long collectionId) throws Exception {
                    APIResponse<List<NftLayerConfigVo>> response = layerInfoApi.queryUserCollectionByCollectionIdList(Lists.newArrayList(collectionId));
                    checkResponse(response);
                    if (CollectionUtils.isEmpty(response.getData())) {
                        return new NftLayerConfigVo();
                    }
                    return Optional.ofNullable(response.getData().get(0)).orElse(new NftLayerConfigVo());
                }

                @Override
                public Map<Long, NftLayerConfigVo> loadAll(Iterable<? extends Long> keys) throws Exception {
                    List<Long> collectionIdList = ((Set<Long>) keys).stream().collect(Collectors.toList());
                    APIResponse<List<NftLayerConfigVo>> response = layerInfoApi.queryUserCollectionByCollectionIdList(collectionIdList);
                    checkResponse(response);
                    List<NftLayerConfigVo> nftLayerConfigVos = Optional.ofNullable(response.getData()).orElse(Lists.newArrayList());
                    Map<Long, NftLayerConfigVo> collectionId2Config = Maps.newHashMap();
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(nftLayerConfigVos)) {
                        collectionId2Config = nftLayerConfigVos.stream().collect(HashMap::new, (k, v) -> k.put(v.getCollectionId(), v), HashMap::putAll);
                    }
                    Map<Long, NftLayerConfigVo> result = Maps.newHashMap();
                    Map<Long, NftLayerConfigVo> finalCollectionId2Config = collectionId2Config;
                    keys.forEach(f -> {
                        if (finalCollectionId2Config.containsKey(f)) {
                            result.put(f, finalCollectionId2Config.get(f));
                        } else {
                            result.put(f, new NftLayerConfigVo());
                        }
                    });
                    return result;
                }
            }, Executors.newFixedThreadPool(1)));


    public NftLayerConfigVo getNftLayerConfigVo(Long collectionId) {
        try {
            if (Objects.isNull(collectionId)) {
                return NftLayerConfigVo.builder().build();
            }
            return NFT_LAYER_CONFIG_CACHE.get(collectionId);
        } catch (Exception e) {
            log.error("NftLayerHelper:getNftLayerConfigVoMap failed", e);
        }
        return new NftLayerConfigVo();
    }

    public void fillPropertiesByAddressV2(Long collectionId, List<NftAssetPropertiesInfoVo> tokenAttributes) {
        if (collectionId == null || CollectionUtils.isEmpty(tokenAttributes)) {
            return;
        }

        if (mgsNftProperties.getCollectionBlackList().contains(collectionId)) {
            return;
        }

        List<NftAssetPropertiesInfoDto> nftAssetPropertiesInfoDtos = nftChainAssetHelper.proxyFetchAttributesByCollectionId(collectionId);

        // todo debug
        log.debug("nftAssetPropertiesInfoDtos :: {}", JsonUtils.toJsonHasNullKey(nftAssetPropertiesInfoDtos));

        if (CollectionUtils.isNotEmpty(nftAssetPropertiesInfoDtos)) {

            Map<String, NftAssetPropertiesInfoDto> allMap = new HashMap<>();
            nftAssetPropertiesInfoDtos.forEach(
                    nftAssetPropertiesInfoDto -> {
                        allMap.put(
                                nftAssetPropertiesInfoDto.getPropertyType().concat(nftAssetPropertiesInfoDto.getPropertyName()),
                                nftAssetPropertiesInfoDto);
                    }
            );

            tokenAttributes.forEach(s -> {
                NftAssetPropertiesInfoDto dto = allMap.get(s.getPropertyType().concat(s.getPropertyName()));
                if (Objects.nonNull(dto)) {
                    s.setTotal(dto.getTotal());
                    s.setRate(dto.getRate());
                }
            });

        }
    }

    /**
     * <h2>free mint 的前置校验</h2>
     * */
    public CommonRet<PreMintCheckResponse> preMintCheck(Long userId, MintPreCheckRequest request) {

        List<String> rules;

        if (request == null || CollectionUtils.isEmpty(request.getRules())) {
            rules = Arrays.stream(PreMintCheckResponse.TplCodeEnum.values()).map(PreMintCheckResponse.TplCodeEnum::getCode)
                    .collect(Collectors.toList());
        } else {
            rules = request.getRules();
        }

        APIResponse<MinterInfo> minterInfoAPIResponse = mintManagerApi.checkUserMintable(APIRequest.instance(String.valueOf(userId)));
        if (Objects.nonNull(minterInfoAPIResponse.getData()) && minterInfoAPIResponse.getData().isInternal()) {
            return new CommonRet<>();
        }

        Supplier<Stream<APIResponse<MinterInfo>>> source = () -> Stream.of(minterInfoAPIResponse);
        Optional<PreMintCheckResponse> result = Optional.empty();

        // 判断不是测试用户
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.NOT_TEST_USER.getCode())) {
            result = source.get().filter(minter -> (RECON_USER_GRAY_NUMBER.getCode().equalsIgnoreCase(minter.getCode())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.NOT_TEST_USER.getCode()).build())
                    .findAny();
        }

        // 判断总开关关闭
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.SWITCH_OFF.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (baseHelper.isOk(minter) && (minter.getData() == null || !minter.getData().isCreatable())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.SWITCH_OFF.getCode()).build())
                    .findAny();
        }
        // 判断 follow5 人之下
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.FOLLOWER_LIMIT.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (NFT_ASSET_FOLLOW_COUNT_ERROR.getCode().equalsIgnoreCase(minter.getCode())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.FOLLOWER_LIMIT.getCode()).build())
                    .findAny();
        }
        // 判断用户触发自动 ban
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.USER_BANED.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (baseHelper.isOk(minter) && "Suspended".equalsIgnoreCase(minter.getData().getStatus())))
                    .map(minter ->
                            PreMintCheckResponse.builder()
                                    .tplCode(PreMintCheckResponse.TplCodeEnum.USER_BANED.getCode())
                                    .extendInfo(
                                            ImmutableMap.of("isAdminBanned", Boolean.FALSE,
                                                    "reopenTime", (minter.getData().getSuspendTime().getTime() -
                                                            TimeHelper.getUTCTime()))).build())
                    .findAny();
        }
        // 判断 admin 手动 ban
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.USER_BANED.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (baseHelper.isOk(minter) && "Admin Banned".equalsIgnoreCase(minter.getData().getStatus())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.USER_BANED.getCode()).extendInfo(ImmutableMap.of("isAdminBanned", Boolean.TRUE)).build())
                    .findAny();
        }

        // 判断数量超限制
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.MINT_COUNT_LIMIT.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (baseHelper.isOk(minter) && !NumberUtils.INTEGER_MINUS_ONE.equals(Optional.ofNullable(minterInfoAPIResponse.getData().getTotalMintCount()).orElse(0))
                            && Optional.ofNullable(minterInfoAPIResponse.getData().getTotalMintCount()).orElse(0) - Optional.ofNullable(minterInfoAPIResponse.getData().getMintedCount()).orElse(0) <= 0))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.MINT_COUNT_LIMIT.getCode()).extendInfo(ImmutableMap.of("limitMintCount", Optional.ofNullable(minter.getData().getTotalMintCount()).orElse(0),
                            "remainMintCount", Optional.ofNullable(minterInfoAPIResponse.getData().getTotalMintCount()).orElse(0) - Optional.ofNullable(minterInfoAPIResponse.getData().getMintedCount()).orElse(0))).build())
                    .findAny();
        }

        // 判断用户未同意过用户协议
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.USER_NOT_AGREED.getCode()) && !result.isPresent() && !redisTemplate.hasKey(String.format(
                RedisCommonConfig.NFT_MINT_AGREED_RISK_REMINDER, userId))) {
            result = Optional.of(PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.USER_NOT_AGREED.getCode()).build());
        }

        // 处理其他异常
        if (!result.isPresent()
                && !RECON_USER_GRAY_NUMBER.getCode().equalsIgnoreCase(minterInfoAPIResponse.getCode())
                && !NFT_ASSET_FOLLOW_COUNT_ERROR.getCode().equalsIgnoreCase(minterInfoAPIResponse.getCode())) {
            baseHelper.checkResponse(minterInfoAPIResponse);
        }

        return new CommonRet(result.orElse(null));
    }

    public void preMintCheckForCollection(Long userId, MintPreCheckRequest request) {
        List<String> rules;
        if (request == null || CollectionUtils.isEmpty(request.getRules())) {
            rules = Arrays.stream(PreMintCheckResponse.TplCodeEnum.values()).map(x -> x.getCode()).collect(Collectors.toList());
        } else {
            rules = request.getRules();
        }
        APIResponse<MinterInfo> minterInfoAPIResponse = mintManagerApi.checkUserMintable(APIRequest.instance(String.valueOf(userId)));
        if (Objects.nonNull(minterInfoAPIResponse.getData()) && minterInfoAPIResponse.getData().isInternal()) {
            return;
        }
        Supplier<Stream<APIResponse<MinterInfo>>> source = () -> Arrays.asList(minterInfoAPIResponse).stream();
        Optional<PreMintCheckResponse> result = Optional.empty();


        //判断总开关关闭
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.SWITCH_OFF.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (baseHelper.isOk(minter) && (minter.getData() == null || !minter.getData().isCreatable())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.SWITCH_OFF.getCode()).build())
                    .findAny();
        }
        //判断follow5人之下
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.FOLLOWER_LIMIT.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (NFT_ASSET_FOLLOW_COUNT_ERROR.getCode().equalsIgnoreCase(minter.getCode())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.FOLLOWER_LIMIT.getCode()).build())
                    .findAny();
        }

        //判断admin手动ban
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.USER_BANED.getCode()) && !result.isPresent()) {
            result = source.get().filter(minter -> (baseHelper.isOk(minter) && "Admin Banned".equalsIgnoreCase(minter.getData().getStatus())))
                    .map(minter -> PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.USER_BANED.getCode()).extendInfo(ImmutableMap.of("isAdminBanned", Boolean.TRUE)).build())
                    .findAny();
        }

        //判断用户未同意过用户协议
        if (rules.contains(PreMintCheckResponse.TplCodeEnum.USER_NOT_AGREED.getCode()) && !result.isPresent() && !redisTemplate.hasKey(String.format(
                RedisCommonConfig.NFT_MINT_AGREED_RISK_REMINDER, userId))) {
            result = Optional.of(PreMintCheckResponse.builder().tplCode(PreMintCheckResponse.TplCodeEnum.USER_NOT_AGREED.getCode()).build());
        }

        if (result.isPresent()) {
            throw new BusinessException(NftAssetErrorCode.USER_NOT_IN_WHITE_LIST);
        }
    }

    public APIResponse<List<AvailableStakeNftDto>> getAvailableStakeNft(String collectionName, Long userId) {
        log.error("apeAllowStakeCollectionMap,{}", JSON.toJSON(apeAllowStakeCollectionMap));
        Long collectionId = apeAllowStakeCollectionMap.get(collectionName);
        return nftAssetApi.getAvailableStakeNft(collectionId, userId);
    }

    public APIResponse<List<AvailableStakeNftDto>> getAvailableStakeNftV2(Long collectionId, Long userId) {
        log.error("apeAllowStakeCollectionMap,{}", JSON.toJSON(apeAllowStakeCollectionMap));
        return nftAssetApi.getAvailableStakeNft(collectionId, userId);
    }

    @SneakyThrows
    public CommonRet<IPage<NftProfileCollectionVo>> getUserCollections(GetUserCollectionsRequest request, boolean isGray) {
        Integer type = request.getOwnerType().get(0);
        if (!isGray) {
            UserProfileRequest noGrayRequest = UserProfileRequest.builder().userId(request.getUserId())
                    .profileId(request.getProfileId()).profileStrId(request.getProfileStrId())
                    .isOwner(Objects.equals(request.getUserId(), request.getProfileId()))
                    .type(type)
                    .nftType(org.apache.commons.collections4.CollectionUtils.emptyIfNull(request.getAssetType()).size() == 1 ? request.getAssetType().stream().findFirst().orElse(null) : null)
                    .page(request.getPage()).pageSize(request.getPageSize()).build();
            return new CommonRet<>(fetchUserProfileCollections(noGrayRequest, false));
        }
        APIResponse<Page<NftUserCollectionVo>> response = iNftInfoApi.getUserCollections(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        IPage<NftUserCollectionVo> userCollectionList = response.getData();
        Map<Long, CollectionPriceVo> collectionPriceVoMap = new HashMap<>();
        List<Long> regularLayerIds = new ArrayList<>();
        List<Long> mysteryLayerIds = new ArrayList<>();
        //get collection price
        for (NftUserCollectionVo nftProfileCollectionDto : userCollectionList.getRecords()) {
            if (Objects.nonNull(nftProfileCollectionDto.getCollectionType()) && (nftProfileCollectionDto.getCollectionType() == 1)) {
                regularLayerIds.add(nftProfileCollectionDto.getLayerId());
            } else {
                mysteryLayerIds.add(nftProfileCollectionDto.getLayerId());
            }
        }
        if (regularLayerIds.size() > 0) {
            APIResponse<List<CollectionVolumePriceRateVo>> resultList = nftMarketCollectionApi.getCollectionVolumeAndPriceRate24HByCollectionIds(APIRequest.instance(regularLayerIds));
            baseHelper.checkResponse(resultList);
            if (Objects.nonNull(resultList.getData()) && resultList.getData().size() > 0) {
                for (CollectionVolumePriceRateVo collectionVolume : resultList.getData()) {
                    CollectionPriceVo collectionPriceVo = CollectionPriceVo.builder().layerId(collectionVolume.getCollectionId())
                            .floorPrice(FloorPriceDetailVo.builder().amount(collectionVolume.getFloorPrice()).currency("USDT").build())
                            .dailyTradePrice(DailyTradePriceVo.builder().amount(collectionVolume.getTotalVolume()).currency("USDT").build())
                            .build();
                    collectionPriceVoMap.put(collectionPriceVo.getLayerId(), collectionPriceVo);
                }
            }
        }
        if (mysteryLayerIds.size() > 0) {
            for (Long layerId : mysteryLayerIds) {
                CollectionDetailRequest collectionDetailRequest = CollectionDetailRequest.builder()
                        .layerId(layerId).gray(isGray)
                        .build();
                MysteryBoxCollectionPriceVo mysteryBoxCollectionPriceVo = collectionCacheProxy.mysteryLayerPrice(collectionDetailRequest);
                if (Objects.nonNull(mysteryBoxCollectionPriceVo)) {
                    DailyTradePriceVo dailyTradePriceVo = DailyTradePriceVo.builder()
                            .amount(mysteryBoxCollectionPriceVo.getDailyTradePriceVo().getAmount())
                            .currency(mysteryBoxCollectionPriceVo.getDailyTradePriceVo().getCurrency())
                            .build();
                    CollectionPriceVo collectionPriceVo = CollectionPriceVo.builder()
                            .floorPrice(mysteryBoxCollectionPriceVo.getFloorPriceDetailVo())
                            .dailyTradePrice(dailyTradePriceVo)
                            .build();
                    collectionPriceVoMap.put(layerId, collectionPriceVo);
                }
            }
        }
        IPage<NftProfileCollectionVo> nftProfileCollectionVoPage = userCollectionList.convert(nftProfileCollectionDto -> {
            CollectionPriceVo collectionPriceVo = collectionPriceVoMap.get(nftProfileCollectionDto.getLayerId());
            NftProfileCollectionVo nftProfileCollectionVo = NftProfileCollectionVo
                    .builder()
                    .layerId(nftProfileCollectionDto.getLayerId())
                    .layerName(nftProfileCollectionDto.getLayerName())
                    .layerStatus(nftProfileCollectionDto.getLayerStatus())
                    .description(nftProfileCollectionDto.getDescription())
                    .avatarUrl(nftProfileCollectionDto.getAvatarUrl())
                    .bannerUrl(nftProfileCollectionDto.getBannerUrl())
                    .verifyType(nftProfileCollectionDto.getVerifyType())
                    .banned(nftProfileCollectionDto.getBanned())
                    .collectionType(nftProfileCollectionDto.getCollectionType())
                    .freezeReason(freezeReasonHelper.getViewTextByReason(nftProfileCollectionDto.getFreezeReason()))
                    .bannedReason(freezeReasonHelper.getViewTextByReason(nftProfileCollectionDto.getBannedReason()))
                    .isCreator(nftProfileCollectionDto.isCreator())
                    .build();
            if (Objects.nonNull(collectionPriceVo)) {
                nftProfileCollectionVo.setFloorPrice(collectionPriceVo.getFloorPrice());
                nftProfileCollectionVo.setDailyTradePrice(collectionPriceVo.getDailyTradePrice());
            }
            return nftProfileCollectionVo;
        });
        return new CommonRet<>(nftProfileCollectionVoPage);
    }

    @SneakyThrows
    public Page<NftProfileAssetVo> userAssetList(ProfileAssetFilterRequest request, boolean isGray) {
        APIResponse<Page<NftProfileAssetDto>> nftResponse;
        Integer type = request.getOwnerType().get(0);
        if(Constants.TYPE_CREATOR.equals(type) && Objects.equals(2, org.apache.commons.collections4.CollectionUtils.emptyIfNull(request.getAssetType()).stream().findFirst().orElse(null))) {
            //前端同时勾选created和mystery直接返回空
            return new Page<>(request.getPage(), request.getPageSize());
        }
        if (!isGray) {
            Byte marketStatus = null;
            if (request.getTradeType() != null && request.getTradeType().size() == 1) {
                if (request.getTradeType().get(0) == -1) {
                    marketStatus = 25;
                } else if (request.getTradeType().get(0) == -2) {
                    marketStatus = 20;
                }
            }
            Long layerId = request.getCollections() != null ? request.getCollections().stream().findFirst().orElse(null) : null;
            UserProfileRequest noGrayRequest = UserProfileRequest.builder().userId(request.getUserId())
                    .profileId(request.getProfileUserId()).profileStrId(request.getProfileStrId())
                    .isOwner(Objects.equals(request.getUserId(), request.getProfileUserId()))
                    .type(type).marketStatus(marketStatus)
                    .nftType(org.apache.commons.collections4.CollectionUtils.emptyIfNull(request.getAssetType()).size() == 1 ? request.getAssetType().stream().findFirst().orElse(null) : null)
                    .layerId(layerId)
                    .page(request.getPage()).pageSize(request.getPageSize()).build();
            nftResponse = iNftInfoApi.getUserProfileAssets(APIRequest.instance(noGrayRequest));
        } else {
            UserProfileAssetRequest targetRequest = convert2UserProfileAssetRequest(request);
            nftResponse = iNftInfoApi.getUserProfileAssetsV2(APIRequest.instance(targetRequest));
        }
        baseHelper.checkResponse(nftResponse);
        Page<NftProfileAssetDto> nftProfileAssetDtoPage = nftResponse.getData();
        AtomicReference<Boolean> modifyTotalFlag = new AtomicReference<>(Boolean.FALSE);
        Page<NftProfileAssetVo> resultPage = new Page<>();
        resultPage.setCurrent(nftProfileAssetDtoPage.getCurrent());
        resultPage.setSize(nftProfileAssetDtoPage.getSize());
        resultPage.setPages(nftProfileAssetDtoPage.getPages());
        List<Long> nftIds = Lists.newArrayList();
        List<Long> collectionIds = Lists.newArrayList();
        List<NftProfileAssetVo> retList;
        if (!ProfileViewModeEnum.LIST.equalsByCode(request.getViewType())) {
            //卡片模式
            List<NftProductInfoVo> nftProductInfoVoList = productHelper.getUserProfileProductInfo(nftProfileAssetDtoPage, modifyTotalFlag);
            retList = productHelper.convertNftProfileAssetVoList(nftProductInfoVoList, nftProfileAssetDtoPage);
            retList.forEach(it -> activityCR7Helper.pendingCR7Info(it));
        } else {
            //列表模式
            Stopwatch stopWatch = Stopwatch.createStarted();
            nftProfileAssetDtoPage.getRecords().forEach(it->{
                nftIds.add(it.getNftInfoId());
                collectionIds.add(it.getLayerId());
            });
            CompletableFuture<List<NftProductInfoVo>> future1 = CompletableFuture.supplyAsync(() -> productHelper.getUserProfileProductInfo(nftProfileAssetDtoPage, modifyTotalFlag), executorService);
            CompletableFuture<Map<Long, FloorPriceVo>> future2 = CompletableFuture.supplyAsync(() -> getFloorPriceByIds(collectionIds), executorService);
            CompletableFuture<Map<Long, MarketBestOfferVO>> future3 = CompletableFuture.supplyAsync(() -> getBestOfferByIds(nftIds), executorService);
            List<NftProductInfoVo> nftProductInfoVoList = future1.get();
            retList = productHelper.convertNftProfileAssetVoList(nftProductInfoVoList, nftProfileAssetDtoPage);
            Map<Long, FloorPriceVo> floorPriceMap = future2.get();
            Map<Long, MarketBestOfferVO> bestOfferMap = future3.get();
            retList.forEach(it -> activityCR7Helper.pendingCR7Info(it));
            assembleListViewAsset(retList, floorPriceMap, bestOfferMap);
            log.info("profile asset, list mode assemble cost {}ms", stopWatch.elapsed(TimeUnit.MILLISECONDS));
        }
        resultPage.setTotal(modifyTotalFlag.get() ? nftProfileAssetDtoPage.getTotal() - NumberUtils.LONG_ONE : nftProfileAssetDtoPage.getTotal());
        resultPage.setRecords(retList);
        return resultPage;
    }

    @SneakyThrows
    public Page<NftProfileAssetVo> approveAssetList(ProfileAssetFilterRequest request, boolean isGray) {
        if (!isGray) {
            Long layerId = request.getCollections() != null ? request.getCollections().stream().findFirst().orElse(null) : null;
            UserProfileRequest noGrayRequest = UserProfileRequest.builder().userId(request.getUserId())
                    .profileId(request.getProfileUserId()).profileStrId(request.getProfileStrId())
                    .isOwner(Objects.equals(request.getUserId(), request.getProfileUserId()))
                    .type(3).layerId(layerId).page(request.getPage()).pageSize(request.getPageSize()).build();
            return fetchUserProfileAssets(noGrayRequest);
        }
        UserProfileAssetRequest targetRequest = convert2UserProfileAssetRequest(request);
        APIResponse<Page<NftProfileAssetDto>> nftResponse = iNftInfoApi.getProfileApproveAssetsV2(APIRequest.instance(targetRequest));
        baseHelper.checkResponse(nftResponse);
        Page<NftProfileAssetDto> nftProfileAssetDtoPage = nftResponse.getData();
        AtomicReference<Boolean> modifyTotalFlag = new AtomicReference<>(Boolean.FALSE);
        Page<NftProfileAssetVo> resultPage = new Page<>();
        resultPage.setCurrent(nftProfileAssetDtoPage.getCurrent());
        resultPage.setSize(nftProfileAssetDtoPage.getSize());
        resultPage.setPages(nftProfileAssetDtoPage.getPages());
        List<Long> nftIds = Lists.newArrayList();
        List<Long> collectionIds = Lists.newArrayList();
        List<NftProfileAssetVo> retList;
        if (!ProfileViewModeEnum.LIST.equalsByCode(request.getViewType())) {
            //卡片模式
            List<NftProductInfoVo> nftProductInfoVoList = productHelper.getUserProfileProductInfo(nftProfileAssetDtoPage, modifyTotalFlag);
            retList = productHelper.convertNftProfileAssetVoList(nftProductInfoVoList, nftProfileAssetDtoPage);
            retList.forEach(it -> activityCR7Helper.pendingCR7Info(it));
        } else {
            //列表模式
            Stopwatch stopWatch = Stopwatch.createStarted();
            nftProfileAssetDtoPage.getRecords().forEach(it->{
                nftIds.add(it.getNftInfoId());
                collectionIds.add(it.getLayerId());
            });
            CompletableFuture<List<NftProductInfoVo>> future1 = CompletableFuture.supplyAsync(() -> productHelper.getUserProfileProductInfo(nftProfileAssetDtoPage, modifyTotalFlag), executorService);
            CompletableFuture<Map<Long, FloorPriceVo>> future2 = CompletableFuture.supplyAsync(() -> getFloorPriceByIds(collectionIds), executorService);
            CompletableFuture<Map<Long, MarketBestOfferVO>> future3 = CompletableFuture.supplyAsync(() -> getBestOfferByIds(nftIds), executorService);
            List<NftProductInfoVo> nftProductInfoVoList = future1.get();
            retList = productHelper.convertNftProfileAssetVoList(nftProductInfoVoList, nftProfileAssetDtoPage);
            Map<Long, FloorPriceVo> floorPriceMap = future2.get();
            Map<Long, MarketBestOfferVO> bestOfferMap = future3.get();
            retList.forEach(it -> activityCR7Helper.pendingCR7Info(it));
            assembleListViewAsset(retList, floorPriceMap, bestOfferMap);
            log.info("approve asset, list mode assemble cost {}ms", stopWatch.elapsed(TimeUnit.MILLISECONDS));
        }
        resultPage.setTotal(modifyTotalFlag.get() ? nftProfileAssetDtoPage.getTotal() - NumberUtils.LONG_ONE : nftProfileAssetDtoPage.getTotal());
        resultPage.setRecords(retList);
        return resultPage;
    }

    private UserProfileAssetRequest convert2UserProfileAssetRequest(ProfileAssetFilterRequest request) {
        UserProfileAssetRequest userProfileAssetRequest = new UserProfileAssetRequest();
        userProfileAssetRequest.setProfileId(request.getProfileUserId());
        userProfileAssetRequest.setUserId(request.getUserId());
        if (request.getAssetType().size() == 1) {
            userProfileAssetRequest.setNftType(request.getAssetType().get(0));
        }
        userProfileAssetRequest.setVerifyType(request.getVerifyType());
        userProfileAssetRequest.setIsOwner(Objects.equals(request.getProfileUserId(), request.getUserId()));
        userProfileAssetRequest.setType(request.getOwnerType());
        if (request.getTradeType() != null && request.getTradeType().size() == 1) {
            if (request.getTradeType().get(0) == -1) {
                userProfileAssetRequest.setListed(true);
            } else if (request.getTradeType().get(0) == -2) {
                userProfileAssetRequest.setListed(false);
            }
        }
        userProfileAssetRequest.setLayerIds(request.getCollections());
        userProfileAssetRequest.setKeyword(request.getKeyword());
        userProfileAssetRequest.setPage(request.getPage());
        userProfileAssetRequest.setPageSize(request.getPageSize());
        return userProfileAssetRequest;
    }

    private void assembleListViewAsset(List<NftProfileAssetVo> profileAssetList, Map<Long, FloorPriceVo> floorPriceMap, Map<Long, MarketBestOfferVO> bestOfferMap) {
        for (NftProfileAssetVo nftProfileAssetVo : profileAssetList) {
            if (nftProfileAssetVo.getNftInfo() == null) {
                return;
            }
            try {
                nftProfileAssetVo.setFloorPriceVo(floorPriceMap.get(nftProfileAssetVo.getNftInfo().getLayerId()));
                nftProfileAssetVo.setBestOffer(bestOfferMap.get(nftProfileAssetVo.getNftInfo().getNftInfoId()));
            } catch (Exception e) {
                log.warn("assembleListViewAsset fail", e);
            }
        }
    }

    private Map<Long, FloorPriceVo> getFloorPriceByIds(List<Long> collectionIds) {
        try {
            log.info("query floor price, param is {}", JSONObject.toJSONString(collectionIds));
            APIResponse<Map<Long,FloorPriceVo>> response = nftProfileApi.getSimpleFloorPriceList(APIRequest.instance(GetSimpleFloorPriceListRequest.builder().collectionIds(collectionIds).build()));
            baseHelper.checkResponse(response);
            return response.getData();
        } catch (Exception e) {
            log.warn("profile asset get floor price fail", e);
            return Maps.newHashMap();
        }
    }

    private Map<Long, MarketBestOfferVO> getBestOfferByIds(List<Long> nftIds) {
        try {
            log.info("query best offers, param is {}", JSONObject.toJSONString(nftIds));
            APIResponse<Map<Long, MarketBestOfferVO>> bestOffersResponse = nftProfileApi.getNftBestOffers(APIRequest.instance(GetBestOffersRequest.builder().nftIds(nftIds).build()));
            baseHelper.checkResponse(bestOffersResponse);
            return bestOffersResponse.getData();
        } catch (Exception e) {
            log.warn("profile asset get best offer fail", e);
            return Maps.newHashMap();
        }
    }

    private boolean canAssetExpose(Long collectionId, Long nftInfoId) {
        if (!isExternal(nftInfoId)) {
            return true;
        }
        if (null == collectionId) {
            return false;
        }
        return CollectionUtils.isNotEmpty(mgsNftProperties.getExposeDexAssetCollections()) && (mgsNftProperties.getExposeDexAssetCollections().contains(collectionId)
                || mgsNftProperties.getExposeDexAssetCollections().contains(101L));
    }

    public void appendDexFlag(NftAssetDetailResponse response) {
        if (null == response || null == response.getNftInfoDetailMgsVo()) {
            return;
        }

        Long nftId = response.getNftInfoDetailMgsVo().getNftId();
        if (!isExternal(nftId)) {
            response.setDexFlag(0);
            return;
        }

        // dex on sale
        if (response.getProductDetailMgsVo() != null) {
            response.setDexFlag(1);
            return;
        }

        // dex no sale
        response.setDexFlag(2);
    }
}
