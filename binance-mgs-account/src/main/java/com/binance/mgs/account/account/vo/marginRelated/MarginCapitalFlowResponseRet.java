package com.binance.mgs.account.account.vo.marginRelated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author sean w
 * @date 2021/10/9
 **/
@Data
@ApiModel("资金流水返回 response")
public class MarginCapitalFlowResponseRet {

    @ApiModelProperty("流水id")
    private Long tranId;

    @ApiModelProperty("发生时间")
    private Long timestamp;

    @ApiModelProperty("资产名称")
    private String asset;

    @ApiModelProperty("交易对")
    private String symbol;

    @ApiModelProperty("流水类型")
    private String type;

    @ApiModelProperty("金额")
    private BigDecimal amount;
}
