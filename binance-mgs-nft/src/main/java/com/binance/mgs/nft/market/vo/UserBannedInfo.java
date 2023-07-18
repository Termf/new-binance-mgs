package com.binance.mgs.nft.market.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBannedInfo implements Serializable {

    private boolean isAdminBanned;
    private long reopenTime;


}
