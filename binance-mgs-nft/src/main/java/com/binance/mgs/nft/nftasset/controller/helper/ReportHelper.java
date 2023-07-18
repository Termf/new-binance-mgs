package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIResponse;
import com.binance.nft.assetservice.api.data.vo.report.ReportVo;
import com.binance.nft.assetservice.api.report.INftReportApi;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportHelper {

    private final INftReportApi reportApi;

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public ReportVo reportVo(Long nftInfoId, Long userId) {

        APIResponse<ReportVo> response = reportApi.checkReport(nftInfoId, userId);
        BaseHelper.checkResponse(response);
        return response.getData();
    }
}
