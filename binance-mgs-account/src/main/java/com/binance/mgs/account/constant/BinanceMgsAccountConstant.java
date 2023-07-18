package com.binance.mgs.account.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pcx
 *
 * 用来存放缓存key的前缀
 */
public class BinanceMgsAccountConstant {

    public static final String BINANCE_MGS_ACCOUNT_REGISTER_TOKEN = "r10t";

    public static final String THREE_PARTY_SOURCE = "ThreeParty";

    /**
     * 无密码登录的版本
     */
    public static final String ENABLE_PASSOWRDLESS_LOGIN_HEADER_NAME = "enable_pwdlesslogin_version";

    /**
     * 风控挑战灰度标签
     * */
    public static final String RISK_CHALLENGE_FLAG = "risk_challenge";

    /**
     * commonRule接口ip字段
     * */
    public static final String COMMON_RULE_IP = "ip";

    /**
     * commonRule接口uid字段
     * */
    public static final String COMMON_RULE_UID = "uid";

    /**
     * commonRule接口scenes_code字段
     * */
    public static final String COMMON_RULE_SENSE_CODE = "scenes_code";

    /**
     * commonRule接口platform字段
     * */
    public static final String COMMON_RULE_PLATFORM = "platform";

    /**
     * commonRule接口fvideoid字段
     * */
    public static final String COMMON_RULE_FVIDEOID = "fvideoid";

    /**
     * risk返回风控挑战字段
     */
    public static final String RISK_CHALLENGE_FLOW_ENABLE = "risk_challenge_enable_flow";
    /**
     * 挑战通过/免密通过/风控拒绝
     */
    public static final String RISK_CHALLENGE_RESULT = "risk_challenge_result";

    /**
     * useroperation log中fido绑定类型的
     */
    public static final String BIND_FIDO_TYPE_KEY = "fidoType";

    /**
     * 风控挑战流水号
     */
    public static final String RISK_CHALLENGE_BIZ_NO_HEADER_KEY = "risk_challenge_biz_no";

    /**
     * mfa密码验证类型key
     */
    public static final String MFA_PASSWORD_VERIFY_TYPE = "PASSWORD";
}
