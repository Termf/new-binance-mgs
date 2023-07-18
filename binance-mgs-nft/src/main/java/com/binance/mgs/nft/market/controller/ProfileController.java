package com.binance.mgs.nft.market.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.commons.SearchResult;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.mgs.nft.market.request.ProfileAssetFilterRequest;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.mgs.nft.nftasset.controller.helper.ProfileHelper;
import com.binance.mgs.nft.nftasset.vo.NftProfileAssetVo;
import com.binance.mgs.nft.nftasset.vo.NftProfileCollectionVo;
import com.binance.mgs.nft.trade.response.ListingProductMgsVo;
import com.binance.nft.assetservice.api.data.request.GetUserCollectionsRequest;
import com.binance.nft.assetservice.api.data.request.UserProfileAssetCountRequest;
import com.binance.nft.assetservice.api.data.vo.NftProfileAssetCountVo;
import com.binance.nft.market.request.GetBestOffersRequest;
import com.binance.nft.market.vo.MarketBestOfferVO;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.nft.tradeservice.enums.TradeTypeEnum;
import com.binance.nft.tradeservice.request.ListingProductRequest;
import com.binance.nft.tradeservice.vo.ListingProductVO;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author joy
 * @date 2022/12/12 10:26
 */
@Slf4j
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final BaseHelper baseHelper;
    private final ProfileHelper profileHelper;
    private final NftAssetHelper nftAssetHelper;
    private final MgsNftProperties mgsNftProperties;

    private final NftInboxHelper nftInboxHelper;

    @PostMapping(value = "/friendly/nft/asset/market/user-asset-list")
    CommonRet<Page<NftProfileAssetVo>> userAssetList(@Valid @RequestBody ProfileAssetFilterRequest request) {
        Long userId = baseHelper.getUserId();
        Long profileId = profileHelper.getProfileId(request.getProfileStrId());
        request.setProfileUserId(profileId);
        request.setUserId(userId);
        Page<NftProfileAssetVo> resp = nftAssetHelper.userAssetList(request, isGrayProfileUser(userId));
        return new CommonRet<>(resp);
    }

    @PostMapping("/friendly/nft/profile/user/collections")
    public CommonRet<IPage<NftProfileCollectionVo>> fetchUserCollections(@RequestBody GetUserCollectionsRequest request) throws Exception {
        Long userId = baseHelper.getUserId();
        Long profileId = profileHelper.getProfileId(request.getProfileStrId());
        request.setProfileId(profileId);
        request.setUserId(userId);
        return nftAssetHelper.getUserCollections(request, isGrayProfileUser(userId));
    }

    @PostMapping(value = "/friendly/nft/asset/market/user-asset-count")
    CommonRet<NftProfileAssetCountVo> userAssetCount(@Valid @RequestBody UserProfileAssetCountRequest request)  {
        Long userId = baseHelper.getUserId();
        Long profileId = profileHelper.getProfileId(request.getProfileStrId());
        request.setProfileUserId(profileId);
        request.setUserId(userId);
        NftProfileAssetCountVo response = profileHelper.getProfileAssetCount(request);
        return new CommonRet<>(response);
    }

    @PostMapping(value = "/friendly/nft/product/market/user-product-list")
    CommonRet<SearchResult<ListingProductMgsVo>> userProductList(@Valid @RequestBody ListingProductRequest request)  {
        Long userId = baseHelper.getUserId();
        Long profileId = profileHelper.getProfileId(request.getProfileStrId());
        request.setProfileUserId(profileId);
        request.setUserId(userId);
        SearchResult<ListingProductVO> userMarketProduct = profileHelper.getUserProfileProduct(request);
        List<ListingProductVO> rows = Optional.ofNullable(userMarketProduct.getRows()).orElse(Lists.newArrayList());
        BizIdModel auction = new BizIdModel(HistoryType.LIST_AUCTION.name(),  rows.stream()
                .filter(p->TradeTypeEnum.AUCTION.typeEquals(p.getTradeType())).map(ListingProductVO::getProductId).collect(Collectors.toList()));

        BizIdModel fixed = new BizIdModel(HistoryType.LIST_FIXED.name(), rows.stream()
                .filter(p->TradeTypeEnum.FIXED.typeEquals(p.getTradeType())).map(ListingProductVO::getProductId).collect(Collectors.toList()));

        SearchResult<ListingProductMgsVo> auctionResultWithFlag = nftInboxHelper.searchResultWithFlag(userMarketProduct,
                ListingProductMgsVo.class,ListingProductMgsVo::getProductId, Arrays.asList(auction,fixed),ListingProductMgsVo::setUnreadFlag);

        return new CommonRet<>(auctionResultWithFlag);
    }

    @PostMapping(value = "/friendly/nft/profile/user-approve/asset-list")
    public CommonRet<Page<NftProfileAssetVo>> getUserApproveAsset(@Valid @RequestBody ProfileAssetFilterRequest request) throws Exception {
        Long userId = baseHelper.getUserId();
        Long profileId = profileHelper.getProfileId(request.getProfileStrId());
        request.setUserId(userId);
        request.setProfileUserId(profileId);
        Page<NftProfileAssetVo> response = nftAssetHelper.approveAssetList(request, isGrayProfileUser(userId));
        return new CommonRet<>(response);
    }

    @PostMapping(value = "/friendly/nft/profile/gray")
    public CommonRet<Boolean> profileGray() {
        Long userId = baseHelper.getUserId();
        if(userId == null || userId == 0) {
            return new CommonRet<>(false);
        }
        return new CommonRet<>(isGrayProfileUser(userId));
    }

    @PostMapping(value = "/friendly/nft/get-best-offer")
    public CommonRet<Map<Long, MarketBestOfferVO>> getBestOffer(@RequestBody GetBestOffersRequest request) {
        Map<Long, MarketBestOfferVO> result = profileHelper.getBestOfferByNftIds(request);
        return new CommonRet<>(result);
    }

    private boolean isGrayProfileUser(Long userId) {
        if(userId == null || userId == 0) {
            return false;
        }
        return CollectionUtils.containsAny(mgsNftProperties.getProfileWhitelist(), userId) || userId%100 < mgsNftProperties.getProfileWhiteSegment();
    }
}
