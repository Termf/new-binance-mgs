package com.binance.mgs.nft.core.redis;

public class RedisCommonConfig {


    public static final String GOOGLE_LIMIT_NEXT_CHECK_PERIOD_KEY = "google:recaptcha:limit:next:check:%s";

    public static final String GOOGLE_LIMIT_REFRESH_TOKEN_KEY = "google:recaptcha:limit:refresh:token:%s";

    public static final String MGS_RATE_LIMITER_KEY = "mgs:rate:limiter:%s:%s:%s";


    public static final String NFT_WHITE_LIMIT_MINT_COUNT = "nft:white:limit:mint:count:%s:%s";

    public static final String NFT_WHITE_LIMIT_ONSALE_COUNT = "nft:white:limit:onsale:count:%s:%s";

    public static final String NFT_WHITE_LIMIT_ONSALE_NFTLIST = "nft:white:limit:onsale:list:%s:%s";

    public static final String NFT_MINT_AGREED_RISK_REMINDER = "nft:mint:%s:agreed:risk:reminder";

    public static final String NFT_MINT_INAPPROPRIATE_ATTEMPT_COUNT = "nft:mint:%s:inappropriate:attempt:count";

    public static final String NFT_MINT_AUDIT_CHECK_RESULT = "nft:mint:audit:check:result:%s";
}
