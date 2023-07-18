package com.binance.mgs.nft.sql;

import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
@Aspect
@Slf4j
public class SqlInjectAop {

    @Around("@annotation(SqlInject)")
    public Object around(ProceedingJoinPoint point) throws Throwable{
        if (!checkSqlValid(point)){
            return initErrorCommonRet();
        }
        return point.proceed();
    }

    private boolean checkSqlValid(ProceedingJoinPoint point) throws InvocationTargetException, IllegalAccessException {

        Method method = ((MethodSignature)point.getSignature()).getMethod();
        SqlInject sqlInject = method.getAnnotation(SqlInject.class);

        if (null == sqlInject.params() || sqlInject.params().length <= 0){
            //不校验
            return true;
        }

        Map<String,Object> paramMap = getNameAndValue(point);

        if (CollectionUtils.isEmpty(paramMap)){
            return true;
        }

        for (String param : sqlInject.params()){
            if (paramMap.containsKey(param) && null != paramMap.get(param) && !SqlInjectUtils.isSqlValid(paramMap.get(param).toString())){
                return false;
            }
        }

        return true;
    }

    Map<String, Object> getNameAndValue(ProceedingJoinPoint point) throws InvocationTargetException, IllegalAccessException {
        Map<String, Object> paramMap = new HashMap<>();

        Object[] paramValues = point.getArgs();
        String[] paramNames = ((CodeSignature)point.getSignature()).getParameterNames();

        for (int i = 0; i < paramNames.length; i++) {
            if (baseType(paramValues[i])){
                paramMap.put(paramNames[i],paramValues[i]);
            }else {
                Object obj = paramValues[i];

                Method[] methods = obj.getClass().getDeclaredMethods();

                for (Method method : methods){
                    String param;
                    String methodName = method.getName();

                    if (methodName.startsWith("is")){
                        param = methodName.substring(2,3).toLowerCase() + methodName.substring(3);
                    }else if (methodName.startsWith("get")){
                        param = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
                    }else {
                        continue;
                    }

                    paramMap.put(param,method.invoke(obj));
                }
            }
        }

        return paramMap;
    }

    private CommonRet<?> initErrorCommonRet() {
        CommonRet<?> commonRet = new CommonRet<>();
        commonRet.setCode("10000222");
        commonRet.setMessage("Error encountered in Google recaptcha validation, please try again later");
        return commonRet;
    }

    private boolean baseType(Object value){
        return value instanceof Short ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Float ||
                value instanceof Double ||
                value instanceof Boolean ||
                value instanceof Byte ||
                value instanceof Character ||
                value instanceof String;
    }
}
