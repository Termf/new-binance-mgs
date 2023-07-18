package com.binance.mgs.account.account.vo.future;

import com.binance.futurestreamer.api.response.trade.TradeDetailVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;


@Data
public class TradeRet implements Serializable {

    private Long tradeId;

    private String price;

    private Long insertTime;

    private String symbol;

    private String side;

    private Boolean activeBuy;

    private String qty;

    private String fee;

    private String commissionAsset;

    private String totalQuota;

    private String productName;

    private String baseAsset;

    private String quoteAsset;

    private String marginAsset;

    private String realizedProfit;

    private String id;

    private String positionSide;

    public static TradeRet of(com.binance.futurestreamer.api.response.trade.TradeVo source) {
        TradeRet ret = new TradeRet();
        BeanUtils.copyProperties(source, ret);
        if (StringUtils.isBlank(ret.getPositionSide())) {
            ret.setId(source.getTradeId() + "_" + source.getSymbol() + "_" + source.getSide());
        } else {
            ret.setId(source.getTradeId() + "_" + source.getSymbol() + "_" + source.getSide() + "_" + ret.getPositionSide());
        }

        return ret;
    }

    public static TradeRet of(TradeDetailVo source) {
        TradeRet ret = new TradeRet();
        BeanUtils.copyProperties(source, ret);
        ret.setCommissionAsset(source.getFeeAsset());
        if (StringUtils.isBlank(ret.getPositionSide())) {
            ret.setId(source.getTradeId() + "_" + source.getSymbol() + "_" + source.getSide());
        } else {
            ret.setId(source.getTradeId() + "_" + source.getSymbol() + "_" + source.getSide() + "_" + ret.getPositionSide());
        }
        return ret;
    }
}
