package com.binance.mgs.nft.access;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccessEvent {
    DEPOSIT(1,"deposit"),
    WITHDRAW(2,"withdraw");

    private int code;
    private String name;
}
