package com.binance.mgs.account.constant;

import com.binance.master.error.ErrorCode;
import lombok.AllArgsConstructor;

/**
 * account内部的一些错误码,使用BusinessException抛出即可。
 */
@AllArgsConstructor
public enum AccountMgsErrorCode implements ErrorCode {
    NICKNAME_NOT_VALID("024049", "昵称仅支持20位中英文或数字"),
    DEVICE_AUTH_CODE_IS_EXPIRE("024060", "设备授权码已失效"),

    USER_MOBILE_NOT_CORRECT("024072", "手机号输入不正确，请重新输入"),
    COUNTRY_NOT_SUPPORT("024073", "根据您当前的IP地址，Binance无法为您所在的国家/地区提供服务。"),
    ONEBUTTON_OPEN_TO_NEW_USER("024074", "仅限新用户领取"),
    ILLEGAL_DOMAIN("024077", "访问域名不在白名单中"),
    ACCOUNT_OR_PASSWORD_ERROR("024078", "账号或密码错误"),

    //TODO 后续account错误码统一以208开头，区别其他mgs错误码，例如 ILLEGAL_DOMAIN "208001"
    UPDATE_VERSION("208001", "请升级版本后再使用"),
    PLEASE_LOG_OUT_BEFORE_REGISTER("208002", "请先登出后再注册"),
    EMAIL_VERIFICATION_CODE_IS_SEND("208003", "邮件验证码已发送。请检查"),
    MOBILE_VERIFICATION_CODE_IS_SEND("208004", "手机验证码已发送。请检查"),
    BIZ_SCENE_NOT_SUPPORT("208005","场景不支持"),
    ACCOUNT_OVERLIMIT("208006","请求过于频繁"),//ddos专用错误码
    NOT_ALLOW_SIGNLVT_ERROR("208007", "您被禁止交易币安杠杆代币"),
    BIND_MOBILE_SEND_MOBILE_CODE_OVERLIMIT("208008", "绑定手机场景下发送短信过于频繁"),
    SUBUSER_ALREADY_SIGNED_LVT("208009", "该子账号已签署LVT"),
    SUBUSER_SIGNLVT_NOT_ACTIVE("208010", "该子账号未激活，不能签署lVT"),
    PLEASE_ENTER_VERIFYCODE("208011", "请输入验证码"),
    EMAIL_VERIFICATION_IS_RECOMMEND("208012", "您的手机号已绑定邮箱，请输入邮箱账号进行忘记密码操作"),
    INVALID_ACCOUNT_TYPE("208013", "无效的账户类型"),

    ACCOUNT_HAS_BEEN_REGISTERED("208014", "该账户已经被注册"),
    VOICE_SMS_NOT_SUPPORT("208015", "语音短信服务升级中，请使用文字短信重试。"),
    
    MERGE_SEND_EMAIL_OVERLIMIT("208018", "发送邮件/短信超出限制"),
    MERGE_FLOW_EXPIRE("208019", "账号合并请求已过期"),
    USER_NOT_OWN_FUTURE("208020", "该账号不存在合约账户"),

    FOREGT_PWD_NOT_SUPPORT_FOR_APP("208021", "请去web端使用此功能"),


    USER_ACTIVE_NOT_SUPPORT_FOR_APP("208022", "请去web端使用此功能"),

    API_NEED_KYC_COMPLETE("208023","创建API前请先完成KYC"),
    API_UPDATE_NEED_KYC_COMPLETE("208024", "个人用户请先完成中级及以上身份认证，企业用户请先完成企业认证"),

    ONE_2FA_UNFIT_MIGRATION_ERROR("208025","账户B只绑了1个2fa，不允许账户迁移"),

    VERIFY_CODE_IS_SEND("208026", "若您的账号确实存在，我们才会向您发送重置密码邮件"), // 发短信、邮件通用error code
    USER_IS_NOT_MANAGE_SUBUSER("208127", "当前账户非托管子账户"),
    USER_IS_NOT_BINDED_MANAGE_SUBUSER("208128", "当前账户与托管子账户不存在绑定关系"),

    EMAIL_IS_ESSENTIAL_CONDITION_TO_RESET_PWD("208027", "需使用邮箱账号进行重置密码操作"),

    PLEASE_CONTACT_CUSTOMER_SERVICE("208028", "Please contact customer service"),

    SUB_USER_CHECK_EMAIL_EXCEED("208029", "创建和修改次数已达到上限，请稍后再试。"),

    REGISTER_IS_DONE("208030", "如果你的账号已经在我方注册，我们不会给你发注册验证码，请尝试登录"), // 注册通用error code

    REGISTER_ANTI_BOT_CHECK_FAILED("208031", "您的信息触发了我们的安全规则，如果有问题请联系我们客服。"),

    FUTURE_ACCOUNT_NOT_EXISTS("208032", "账户不存在，请开通期货账户。"),


    USER_NOT_OWN_MARGIN("208033", "该账号不存在杠杆账户"),

    SEARCH_TIME_GREATER_THAN_THREE_MONTH("208034", "结束时间-开始时间不能超过3个月"),
    NICKNAME_CANNOT_MODIFY("208035","当前版本不允许修改昵称"),

    REGISTER_VALIDATE_FAILED("208036","当前版本过低，请升级到最新版本"),

    NO_SEND_CODE_IF_USER_EXISTS("208037","如果当前创建的邮箱已存在，将不会发送验证码"),
    IMG_UPLOAD_LIMIT("208038","超出图片上传次数限制"),

    GOOGLE_OAUTH_LOGIN_IS_CLOSE("208042", "google三方登陆已被临时关闭"),

    APPLE_OAUTH_LOGIN_IS_CLOSE("208043", "apple三方登陆已被临时关闭"),

    THIRD_OAUTH_LOGIN_IS_CLOSE("208046", "三方登陆已被临时关闭"),

    PLEASE_CHOISE_REGISTER_LOGIN_COUNTRY("208047", "请到web端选择注册国家"),

    TWO_USER_ID_NOT_MANAGER_SUB_USER_BOUND("208038", "两个账户不是托管母子账户关系"),

    THREE_PARTY_BINDUNBIND_IS_CLOSE("208048", "三方绑定解绑开关已被临时关闭"),

    USER_EXIST_RISK("208049", "用户存在风险"),

    RISK_COMMON_RULE_FAIL("208055", "您当前的操作存在风险，请稍后再试"),

    PLEASE_USE_PASSWORD_LOGIN("001512", "请输入密码登录"),

    WITHDRAW_ADDRESS_MEMO_FORMAT_INVALID("208056", "取款地址/MEMO格式無效"),

    WITHDRAW_ASSET_IS_EMPTY("208057", "提现失败，未找到asset，请检查并重试"),

    TOO_MANY_IPS("208058", "ip数量太多"),

    OPERATION_NOT_ALLOWED_WITHIN_DAYS("208059", "注册N天内不允许该操作"),

    // 需要解析风控具体文案，此code不添加翻译
    USER_HIT_RISK_AND_CUSTOMIZE_MSG("208060","你的账户已被限制登录"),

    // 安全提供具体文案，此code不添加翻译
    ANTI_BOT_CHECK_REJECT("208061", "您的信息触发了我们的安全规则，如果有问题请联系我们客服。"),
    
    ;


    private final String code;

    private final String message;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
