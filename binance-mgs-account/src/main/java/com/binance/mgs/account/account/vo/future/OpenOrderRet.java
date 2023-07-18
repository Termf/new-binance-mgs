package com.binance.mgs.account.account.vo.future;


import com.binance.futurestreamer.api.response.order.OpenOrderVo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

@Data
public class OpenOrderRet implements Serializable {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;

    private Long orderId;

    private String symbol;

    private String clientOrderId;

    private String origClientOrderId;

    private String price;

    private String avgPrice;

    private String origQty;

    private String money;

    private String executedQty;

    private String executedQuoteQty;

    private String status;

    private String timeInForce;

    private String type;

    private String side;

    private String stopPrice;

    private Long insertTime;

    private String delegateMoney;

    private Integer targetStrategy;

    private Integer priceProtect;

    private Boolean reduceOnly = false;

    private String workingType;

    private String origType;

    private String positionSide;

    private String activatePrice;

    private String priceRate;

    private Boolean closePosition;

    private Long strategyId;

    private Integer strategySubId;

    private String strategyType;

    public static OpenOrderRet of(OpenOrderVo source) {
        OpenOrderRet ret = new OpenOrderRet();
        BeanUtils.copyProperties(source, ret);
        if (StringUtils.isBlank(ret.getPositionSide())) {
            ret.setId(source.getOrderId() + "_" + source.getSymbol());
        } else {
            ret.setId(source.getOrderId() + "_" + source.getSymbol() + "_" + ret.getPositionSide());
        }
        return ret;
    }
}
