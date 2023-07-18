package com.binance.mgs.account.account.controller;

import com.binance.account.api.TradeLevelApi;
import com.binance.account.vo.user.TradeLevelVo;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.commission.api.CommissionTradeLevelManagerApi;
import com.binance.commission.api.UserCommissionApi;
import com.binance.commission.vo.commissiontradelevelmanager.response.FutureTradeLevelBusdResponse;
import com.binance.commission.vo.commissiontradelevelmanager.response.SpotTradeLevelBusdResponse;
import com.binance.commission.vo.user.NextVo;
import com.binance.commission.vo.user.TradeVo;
import com.binance.commission.vo.user.UserGasVo;
import com.binance.commission.vo.user.request.LongIdRequest;
import com.binance.commission.vo.user.response.TradeNumberResponse;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.account.vo.CommissionTradeInfoRet;
import com.binance.mgs.account.account.vo.TradeLevelRet;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.ListTransformUtil;
import com.binance.report.api.IUserCommissionApi;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1")
public class UserTradeLevelController extends BaseAction {

    @Value("${commission.busd.trade.level.switch:true}")
    private Boolean commissionBusdTradeLevelSwtich;
    @Resource
    private TradeLevelApi tradeLevelApi;
    @Autowired
    private UserCommissionApi accountUserCommissionApi;
    @Autowired
    private CommissionTradeLevelManagerApi commissionTradeLevelManagerApi;



    /**
     * 交易等级配置信息
     *
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/public/account/trade-level/get")
    public CommonRet<List<TradeLevelRet>> getTradeLevels() throws Exception {
        CommonRet<List<TradeLevelRet>> ret = new CommonRet<>();
        if (commissionBusdTradeLevelSwtich){
            APIResponse<List<SpotTradeLevelBusdResponse>> apiResponse = commissionTradeLevelManagerApi.commissionSpotManageList();
            checkResponse(apiResponse);
            List<TradeLevelRet> data = ListTransformUtil.transform(apiResponse.getData(), TradeLevelRet.class);
            ret.setData(data);
        }else{
            APIResponse<List<TradeLevelVo>> apiResponse = tradeLevelApi.manageList();
            checkResponse(apiResponse);
            List<TradeLevelRet> data = ListTransformUtil.transform(apiResponse.getData(), TradeLevelRet.class);
            ret.setData(data);
        }
        return ret;
    }

    @GetMapping(value = "/public/account/futures-trade-level/get")
    public CommonRet<List<TradeLevelRet>> getFuturesTradeLevels() throws Exception {
        CommonRet<List<TradeLevelRet>> ret = new CommonRet<>();
        if (commissionBusdTradeLevelSwtich){
            APIResponse<List<FutureTradeLevelBusdResponse>> apiResponse = commissionTradeLevelManagerApi.comissionFuturesManageList();
            checkResponse(apiResponse);
            List<TradeLevelRet> data = ListTransformUtil.transform(apiResponse.getData(), TradeLevelRet.class);
            ret.setData(data);
        }else{
            APIResponse<List<TradeLevelVo>> apiResponse = tradeLevelApi.futuresManageList();
            checkResponse(apiResponse);
            List<TradeLevelRet> data = ListTransformUtil.transform(apiResponse.getData(), TradeLevelRet.class);
            ret.setData(data);
        }

        return ret;
    }

    /**
     * 获取用户近30天交易量、当前交易等级及费率、晋级下一级所需交易额或者持仓
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/private/account/trade-info/get")
    public CommonRet<CommissionTradeInfoRet> getTradeNumber() throws Exception {
        CommonRet<CommissionTradeInfoRet> ret = new CommonRet<>();
        Long userId = getUserId();
        Object value = ShardingRedisCacheUtils.get(userId.toString(), CommissionTradeInfoRet.class, CacheConstant.ACCOUNT_COMMISSION_TRADEV1_INFO_PREFIX);
        if (value != null){
            CommissionTradeInfoRet tradeInfoRet = JsonUtils.toObj(JsonUtils.toJsonHasNullKey(value), CommissionTradeInfoRet.class);
            ret.setData(tradeInfoRet);
            return ret;
        }
        TradeNumberResponse response = null;
        LongIdRequest longIdRequest = new LongIdRequest();
        longIdRequest.setId(userId);
        APIResponse<TradeNumberResponse> apiResponse = accountUserCommissionApi.tradeNumber(getInstance(longIdRequest));
        checkResponse(apiResponse);

        TradeNumberResponse tradeNumberResponse = apiResponse.getData();
        if (tradeNumberResponse != null) {
            response = new TradeNumberResponse();
            List<TradeVo> trades = tradeNumberResponse.getTrades().stream().map(tradeVo -> {
                TradeVo vo = new TradeVo();
                BeanUtils.copyProperties(tradeVo, vo);
                return vo;
            }).collect(Collectors.toList());

            NextVo nextVo = new NextVo();
            BeanUtils.copyProperties(tradeNumberResponse.getNext(), nextVo);

            UserGasVo userGasVo = new UserGasVo();
            BeanUtils.copyProperties(tradeNumberResponse.getUser(), userGasVo);

            response.setTrades(trades);
            response.setNext(nextVo);
            response.setUser(userGasVo);
        }

        if (response != null) {
            CommissionTradeInfoRet data = new CommissionTradeInfoRet();
            ret.setData(data);
            if (!CollectionUtils.isEmpty(response.getTrades())) {
                // 用户交易量信息
                List<CommissionTradeInfoRet.CommissionTrade> tradeList =
                        ListTransformUtil.transform(response.getTrades(), CommissionTradeInfoRet.CommissionTrade.class);
                data.setTrades(tradeList);
            }
            if (response.getNext() != null) {
                // 用户距离下一等级信息
                CommissionTradeInfoRet.CommissionNext next = new CommissionTradeInfoRet.CommissionNext();
                BeanUtils.copyProperties(response.getNext(), next);
                next.setNextBusd(response.getNext().getNextBusd());
                data.setNext(next);
            }
            if (response.getUser() != null) {
                // 用户交易等级费率信息
                CommissionTradeInfoRet.UserCommissionGas userGas = new CommissionTradeInfoRet.UserCommissionGas();
                BeanUtils.copyProperties(response.getUser(), userGas);
                userGas.setBusd30(response.getUser().getBusd30());
                data.setUser(userGas);
            }
        }
        ShardingRedisCacheUtils.set(userId.toString(), ret.getData(), CacheConstant.HOUR, CacheConstant.ACCOUNT_COMMISSION_TRADEV1_INFO_PREFIX);
        return ret;
    }


}
