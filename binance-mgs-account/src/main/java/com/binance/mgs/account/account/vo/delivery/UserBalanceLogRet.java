package com.binance.mgs.account.account.vo.delivery;

import com.binance.deliverystreamer.api.response.balance.UserBalanceLogVo;
import com.binance.deliverystreamer.constant.BalanceType;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;


@Data
public class UserBalanceLogRet implements Serializable {

    private String id;

    private Long tranId;

    private String asset;

    private BalanceType balancetype;

    private BigDecimal balanceDelta;

    private String balanceInfo;

    private Long time;

    private String symbol;

    public static UserBalanceLogRet of(UserBalanceLogVo source) {
        UserBalanceLogRet userBalanceLogRet = new UserBalanceLogRet();
        BeanUtils.copyProperties(source, userBalanceLogRet);
        userBalanceLogRet.setId(source.getTranId() + "_" + source.getBalancetype().name());
        return userBalanceLogRet;
    }

}
