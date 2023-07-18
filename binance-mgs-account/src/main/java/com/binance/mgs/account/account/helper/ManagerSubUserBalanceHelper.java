package com.binance.mgs.account.account.helper;


import com.alibaba.fastjson.JSON;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.assetservice.vo.response.asset.AssetResponse;
import com.binance.mgs.account.account.vo.AccountAssetRet;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.business.account.helper.CommonAccountHelper;
import com.binance.platform.mgs.enums.AccountType;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;
import org.javasimon.aop.Monitored;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class ManagerSubUserBalanceHelper extends BaseHelper {
    @Autowired
    @Qualifier("subUserBalanceExecutor")
    private ExecutorService executor;
    @Value("${manager.subUser.asset.balance.timeout.millisecond:1000}")
    private int totalBalanceTimeoutMillisecond;
    @Autowired
    private CommonAccountHelper commonAccountHelper;
    @Autowired
    private AccountAssetHelper accountAssetHelper;
    @Autowired
    private AccountAssetV2Helper accountAssetV2Helper;
    @Autowired
    private AccountBalanceCacheHelper accountBalanceCacheHelper;

    @Monitored
    public List<AccountAssetRet> getWalletBalanceRets(List<AccountType> accountTypeList, Long userId, boolean needBalanceDetail, String currentHost)
            throws Exception {
        log.info("get wallet balance from wss userId={},accountTypeList={},needBalanceDetail={}", userId, JSON.toJSONString(accountTypeList),
                needBalanceDetail);
        List<AccountAssetRet> data = new ArrayList<>();
        UserInfoVo userInfoVo = commonAccountHelper.getUserInfoVoById(userId);
        Map<String, AssetResponse> allAsset = getAssetInfoByDomain(currentHost, needBalanceDetail);
        List<Callable<AccountAssetRet>> assetTasks = Lists.newArrayList();
        String traceId = TrackingUtils.getTrace();
        for (AccountType accountType : accountTypeList) {
            assetTasks.add(() -> {
                RpcContext.getContext().set("HOST", currentHost);
                return getWalletBalanceRet(userInfoVo, allAsset, accountType, traceId, needBalanceDetail);
            });
        }
        if (CollectionUtils.isEmpty(assetTasks)) {
            return data;
        }
        String userIdStr = String.valueOf(userId);
        List<Future<AccountAssetRet>> taskResults = executor.invokeAll(assetTasks, totalBalanceTimeoutMillisecond, TimeUnit.MILLISECONDS);
        Long time = System.currentTimeMillis();
        final Iterator<AccountType> accountTypeIterator = accountTypeList.iterator();
        for (Future<AccountAssetRet> result : taskResults) {
            final AccountType accountType = accountTypeIterator.next();
            try {
                final AccountAssetRet accountAssetRet = result.get();
                accountAssetRet.setTime(time);
                data.add(accountAssetRet);
                // 保存到缓存备用
                accountBalanceCacheHelper.saveV1(userIdStr, accountAssetRet);
            } catch (Exception e) {
                log.warn("userId={},accountType={} get fail", userIdStr, accountType);
                // 2s拿不到则去缓存拿，缓存没有则给默认值0
                AccountAssetRet walletBalanceRet = accountBalanceCacheHelper.getV1(userIdStr, accountType, userInfoVo);
                data.add(walletBalanceRet);
            }
        }
        return data;
    }

    private Map<String, AssetResponse> getAssetInfoByDomain(String topDomain, boolean needBalanceDetail) throws Exception {
        Map<String, AssetResponse> map = null;
        if (needBalanceDetail) {
            map = new HashMap<>();
            List<AssetResponse> allAssetResponse = accountAssetHelper.getAllAssetResponseByTopDomain(topDomain);
            if (!CollectionUtils.isEmpty(allAssetResponse)) {
                // assetList 若为空，则返回所有
                for (AssetResponse assetResponse : allAssetResponse) {
                    map.put(assetResponse.getAssetCode(), assetResponse);
                }
            }
        }
        return map;
    }

    public AccountAssetRet getWalletBalanceRet(UserInfoVo userInfoVo, Map<String, AssetResponse> allAsset,
                                               AccountType accountType, String traceId, boolean needBalanceDetail) throws Exception {
        AccountAssetRet accountAssetRet = new AccountAssetRet();
        accountAssetRet.setAccountType(accountType);
        try {
            TrackingUtils.saveTrace(traceId);
            switch (accountType) {
                case MAIN:
                    accountAssetRet.setActivate(true);
                    accountAssetV2Helper.setWalletBalance(userInfoVo.getUserId(), accountAssetRet, null, needBalanceDetail);
                    break;
                case MARGIN:
                    accountAssetRet.setActivate(true);
                    if (userInfoVo.getMarginUserId() != null) {
                        accountAssetV2Helper.setMarginWalletBalance(userInfoVo.getUserId(), accountAssetRet, allAsset, needBalanceDetail);
                    }
                    break;
                case ISOLATED_MARGIN:
                    accountAssetRet.setActivate(true);
                    accountAssetV2Helper.setIsolatedMarginWalletBalance(userInfoVo.getUserId(), accountAssetRet, allAsset, needBalanceDetail);
                    break;
                case FUTURE:
                    if (userInfoVo.getFutureUserId() != null) {
                        accountAssetRet.setActivate(true);
                        accountAssetV2Helper.setFutureWalletBalance(userInfoVo.getFutureUserId(), accountAssetRet, allAsset, needBalanceDetail);
                    }
                    break;
                case DELIVERY:
                    if (userInfoVo.getFutureUserId() != null) {
                        accountAssetRet.setActivate(true);
                        accountAssetV2Helper.setDeliveryWalletBalance(userInfoVo.getFutureUserId(), accountAssetRet, allAsset, needBalanceDetail);
                    }
                    break;
            }
        } finally {
            TrackingUtils.clearTrace();
        }
        return accountAssetRet;
    }
}
