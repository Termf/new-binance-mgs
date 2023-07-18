package com.binance.mgs.account.account.vo;

import com.binance.account.vo.user.enums.UserTransferWalletEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author rudy.c
 * @date 2021-09-15 11:14
 */
@Data
public class UserTransferWalletSetArg {
    @ApiModelProperty(value = "用户出入金钱包配置", required = true)
    @NotNull
    private UserTransferWalletEnum userTransferWallet;
}
