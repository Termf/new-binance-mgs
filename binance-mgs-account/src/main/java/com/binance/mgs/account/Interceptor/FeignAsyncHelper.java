package com.binance.mgs.account.Interceptor;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;


public class FeignAsyncHelper {

    private static final ThreadLocal<Map<String, String>> localLog = new ThreadLocal<>();

    public static Map<String, String> getAndClearLocalHead() {
        Map<String, String> temp = localLog.get();
        localLog.set(null);
        return temp;
    }

    /**
     * * @param content
     */
    public static void addHead(Map<String, String> content) {
        if (localLog.get() == null) {
            localLog.set(new HashMap<>(content));
        } else {
            localLog.get().putAll(content);
        }
    }

    public static void addHead(String key, String val) {
        if (val == null) {
            val = "";
        }
        if (localLog.get() == null) {
            localLog.set(new HashMap<>(ImmutableMap.of(key, val)));
        } else {
            localLog.get().put(key, val);
        }
    }






}
