package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIResponse;
import com.binance.nft.assetservice.api.INftChainAssetApi;
import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesInfoDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NftChainAssetHelper {
    private final BaseHelper baseHelper;
    private final INftChainAssetApi nftChainAssetApi;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_BIG)
    public List<NftAssetPropertiesInfoDto> proxyFetchAttributesByCollectionId(Long collectionId){
        try{
            APIResponse<List<NftAssetPropertiesInfoDto>> apiResponse = nftChainAssetApi.fetchAttributesByCollectionId(collectionId);
            if(!baseHelper.isOk(apiResponse) || apiResponse.getData()==null){
                return null;
            }
            return apiResponse.getData();
        }catch (Exception ex){
            log.error("proxyFetchAttributesByCollectionId error, collectionId:{}", collectionId, ex);
            return null;
        }

    }
}
