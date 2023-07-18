package com.binance.mgs.nft.sql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlInjectUtils {

    static String reg = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
            + "(\\b(select|update|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

    static Pattern sqlPattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);

    /***************************************************************************
     * 参数校验
     *
     * @param str ep: "or 1=1"
     */
    public static boolean isSqlValid(String str) {
        Matcher matcher = sqlPattern.matcher(str);

        return !matcher.find();
    }
}
