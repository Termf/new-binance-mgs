package com.binance.mgs.nft.reconciliation.config;

import com.binance.nft.reconciliaction.dto.NftFeesDto;
import com.binance.nft.reconciliaction.richapi.NftFeeQueryService;
import com.binance.nft.reconciliaction.richapi.impl.NftFeeQueryServiceImpl;
import com.binance.nft.reconciliaction.richapi.impl.cache.CacheService;
import com.binance.nft.reconciliaction.richapi.impl.cache.NftFeesCacheServiceImpl;
import com.binance.nft.reconcilication.api.INftFeeConfigQueryApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class ReconciliationConfiguration {

    @Resource
    private INftFeeConfigQueryApi nftFeeConfigQueryApi;

    @Bean
    public CacheService<NftFeesDto> cacheService() {
        return new NftFeesCacheServiceImpl();
    }

    @Bean
    public NftFeeQueryService nftFeeQueryService() {
        return new NftFeeQueryServiceImpl(nftFeeConfigQueryApi, cacheService());
    }
}
