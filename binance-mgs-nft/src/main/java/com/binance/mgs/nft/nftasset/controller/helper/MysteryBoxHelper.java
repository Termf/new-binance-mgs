package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxAdminApi;
import com.binance.nft.mystery.api.vo.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxSerialsSimpleVo;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxSerialsSimpleVoTotal;
import com.binance.nft.mystery.api.iface.INFTMysteryBoxQueryApi;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.mystery.api.vo.query.MysteryBoxMetaResponse;
import com.binance.nft.mystery.api.vo.query.common.MysteryBoxMetaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysteryBoxHelper {

    private final NFTMysteryBoxApi mysteryBoxApi;
    private final BaseHelper baseHelper;
    private final INFTMysteryBoxQueryApi inftMysteryBoxQueryApi;
    private final RedisTemplate redisTemplate;
    private final NFTMysteryBoxAdminApi nftMysteryBoxAdminApi;

    public static final String MYSTERY_BOX_META_KEY = "mystery:box:meta:";
    public static final Integer MYSTERY_BOX_META_EXPIRE_TIME = 60 * 60;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_CONFIG)
    public MysteryBoxProductDetailVo queryMysteryBoxDetailForMeta(Long serialsNo){
        APIResponse<MysteryBoxProductDetailVo> apiResponse = APIResponse.getOKJsonResult();
        try {
            apiResponse = mysteryBoxApi.queryMysteryBoxDetailForMeta(APIRequest.instance(
                    QueryMysteryBoxDetailForMetaRequest.builder()
                            .serialNo(serialsNo)
                            .build()));

            baseHelper.checkResponse(apiResponse);
            MysteryBoxProductDetailVo mysteryBoxDetailForMeta = apiResponse.getData();
            if (Objects.isNull(mysteryBoxDetailForMeta)
                    || Objects.isNull(mysteryBoxDetailForMeta.getSecondMarketSellingDelay())
                    || Objects.isNull(mysteryBoxDetailForMeta.getStartTime())){
                CommonPageRequest<ListMysteryBoxRequest> boxRequest = CommonPageRequest.<ListMysteryBoxRequest>builder()
                        .params(ListMysteryBoxRequest.builder().batchId(String.valueOf(serialsNo)).build())
                        .page(1).size(1).build();
                APIResponse<CommonPageResponse<ListMysteryBoxResponse>> response = nftMysteryBoxAdminApi.listMysteryBox(APIRequest.instance(boxRequest));
                baseHelper.checkResponse(response);
                if (CollectionUtils.isEmpty(response.getData().getData()) || response.getData().getData().get(0).getListStartTime() == null) {
                    return apiResponse.getData();
                }
                Date listStartTime = response.getData().getData().get(0).getListStartTime();
                final long c = System.currentTimeMillis();
                if (listStartTime.getTime() >= c) {
                    Long sellingDelayRemaining = listStartTime.getTime() - c;
                    Long hourRemaining = sellingDelayRemaining / 3600000;
                    mysteryBoxDetailForMeta.setDuration(String.valueOf(sellingDelayRemaining));
                    mysteryBoxDetailForMeta.setSecondMarketSellingDelay(hourRemaining);
                }
            } else {
                final Long secondMarketSellingDelay = mysteryBoxDetailForMeta.getSecondMarketSellingDelay();
                final Long openTime = mysteryBoxDetailForMeta.getStartTime().getTime() + secondMarketSellingDelay * 60 * 60 * 1000;
                final long c = System.currentTimeMillis();
                if (openTime.compareTo(c) >= 0){
                    mysteryBoxDetailForMeta.setDuration(String.valueOf(openTime - c));
                } else {
                    mysteryBoxDetailForMeta.setSecondMarketSellingDelay(null);
                }
            }
        }catch (Exception ex){
            log.warn("queryMysteryBoxDetailForMeta :: ",ex);
        }

        return apiResponse.getData();

    }

    public void getMysteryMetaBatch(MysteryBoxSerialsSimpleVoTotal mysteryBoxSerialsSimpleVoTotal){
        try {
            if(Objects.isNull(mysteryBoxSerialsSimpleVoTotal.getMysteryBoxSerialsSimpleVo()) || mysteryBoxSerialsSimpleVoTotal.getMysteryBoxSerialsSimpleVo().size() <= 0) {
                return;
            }
            List<Long> serialsNoList = new ArrayList<>();
            for (MysteryBoxSerialsSimpleVo mysteryBoxSerialsSimpleVo : mysteryBoxSerialsSimpleVoTotal.getMysteryBoxSerialsSimpleVo()) {
                serialsNoList.add(mysteryBoxSerialsSimpleVo.getSerialsNo());
            }
            Map<Long, MysteryBoxMetaDto> mysteryBoxMetaDtoMap = getMysteryBoxMetaDtoMap(serialsNoList);
            if(Objects.nonNull(mysteryBoxMetaDtoMap)) {
                for (MysteryBoxSerialsSimpleVo mysteryBoxSerialsSimpleVo : mysteryBoxSerialsSimpleVoTotal.getMysteryBoxSerialsSimpleVo()) {
                    MysteryBoxMetaDto mysteryBoxMetaDto = mysteryBoxMetaDtoMap.get(mysteryBoxSerialsSimpleVo.getSerialsNo());
                    if(Objects.nonNull(mysteryBoxMetaDto)) {
                        mysteryBoxSerialsSimpleVo.setZippedUrl(mysteryBoxMetaDto.getBatchCoverImage());
                        mysteryBoxSerialsSimpleVo.setSerialsName(mysteryBoxMetaDto.getName());
                    }
                }
            }
        }catch (Exception e) {
            log.error("[getMysteryMetaBatch] mysteryBoxSerialsSimpleVoTotal:{} error:{}", mysteryBoxSerialsSimpleVoTotal, e);
        }
    }

    private Map<Long, MysteryBoxMetaDto> getMysteryBoxMetaDtoMap(List<Long> serialsNoList) {
        Map<Long, MysteryBoxMetaDto> mysteryBoxMetaDtoMap = new HashMap<>();
        try {
            List<String> redisSerialsNoLists = new ArrayList<>();
            for (Long serialsNo : serialsNoList) {
                redisSerialsNoLists.add(MYSTERY_BOX_META_KEY + serialsNo);
            }
            List<MysteryBoxMetaDto> mysteryBoxMetaDtos = redisTemplate.opsForValue().multiGet(redisSerialsNoLists);
            if(Objects.nonNull(mysteryBoxMetaDtos) && mysteryBoxMetaDtos.size() >0) {
                for (MysteryBoxMetaDto mysteryBoxMetaDto : mysteryBoxMetaDtos) {
                    if(Objects.nonNull(mysteryBoxMetaDto)) {
                        mysteryBoxMetaDtoMap.put(mysteryBoxMetaDto.getSerialsNo(), mysteryBoxMetaDto);
                    }
                }
            }
            if(Objects.nonNull(mysteryBoxMetaDtos) && mysteryBoxMetaDtoMap.size() == serialsNoList.size()) {
                //全部meta获取到
                return mysteryBoxMetaDtoMap;
            }else {
                //获取到部分，需要对剩下的进行计算
                List<Long> otherSerialsNoList = getNoCacheNos(mysteryBoxMetaDtoMap, serialsNoList);
                APIResponse<MysteryBoxMetaResponse> mysteryBoxMetaResponseAPIResponse = inftMysteryBoxQueryApi.queryMysteryMetaBatch(
                        APIRequest.instance(otherSerialsNoList));
                baseHelper.checkResponse(mysteryBoxMetaResponseAPIResponse);
                if(Objects.nonNull(mysteryBoxMetaResponseAPIResponse.getData())
                        && Objects.nonNull(mysteryBoxMetaResponseAPIResponse.getData().getMysteryBoxMetaDtos())
                        && mysteryBoxMetaResponseAPIResponse.getData().getMysteryBoxMetaDtos().size() > 0) {
                    for (MysteryBoxMetaDto mysteryBoxMetaDto : mysteryBoxMetaResponseAPIResponse.getData().getMysteryBoxMetaDtos()) {
                        mysteryBoxMetaDtoMap.put(mysteryBoxMetaDto.getSerialsNo(), mysteryBoxMetaDto);
                        redisTemplate.opsForValue().set(MYSTERY_BOX_META_KEY + mysteryBoxMetaDto.getSerialsNo(), mysteryBoxMetaDto, MYSTERY_BOX_META_EXPIRE_TIME, TimeUnit.SECONDS);
                    }
                }
            }
            return mysteryBoxMetaDtoMap;
        }catch (Exception e) {
            log.error("[getMysteryMetaBatch] serialsNoList:{} error:{}", serialsNoList, e);
            return mysteryBoxMetaDtoMap;
        }
    }

    private List<Long> getNoCacheNos(Map<Long, MysteryBoxMetaDto> mysteryBoxMetaDtoMap, List<Long> serialsNoList) {
        List<Long> noCacheNos = new ArrayList<>();
        for (Long serialsNo: serialsNoList) {
            if(Objects.isNull(mysteryBoxMetaDtoMap.get(serialsNo))) {
                noCacheNos.add(serialsNo);
            }
        }
        return noCacheNos;
    }

}
