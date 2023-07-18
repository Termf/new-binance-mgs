package com.binance.mgs.nft.trade.service;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.DateUtils;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxAdminApi;
import com.binance.nft.mystery.api.vo.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.data.request.SelectUserNftByCollectionRequest;
import com.binance.nft.assetservice.api.data.vo.NftInfoDto;
import com.binance.nft.assetservice.enums.NFTExtendMarketStatusEnum;
import com.binance.nft.assetservice.enums.NftSourceEnum;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.tradeservice.api.IProductApi;
import com.binance.nft.tradeservice.enums.NftTypeEnum;
import com.binance.nft.tradeservice.request.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final NFTMysteryBoxApi mysteryBoxApi;

    private final BaseHelper baseHelper;

    private final INftInfoApi nftInfoApi;

    private final IProductApi productApi;

    private final NFTMysteryBoxAdminApi nftMysteryBoxAdminApi;

    public Long canOnsale(Long relateId, Integer nftType) {

        if(NftTypeEnum.NORMAL.typeEquals(nftType)) return 0L;

        boolean openedBox = NftTypeEnum.OPENED_BOX.typeEquals(nftType);
        Long serialNo = openedBox ? null : relateId;
        Long itemId = openedBox ? relateId : null;
        MysteryBoxDetailQueryType type = openedBox ? MysteryBoxDetailQueryType.ITEM_ID : MysteryBoxDetailQueryType.SERIALS_NO;
        QueryMysteryBoxDetailRequest request = QueryMysteryBoxDetailRequest.builder()
                .mysteryBoxDetailQueryType(type)
                .serialsNo(serialNo)
                .itemId(itemId)
                .build();
        APIResponse<MysteryBoxProductDetailVo> productDetailVoAPIResponse = mysteryBoxApi.queryMysteryBoxDetail(APIRequest.instance(request));
        if (baseHelper.isOk(productDetailVoAPIResponse)) {
            return Optional.ofNullable(productDetailVoAPIResponse.getData().getSellingDelayRemaining())
                    .orElse(0L);
        }

        return 0L;
    }

    public void checkAndSetSecondDelay(MysteryBoxProductDetailVo mysteryBoxProductDetailVo){
        if(mysteryBoxProductDetailVo == null || mysteryBoxProductDetailVo.getStartTime() == null || mysteryBoxProductDetailVo.getSecondMarketSellingDelay() == null) {
            return;
        }
        final Long openTime = mysteryBoxProductDetailVo.getStartTime().getTime() + mysteryBoxProductDetailVo.getSecondMarketSellingDelay() * 60 * 60 * 1000;
        final long c = System.currentTimeMillis();
        if (openTime.compareTo(c) < 0){
            mysteryBoxProductDetailVo.setSecondMarketSellingDelay(null);
        }
    }

    public boolean needLimitOnSale(ProductCreateRequest request, List<Long> nftIds) {
        if(NftTypeEnum.NORMAL.typeEquals(request.getNftType()) && CollectionUtils.isNotEmpty(nftIds)) {
            APIResponse<NftInfoDto> response = nftInfoApi.getNFTInfo(nftIds.get(0));
            baseHelper.checkResponse(response);
            NftInfoDto nftInfo = response.getData();
            final boolean alreadyPassAudited = Objects.nonNull(nftInfo.getMarketSwitches())
                    && nftInfo.getMarketSwitches().get(NFTExtendMarketStatusEnum.AUDITED.getPosition());
            final boolean isNFT = NftSourceEnum.BINANCE_NFT.getDescription().equals(nftInfo.getNftSource());
            return response.getData() != null && response.getData().getInitiator() != null &&
                    nftInfo.getInitiator().equals(baseHelper.getUserId())
                    && !alreadyPassAudited && isNFT;
        }
        return false;
    }
}
