package com.binance.mgs.account.account.controller;

import com.binance.account.api.TradeLevelApi;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.commission.api.CommissionTradeLevelManagerApi;
import com.binance.commission.api.UserCommissionApi;
import com.binance.commission.vo.commissiontradelevelmanager.response.SpotTradeLevelBusdResponse;
import com.binance.commission.vo.user.UserGasVo;
import com.binance.commission.vo.user.request.LongIdRequest;
import com.binance.commission.vo.user.response.TradeNumberResponse;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.account.vo.CommissionTradeInfoV2Ret;
import com.binance.mgs.account.account.vo.TradeLevelV2Ret;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.ListTransformUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/v2")
public class UserTradeLevelV2Controller extends BaseAction {
    @Autowired
    private CommissionTradeLevelManagerApi commissionTradeLevelManagerApi;
    @Autowired
    private UserCommissionApi userCommissionApi;
    @Value("${trade.task1Time:0}")
    private String task1Time;
    @Value("${trade.task2Time:1}")
    private String task2Time;
    private final static BigDecimal BD_100 = new BigDecimal("100");

    /**
     * 交易等级配置信息
     *
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/public/account/trade-level/get")
    public CommonRet<TradeLevelV2Ret> getTradeLevels() throws Exception {
        CommonRet<TradeLevelV2Ret> ret = new CommonRet<>();
        APIResponse<List<SpotTradeLevelBusdResponse>> accountResponse = commissionTradeLevelManagerApi.commissionSpotManageList();
        checkResponse(accountResponse);
        LongIdRequest longIdRequest = new LongIdRequest();
        longIdRequest.setId(getUserId());
        APIResponse<TradeNumberResponse> apiResponse = userCommissionApi.tradeNumber(getInstance(longIdRequest));
        checkResponse(apiResponse);
        BigDecimal gasRate = new BigDecimal(apiResponse.getData().getUser().getGasRate());
        List<TradeLevelV2Ret.TradeLevelStringVo> levels = new ArrayList<>();
        for(SpotTradeLevelBusdResponse vo : accountResponse.getData()){
            TradeLevelV2Ret.TradeLevelStringVo stringVo = new TradeLevelV2Ret.TradeLevelStringVo();
            stringVo.setLevel(vo.getLevel());
            stringVo.setBnbFloor(vo.getBnbFloor().toPlainString());
            stringVo.setBnbCeil(vo.getBnbCeil().toPlainString());
            stringVo.setBtcBusdCeil(vo.getBtcBusdCeil().toPlainString());
            stringVo.setBtcBusdFloor(vo.getBtcBusdFloor().toPlainString());
            stringVo.setMakerCommission(vo.getMakerCommission().multiply(BD_100).toPlainString());
            stringVo.setTakerCommission(vo.getTakerCommission().multiply(BD_100).toPlainString());
            stringVo.setOldMakerCommission(vo.getOldMakerCommission().multiply(BD_100).toPlainString());
            stringVo.setOldTakerCommission(vo.getOldTakerCommission().multiply(BD_100).toPlainString());
            stringVo.setBuyerCommission(vo.getBuyerCommission().multiply(BD_100).toPlainString());
            stringVo.setSellerCommission(vo.getSellerCommission().multiply(BD_100).toPlainString());
            stringVo.setBnbMakerCommission((BigDecimal.ONE.subtract(gasRate)).multiply(vo.getMakerCommission()).multiply(BD_100).toPlainString());
            stringVo.setBnbTakerCommission((BigDecimal.ONE.subtract(gasRate)).multiply(vo.getTakerCommission()).multiply(BD_100).toPlainString());
            levels.add(stringVo);
        }
        TradeLevelV2Ret v2 = new TradeLevelV2Ret();
        v2.setLevels(levels);
        TradeLevelV2Ret.TradeLevelPropertiesResponse properties = new TradeLevelV2Ret.TradeLevelPropertiesResponse();
        properties.setTask1Time(task1Time);
        properties.setTask2Time(task2Time);
        properties.setGasRate(apiResponse.getData().getUser().getGasRate());
        v2.setProperties(properties);
        ret.setData(v2);
        return ret;
    }

    /**
     * 获取用户近30天交易量、当前交易等级及费率、晋级下一级所需交易额或者持仓
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/trade-info/get")
    public CommonRet<CommissionTradeInfoV2Ret> getTradeNumber() throws Exception {
        Long userId = getUserId();
        CommonRet<CommissionTradeInfoV2Ret> ret = new CommonRet<>();
        Object value = ShardingRedisCacheUtils.get(userId.toString(), CommissionTradeInfoV2Ret.class, CacheConstant.ACCOUNT_COMMISSION_TRADEV2_INFO_PREFIX);
        if (value != null) {
            CommissionTradeInfoV2Ret tradeInfoV2Ret = JsonUtils.toObj(JsonUtils.toJsonHasNullKey(value), CommissionTradeInfoV2Ret.class);
            ret.setData(tradeInfoV2Ret);
            return ret;
        }
        LongIdRequest request = new LongIdRequest();
        request.setId(userId);
        APIResponse<TradeNumberResponse> apiResponse = userCommissionApi.tradeNumber(getInstance(request));
        checkResponse(apiResponse);

        if (apiResponse.getData() != null) {
            TradeNumberResponse response = apiResponse.getData();
            CommissionTradeInfoV2Ret data = new CommissionTradeInfoV2Ret();
            ret.setData(data);
            if (!CollectionUtils.isEmpty(response.getTrades())) {
                // 用户交易量信息
                List<CommissionTradeInfoV2Ret.CommissionTrade> tradeList =
                        ListTransformUtil.transform(response.getTrades(), CommissionTradeInfoV2Ret.CommissionTrade.class);
                data.setTrades(tradeList);
            }
            if (response.getNext() != null) {
                // 用户距离下一等级信息
                CommissionTradeInfoV2Ret.CommissionNext next = new CommissionTradeInfoV2Ret.CommissionNext();
                BeanUtils.copyProperties(response.getNext(), next);
                data.setNext(next);
            }
            if (response.getUser() != null) {
                // 用户交易等级费率信息
                UserGasVo user = response.getUser();
                BigDecimal gas = new BigDecimal(user.getGasRate());
                BigDecimal takerCommission = new BigDecimal(user.getTakerCommission());
                BigDecimal makerCommission = new BigDecimal(user.getMakerCommission());
                CommissionTradeInfoV2Ret.UserCommissionGas userGas = new CommissionTradeInfoV2Ret.UserCommissionGas();
                userGas.setLevel(user.getLevel());
                userGas.setGasRate(user.getGasRate());
                userGas.setTakerCommission(takerCommission.multiply(BD_100).toPlainString());
                userGas.setMakerCommission(makerCommission.multiply(BD_100).toPlainString());
                userGas.setBnbTakerCommission((BigDecimal.ONE.subtract(gas)).multiply(takerCommission).multiply(BD_100).toPlainString());
                userGas.setBnbMakerCommission((BigDecimal.ONE.subtract(gas)).multiply(makerCommission).multiply(BD_100).toPlainString());
                userGas.setBnb(user.getBnb());
                userGas.setBtc30(user.getBtc30());
                userGas.setBusd30(user.getBusd30());
                data.setUser(userGas);
            }
        }
        ShardingRedisCacheUtils.set(userId.toString(), ret.getData(), CacheConstant.HOUR, CacheConstant.ACCOUNT_COMMISSION_TRADEV2_INFO_PREFIX);
        return ret;
    }

}
