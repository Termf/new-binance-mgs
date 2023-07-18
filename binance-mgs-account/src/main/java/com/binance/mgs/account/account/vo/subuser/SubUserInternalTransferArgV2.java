package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 */
@Data
public class SubUserInternalTransferArgV2 {
    @NotNull
    private String senderUserId;
    @NotNull
    private String recipientUserId;
    @NotNull
    private String asset;

    private String symbol;
    @NotNull
    private String amount;
    @ApiModelProperty(required = true, notes = "转出方账户类型，比方说从现货转出那么就填spot")
    @NotNull
    private FunctionAccountType senderFunctionAccountType;
    @ApiModelProperty(required = true, notes = "转入方账户类型，比方说从现货转出那么就填spot，具体看你想往哪儿个功能性账户转")
    @NotNull
    private FunctionAccountType recipientFunctionAccountType;
}
