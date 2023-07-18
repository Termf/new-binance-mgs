package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
public class ManagerSubUserTransferArg {
    @ApiModelProperty(required = true, notes = "转出方email")
    @NotNull
    private String senderUserEmail;

    @ApiModelProperty(required = true, notes = "转入方email")
    @NotNull
    private String recipientUserEmail;

    @ApiModelProperty(required = true, notes = "资产名字(例如BTC)")
    @NotNull
    private String asset;

    @ApiModelProperty(required = true, notes = "划转数量")
    @NotNull
    private BigDecimal amount;


    @ApiModelProperty(required = true, notes = "期望执行时间（时间戳毫秒级的）")
    private Long expectExecuteTime;
}
