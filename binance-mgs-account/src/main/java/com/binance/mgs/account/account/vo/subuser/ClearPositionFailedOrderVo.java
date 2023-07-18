package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("ClearPositionFailedOrderVo")
public class ClearPositionFailedOrderVo {

    private String symbol;

    private String code;

    private String message;

    private String positionSide;

    private String amount;

    private String orderSide;
}
