package com.binance.mgs.nft.twofa;

import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.bnbgtwservice.api.data.dto.SecurityV2Dto;
import com.binance.nft.bnbgtwservice.api.iface.ISecurity2faApi;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.google.api.client.util.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: allen.f
 * @date: 2021/9/22
 **/
@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class TwoFaAop implements InitializingBean {

    @Resource
    private ISecurity2faApi security2faApi;
    @Resource
    private BaseHelper baseHelper;

    private final String twoFaSwitch = "nft.access.2fa.switch";

    private final Config config = ConfigService.getAppConfig();

    private volatile Map<String,Boolean> map = Maps.newHashMap();

    @Around("@annotation(TwoFa)")
    public Object around(ProceedingJoinPoint point) throws Throwable{

        if (!checkAccessAble(point)){
            return initErrorCommonRet();
        }

        return point.proceed();
    }

    private boolean checkAccessAble(ProceedingJoinPoint point) {

        Long userId = baseHelper.getUserId();

        if (null == userId){
            return false;
        }

        TwoFa twoFa = ((MethodSignature)point.getSignature()).getMethod().getAnnotation(TwoFa.class);

        if (null == twoFa.scene()){
            return false;
        }

        if (null == map || null == map.get(twoFa.scene().getCode())){
            return false;
        }

        if (!map.get(twoFa.scene().getCode())){
            return true;
        }

        try {
            SecurityV2Dto request = new SecurityV2Dto();

            request.setUserId(userId);
            request.setCode(twoFa.scene().getCode());

            fillRequest(request,getNameAndValue(point));

            log.warn("2fa check request : {}.",JsonUtils.toJsonHasNullKey(request));

            APIResponse<Boolean> response = security2faApi.validate2faV2(request);

            log.warn("2fa check response : {}.",JsonUtils.toJsonHasNullKey(response));

            return null != response && response.getData();
        }catch (Exception e){
            log.error("exec validate 2fa v2 err.",e);
            return false;
        }
    }

    private void fillRequest(SecurityV2Dto request, Map<String, Object> nameAndValue) throws InvocationTargetException, IllegalAccessException {

        Method[] methods = request.getClass().getDeclaredMethods();

        for (Method method : methods){

            String methodName = method.getName();

            if (methodName.contains("set")){
                String param = methodName.substring(3,4).toLowerCase() + methodName.substring(4);

                if (null == nameAndValue.get(param)){
                    continue;
                }

                method.invoke(request,nameAndValue.get(param));
            }
        }
    }

    private CommonRet<?> initErrorCommonRet() {
        CommonRet<?> commonRet = new CommonRet<>();
        commonRet.setCode(NftAssetErrorCode.CHECK_2FA_ERR.getCode());
        commonRet.setMessage("Sorry, you do not have permission here.");
        return commonRet;
    }

    private Map<String, Object> getNameAndValue(ProceedingJoinPoint point) throws InvocationTargetException, IllegalAccessException {
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

    /**
     * 使用主动方式，避免基于注解的推送不到
     */
    @Scheduled(fixedDelay = 5_000)
    public void refresh(){
        try {
            String value = config.getProperty(twoFaSwitch, null);

            if (StringUtils.isBlank(value)){
                return;
            }

            Map<String,Boolean> map = JsonUtils.toMap(value,String.class,Boolean.class);

            this.map = map;
        }catch (Exception e){
            log.error("refresh two fa config.",e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        refresh();
    }
}
