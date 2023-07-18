package com.binance.mgs.account.util;

import com.binance.mgs.account.security.vo.SecurityCheckArg;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2022/1/11
 */
public class CaptchaCheckUtil {

    public static boolean checkCaptchaPattern(ValidateCodeArg arg) {
        switch (arg.getValidateCodeType()) {
            case "gt":
                return checkGtPattern(arg.getGeetestChallenge(), arg.getGeetestValidate(), arg.getGeetestSecCode());
            case "bCAPTCHA":
            case "bCAPTCHA2":
                return checkBCaptchaPattern(arg.getBCaptchaToken());
            case "reCAPTCHA":
                return checkReCaptchaPattern(arg.getRecaptchaResponse());
            default:
                return false;
        }
    }

    public static boolean checkCaptchaPattern(String captchaType, SecurityCheckArg arg) {
        if (!StringUtils.equals(arg.getValidateCodeType(), captchaType)) {
            return false;
        }
        return checkCaptchaPattern(arg);
    }

    public static boolean checkGtPattern(String geetestChallenge, String geetestValidate, String geetestSecCode) {
        if (StringUtils.isAnyBlank(geetestChallenge, geetestValidate, geetestSecCode)){
            return false;
        }
        return true;
    }

    public static boolean checkBCaptchaPattern(String bCaptchaToken) {
        if (StringUtils.isBlank(bCaptchaToken)){
            return false;
        }
        if (!bCaptchaToken.startsWith("captcha#")) {
            return false;
        }
        return true;
    }

    public static boolean checkReCaptchaPattern(String recaptchaResponse) {
        if (StringUtils.isAnyBlank(recaptchaResponse)){
            return false;
        }
        return true;
    }
}
