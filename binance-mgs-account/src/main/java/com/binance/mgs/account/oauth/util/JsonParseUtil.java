package com.binance.mgs.account.oauth.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.util.ParameterizedTypeImpl;
import com.binance.platform.mgs.base.vo.CommonRet;

import java.lang.reflect.Type;
import java.util.List;

public class JsonParseUtil {

    public static <T> CommonRet<T> parseObject(String json, Class<T> clazz) {
        return JSONObject.parseObject(json, new TypeReference<CommonRet<T>>(clazz) {
        });
    }

    public static <T> CommonRet<List<T>> parseList(String json, Class<T> clazz) {
        ParameterizedTypeImpl inner = new ParameterizedTypeImpl(new Type[]{clazz}, null, List.class);
        ParameterizedTypeImpl outer = new ParameterizedTypeImpl(new Type[]{inner}, null, CommonRet.class);
        return JSONObject.parseObject(json, outer);
    }

}
