package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.market.ifae.IgoLandingPageApi;
import com.binance.nft.market.request.igo.IgoSearchRequest;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nft.market.vo.igo.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/public/nft/igo-landing-page")
@RequiredArgsConstructor
public class IgoLandingPageController {

    private final IgoLandingPageApi igoLandingPageApi;

    private final BaseHelper baseHelper;

    private final CrowdinHelper crowdinHelper;

    @GetMapping("/activity-list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'findDataOutlineAndActivities'")
    public CommonRet<ActivityDetailVo> findDataOutlineAndActivities() throws Exception {
        APIResponse<ActivityDetailVo> response = igoLandingPageApi.findDataOutlineAndActivities();
        baseHelper.checkResponse(response);
        List<IgoActivityVo> igoActivityVos = response.getData().getLaunchpadActivityVo();
        if (CollectionUtils.isNotEmpty(igoActivityVos)) {
            igoActivityVos.stream().filter(t-> StringUtils.isNotBlank(t.getDescription())).forEach(t ->
                    t.setDescription(crowdinHelper.getMessageByKey(t.getDescription(), baseHelper.getLanguage())));
        }
        return new CommonRet<ActivityDetailVo>(response.getData());
    }

    @PostMapping("/igo-offering-list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'findIgoOfferingDetail-'+#request")
    public CommonRet<CommonPageResponse<IgoOfferingVo>> findIgoOfferingDetail(@RequestBody IgoSearchRequest request) throws Exception {
        APIResponse<CommonPageResponse<IgoOfferingVo>> response = igoLandingPageApi.findIgoOfferingDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<CommonPageResponse<IgoOfferingVo>>(response.getData());
    }

    @PostMapping("/other-offering-list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'findOtherOfferingDetail-'+#request")
    public CommonRet<CommonPageResponse<IgoOtherOfferingVo>> findOtherOfferingDetail(@RequestBody IgoSearchRequest request) {
        APIResponse<CommonPageResponse<IgoOtherOfferingVo>> response = igoLandingPageApi.findOtherOfferingDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<CommonPageResponse<IgoOtherOfferingVo>>(response.getData());
    }

    @GetMapping("/news-project-list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'findNewProjectDetail'")
    public CommonRet<List<NewsActivityVo>> findNewProjectDetail() {
        APIResponse<List<NewsActivityVo>> response = igoLandingPageApi.findNewProjectDetail();
        baseHelper.checkResponse(response);
        return new CommonRet<List<NewsActivityVo>>(response.getData());
    }

}