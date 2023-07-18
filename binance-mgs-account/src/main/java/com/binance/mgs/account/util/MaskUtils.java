package com.binance.mgs.account.util;


import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

/**
 * Created by yangyang on 2019/7/23.
 */
public class MaskUtils {

    public static String maskEmail(String email) {
        // 对于邮箱格式不正确的直接返回，不脱敏
        if (StringUtils.isBlank(email) || -1 == email.lastIndexOf("@") || -1 == email.lastIndexOf(".")
                || ((email.lastIndexOf("@"))+1 >= email.lastIndexOf("."))){
            return "***";
        }

        String name = email.substring(0, email.lastIndexOf("@"));
        String midle = email.substring(email.lastIndexOf("@") + 1, email.lastIndexOf("."));
        String fix = email.substring(email.lastIndexOf("@"), email.length()).replaceFirst(midle, "***");
        int size = name.length();
        if (size <= 2) {
            return name + "***" + fix;
        } else {
            return name.substring(0, 2) + "***" + fix;
        }
    }

    public static String maskHalfOpenEmail(String email) {
        // 对于邮箱格式不正确的直接返回，不脱敏
        if (StringUtils.isBlank(email) || -1 == email.lastIndexOf("@") || -1 == email.lastIndexOf(".")
                || ((email.lastIndexOf("@"))+1 >= email.lastIndexOf("."))){
            return "***";
        }

        String name = email.substring(0, email.lastIndexOf("@"));
        String fix = email.substring(email.lastIndexOf("@"), email.length());
        int size = name.length();
        if (size <= 2) {
            return name + "***" + fix;
        } else if (size < 5){
            return name.substring(0, 2) + "***" + fix;
        } else if (size >= 5){
            return name.substring(0, 2)+ "***" +name.substring(size-2)+fix;
        }else {
            //找个应该不存在了
            return name.substring(0, 2) + "***" + fix;
        }
    }

    public static String maskMobileNo(String mobile) {
        if (StringUtils.isBlank(mobile) || mobile.length() <= 3) {
            return "***";
        }
        int length = mobile.length();
        int maskIndexBegin = length > 7 ? 3 : (length > 5 ? 2 : 1);
        int maskIndexEnd = length - maskIndexBegin - 1;
        String mask = String.join("", Collections.nCopies(maskIndexEnd - maskIndexBegin, "*"));
        return mobile.substring(0, maskIndexBegin) + mask + mobile.substring(maskIndexEnd);
    }

    public static String maskHalfEmail(String email) {
        if (StringUtils.isBlank(email) || -1 == email.lastIndexOf("@") || -1 == email.lastIndexOf(".")
                || ((email.lastIndexOf("@"))+1 >= email.lastIndexOf("."))){
            return "***";
        }

        String name = email.substring(0, email.lastIndexOf("@"));
        String fix = email.substring(email.lastIndexOf("@"));
        return name.length() > 3 ? (name.substring(0, 3) + "***" + fix) : (name + "***" + fix);
    }
}
