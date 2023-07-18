package com.binance.mgs.nft.inbox;

import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public enum HistoryType {
    PURCHASE("purchase history"),
    SALES("Sales History"),
    CREATE("Nft mint"),
    LAYER("Nft collection(layer)"),
    DEPOSITS("Nft Deposits"),
    WITHDRAWS("Nft Withdraws"),
    DISTRIBUTION("Distribution"),
    LIST_AUCTION("List Auction"),
    LIST_FIXED("List Fixed");

    private String desc;

    private static final Set<String> allTypes = Stream.of(values()).map(HistoryType::name).collect(Collectors.toSet());

    public static boolean hasBiz(String bizType) {
        return allTypes.contains(bizType);
    }
}
