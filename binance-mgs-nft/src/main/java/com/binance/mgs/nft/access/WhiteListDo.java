package com.binance.mgs.nft.access;

import lombok.Data;

import java.util.List;

@Data
public class WhiteListDo {
    private boolean isGlobalForbidden;
    private boolean globalSwitch;
    private List<Long> uids;
    private List<String> suspendedNetwork;
}
