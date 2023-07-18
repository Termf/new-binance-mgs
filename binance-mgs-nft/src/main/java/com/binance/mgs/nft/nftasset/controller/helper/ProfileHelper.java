package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.Assert;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.data.request.UserProfileAssetCountRequest;
import com.binance.nft.assetservice.api.data.vo.NftInfoSimpleVO;
import com.binance.nft.assetservice.api.data.vo.NftProfileAssetCountVo;
import com.binance.nft.assetservice.api.function.ILayerInfoApi;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.market.ifae.NftProfileApi;
import com.binance.nft.market.request.GetBestOffersRequest;
import com.binance.nft.market.request.GetUserVolumeRequest;
import com.binance.nft.market.request.ProfileProductRequest;
import com.binance.nft.market.vo.MarketBestOfferVO;
import com.binance.nft.market.vo.ProfileProductVO;
import com.binance.nft.market.vo.UserVolumeVO;
import com.binance.nft.tradeservice.api.ISellOrderApi;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import com.binance.nft.tradeservice.request.ListingProductRequest;
import com.binance.nft.tradeservice.vo.ListingProductVO;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author joy
 * @date 2022/12/12 10:19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileHelper {
    private final INftInfoApi nftInfoApi;
    private final BaseHelper baseHelper;
    private final NftProfileApi marketProfileApi;
    private final ILayerInfoApi layerInfoApi;
    private final ISellOrderApi sellOrderApi;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    public Long getProfileId(String profileStrId) {
        Long userId = baseHelper.getUserId();
        Long profileId = null;

        if (StringUtils.isNotEmpty(profileStrId)) {
            if (NumberUtils.isDigits(profileStrId)) {
                profileId = Long.parseLong(profileStrId);
            } else {
                try {
                    profileId = Long.parseLong(AesUtil.decrypt(profileStrId, AES_PASSWORD));
                } catch (Exception e) {
                    throw new BusinessException(TradeErrorCode.PARAM_ERROR);
                }
            }
            return profileId;
        }
        Assert.isTrue(userId != null, NftAssetErrorCode.BAD_REQUEST.getCode());
        return userId;
    }

    public NftProfileAssetCountVo getProfileAssetCount(UserProfileAssetCountRequest request) {
        APIResponse<NftProfileAssetCountVo> response = nftInfoApi.getProfileAssetCount(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public BigDecimal getProfileVolume(Long userId) {
        try {
            APIResponse<UserVolumeVO> response = marketProfileApi.getUserVolume(APIRequest.instance(GetUserVolumeRequest.builder().userId(userId).build()));
            baseHelper.checkResponse(response);
            return response.getData().getVolume();
        } catch (Exception e) {
            log.error("get profile user[{}}] volume error", userId, e);
            return BigDecimal.ZERO;
        }
    }

    @SneakyThrows
    public SearchResult<ListingProductVO> getUserProfileProduct(ListingProductRequest request) {
//        APIResponse<SearchResult<ProfileProductVO>> response = marketProfileApi.userProductList(APIRequest.instance(request));
        APIResponse<SearchResult<ListingProductVO>> response = sellOrderApi.queryUserListingProduct(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        SearchResult<ListingProductVO> result = response.getData();
        if(result == null || CollectionUtils.isEmpty(result.getRows())) {
            return result;
        }
        assembleProductCollection(result);
        return result;
    }

    public Map<Long, MarketBestOfferVO> getBestOfferByNftIds(GetBestOffersRequest request) {
        APIResponse<Map<Long, MarketBestOfferVO>> response = marketProfileApi.getNftBestOffers(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    private void assembleProductCollection(SearchResult<ListingProductVO> result) {
        List<Long> nftIdList = result.getRows().stream().map(ListingProductVO::getNftId).distinct().collect(Collectors.toList());
        try {
            APIResponse<List<NftInfoSimpleVO>> response = nftInfoApi.getNftSimpleInfo(APIRequest.instance(nftIdList));
            baseHelper.checkResponse(response);
            Map<Long, NftInfoSimpleVO> nftInfoMap = response.getData().stream().collect(Collectors.toMap(NftInfoSimpleVO::getNftInfoId, it -> it));
            result.getRows().forEach(record->{
                nftInfoMap.computeIfPresent(record.getNftId(), (key, nftInfoVO)->{
                    record.setCollectionId(nftInfoVO.getLayerId());
                    record.setCollectionName(nftInfoVO.getLayerName());
                    record.setCoverUrl(nftInfoVO.getZippedUrl());
                    return null;
                });
            });
        } catch (Exception e) {
            log.error("assemble product collection error, {}", nftIdList);
        }
    }
}
