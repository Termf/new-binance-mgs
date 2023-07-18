package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.commission.api.UserCommissionApi;
import com.binance.commission.vo.user.SubUserTradeNumberVo;
import com.binance.commission.vo.user.SubUserTradingVolumeVo;
import com.binance.commission.vo.user.request.LongIdRequest;
import com.binance.commission.vo.user.response.SubUserTradeNumberResponse;
import com.binance.commission.vo.user.response.SubUserTradingVolumeResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.account.helper.AccountDdosRedisHelper;
import com.binance.mgs.account.account.vo.subuser.SubUserTradeRecent30Arg;
import com.binance.mgs.account.account.vo.subuser.SubUserTradeRecent30Ret;
import com.binance.mgs.account.account.vo.subuser.SubUserTradeRecent30RetV2;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.service.CommissionClient;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@RequestMapping("/v1/private/account/subuser/commission")
public class SubUserCommissionController extends BaseAction {

    @Autowired
    private UserCommissionApi userCommissionApi;

    @Autowired
    private VerifyRelationService verifyRelationService;
    @Autowired
    private CommissionClient commissionClient;
    @Value("${sub.account.trade.date.expire.time:3600}")
    private int subUserTradeExpireTime;


    @PostMapping(value = "/trade/queryRecent30")
    public CommonRet<SubUserTradeRecent30Ret> queryRecent30(@RequestBody @Validated SubUserTradeRecent30Arg arg) throws Exception {
        UserVo userVo = verifyRelationService.checkBindingAndGetSubUser(arg.getSubUserEmail());
        if (userVo == null) {
            throw new BusinessException(GeneralCode.NOT_SUB_USER);
        }

        Long subUserId = userVo.getUserId();
        UserStatusEx userStatusEx = new UserStatusEx(userVo.getStatus());


        Object value = ShardingRedisCacheUtils.get(subUserId.toString(), String.class, CacheConstant.ACCOUNT_COMMISSION_TRADE_RECENT30_PREFIX);
        if (value == null) {
            LongIdRequest longIdRequest = new LongIdRequest();
            longIdRequest.setId(subUserId);
            APIRequest<LongIdRequest> apiRequest = APIRequest.instance(longIdRequest);
            APIResponse<SubUserTradeNumberResponse> apiResponse = userCommissionApi.subUserTradeNumber(apiRequest);
            checkResponse(apiResponse);

            value = JSON.toJSONString(apiResponse.getData());
            ShardingRedisCacheUtils.set(subUserId.toString(), value, CacheConstant.HOUR, CacheConstant.ACCOUNT_COMMISSION_TRADE_RECENT30_PREFIX);
        }

        SubUserTradeNumberResponse response = JSON.parseObject((String) value, SubUserTradeNumberResponse.class);

        SubUserTradeRecent30Ret subUserTradeRecent30Ret = new SubUserTradeRecent30Ret();
        subUserTradeRecent30Ret.setSubUserId(subUserId);
        subUserTradeRecent30Ret.setSubUserEmail(arg.getSubUserEmail());
        subUserTradeRecent30Ret.setIsSubUserEnabled(userStatusEx.getIsSubUserEnabled());
        List<SubUserTradeNumberVo> trades = response.getTrades();

        BigDecimal recent30Total = BigDecimal.ZERO;
        BigDecimal recent30FuturesTotal = BigDecimal.ZERO;
        BigDecimal recent1Total = BigDecimal.ZERO;
        BigDecimal recent1FuturesTotal = BigDecimal.ZERO;
        BigDecimal recent30BusdTotal = BigDecimal.ZERO;
        BigDecimal recent30BusdFuturesTotal = BigDecimal.ZERO;
        BigDecimal recent1BusdTotal = BigDecimal.ZERO;
        BigDecimal recent1BusdFuturesTotal = BigDecimal.ZERO;
        // 处理小数位数以及计算汇总值
        if (CollectionUtils.isNotEmpty(trades)) {
            for (int i = 0; i < trades.size(); i++) {
                SubUserTradeNumberVo subUserTradeNumberVo = trades.get(i);
                BigDecimal btc = subUserTradeNumberVo.getBtc() == null ? BigDecimal.ZERO : subUserTradeNumberVo.getBtc();
                BigDecimal btcFutures = subUserTradeNumberVo.getBtcFutures() == null ? BigDecimal.ZERO : subUserTradeNumberVo.getBtcFutures();
                BigDecimal busd = subUserTradeNumberVo.getBusd() == null ? BigDecimal.ZERO : subUserTradeNumberVo.getBusd();
                BigDecimal busdFutures = subUserTradeNumberVo.getBusdFutures() == null ? BigDecimal.ZERO : subUserTradeNumberVo.getBusdFutures();
                if (i == trades.size() - 1) {
                    recent1Total = btc;
                    recent1FuturesTotal = btcFutures;
                    recent1BusdTotal = busd;
                    recent1BusdFuturesTotal = busdFutures;
                }
                recent30Total = recent30Total.add(btc);
                recent30FuturesTotal = recent30FuturesTotal.add(btcFutures);
                recent30BusdTotal = recent30BusdTotal.add(busd);
                recent30BusdFuturesTotal = recent30BusdFuturesTotal.add(busdFutures);

                subUserTradeNumberVo.setBtc(btc.setScale(8,BigDecimal.ROUND_UP));
                subUserTradeNumberVo.setBtcFutures(btcFutures.setScale(8,BigDecimal.ROUND_UP));
                subUserTradeNumberVo.setBusd(busd.setScale(8,BigDecimal.ROUND_UP));
                subUserTradeNumberVo.setBusdFutures(busdFutures.setScale(8,BigDecimal.ROUND_UP));
            }
        }

        subUserTradeRecent30Ret.setRecent1Total(recent1Total.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent1FuturesTotal(recent1FuturesTotal.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent30Total(recent30Total.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent30FuturesTotal(recent30FuturesTotal.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent1BusdTotal(recent1BusdTotal.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent1BusdFuturesTotal(recent1BusdFuturesTotal.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent30BusdTotal(recent30BusdTotal.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setRecent30BusdFuturesTotal(recent30BusdFuturesTotal.setScale(8,BigDecimal.ROUND_UP));
        subUserTradeRecent30Ret.setTrades(trades);

        CommonRet<SubUserTradeRecent30Ret> commonRet = new CommonRet<>();
        commonRet.setData(subUserTradeRecent30Ret);
        return commonRet;
    }

    @PostMapping(value = "/trade/queryRecent30V2")
    public CommonRet<SubUserTradeRecent30RetV2> queryRecent30V2(@RequestBody @Validated SubUserTradeRecent30Arg arg) throws Exception {

        UserVo userVo = verifyRelationService.checkBindingAndGetSubUser(arg.getSubUserEmail());
        if (userVo == null) {
            throw new BusinessException(GeneralCode.NOT_SUB_USER);
        }
        Long subUserId = userVo.getUserId();
        UserStatusEx userStatusEx = new UserStatusEx(userVo.getStatus());

        Object value = ShardingRedisCacheUtils.get(subUserId.toString(), String.class, CacheConstant.ACCOUNT_COMMISSION_TRADEV2_RECENT30_PREFIX);
        if (value == null) {
            SubUserTradingVolumeResponse subUserTradingVolumeResponse = commissionClient.getSubUserTradeData(subUserId);
            value = JSON.toJSONString(subUserTradingVolumeResponse);
            ShardingRedisCacheUtils.set(subUserId.toString(), value, subUserTradeExpireTime, CacheConstant.ACCOUNT_COMMISSION_TRADEV2_RECENT30_PREFIX);
        }
        SubUserTradingVolumeResponse response = JSON.parseObject((String) value, SubUserTradingVolumeResponse.class);
        log.info("queryRecent30V2 subUserId:{} response:{}", subUserId, JsonUtils.toJsonHasNullKey(response));
        SubUserTradeRecent30RetV2 subUserTradeRecent30RetV2 = new SubUserTradeRecent30RetV2();
        subUserTradeRecent30RetV2.setSubUserId(subUserId);
        subUserTradeRecent30RetV2.setSubUserEmail(arg.getSubUserEmail());
        subUserTradeRecent30RetV2.setIsSubUserEnabled(userStatusEx.getIsSubUserEnabled());
        List<SubUserTradingVolumeVo> trades = response.getTradingVolumeList();

        BigDecimal recent30Total = BigDecimal.ZERO;
        BigDecimal recent30FuturesTotal = BigDecimal.ZERO;
        BigDecimal recent30MarginTotal = BigDecimal.ZERO;
        BigDecimal recent1Total = BigDecimal.ZERO;
        BigDecimal recent1FuturesTotal = BigDecimal.ZERO;
        BigDecimal recent1MarginTotal = BigDecimal.ZERO;
        BigDecimal recent30BusdTotal = BigDecimal.ZERO;
        BigDecimal recent30BusdFuturesTotal = BigDecimal.ZERO;
        BigDecimal recent30BusdMarginTotal = BigDecimal.ZERO;
        BigDecimal recent1BusdTotal = BigDecimal.ZERO;
        BigDecimal recent1BusdFuturesTotal = BigDecimal.ZERO;
        BigDecimal recent1BusdMarginTotal = BigDecimal.ZERO;
        // 处理小数位数以及计算汇总值
        if (CollectionUtils.isNotEmpty(trades)) {
            for (int i = 0; i < trades.size(); i++) {
                SubUserTradingVolumeVo subUserTradingVolumeVo = trades.get(i);
                BigDecimal btc = subUserTradingVolumeVo.getBtc()==null?BigDecimal.ZERO:subUserTradingVolumeVo.getBtc();
                BigDecimal btcFutures = subUserTradingVolumeVo.getBtcFutures()==null?BigDecimal.ZERO:subUserTradingVolumeVo.getBtcFutures();
                BigDecimal btcMargin = subUserTradingVolumeVo.getBtcMargin()==null?BigDecimal.ZERO:subUserTradingVolumeVo.getBtcMargin();
                BigDecimal busd = subUserTradingVolumeVo.getBusd()==null?BigDecimal.ZERO:subUserTradingVolumeVo.getBusd();
                BigDecimal busdFutures = subUserTradingVolumeVo.getBusdFutures()==null?BigDecimal.ZERO:subUserTradingVolumeVo.getBusdFutures();
                BigDecimal busdMargin = subUserTradingVolumeVo.getBusdMargin()==null?BigDecimal.ZERO:subUserTradingVolumeVo.getBusdMargin();
                if (i == trades.size() - 1) {
                    recent1Total = btc;
                    recent1FuturesTotal = btcFutures;
                    recent1MarginTotal = btcMargin;
                    recent1BusdTotal = busd;
                    recent1BusdFuturesTotal = busdFutures;
                    recent1BusdMarginTotal = busdMargin;
                }
                recent30Total = recent30Total.add(btc);
                recent30FuturesTotal = recent30FuturesTotal.add(btcFutures);
                recent30MarginTotal = recent30MarginTotal.add(btcMargin);

                recent30BusdTotal = recent30BusdTotal.add(busd);
                recent30BusdFuturesTotal = recent30BusdFuturesTotal.add(busdFutures);
                recent30BusdMarginTotal = recent30BusdMarginTotal.add(busdMargin);

                subUserTradingVolumeVo.setBtc(btc.setScale(8, RoundingMode.UP));
                subUserTradingVolumeVo.setBtcFutures(btcFutures.setScale(8, RoundingMode.UP));
                subUserTradingVolumeVo.setBtcMargin(btcMargin.setScale(8, RoundingMode.UP));
                subUserTradingVolumeVo.setBusd(busd.setScale(8, RoundingMode.UP));
                subUserTradingVolumeVo.setBusdFutures(busdFutures.setScale(8, RoundingMode.UP));
                subUserTradingVolumeVo.setBusdMargin(busdMargin.setScale(8, RoundingMode.UP));
            }
        }

        subUserTradeRecent30RetV2.setRecent1Total(recent1Total.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent1FuturesTotal(recent1FuturesTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent1MarginTotal(recent1MarginTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent30Total(recent30Total.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent30FuturesTotal(recent30FuturesTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent30MarginTotal(recent30MarginTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent1BusdTotal(recent1BusdTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent1BusdFuturesTotal(recent1BusdFuturesTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent1BusdMarginTotal(recent1BusdMarginTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent30BusdTotal(recent30BusdTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent30BusdFuturesTotal(recent30BusdFuturesTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setRecent30BusdMarginTotal(recent30BusdMarginTotal.setScale(8, RoundingMode.UP));
        subUserTradeRecent30RetV2.setTrades(trades);

        CommonRet<SubUserTradeRecent30RetV2> commonRet = new CommonRet<>();
        commonRet.setData(subUserTradeRecent30RetV2);
        return commonRet;
    }
}
