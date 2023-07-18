package com.binance.mgs.account.account.vo.delivery;


import com.binance.deliverystreamer.api.response.order.OrderVo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

@Data
public class OrderHistoryRet implements Serializable {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;

    private Long orderId;

    private String symbol;

    private String clientOrderId;

    private String origClientOrderId;

    private String price;

    private String origQty;

    private String executedQty;

    private String executedQuoteQty;

    private String status;

    private String timeInForce;

    private String type;

    private String side;

    private String stopPrice;

    private Long insertTime;

    private String delegateMoney;

    private String avgPrice;

    private Boolean hasDetail;

    private Integer targetStrategy;

    private Integer priceProtect;

    private Boolean reduceOnly = false;

    private String workingType;

    private String origType;

    private String positionSide;

    private String activatePrice;

    private String priceRate;
    
    private Boolean closePosition;

    private String baseAsset;

    private String quoteAsset;

    private Long strategyId;

    private Integer strategySubId;

    private String strategyType;

    private String markPrice;

    public static OrderHistoryRet of(OrderVo source) {
        OrderHistoryRet ret = new OrderHistoryRet();
        BeanUtils.copyProperties(source, ret);
        if (StringUtils.isBlank(ret.getPositionSide())) {
            ret.setId(source.getOrderId() + "_" + source.getSymbol());
        } else {
            ret.setId(source.getOrderId() + "_" + source.getSymbol() + "_" + ret.getPositionSide());
        }
        return ret;
    }
}
