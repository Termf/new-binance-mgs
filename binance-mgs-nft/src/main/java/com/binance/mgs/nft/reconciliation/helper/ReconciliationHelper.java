package com.binance.mgs.nft.reconciliation.helper;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.nft.reconciliaction.constant.FeeTypeEnum;
import com.binance.nft.reconciliaction.constant.NftReconciliactionErrorCode;
import com.binance.nft.reconciliaction.dto.NftFeesDto;
import com.binance.nft.reconciliaction.richapi.NftFeeQueryService;
import com.binance.nft.reconcilication.api.routing.ITradingRouteQueryApi;
import com.binance.nft.reconcilication.enums.TradingRoutingChannelEnums;
import com.binance.nft.reconcilication.enums.TradingRoutingSceneEnums;
import com.binance.nft.reconcilication.req.TradingRoutingCommonQueryReq;
import com.binance.nft.reconcilication.vo.TradingRoutingVo;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationHelper {

    @Value("${nft.mint.origin.fee:0}")
    private BigDecimal ORIGINAL_FEE;
    @Value("${nft.mint.asset:BNB}")
    private String MINT_ASSET;
    @Value("${nft.mint.origin.burnFee:0}")
    private BigDecimal ORIGINAL_BURN_FEE;
    @Value("${nft.burn.asset:BNB}")
    private String BURN_ASSET;
    @Value("${nft.mgs.settle.fee.new:false}")
    private boolean newFeeConfig;

    @Getter
    @Value("${nft.reconciliaction.trading.routing.default:Spot}")
    private String tradingRoutingDefault;

    @Resource
    private NftFeeQueryService nftFeeQueryService;


    public ReconciliationDto getReconciliationDtoByFeeType(Integer feeType) {

        NftFeesDto feeDto = findMintFeeDto("", feeType);
        return ReconciliationDto.builder()
                .amount(feeDto.getAmount()).currency(feeDto.getCurrency())
                .build();

    }

    public ReconciliationDto getReconciliationDto(){

        if(!newFeeConfig){
            return ReconciliationDto.builder()
                    .amount(ORIGINAL_FEE).currency(MINT_ASSET)
                    .build();
        }else{
            NftFeesDto feeDto = findMintFeeDto("BSC",
                    FeeTypeEnum.MINTING_BSC.getCode());
            return ReconciliationDto.builder()
                    .amount(feeDto.getAmount()).currency(feeDto.getCurrency())
                    .build();
        }
    }

    public NftFeesDto findMintFeeDto(String network, Integer code){

        List<NftFeesDto> nftFeesDtos = nftFeeQueryService.queryNftFeesDtoListByCodes(Arrays.asList(code));
        if(CollectionUtils.isEmpty(nftFeesDtos)){
            throw new BusinessException(NftReconciliactionErrorCode.RECON_CURRENCY_NOT_EXIST);
        }
        return nftFeesDtos.get(0);
    }

    @Data
    @Builder
    public static class ReconciliationDto{
        private BigDecimal amount;
        private String currency;
    }
}
