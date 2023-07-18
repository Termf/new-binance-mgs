package com.binance.mgs.nft.activity.service;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.nft.activityservice.api.NftActivityStackeApi;
import com.binance.nft.activityservice.dto.ApeProjectDTO;
import com.binance.nft.activityservice.request.AprRequest;
import com.binance.nft.bnbgtwservice.api.data.dto.ApeProjectDto;
import com.binance.nft.bnbgtwservice.api.data.dto.ApeProjectListRequest;
import com.binance.nft.bnbgtwservice.api.data.dto.CollectionApyDTO;
import com.binance.nft.bnbgtwservice.api.iface.StakeApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.google.api.client.util.Lists;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: felix
 * @date: 22.10.22
 * @description:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityService {

    @Resource
    private StakeApi stakeApi;

    @Resource
    private NftActivityStackeApi activityStackeApi;

    @Resource
    private BaseHelper baseHelper;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    private final LoadingCache<Long, ApeProjectDto> PROJECT_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(50)
                    .refreshAfterWrite(3, TimeUnit.MINUTES)
                    .build(CacheLoader.asyncReloading(new CacheLoader<Long, ApeProjectDto>() {
                        @Override
                        public ApeProjectDto load(Long projectId) throws Exception {
                            APIResponse<List<ApeProjectDto>> response = stakeApi.projectList(APIRequest.instance(new ApeProjectListRequest()));
                            baseHelper.checkResponse(response);
                            log.info("PROJECT_CACHE,{}", JSON.toJSON(response));
                            Map<Long, ApeProjectDto> map = response.getData().stream().collect(HashMap::new, (k, v) -> k.put(v.getId(), v), HashMap::putAll);
                            return map.get(projectId);
                        }
                    }, executorService));

    @SneakyThrows
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_CONFIG)
    public Map<String, CollectionApyDTO> getApyByCollection() {
        APIResponse<Map<String, CollectionApyDTO>> response = stakeApi.getApyByCollecion(APIRequest.instance(ApeProjectListRequest.builder().collection(null).timestamp(String.valueOf(System.currentTimeMillis())).build()));
        baseHelper.checkResponse(response);
//        response.getData().forEach((key, value) -> {
//            AprRequest request = new AprRequest();
//            request.setAmount(new BigDecimal(value.getDailyReward()));
//            request.setFeCollectionName(key);
//            try {
//                APIResponse<String> res = activityStackeApi.getNftApr(APIRequest.instance(request));
//                baseHelper.checkResponse(res);
//                String apr = StringUtils.isNotEmpty(res.getData()) ? res.getData() : value.getApr();
//                value.setApr(apr);
//            } catch (Exception e) {
//                log.error("calculate apr error");
//            }
//        });
        return response.getData();
    }

    @SneakyThrows
    public List<ApeProjectDTO> projectList(List<Long> projectIdList) {
        if (CollectionUtils.isEmpty(projectIdList)){
            return Collections.emptyList();
        }
        List<ApeProjectDTO> result = Lists.newArrayList();
        projectIdList.forEach(projectId->{
            ApeProjectDto projectDto;
            try {
                projectDto = PROJECT_CACHE.get(projectId);
//                AprRequest request = new AprRequest();
//                request.setFeCollectionName(projectDto.getCollection());
//                request.setAmount(projectDto.getDailyReward());
//                APIResponse<String> nftApr = activityStackeApi.getNftApr(APIRequest.instance(request));
//                baseHelper.checkResponse(nftApr);
//                projectDto.setApr(nftApr.getData());
            } catch (Exception e) {
                log.error("project is error,{}",projectId,e);
                projectDto = new ApeProjectDto();
            }
            result.add(CopyBeanUtils.fastCopy(projectDto,ApeProjectDTO.class));
        });
       return result ;
    }
}
