package com.binance.mgs.nft.nftasset.controller.helper;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.nftasset.vo.NftEventVoRet;
import com.binance.nft.assetservice.api.INftAssetApi;
import com.binance.nft.assetservice.api.INftAssetLogApi;
import com.binance.nft.assetservice.api.data.vo.NftEventVo;
import com.binance.nft.assetservice.api.data.vo.UserSimpleInfoDto;
import com.binance.nft.assetservice.enums.NftEventTypeEnum;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class NftLoggerHelper {

    private final INftAssetLogApi nftAssetLogApi;

    private final INftAssetApi nftAssetApi;

    private final BaseHelper baseHelper;
    @Value("${nft.version.switch.provenance:0}")
    private boolean NFT_EVENT_VERSION_SWITCH;

    @Value("${nft.asset.log.pagesize:6}")
    private int defaultPageSize;
    @Value("${nft.mgs.event.expire.time:1800}")
    private int defaultExpiredTimeSecond;

    private final PojoConvertor pojoConvertor;

    private final RedisTemplate redisTemplate;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_BIG)
    public Page<NftEventVoRet> fetchNftSimpleEventPage(int page, int pageSize, boolean salesOnlyFlag, Long nftInfoId){

        final Object o = redisTemplate.opsForValue().get(buildKey(nftInfoId, salesOnlyFlag, page, defaultPageSize));
        Page<NftEventVo> resultPage = new Page<>();
        if (Objects.isNull(o)){

            APIResponse<Page<NftEventVo>> apiResponse = NFT_EVENT_VERSION_SWITCH ? nftAssetLogApi
                    .fetchNftSimpleEventPage(page, pageSize, nftInfoId) : nftAssetLogApi.fetchNftSimpleEventPageV2(page, pageSize, salesOnlyFlag, nftInfoId);

            baseHelper.checkResponse(apiResponse);
            resultPage = apiResponse.getData();
            if(CollectionUtils.isNotEmpty(resultPage.getRecords())) {
                replaceWhiteUserNickName(resultPage,nftInfoId);
            }
            APIResponse<UserSimpleInfoDto> response = fetchNickName(nftInfoId);
            baseHelper.checkResponse(response);
            redisTemplate.opsForValue().set(buildOwnerKey(nftInfoId), JsonUtils.toJsonHasNullKey(response.getData()),defaultExpiredTimeSecond, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(buildKey(nftInfoId, salesOnlyFlag, page, defaultPageSize), resultPage, defaultExpiredTimeSecond, TimeUnit.SECONDS);
            return pojoConvertor.copyNftEventVoRetPage(resultPage);
        }else {
            return pojoConvertor.copyNftEventVoRetPage((Page<NftEventVo>) o);
        }

    }

    private APIResponse<UserSimpleInfoDto> fetchNickName(Long nftInfoId) {
        return nftAssetLogApi.fetchNickName(nftInfoId);
    }


    private void replaceWhiteUserNickName(Page<NftEventVo> resultPage, Long nftInfoId){

        NftEventVo nftEventVo = resultPage.getRecords().stream().min(Comparator.comparing(NftEventVo::getId)).get();
        APIResponse<NftEventVo> response = nftAssetLogApi.fetchNftNextEvent(nftEventVo.getId(), nftInfoId);
        baseHelper.checkResponse(response);
        NftEventVo lastNftEvent = response.getData();
        AtomicReference<String> nickName = new AtomicReference<>();
        if( lastNftEvent != null && StringUtils.isNotBlank(lastNftEvent.getNickName())) {
            nickName.set(lastNftEvent.getNickName());
        }

        for (int i = resultPage.getRecords().size() - 1; i >= 0 ; i --) {
            NftEventVo item = resultPage.getRecords().get(i);
            if(item.getEventType().compareTo(NftEventTypeEnum.USER_TRADE.getCode()) == 0) {
                nickName.set(null);
            }
            if(StringUtils.isNotBlank(item.getNickName())) {
                nickName.set(item.getNickName());
            }
            if(StringUtils.isNotBlank(nickName.get())) {
                item.setMessage(buildMessage(item.getEventType(), nickName.get()));
                item.setUserNickName(nickName.get());
            }
        }
    }

    private String buildMessage(Byte eventType, String userNickName) {
       return NftEventTypeEnum.getEnumByCode(eventType).getMessage(new String[]{userNickName});
    }

    private Object buildKey(Long nftInfoId, boolean salesOnlyFlag, int page, int pageSize) {
        return String.format("nft:asset:log:%s:%s:%s:%s", nftInfoId, salesOnlyFlag, page, pageSize);
    }

    public UserSimpleInfoDto fetchUserOwner(Long nftInfoId) {
        Object o = redisTemplate.opsForValue().get(buildOwnerKey(nftInfoId));
        log.debug("NFT fetchUserOwner nftInfoId:{},ownerInfo:{}", nftInfoId, JSON.toJSONString(o));
        if(o != null && StringUtils.isNotBlank(o.toString())) {
            return JsonUtils.parse(o.toString(),UserSimpleInfoDto.class);
        }
        APIResponse<UserSimpleInfoDto> response = fetchNickName(nftInfoId);
        baseHelper.checkResponse(response);
        return response.getData();
    }

    private Object buildOwnerKey(Long nftInfoId) {
        return String.format("nft:asset:ownerId:%s", nftInfoId);
    }
}
