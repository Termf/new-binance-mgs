package com.binance.mgs.nft.nftasset.controller.helper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.nftasset.vo.NftProfileAssetVo;
import com.binance.nft.assetservice.api.data.vo.NftProfileAssetDto;
import com.binance.nft.assetservice.enums.FreezeReasonEnum;
import com.binance.nft.fantoken.ifae.IActivityPrizePoolManagerApi;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import com.binance.nft.tradeservice.api.IProductV2Api;
import com.binance.nft.tradeservice.constant.Constants;
import com.binance.nft.tradeservice.response.ProductDetailV2Response;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.mgs.nft.market.helper.ArtistHelper;
import com.binance.mgs.nft.nftasset.vo.MysteryBoxProductSimpleRet;
import com.binance.mgs.nft.nftasset.vo.NftAssetPersonalRet;
import com.binance.mgs.nft.nftasset.vo.ProductItemWithApprove;
import com.binance.nft.assetservice.api.data.vo.ApproveUserListVo;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxOnSaleSimpleVo;
import com.binance.nft.assetservice.api.data.vo.NftAssetPersonalVo;
import com.binance.nft.assetservice.enums.NftAssetStatusEnum;
import com.binance.nft.market.vo.UserApproveInfo;
import com.binance.nft.tradeservice.api.IProductApi;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import com.binance.nft.tradeservice.enums.TradeTypeEnum;
import com.binance.nft.tradeservice.request.NftProductInfoRequest;
import com.binance.nft.tradeservice.request.ProductDetailRequest;
import com.binance.nft.tradeservice.response.ProductDetailResponse;
import com.binance.nft.tradeservice.vo.ArtistUserInfo;
import com.binance.nft.tradeservice.vo.NftProductInfoVo;
import com.binance.nft.tradeservice.vo.ProductInfoVo;
import com.binance.nft.tradeservice.vo.UserInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductHelper {

    private final IProductApi productApi;

    private final IProductV2Api iProductV2Api;

    private final ArtistHelper artistHelper;

    private final BaseHelper baseHelper;

    private final FreezeReasonHelper freezeReasonHelper;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public ProductDetailResponse queryProductDetailById(Long userId, Long productId) {

        ProductDetailRequest request = new ProductDetailRequest();
        request.setProductId(productId);
        request.setUserId(userId);

        APIRequest<ProductDetailRequest> instance = APIRequest.instance(request);
        try {
            APIResponse<ProductDetailResponse> detail = productApi.detail(instance);
            baseHelper.checkResponse(detail);
            return detail.getData();
        } catch (Exception e) {
            log.error("query product detail error");
            throw new BusinessException(TradeErrorCode.PRODUCT_NOT_EXISTS);
        }
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public List<ProductItemWithApprove> queryProductItemVo(List<ApproveUserListVo> userListVos) {

        if (CollectionUtils.isEmpty(userListVos)) {
            return Collections.emptyList();
        }
        List<Long> productIdList = userListVos.stream().map(item ->
                item.getProductId()).filter(item -> item > 0).distinct().collect(Collectors.toList());

        Map<Long, ApproveUserListVo> approveUserListVoMap = userListVos.stream().collect(Collectors.
                toMap(ApproveUserListVo::getProductId, Function.identity(), (v1, v2) -> v1));
        try {
            APIResponse<List<ProductInfoVo>> response = productApi.
                    productInfosWithId(APIRequest.instance(productIdList));

            baseHelper.checkResponse(response);

            List<Long> artist = getArtistByUserId(response.getData());

            return Optional.ofNullable(response.getData())
                    .orElse(Collections.emptyList()).stream()
                    .map(item -> {
                        ProductItemWithApprove approve = new ProductItemWithApprove();
                        BeanUtils.copyProperties(item, approve);
                        ArtistUserInfo creator = item.getCreator();
                        if(creator == null) {
                            approve.setCreator(null);
                        } else {
                            creator.setArtist(check(creator, artist));
                            approve.setCreator(creator);
                        }
                        ApproveUserListVo userListVo = approveUserListVoMap.get(item.getProductId());
                        if (userListVo != null) {
                            approve.setUserApproveInfo(UserApproveInfo.builder()
                                    .count(userListVo.getCount())
                                    .approve(true)
                                    .build());
                        }
                        approve.setMediaType(userListVo.getMediaType());
                        return approve;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("query product info list error, the request is {}", productIdList, e);
        }
        return Collections.emptyList();
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public ProductDetailV2Response getProductDetailV2Response(Long userId, Long nftInfoId) {
        ProductDetailRequest request = new ProductDetailRequest();
        request.setUserId(userId);
        request.setNftId(nftInfoId);
        try {
            APIResponse<ProductDetailV2Response> productDetailV2ResponseAPIResponse = iProductV2Api.detail(APIRequest.instance(request));
            if(!baseHelper.isOk(productDetailV2ResponseAPIResponse) || Objects.isNull(productDetailV2ResponseAPIResponse.getData())) {
                return null;
            }
            return productDetailV2ResponseAPIResponse.getData();
        }catch (Exception e) {
            log.error("[getProductDetailV2Response]get productDetail error:{}, userId:{},nftInfoId:{}", e, userId, nftInfoId);
            return null;
        }
    }

    public List<NftProductInfoVo> getProductInfoMysteryBox(Page<MysteryBoxOnSaleSimpleVo> pageData, AtomicReference<Boolean> modifyTotalFlag) {
        List<NftProductInfoRequest.NftProductParam> arrayList = new ArrayList<>();

        pageData.getRecords()
                .forEach(x -> {
                            if (NumberUtils.LONG_ONE.equals(x.getOrderNo())) {
                                modifyTotalFlag.set(Boolean.TRUE);
                                return;
                            }
                            arrayList.add(
                                    new NftProductInfoRequest.NftProductParam(
                                            x.getOrderNo(), x.getNftInfoId()
                                    )
                            );
                        }
                );
        log.debug("NftProductParamList :: {}", JsonUtils.toJsonHasNullKey(arrayList));

        NftProductInfoRequest productInfoRequest = new NftProductInfoRequest();
        productInfoRequest.setParamList(arrayList);
        log.debug("NftProductInfoRequest :: {}", JsonUtils.toJsonHasNullKey(productInfoRequest));
        APIResponse<List<NftProductInfoVo>> apiResponsePrd = null;

        try {
            apiResponsePrd = productApi.getNftProductInfos(APIRequest.instance(productInfoRequest));
            log.debug("apiResponsePrd :: {}", apiResponsePrd);
        } catch (Exception ex) {
            log.warn("Pageable :: {}", ex.getMessage());
        }

        baseHelper.checkResponse(apiResponsePrd);

        return apiResponsePrd.getData();
    }

    public List<NftProductInfoVo> getProductInfoRegular(Page<NftAssetPersonalVo> pageData, AtomicReference<Boolean> modifyTotalFlag) {
        List<NftProductInfoRequest.NftProductParam> arrayList = new ArrayList<>();

        pageData.getRecords()
                .forEach(x -> {
                            if (NumberUtils.LONG_ONE.equals(x.getOrderNo())) {
                                modifyTotalFlag.set(Boolean.TRUE);
                                return;
                            }
                            arrayList.add(
                                    new NftProductInfoRequest.NftProductParam(
                                            x.getOrderNo(), x.getNftInfoId()
                                    )
                            );
                        }
                );
        log.debug("NftProductParamList :: {}", JsonUtils.toJsonHasNullKey(arrayList));

        NftProductInfoRequest productInfoRequest = new NftProductInfoRequest();
        productInfoRequest.setParamList(arrayList);
        log.debug("NftProductInfoRequest :: {}", JsonUtils.toJsonHasNullKey(productInfoRequest));
        APIResponse<List<NftProductInfoVo>> apiResponsePrd = null;

        try {
            apiResponsePrd = productApi.getNftProductInfos(APIRequest.instance(productInfoRequest));
            log.debug("apiResponsePrd :: {}", apiResponsePrd);
        } catch (Exception ex) {
            log.warn("Pageable :: {}", ex.getMessage());
        }

        baseHelper.checkResponse(apiResponsePrd);

        return apiResponsePrd.getData();
    }

    public List<NftProductInfoVo> getUserProfileProductInfo(Page<NftProfileAssetDto> pageData, AtomicReference<Boolean> modifyTotalFlag) {
        List<NftProductInfoRequest.NftProductParam> arrayList = new ArrayList<>();

        pageData.getRecords()
                .forEach(x -> {
                            if (NumberUtils.LONG_ONE.equals(x.getOrderNo())) {
                                modifyTotalFlag.set(Boolean.TRUE);
                                return;
                            }
                            arrayList.add(
                                    new NftProductInfoRequest.NftProductParam(
                                            x.getOrderNo(), x.getNftInfoId()
                                    )
                            );
                        }
                );
        log.debug("NftProductParamList :: {}", JsonUtils.toJsonHasNullKey(arrayList));

        NftProductInfoRequest productInfoRequest = new NftProductInfoRequest();
        productInfoRequest.setParamList(arrayList);
        log.debug("NftProductInfoRequest :: {}", JsonUtils.toJsonHasNullKey(productInfoRequest));
        APIResponse<List<NftProductInfoVo>> apiResponsePrd = null;

        try {
            apiResponsePrd = productApi.getNftProductInfos(APIRequest.instance(productInfoRequest));
            log.debug("apiResponsePrd :: {}", apiResponsePrd);
        } catch (Exception ex) {
            log.warn("Pageable :: {}", ex.getMessage());
        }

        baseHelper.checkResponse(apiResponsePrd);

        return apiResponsePrd.getData();
    }


    public List<NftProfileAssetVo> convertNftProfileAssetVoList(List<NftProductInfoVo> data,
                                                                Page<NftProfileAssetDto> pageData) {
        List<NftProfileAssetVo> list = new ArrayList<>(pageData.getRecords().size());
        if (CollectionUtils.isEmpty(data)) {

            pageData.getRecords().forEach(x -> {
                if (Objects.isNull(x)) return;

                if(x.getFreezeReasonId() != null && x.getFreezeReasonId() > 0){
                    x.setFreezeReason(freezeReasonHelper.getViewTextByReason(
                            String.valueOf(x.getFreezeReasonId())));
                }
                if(x.getBannedReasonId() != null && x.getBannedReasonId() > 0){
                    x.setBannedReason(freezeReasonHelper.getViewTextByReason(
                            String.valueOf(x.getBannedReasonId())));
                }
                NftProfileAssetVo ret = NftProfileAssetVo.builder()
                        .nftInfo(x)
                        .build();
                list.add(ret);
            });
            return list;
        }

        Map<Long, NftProductInfoVo> infoVoMap = data.stream().collect(
                Collectors.toMap(
                        NftProductInfoVo::getNftId, productInfoVo -> productInfoVo
                )
        );

        pageData.getRecords().forEach(x -> {
            if(x.getFreezeReasonId() != null && x.getFreezeReasonId() > 0){
                x.setFreezeReason(freezeReasonHelper.getViewTextByReason(
                        String.valueOf(x.getFreezeReasonId())));
            }
            if(x.getBannedReasonId() != null && x.getBannedReasonId() > 0){
                x.setBannedReason(freezeReasonHelper.getViewTextByReason(
                        String.valueOf(x.getBannedReasonId())));
            }
            NftProductInfoVo vo = infoVoMap.get(x.getNftInfoId());
            if (Objects.isNull(vo)) {
                NftProfileAssetVo ret = NftProfileAssetVo.builder()
                        .nftInfo(x)
                        .build();
                list.add(ret);
                return;
            }

            MysteryBoxProductSimpleRet productInfo = MysteryBoxProductSimpleRet.builder()
                    .nftUrl(x.getZippedUrl())
                    .marketStatus(x.getMarketStatus())
                    .asset(vo.getCurrency())
                    .productId(String.valueOf(vo.getProductId()))
                    .productName(vo.getTitle())
                    .status(vo.getStatus())
                    .remaining(vo.getSetEndTime())
                    .setStartTime(vo.getSetStartTime())
                    .setEndTime(vo.getSetEndTime())
                    .timestamp(System.currentTimeMillis())
                    .totalOnSale(vo.getTotalOnsale())
                    .offerType(vo.getTradeType().byteValue())
                    .totalListed(vo.getTotalListed())
                    .openListPlatforms(Optional.ofNullable(vo.getOpenListPlatforms()).orElse(Collections.emptyList()))
                    .build();

            if (TradeTypeEnum.FIXED.typeEquals(vo.getTradeType())) {
                productInfo.setPrice(
                        Optional.ofNullable(vo.getAmount())
                                .orElse(BigDecimal.ZERO).toPlainString()
                );
                productInfo.setStartPrice(Optional.ofNullable(vo.getAmount()).orElse(BigDecimal.ZERO).toPlainString());
            } else if (TradeTypeEnum.AUCTION.typeEquals(vo.getTradeType())) {

                final BigDecimal bidCurrentAmount = Optional.ofNullable(vo.getCurrentAmount())
                        .orElse(BigDecimal.ZERO);

                BigDecimal amount;
                if (bidStartFlag(bidCurrentAmount)) {
                    amount = bidCurrentAmount;
                    productInfo.setBidStatus(NumberUtils.BYTE_ONE);
                } else {
                    amount = vo.getAmount();
                    productInfo.setBidStatus(NumberUtils.BYTE_ZERO);
                }

                productInfo.setPrice(amount.toPlainString());
                productInfo.setStartPrice(Optional.ofNullable(vo.getAmount()).orElse(BigDecimal.ZERO).toPlainString());
            }
            NftProfileAssetVo ret = NftProfileAssetVo.builder()
                    .nftInfo(x)
                    .productInfo(productInfo)
                    .build();


            list.add(ret);

        });
        return list;
    }

    public List<NftAssetPersonalRet> convertDataRegular(List<NftProductInfoVo> data, Page<NftAssetPersonalVo> pageData) {
        List<NftAssetPersonalRet> list = new ArrayList<>(pageData.getRecords().size());

        if (CollectionUtils.isEmpty(data)) {

            pageData.getRecords().forEach(x -> {
                if (Objects.isNull(x)) return;

                if(x.getFreezeReasonId() != null && x.getFreezeReasonId() > 0){
                    x.setFreezeReason(freezeReasonHelper.getViewTextByReason(
                            String.valueOf(x.getFreezeReasonId()))
                    );
                }
                NftAssetPersonalRet ret = NftAssetPersonalRet.builder()
                        .nftInfo(x)
                        .build();

                list.add(ret);
            });
            return list;
        }

        Map<Long, NftProductInfoVo> infoVoMap = data.stream().collect(
                Collectors.toMap(
                        NftProductInfoVo::getNftId, productInfoVo -> productInfoVo
                )
        );


        pageData.getRecords().forEach(x -> {

            NftProductInfoVo vo = infoVoMap.get(x.getNftInfoId());
            if (Objects.isNull(vo)) {
                return;
            }

            MysteryBoxProductSimpleRet productInfo = MysteryBoxProductSimpleRet.builder()
                    .nftUrl(x.getZippedUrl())
                    .marketStatus(x.getMarketStatus())
                    .asset(vo.getCurrency())
                    .productId(String.valueOf(vo.getProductId()))
                    .productName(vo.getTitle())
                    .remaining(vo.getSetEndTime())
                    .totalOnSale(vo.getTotalOnsale())
                    .offerType(vo.getTradeType().byteValue())
                    .totalListed(vo.getTotalListed())
                    .build();

            if (TradeTypeEnum.FIXED.typeEquals(vo.getTradeType())) {
                productInfo.setPrice(
                        Optional.ofNullable(vo.getAmount())
                                .orElse(BigDecimal.ZERO).toPlainString()
                );
            } else if (TradeTypeEnum.AUCTION.typeEquals(vo.getTradeType())) {

                final BigDecimal bidCurrentAmount = Optional.ofNullable(vo.getCurrentAmount())
                        .orElse(BigDecimal.ZERO);

                BigDecimal amount;
                if (bidStartFlag(bidCurrentAmount)) {
                    amount = bidCurrentAmount;
                    productInfo.setBidStatus(NumberUtils.BYTE_ONE);
                } else {
                    amount = vo.getAmount();
                    productInfo.setBidStatus(NumberUtils.BYTE_ZERO);
                }

                productInfo.setPrice(amount.toPlainString());
            }
            if(x.getFreezeReasonId() != null && x.getFreezeReasonId() > 0){
                x.setFreezeReason(freezeReasonHelper.getViewTextByReason(
                        String.valueOf(x.getFreezeReasonId()
                        )));
            }
            NftAssetPersonalRet ret = NftAssetPersonalRet.builder()
                    .nftInfo(x)
                    .productInfo(productInfo)
                    .build();


            list.add(ret);

        });
        return list;
    }

    public List<MysteryBoxProductSimpleRet> convertData(List<NftProductInfoVo> data, Page<MysteryBoxOnSaleSimpleVo> pageData) {
        Map<Long, NftProductInfoVo> infoVoMap = data.stream().collect(
                Collectors.toMap(
                        NftProductInfoVo::getNftId, productInfoVo -> productInfoVo
                )
        );

        List<MysteryBoxProductSimpleRet> list = new ArrayList<>(pageData.getRecords().size());

        pageData.getRecords().forEach(x -> {
            NftProductInfoVo vo = infoVoMap.get(x.getNftInfoId());
            final boolean exceptionProduct = checkIsExceptionProduct(x);
            if (Objects.isNull(vo) && !exceptionProduct) {
                return;
            }

            if (exceptionProduct){
                final Integer exceptionQuantity = getExceptionQuantity(x.getFreezeQty(), x.getQuantity());
                MysteryBoxProductSimpleRet build = MysteryBoxProductSimpleRet.builder()
                        .nftUrl(x.getNftUrl())
                        .marketStatus(x.getMarketStatus())
                        .rarity(x.getRarity())
                        .nftType(x.getNftType())
                        .freezeQty(x.getFreezeQty())
                        .quantity(x.getQuantity())
                        .totalListed(exceptionQuantity)
                        .totalOnSale(exceptionQuantity)
                        .build();
                list.add(build);
                return;
            }

            MysteryBoxProductSimpleRet build = MysteryBoxProductSimpleRet.builder()
                    .nftUrl(x.getNftUrl())
                    .marketStatus(x.getMarketStatus())
                    .rarity(x.getRarity())
                    .nftType(x.getNftType())
                    .freezeQty(x.getFreezeQty())
                    .quantity(x.getQuantity())
                    .asset(vo.getCurrency())
                    .productId(String.valueOf(vo.getProductId()))
                    .productName(vo.getTitle())
                    .remaining(vo.getSetEndTime())
                    .totalOnSale(vo.getTotalOnsale())
                    .network(x.getNetwork())
                    .offerType(vo.getTradeType().byteValue())
                    .totalListed(vo.getTotalListed())
                    .build();

            if (TradeTypeEnum.FIXED.typeEquals(vo.getTradeType())) {
                build.setPrice(
                        Optional.ofNullable(vo.getAmount())
                                .orElse(BigDecimal.ZERO).toPlainString()
                );
            } else if (TradeTypeEnum.AUCTION.typeEquals(vo.getTradeType())) {

                final BigDecimal bidCurrentAmount = Optional.ofNullable(vo.getCurrentAmount())
                        .orElse(BigDecimal.ZERO);

                BigDecimal amount;
                if (bidStartFlag(bidCurrentAmount)) {
                    amount = bidCurrentAmount;
                    build.setBidStatus(NumberUtils.BYTE_ONE);
                } else {
                    amount = vo.getAmount();
                    build.setBidStatus(NumberUtils.BYTE_ZERO);
                }
                build.setPrice(amount.toPlainString());
            }
            list.add(build);

        });
        return list;
    }

    private boolean check(ArtistUserInfo userInfoVo, List<Long> userIdList){

        if(userInfoVo == null || CollectionUtils.isEmpty(userIdList)){
            return false;
        }
        Long userId = userInfoVo.getUserId();
        if(userId != null){
            return userIdList.contains(userId);
        }
        return false;
    }

    private List<Long> getArtistByUserId(List<ProductInfoVo> productInfoVos){

        if(CollectionUtils.isEmpty(productInfoVos)){
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList();
        for(ProductInfoVo productInfoVo : productInfoVos){
            UserInfoVo creator = productInfoVo.getCreator();
            if(creator instanceof ArtistUserInfo){
                Long userId = ((ArtistUserInfo) creator).getUserId();
                if(userId != null){
                    result.add(userId);
                }
            }
        }

        return artistHelper.getUserArtistListByUserIdList(result);
    }

    private boolean bidStartFlag(BigDecimal bidCurrentAmount) {
        return bidCurrentAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean checkIsExceptionProduct(MysteryBoxOnSaleSimpleVo mysteryBoxOnSaleSimpleVo){
        if ((NftAssetStatusEnum.MARKET_READY.getCode() == mysteryBoxOnSaleSimpleVo.getMarketStatus() && mysteryBoxOnSaleSimpleVo.getFreezeQty() > 0)
                || (NftAssetStatusEnum.MARKET_ON_SALE.getCode() == mysteryBoxOnSaleSimpleVo.getMarketStatus() && mysteryBoxOnSaleSimpleVo.getQuantity() > 0)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Integer getExceptionQuantity(Integer freezeQty, Integer quantity){

        return (!Objects.isNull(freezeQty) && freezeQty > 0) ? freezeQty : quantity;
    }
}
