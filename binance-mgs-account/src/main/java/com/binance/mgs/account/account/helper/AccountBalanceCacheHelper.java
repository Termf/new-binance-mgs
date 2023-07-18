package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSON;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.mgs.account.account.vo.AccountAssetRet;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.business.asset.vo.WalletBalanceRet;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.enums.AccountType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * copy from {@link com.binance.mgs.asset.assetservice.controller.WalletTransferCacheHelper}
 *
 * @author kvicii
 * @date 2021/06/09
 */
@Component
public class AccountBalanceCacheHelper {

    @Value("${subUser.asset.balance.expire.hour:1}")
    private Long walletBalanceExpireHour;

    @Async("simpleRequestAsync")
    public void saveV1(String userId, AccountAssetRet accountAssetRet) {
        String cacheKey = CacheKey.getWalletBalanceV1(userId, accountAssetRet.getAccountType().name());
        save(cacheKey, accountAssetRet);
    }

    public AccountAssetRet getV1(String userId, AccountType accountType, UserInfoVo userInfoVo) {
        String cacheKey = CacheKey.getWalletBalanceV1(userId, accountType.name());
        return getOrDefault(cacheKey, accountType, userInfoVo);
    }

    /**
     * redis不保存币种详细情况，避免读写过慢影响性能
     *
     * @param cacheKey
     * @param accountAssetRet
     */
    private void save(String cacheKey, AccountAssetRet accountAssetRet) {
        WalletBalanceRet walletBalanceWithoutDetail = new WalletBalanceRet();
        walletBalanceWithoutDetail.setActivate(accountAssetRet.isActivate());
        walletBalanceWithoutDetail.setBalance(accountAssetRet.getBalance().setScale(8, BigDecimal.ROUND_DOWN));
        walletBalanceWithoutDetail.setTime(accountAssetRet.getTime());
        // 缓存1小时,转换成json能缩小存储大小
        ShardingRedisCacheUtils.set(cacheKey, JSON.toJSONString(walletBalanceWithoutDetail), walletBalanceExpireHour * CacheConstant.HOUR);
    }

    /**
     * redis里若有，则以redis为准，若无，则直接返回默认值
     *
     * @param cacheKey
     * @param accountType
     * @return
     */
    private AccountAssetRet getOrDefault(String cacheKey, AccountType accountType, UserInfoVo userInfoVo) {

        Object value = ShardingRedisCacheUtils.get(cacheKey);
        if (value != null) {
            AccountAssetRet walletBalanceRet = JSON.parseObject((String) value, AccountAssetRet.class);
            if (walletBalanceRet != null) {
                walletBalanceRet.setAccountType(accountType);
            }
            return walletBalanceRet;
        }
        AccountAssetRet accountAssetRet = new AccountAssetRet();
        accountAssetRet.setBalance(BigDecimal.ZERO);
        accountAssetRet.setActivate(getActivateStatus(userInfoVo, accountType));
        accountAssetRet.setAccountType(accountType);
        accountAssetRet.setTime(-1L);
        return accountAssetRet;
    }

    private boolean getActivateStatus(UserInfoVo userInfoVo, AccountType accountType) {
        boolean activate = false;
        switch (accountType) {
            case MAIN:
            case MARGIN:
            case ISOLATED_MARGIN:
                activate = true;
                break;
            case FUTURE:
            case DELIVERY:
                if (userInfoVo.getFutureUserId() != null) {
                    activate = true;
                }
                break;
        }
        return activate;
    }
}
