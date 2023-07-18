package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.nftasset.response.NftAssetDetailResponse;
import com.binance.mgs.nft.nftasset.vo.NftInfoDetailMgsVo;
import com.binance.mgs.nft.nftasset.vo.NftProfileAssetVo;
import com.binance.mgs.nft.nftasset.vo.UserProfileInfoRet;
import com.binance.nft.activityservice.api.CR7InfoApi;
import com.binance.nft.activityservice.api.CR7UserApi;
import com.binance.nft.activityservice.response.CR7ClaimableResponse;
import com.binance.nft.activityservice.response.CR7ConfigResponse;
import com.binance.nft.activityservice.response.CR7RedeemableResponse;
import com.binance.nft.assetservice.enums.NftAssetStatusEnum;
import com.binance.nft.tradeservice.enums.NftTypeEnum;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author joy
 * @date 2022/11/1 17:42
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityCR7Helper {

    private final CR7InfoApi cr7InfoApi;
    private final CR7UserApi cr7UserApi;
    private final CrowdinHelper crowdinHelper;
    private final com.binance.platform.mgs.base.helper.BaseHelper baseHelper;

    private final static String CR7_KEY = "cr7";
    private final LoadingCache<String, CR7ConfigResponse> cr7ConfigCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1)
                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<String, CR7ConfigResponse>() {
                        @Override
                        public CR7ConfigResponse load(String cr7) {
                            APIResponse<CR7ConfigResponse> cr7Config = cr7InfoApi.getCR7Config();
                            BaseHelper.checkResponse(cr7Config);
                            return cr7Config.getData();
                        }
                    }, Executors.newFixedThreadPool(1)));

    public void pendingCR7Info(NftProfileAssetVo profileAsset) {
        try {
            CR7ConfigResponse config = cr7ConfigCache.get(CR7_KEY);
            boolean flag = Objects.equals(config.getRegularCollectionId(), profileAsset.getNftInfo().getLayerId()) || config.getMysteryBoxCollectionIds().contains(profileAsset.getNftInfo().getLayerId());
            profileAsset.setCR7(flag);
        } catch (Exception e) {
            log.error("pendingCR7Info error", e);
        }
    }
    public void pendingCR7Info(NftAssetDetailResponse response) {
        try {
            if(response == null || response.getNftInfoDetailMgsVo() == null) {
                return;
            }
            NftInfoDetailMgsVo nftInfoDetailMgsVo = response.getNftInfoDetailMgsVo();
            CR7ConfigResponse cr7Config = cr7ConfigCache.get(CR7_KEY);
            if (!NftTypeEnum.NORMAL.typeEquals(nftInfoDetailMgsVo.getNftType().intValue())) {
                response.setCR7(cr7Config.getMysteryBoxCollectionIds().contains(nftInfoDetailMgsVo.getCollectionId()));
            } else {
                boolean cr7Flag = Objects.equals(cr7Config.getRegularCollectionId(), nftInfoDetailMgsVo.getCollectionId());
                response.setCR7(cr7Flag);
                if(!cr7Flag) {
                    return;
                }
                APIResponse<CR7RedeemableResponse> redeemableResponse = cr7InfoApi.redeemable(nftInfoDetailMgsVo.getNftId(), nftInfoDetailMgsVo.getItemId());
                BaseHelper.checkResponse(redeemableResponse);
                response.setRedeemable(redeemableResponse.getData().getFlag() && response.getNftInfoDetailMgsVo().getMarketStatus().equals(NftAssetStatusEnum.MARKET_READY.getCode()));
                response.setRedeemRewardName(redeemableResponse.getData().getRedeemRewardName());
                response.setRedemptionTime(redeemableResponse.getData().getRedemptionTime());
            }
        } catch (Exception e) {
            log.warn("pendingCR7Info error", e);
        }
    }

    public void checkCR7Claimable(Long userId, UserProfileInfoRet ret) {
        try {
            APIResponse<CR7ClaimableResponse> response = cr7UserApi.checkClaimableV2(userId);
            BaseHelper.checkResponse(response);
            ret.setCr7Claimable(response.getData().isClaimable());
            ret.setCr7ErrorMessage(crowdinHelper.getMessageByKey(response.getData().getErrorCode(), baseHelper.getLanguage()));
        } catch (Exception e) {
            log.error("check user cr7 claimable error", e);
        }
    }

    public boolean checkCr7Collection(Long serialsNo) {
        try {
            CR7ConfigResponse config = cr7ConfigCache.get(CR7_KEY);
            if(Objects.isNull(config) || CollectionUtils.isEmpty(config.getMysteryBoxCollectionIds())) {
                return false;
            }

            return config.getMysteryBoxCollectionIds().contains(serialsNo);
        } catch (Exception e) {
            log.error("check checkCr7Collection error", e);
            return false;
        }
    }
}
