package com.binance.mgs.account.account.vo.face;

/**
 * @author liliang1
 * @date 2018-12-13 14:46
 */
public enum FaceTransType {

    /** 重置谷歌 */
    google,
    /** 重置手机 */
    mobile,
    /** 用户解禁 */
    enable,
    /** 提现风控人脸识别 */
    withdrawFace,
    /** KYC 个人认证 */
    user,
    /** KYC 企业认证 */
    company,
    /** 变更Email */
    resetEmail,

    RESET_APPLY_2FA,
    RESET_APPLY_UNLOCK,

    ;

}
