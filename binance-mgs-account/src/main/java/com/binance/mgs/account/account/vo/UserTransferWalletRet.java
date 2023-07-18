package com.binance.mgs.account.account.vo;

import com.binance.account.vo.user.enums.UserTransferWalletEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author rudy.c
 * @date 2021-09-15 11:11
 */
@Data
public class UserTransferWalletRet {
    @ApiModelProperty("用户出入金钱包配置")
    private UserTransferWalletEnum userTransferWallet;
}
