package com.binance.mgs.account.account.helper;

import com.binance.assetservice.api.IProductApi;
import com.binance.assetservice.api.IUserAssetApi;
import com.binance.assetservice.vo.request.UserAssetTransferBtcRequest;
import com.binance.assetservice.vo.request.product.PriceConvertRequest;
import com.binance.assetservice.vo.response.UserAssetTransferBtcResponse;
import com.binance.assetservice.vo.response.asset.AssetResponse;
import com.binance.assetservice.vo.response.product.PriceConvertResponse;
import com.binance.delivery.periphery.api.DeliveryBalanceApi;
import com.binance.future.api.BalanceApi;
import com.binance.future.api.request.GetBalanceRequest;
import com.binance.future.api.vo.BalanceVO;
import com.binance.margin.api.bookkeeper.MarginAccountBridgeApi;
import com.binance.margin.api.bookkeeper.response.UserAssetSummaryResponse;
import com.binance.margin.isolated.api.user.UserBridgeApi;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.account.vo.AccountAssetRet;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.javasimon.aop.Monitored;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * copy from {@link com.binance.mgs.business.asset.helper.UserAssetV2Helper}
 *
 * @author kvicii
 * @date 2021/06/08
 */
@Slf4j
@Component
public class AccountAssetV2Helper extends BaseHelper {
    private static final String QUATA_ASSET = "BTC";
    @Value("${wallet.balance.log.enable:false}")
    private boolean balanceLogEnable;
    @Autowired
    private MarginAccountBridgeApi marginAccountBridgeApi;
    @Autowired
    private UserBridgeApi userBridgeApi;
    @Autowired
    private BalanceApi balanceApi;
    @Autowired
    private DeliveryBalanceApi deliveryBalanceApi;
    @Autowired
    private IUserAssetApi userAssetApi;
    @Autowired
    private IProductApi productApi;

    public BigDecimal getUserAsset(Long userId) throws Exception {
        UserAssetTransferBtcRequest request = new UserAssetTransferBtcRequest();
        request.setUserId(String.valueOf(userId));
        APIResponse<UserAssetTransferBtcResponse> apiResponse = userAssetApi.userAssetTransferBtc(getInstance(request));
        checkResponse(apiResponse);
        UserAssetTransferBtcResponse apiData = apiResponse.getData();
        if (apiData != null) {
            return apiData.getTotalTransferBtc();
        }
        return BigDecimal.ZERO;
    }

    /***
     * 现货资产结果封装
     *
     * @param userId 用户id
     * @param accountAssetRet   返回结果
     * @param assetMap          资产明细
     * @param needBalanceDetail 是否展示
     * @throws Exception
     */
    @Monitored
    public void setWalletBalance(Long userId, AccountAssetRet accountAssetRet, Map<String, AssetResponse> assetMap, boolean needBalanceDetail) throws Exception {
        UserAssetTransferBtcRequest request = new UserAssetTransferBtcRequest();
        request.setUserId(String.valueOf(userId));
        APIResponse<UserAssetTransferBtcResponse> apiResponse = userAssetApi.userAssetTransferBtc(getInstance(request));
        checkResponse(apiResponse);
        UserAssetTransferBtcResponse apiData = apiResponse.getData();
        if (apiData != null) {
            accountAssetRet.setBalance(apiData.getTotalTransferBtc());
            List<AccountAssetRet.AssetBalance> assetBalances = new ArrayList<>();
            accountAssetRet.setAssetBalances(assetBalances);
            if (needBalanceDetail) {
                apiData.getAssetTransferBtcList().forEach(e -> {
                    if (e.getFree().add(e.getFreeze()).add(e.getLocked()).add(e.getWithdrawing()).compareTo(BigDecimal.ZERO) > 0) {
                        if (assetMap == null || (assetMap.get(e.getAsset()) != null && assetMap.get(e.getAsset()).getTest() != 1)) {
                            // assetMap为null则不过滤，如果不为null但不存在则需要过滤
                            AccountAssetRet.AssetBalance assetBalance = CopyBeanUtils.copy(e, AccountAssetRet.AssetBalance.class);
                            assetBalances.add(assetBalance);
                        }
                    }
                });
            }
        }
    }

