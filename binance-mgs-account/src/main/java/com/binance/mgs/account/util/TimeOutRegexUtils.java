package com.binance.mgs.account.util;

import com.binance.master.validator.regexp.Regexp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.binance.mgs.account.util.TimeLimitedMatcher.createMatcherWithTimeout;

@Slf4j
@Component
public class TimeOutRegexUtils {

    @Value("${common.email.regex.switch:false}")
    private boolean commonEmailRegexSwitch;

    @Value("${common.email.regex.new.switch:false}")
    private boolean commonEmailRegexForNewSwitch;

    @Value("${validate.email.timeout.millis:3000}")
    private long timeout;
    @Value("${reset.email.check.switch:1}")
    private Integer resetEmailCheckSwitch;
    @Value("${register.email.check.switch:2}")
    private Integer registerEmailFrontCheckSwitch;
    public final static String LOGIN_EMAIL_V2 = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public final static String LOGIN_EMAIL_V3 = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,64}$";
    //跟前端注册较验同步去掉-
    public final static String REGISTER_EMAIL_FRONT = "^[a-zA-Z0-9._%+]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    // Regexp.LOGIN_EMAIL
    public final static String REGEXP_LOGIN_EMAIL =        "^[a-zA-Z0-9_-|\\W]+(\\.[a-zA-Z0-9_-|\\W]+)*@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

    public final static String EMAIL_REGISTER_PATTERN =     "^[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"; //原来的注册接口邮箱正则

    // 安全建议的注册、登录、找回密码、绑定邮箱的通用正则 https://jira.toolsfdg.net/browse/COM-976
    public final static String SAFE_COMMON_EMAIL_PATTERN =  "^[a-zA-Z0-9_+-]+(?:\\.[a-zA-Z0-9_+-]+)*@(?:[a-zA-Z0-9-_]+\\.)+[a-zA-Z]+$";

    // 注册包含加号不允许通过
    public final static String SAFE_COMMON_EMAIL_PATTERN_FOR_REGISTER = "^[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)*@(?:[a-zA-Z0-9-_]+\\.)+[a-zA-Z]+$";

    @Value("#{'${regex.email.prefix.whilteList:test@gmail.com}'.split(',')}")
    private List<String> whiteListPrefix;

    @Value("${new.register.validate:true}")
    private boolean useNewRegisterValidate;

    /**
     * 默认正则匹配
     * 登录、托管子账户邮箱校验等
     */
    public boolean validateEmail(String email) {
        if (commonEmailRegexSwitch){
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN);
        }
        return validateEmailTimeOut(email, Regexp.LOGIN_EMAIL);
    }

    public boolean validateEmailForChangeEmail(String email) {
        if (useNewRegisterValidate) {
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN_FOR_REGISTER) || inWhiteList(email);
        }
        if (commonEmailRegexForNewSwitch){
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN);
        }

        if (1 == registerEmailFrontCheckSwitch) {
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN);
        } else if (2 == registerEmailFrontCheckSwitch) {
            return validateEmailTimeOut(email, REGISTER_EMAIL_FRONT);
        }

        return validateEmailTimeOut(email, Regexp.LOGIN_EMAIL);
    }

    /**
     * 注册
     */
    public boolean validateEmailForRegister(String email) {
        if (useNewRegisterValidate) {
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN_FOR_REGISTER) || inWhiteList(email);
        }
        if (commonEmailRegexForNewSwitch){
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN);
        }
        return validateEmailTimeOut(email, EMAIL_REGISTER_PATTERN);
    }

    private boolean inWhiteList(String email) {
        String[] plusSplit = email.split("\\+");
        String[] atSplit = email.split("@");
        return whiteListPrefix.contains(plusSplit[0] + "@" + atSplit[1]);
    }

    /**
     * 子账户check邮箱
     */
    public boolean validateEmailForSub(String email) {
        if (commonEmailRegexSwitch){
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN);
        }
        return validateEmailTimeOut(email, EMAIL_REGISTER_PATTERN);
    }

    public boolean validateEmailForReset(String email) {
        if (commonEmailRegexSwitch){
            return validateEmailTimeOut(email, SAFE_COMMON_EMAIL_PATTERN);
        }
        if (1 == resetEmailCheckSwitch) {
            return validateEmailTimeOut(email, Regexp.LOGIN_EMAIL);
        } else if (2 == resetEmailCheckSwitch) {
            return validateEmailTimeOut(email, LOGIN_EMAIL_V2);
        } else if (3 == resetEmailCheckSwitch) {
            return validateEmailTimeOut(email, LOGIN_EMAIL_V3);
        }

        return validateEmailTimeOut(email, Regexp.LOGIN_EMAIL);
    }

    /**
     * 使用TimeLimitedMatch, 正则3秒内无法完成匹配报错
     */
    public boolean validateEmailTimeOut(String email, String regex) {
        Pattern pattern = Pattern.compile(regex);
        boolean matched = false;
        try {
            Matcher matcher = createMatcherWithTimeout(
                    email, pattern, timeout, 10000000);
            matched = matcher.matches();
        } catch (TimeLimitedMatcher.RegExpTimeoutException e) {
            log.error("Time out to proceed regex email validation, eText={},regex:{}", email,regex);
        }
        return matched;
    }
}
