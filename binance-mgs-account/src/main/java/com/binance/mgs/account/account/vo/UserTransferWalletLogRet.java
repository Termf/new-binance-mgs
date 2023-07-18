package com.binance.mgs.account.account.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author rudy.c
 * @date 2021-09-15 11:20
 */
@Data
public class UserTransferWalletLogRet {
    private Date operateTime;
    private String userTransferWallet;
}