    /***
     * 合约钱包详情
     *
     * @param futureUserId future端用户id
     * @param accountAssetRet   返回结果
     * @param allAsset          资产明细
     * @param needBalanceDetail 是否展示
     * @throws Exception
     */
    @Monitored
    public void setFutureWalletBalance(Long futureUserId, AccountAssetRet accountAssetRet, Map<String, AssetResponse> allAsset, boolean needBalanceDetail) {
        BigDecimal totalBtc = BigDecimal.ZERO;
        List<AccountAssetRet.AssetBalance> assetBalances = new ArrayList<>();
        GetBalanceRequest getBalanceRequest = new GetBalanceRequest();
        getBalanceRequest.setFutureUid(futureUserId);
        APIResponse<List<BalanceVO>> response = balanceApi.getBalance(APIRequest.instance(getBalanceRequest));
        if (!CollectionUtils.isEmpty(response.getData())) {
            for (BalanceVO balanceVO : response.getData()) {
                if (balanceVO.getMarginBalance() != null && balanceVO.getMarginBalance().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal price = BigDecimal.ONE;
                    if (!StringUtils.equals(balanceVO.getAsset(), QUATA_ASSET)) {
                        price = priceConvert(balanceVO.getAsset(), QUATA_ASSET);
                    }
                    // btc 估值
                    BigDecimal btcValue = price.multiply(balanceVO.getMarginBalance());
                    totalBtc = totalBtc.add(btcValue);
                    if (needBalanceDetail) {
                        AccountAssetRet.AssetBalance assetBalance = new AccountAssetRet.AssetBalance();
                        assetBalance.setAsset(balanceVO.getAsset());
                        assetBalance.setFree(balanceVO.getMarginBalance());
                        assetBalance.setTransferBtc(btcValue);
                        AssetResponse assetResponse = allAsset.get(balanceVO.getAsset());
                        if (assetResponse != null) {
                            assetBalance.setAssetName(assetResponse.getAssetName());
                            assetBalance.setLogoUrl(assetResponse.getLogoUrl());
                        }
                        assetBalances.add(assetBalance);
                    }
                }
            }
        }
        accountAssetRet.setAssetBalances(assetBalances);
        // 总BTC估值金额
        accountAssetRet.setBalance(totalBtc);
    }

    /***
     * 交割合约钱包详情
     *
     * @param futureUserId future端用户id
     * @param accountAssetRet   返回结果
     * @param allAsset          资产明细
     * @param needBalanceDetail 是否展示
     * @throws Exception
     */
    @Monitored
    public void setDeliveryWalletBalance(Long futureUserId, AccountAssetRet accountAssetRet, Map<String, AssetResponse> allAsset, boolean needBalanceDetail) throws Exception {
        BigDecimal totalBtc = BigDecimal.ZERO;
        List<AccountAssetRet.AssetBalance> assetBalances = new ArrayList<>();
        com.binance.delivery.periphery.api.request.core.GetBalanceRequest getBalanceRequest = new com.binance.delivery.periphery.api.request.core.GetBalanceRequest();
        getBalanceRequest.setFutureUid(futureUserId);
        APIResponse<List<com.binance.delivery.periphery.api.vo.core.BalanceVO>> response = deliveryBalanceApi.getBalance(APIRequest.instance(getBalanceRequest));
        if (!CollectionUtils.isEmpty(response.getData())) {
            for (com.binance.delivery.periphery.api.vo.core.BalanceVO balanceVO : response.getData()) {
                if (balanceVO.getMarginBalance() != null && balanceVO.getMarginBalance().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal price = BigDecimal.ONE;
                    if (!StringUtils.equals(balanceVO.getAsset(), QUATA_ASSET)) {
                        price = priceConvert(balanceVO.getAsset(), QUATA_ASSET);
                    }
                    // btc 估值
                    BigDecimal btcValue = price.multiply(balanceVO.getMarginBalance());
                    totalBtc = totalBtc.add(btcValue);
                    if (needBalanceDetail) {
                        AccountAssetRet.AssetBalance assetBalance = new AccountAssetRet.AssetBalance();
                        assetBalance.setAsset(balanceVO.getAsset());
                        assetBalance.setFree(balanceVO.getMarginBalance());
                        assetBalance.setTransferBtc(btcValue);
                        AssetResponse assetResponse = allAsset.get(balanceVO.getAsset());
                        if (assetResponse != null) {
                            assetBalance.setAssetName(assetResponse.getAssetName());
                            assetBalance.setLogoUrl(assetResponse.getLogoUrl());
                        }
                        assetBalances.add(assetBalance);
                    }
                }
            }
        }
        accountAssetRet.setAssetBalances(assetBalances);
        // 总BTC估值金额
        accountAssetRet.setBalance(totalBtc);
    }

