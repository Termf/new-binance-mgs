package com.binance.mgs.account.constant;

/**
 * Created by pcx
 *
 * 用来存放缓存key的前缀
 */
public class CacheConstant {
    public static final int DAY = 86400;
    public static final int HOUR = 3600;
    public static final int HOUR_HALF = 1800;
    public static final int MINUTE_10 = 600;
    public static final int MINUTE_5 = 300;

    public static final String REGISTER_IP_COUNT_USERID = "MGS:REGISTER_IP_COUNT_USERID";

    public static final String MERGE_SEND_EMAIL_COUNT = "MGS:MERGE_SEND_EMAIL_COUNT";

    public static final String MERGE_HAS_SEND_VERIFYCODE = "MGS:MERGE_HAS_SEND_VERIFYCODE";

    public static final String MERGE_ACTIVE_FLOWID = "MGS:MERGE_ACTIVE_FLOWID";

    public static final String ACCOUNT_KYC_US_LOGIN = "ACCOUNT:KYC:US:LOGIN";

    public static final String ACCOUNT_KYC_US_REOPEN = "ACCOUNT:KYC:US:REOPEN";

    public static final String SUB_USER_CHECK_EMAIL = "MGS:SUB_USER:CHECK_EMAIL";

    public static final String FUTURES_ID_REDIS_KEY_PREFIX = "FUT:FUTURE:ID:HASH:";

    public static final String FUTURES_ID_REDIS_KEY_PREFIX_V2 = "accountmgs:fut:future:id";
    public static final String ACCOUNT_VIP_MM_IMAGE_COUNT_V2 = "accountmgs:account:vip:mm:image:";


    public static final String ACCOUNT_REFRESH_TOKEN_KEY = "accountddos:refresh:token";

    public static final String ACCOUNT_DDOS_CAPTCHA_HEALTH_PREFIX = "accountddos:captcha:health";
    public static final String ACCOUNT_DDOS_IP_COUNT_PREFIX = "accountddos:ip:count";
    public static final String ACCOUNT_DDOS_JWKS_KEY = "accountddos:appcheck:jwks";
    public static final String ACCOUNT_DDOS_SUB_ACCOUNT_ACTION_FREQUENCY_PREFIX = "accountddos:sub:account:frequency";
    public static final String ACCOUNT_DDOS_VERIFY_CAPTCHA_CACHE_PREFIX = "accountddos:captcha:verify:result";
    public static final String ACCOUNT_DDOS_SESSION_ID_PREFIX = "accountddos:session:id";
    public static final String ACCOUNT_DDOS_CAPTCHA_TOKEN_COUNT_PREFIX = "accountddos:captcha:token:count";
    public static final String ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_LOCK_KEY = "accountddos:anti:bot:session:id:salt:lock";
    public static final String ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_SALT_KEY = "accountddos:anti:bot:session:id:salt";
    public static final String ACCOUNT_DDOS_ANTI_BOT_SESSION_ID_OLD_SALT_KEY = "accountddos:anti:bot:session:id:old:salt";
    public static final String ACCOUNT_DDOS_CHALLENGE_SESSION_ID_PREFIX = "accountddos:challenge:session:id";

    public static final String ACCOUNT_COMMISSION_TRADEV1_INFO_PREFIX = "accountmgs:accountcommission:tradev1:info";
    public static final String ACCOUNT_COMMISSION_TRADEV2_INFO_PREFIX = "accountmgs:accountcommission:tradev2:info";
    public static final String ACCOUNT_COMMISSION_TRADE_RECENT30_PREFIX = "accountmgs:accountcommission:trade:recent30";
    public static final String ACCOUNT_COMMISSION_TRADEV2_RECENT30_PREFIX = "accountmgs:accountcommission:tradev2:recent30";

    public static final String ACCOUNT_MGS_RECAPTCHA_ASSESSMENT_PREFIX = "accountmgs:recaptcha:assessment";
    public static final String ACCOUNT_MGS_GEETEST_SERIAL_NO = "accountmgs:geetest:%s";
    public static final String ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_KEY_PREFIX = "accountmgs:anti:bot:getuserid";
    public static final String ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_BY_THIRD_KEY_PREFIX = "accountmgs:anti:bot:getuserid:third";
    public static final String ACCOUNT_MGS_ANTI_BOT_GET_USER_DISABLE_LOGIN_STATUS_PREFIX = "accountmgs:anti:bot:user:disablelogin";

    public static final String ACCOUNT_MGS_OAUTH_TOKEN_PREFIX = "accountmgs:oauth:token";

    public static final String ACCOUNT_MGS_MERGE_SEND_EMAIL_COUNT = "accountmgs:merge:email";

    public static final String ACCOUNT_MGS_SUB_USER_RESET_TRADE_TIME = "accountmgs:subuser:resettradetime";

    public static final String ACCOUNT_MGS_MANAGER_SUR_USER_SET_FEE = "accountmgs:managersubuser:setfee";

    public static final String CREATE_QRCODE_LIMIT = "accountmgs:createqrcode:%s";

    public static final String OTP_SEND_LIMIT_PREFIX = "accountmgs:otpsendlimit:";

    public static final String ACCOUNT_MGS_LOGIN_CONTEXT_CACHE_KEY = "accountmgs:login:context";

    public static final String ACCOUNT_MGS_LOGIN_FLOWID_CACHE_KEY = "accountmgs:login:flowid";

    public static String getGeeTestSerialNo(String serialNo) {
        return String.format(ACCOUNT_MGS_GEETEST_SERIAL_NO, serialNo);
    }
    public static String getCreateQrcodeLimit(Long userId) {
        return String.format(CREATE_QRCODE_LIMIT, userId);
    }
}
