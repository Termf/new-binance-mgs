package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MintAuditCheckStatus {
    /**
     * 进行中
     */
    PENDING((byte)1, "PENDING"),
    /**
     * 失败
     */
    FAIL((byte)2, "FAIL"),
    /**
     * 成功
     */
    SUCCESS((byte)3, "SUCCESS"),
    ;

    private Byte type;
    private String desc;
}
