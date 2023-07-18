package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author sean w
 * @date 2022/3/25
 **/
@Data
public class ManagerSubUserMultiTransferArg {

    @ApiModelProperty(required = true, notes = "转出方email")
    @NotNull
    private String senderUserEmail;

    @ApiModelProperty(required = true, notes = "转入方email")
    @NotNull
    private String recipientUserEmail;

    @ApiModelProperty(required = true, notes = "转出方账户类型，比方说从现货转出那么就填spot")
    @NotNull
    private FunctionAccountType senderFunctionAccountType;

    @ApiModelProperty(required = true, notes = "转入方账户类型，比方说从现货转出那么就填spot，具体看你想往哪儿个功能性账户转")
    @NotNull
    private FunctionAccountType recipientFunctionAccountType;

    @ApiModelProperty(required = true, notes = "资产名字(例如BTC)")
    @NotNull
    private String asset;

    @ApiModelProperty(required = true, notes = "划转数量")
    @NotNull
    private BigDecimal amount;

    @ApiModelProperty(required = true, notes = "转出方逐仓margin交易对，逐仓margin是每个交易对一个账号的，所以要告诉我你指定哪儿个交易对的")
    private String senderIsolatedMarginSymbol;

    @ApiModelProperty(required = true, notes = "转入方逐仓margin交易对，逐仓margin是每个交易对一个账号的，所以要告诉我你指定哪儿个交易对的")
    private String recipientIsolatedMarginSymbol;
}
