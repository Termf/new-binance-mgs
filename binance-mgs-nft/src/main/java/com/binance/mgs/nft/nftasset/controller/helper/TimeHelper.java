package com.binance.mgs.nft.nftasset.controller.helper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;

public class TimeHelper {
    public static long getUTCTime(){
        return Timestamp.valueOf(OffsetDateTime.now(Clock.systemUTC()).toLocalDateTime()).getTime();
    }
}