    /**
     * margin账户资产结果封装
     *
     * @param userId            用户id
     * @param accountAssetRet   返回结果
     * @param allAsset          资产明细
     * @param needBalanceDetail 是否展示
     */
    @Monitored
    public void setMarginWalletBalance(Long userId, AccountAssetRet accountAssetRet, Map<String, AssetResponse> allAsset, boolean needBalanceDetail) {
        final APIResponse<UserAssetSummaryResponse> apiResponse = marginAccountBridgeApi.userAssetSummary(userId);
        UserAssetSummaryResponse apiData = apiResponse.getData();
        if (apiData != null) {
            accountAssetRet.setBalance(apiData.getTotalNetAssetOfBtc());
            if (balanceLogEnable) {
                log.info("userId={} setMarginWalletBalance balance={}", userId, apiData.getTotalNetAssetOfBtc());
            }
            List<AccountAssetRet.AssetBalance> assetBalances = new ArrayList<>();
            accountAssetRet.setAssetBalances(assetBalances);
            if (needBalanceDetail) {
                apiData.getUserAssets().forEach(e -> {
                    if (e.getNetAsset().compareTo(BigDecimal.ZERO) > 0) {
                        AccountAssetRet.AssetBalance assetBalance = CopyBeanUtils.copy(e, AccountAssetRet.AssetBalance.class);
                        assetBalance.setFree(e.getNetAsset());
                        assetBalance.setTransferBtc(e.getToBtcRate().multiply(e.getNetAsset()).setScale(8, BigDecimal.ROUND_DOWN));
                        AssetResponse assetResponse = allAsset.get(e.getAsset());
                        if (assetResponse != null) {
                            assetBalance.setLogoUrl(assetResponse.getLogoUrl());
                            assetBalance.setAssetName(assetResponse.getAssetName());
                        }
                        assetBalances.add(assetBalance);
                    }
                });
            }
        }
    }

    /**
     * isolated margin账户资产结果封装
     *
     * @param userId            用户id
     * @param accountAssetRet   返回结果
     * @param allAsset          资产明细
     * @param needBalanceDetail 是否展示
     */
    @Monitored
    public void setIsolatedMarginWalletBalance(Long userId, AccountAssetRet accountAssetRet, Map<String, AssetResponse> allAsset, boolean needBalanceDetail) {
        APIResponse<com.binance.margin.isolated.api.user.response.UserAssetSummaryResponse> apiResponse = userBridgeApi.userAssetSummary(userId);
        com.binance.margin.isolated.api.user.response.UserAssetSummaryResponse apiData = apiResponse.getData();
        if (apiData != null) {
            accountAssetRet.setBalance(apiData.getTotalNetAssetOfBtc());
            if (balanceLogEnable) {
                log.info("userId={} setIsolatedMarginWalletBalance balance={}", userId, apiData.getTotalNetAssetOfBtc());
            }
            List<AccountAssetRet.AssetBalance> assetBalances = new ArrayList<>();
            accountAssetRet.setAssetBalances(assetBalances);
            if (needBalanceDetail) {
                apiData.getUserAssets().forEach(e -> {
                    if (e.getNetAsset().compareTo(BigDecimal.ZERO) > 0) {
                        AccountAssetRet.AssetBalance assetBalance = CopyBeanUtils.fastCopy(e, AccountAssetRet.AssetBalance.class);
                        assetBalance.setFree(e.getNetAsset());
                        assetBalance.setTransferBtc(e.getToBtcRate().multiply(e.getNetAsset()).setScale(8, BigDecimal.ROUND_DOWN));

                        AssetResponse assetResponse = allAsset.get(e.getAsset());
                        if (assetResponse != null) {
                            assetBalance.setLogoUrl(assetResponse.getLogoUrl());
                            assetBalance.setAssetName(assetResponse.getAssetName());
                        }
                        assetBalances.add(assetBalance);
                    }
                });
            }
        }
    }

    /**
     * 计算btc估值
     *
     * @param asset
     * @param totalAssetAmount
     * @return
     */
    public BigDecimal getBtcValue(String asset, BigDecimal totalAssetAmount) {
        BigDecimal btcValue = BigDecimal.ZERO;
        if (totalAssetAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal price = BigDecimal.ONE;
            if (!StringUtils.equals(asset, QUATA_ASSET)) {
                price = priceConvert(asset, QUATA_ASSET);
            }
            btcValue = price.multiply(totalAssetAmount);
        }
        return btcValue;
    }

    /**
     * @return 币种转换
     * @throws Exception
     */
    public BigDecimal priceConvert(String from, String to) {
        try {
            PriceConvertRequest request = new PriceConvertRequest();
            request.setFrom(from);
            request.setTo(to);
            request.setAmount(BigDecimal.ONE);
            APIResponse<PriceConvertResponse> apiResponse = productApi.priceConvert(getInstance(request));
            checkResponse(apiResponse);
            if (apiResponse.getData() != null) {
                return apiResponse.getData().getPrice();
            }
        } catch (Exception e) {
            // 正常不应该走到该逻辑，仅仅为了防御
            log.error("转换失败失败,from={},to={}", from, to);
        }
        return BigDecimal.ZERO;
    }
}