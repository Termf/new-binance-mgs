package com.binance.mgs.account.account.vo.future;

import com.binance.futurestreamer.constant.BalanceType;
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

    public static UserBalanceLogRet of(com.binance.futurestreamer.api.response.balance.UserBalanceLogVo source) {
        UserBalanceLogRet userBalanceLogRet = new UserBalanceLogRet();
        BeanUtils.copyProperties(source, userBalanceLogRet);
        userBalanceLogRet.setId(source.getTranId() + "_" + source.getBalancetype().name());
        return userBalanceLogRet;
    }

}
