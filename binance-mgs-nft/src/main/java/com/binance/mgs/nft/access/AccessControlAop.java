package com.binance.mgs.nft.access;

import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.StringUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author user
 */
@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AccessControlAop {
    private final String prefix = "nft.access.control.";

    private final Config config = ConfigService.getAppConfig();

    private final Map<String,WhiteListDo> whiteListMap = new HashMap();

    private final BaseHelper baseHelper;

    @Around("@annotation(AccessControl)")
    public Object around(ProceedingJoinPoint point) throws Throwable{

        if (!checkAccessAble(point)){
            return initErrorCommonRet();
        }

        return point.proceed();
    }

    private boolean checkAccessAble(ProceedingJoinPoint point) {
        final Long userId = baseHelper.getUserId();

        log.info("[access control] user id : {}.",userId);
        if (null == userId){
            return false;
        }

        AccessControl accessControl = ((MethodSignature)point.getSignature()).getMethod().getAnnotation(AccessControl.class);

        log.info("[access control] access control : {}.",accessControl.event().getName());
        if (null == accessControl.event()){
            return false;
        }

        WhiteListDo whiteListDo = whiteListMap.get(prefix + accessControl.event().getName());

        log.info("[access control] white list : {}.",JsonUtils.toJsonHasNullKey(whiteListDo));
        if (null == whiteListDo || whiteListDo.isGlobalForbidden()){
            return false;
        }

        if (!CollectionUtils.isEmpty(whiteListDo.getSuspendedNetwork())
                && StringUtils.isNotBlank(accessControl.networkType())
                && whiteListDo.getSuspendedNetwork().contains(accessControl.networkType().toUpperCase())) {
            return false;
        }

        return whiteListDo.isGlobalSwitch() || (null != whiteListDo.getUids() && whiteListDo.getUids().contains(userId));
    }

    private CommonRet<?> initErrorCommonRet() {
        CommonRet<?> commonRet = new CommonRet<>();
        commonRet.setCode("10000222");
        commonRet.setMessage("Sorry, you do not have permission here.");
        return commonRet;
    }

    @PostConstruct
    private void init(){

        for (AccessEvent event : AccessEvent.values()){
            String key = prefix + event.getName();
            String prop = config.getProperty(key,null);

            if (StringUtil.isEmpty(prop)){
                continue;
            }

            whiteListMap.put(key,JsonUtils.toObj(prop,WhiteListDo.class));
        }

        config.addChangeListener(changeEvent -> {
            for (String key : changeEvent.changedKeys()){

                if (!key.startsWith(prefix)){
                    return;
                }

                log.info("[access control] access key changed, key : {}.",key);

                ConfigChange change = changeEvent.getChange(key);

                whiteListMap.put(key, JsonUtils.toObj(change.getNewValue(),WhiteListDo.class));
            }
        });
    }
}
