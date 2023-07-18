package com.binance.mgs.nft.nftasset.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserCashBalanceRet implements Serializable {

    private String asset;
    private String free;
    private String originalFee;
    private String actualFee;
    private String logoUrl;
    private String fiatName;
    private String fiatValue;

}
