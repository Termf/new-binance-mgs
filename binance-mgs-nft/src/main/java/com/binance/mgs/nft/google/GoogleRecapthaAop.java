package com.binance.mgs.nft.google;

import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.core.redis.RedisCommonConfig;
import com.binance.mgs.nft.core.schedule.ScheduleCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
@Slf4j
public class GoogleRecapthaAop {

    @Autowired
    private BaseHelper baseHelper;
    @Autowired
   private GoogleRecaptchaConfig googleRecaptchaConfig;
    @Autowired
    private ScheduleCacheService scheduleCacheService;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;


    @Around("@annotation(GoogleRecaptha)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        if (!checkRecaptha(point)) {
            return initErrorCommonRet(point);
        }
        return point.proceed();
    }

    private CommonRet initErrorCommonRet(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        GoogleRecaptha annotation = signature.getMethod().getAnnotation(GoogleRecaptha.class);
        CommonRet commonRet = new CommonRet();
        commonRet.setCode("10000222");
        if(annotation != null && StringUtils.isNotBlank(annotation.message())) {
            commonRet.setMessage(annotation.message());
            return commonRet;
        }
        commonRet.setMessage("Error encountered in Google recaptcha validation, please try again later");
        return commonRet;
    }

    private boolean checkRecaptha(ProceedingJoinPoint point) {
        log.info("checkRecapthaHeader = clientType = " + baseHelper.getClientType()
                + ", checkUserId = " + baseHelper.getUserId()
                +  ", checkRecapthaHeaderToken = token = "
                + WebUtils.getHeader("x-nft-checkbot-token")
                + ", sitekey = "
                + WebUtils.getHeader("x-nft-checkbot-sitekey"));
        boolean isApp = StringUtils.equalsAnyIgnoreCase(baseHelper.getClientType(), "ios", "android");
        if(isApp) {
            return Boolean.TRUE;
        }
        // 全局开启或者白名单，尝试走人机
        if ( googleRecaptchaConfig.getCloseGooleRecaptcha() == 1 || googleRecaptchaConfig.getWhiteUserIdList().contains(baseHelper.getUserId())) {

            MethodSignature signature = (MethodSignature) point.getSignature();
            GoogleRecaptha annotation = signature.getMethod().getAnnotation(GoogleRecaptha.class);

            if( !googleRecaptchaConfig.getGoogleRecaptureUrlList().contains(annotation.value()) && !googleRecaptchaConfig.getWhiteUserIdList().contains(baseHelper.getUserId())) {
                return Boolean.TRUE;
            }

            String token = WebUtils.getHeader("x-nft-checkbot-token");
            String sitekey = WebUtils.getHeader("x-nft-checkbot-sitekey");
            if (StringUtils.isBlank(token) || StringUtils.isBlank(sitekey)) {
                String referer = WebUtils.getHeader("referer");
                if(StringUtils.isNotBlank(referer)) {
                    log.info("error google Recaptha url " + referer);
                }
                return Boolean.FALSE;
            }
            if(checkPreCache()) {
                log.info("checkGoogleRecaptcha skip params = " + baseHelper.getUserId());
                return Boolean.TRUE;
            }

            Map<String, String> params = new HashMap<>();
            params.put("token", token);
            params.put("siteKey", sitekey);
            params.put("expectedAction", annotation.value());
            Map<String, Object> parentParam = new HashMap<>();
            parentParam.put("event", params);
            Object googleAuthToken = scheduleCacheService.getGoogleAuthToken();
            if(googleAuthToken == null) {
                googleAuthToken = scheduleCacheService.refreshGoogleAuthToken();
            }
            boolean checkGoogleRecaptcha = GoogleRecaptchaUtils.checkGoogleRecaptcha(parentParam, googleAuthToken == null ? "" : googleAuthToken.toString() , googleRecaptchaConfig.getProjectId(), findScore(annotation.value()),googleRecaptchaConfig.getGooglelogSwitch());
            afterCacheCheck(checkGoogleRecaptcha);
            return checkGoogleRecaptcha;
        }
        return Boolean.TRUE;
    }

    private Double findScore(String value) {

        Map<String, Double> scoreMap = googleRecaptchaConfig.getGoogleRecapthaMapScore();

        if(MapUtils.isEmpty(scoreMap) || scoreMap.get(value)  == null) {
            return googleRecaptchaConfig.getGoogleRecaptchaScoreMin();
        }
        return scoreMap.get(value);
    }

    private void afterCacheCheck(boolean checkGoogleRecaptcha) {
        if( checkWitchOpen() && baseHelper.getUserId() != null && checkGoogleRecaptcha) {
            String key = String.format(RedisCommonConfig.GOOGLE_LIMIT_NEXT_CHECK_PERIOD_KEY, baseHelper.getUserId());
            redisTemplate.opsForValue().set(key,System.currentTimeMillis(), googleRecaptchaConfig.getLimitNextCheckGooglePeriod(), TimeUnit.SECONDS);
        }
    }

    private boolean checkPreCache() {
        if(checkWitchOpen() && baseHelper.getUserId() != null) {
            String key = String.format(RedisCommonConfig.GOOGLE_LIMIT_NEXT_CHECK_PERIOD_KEY, baseHelper.getUserId());
            Object value = redisTemplate.opsForValue().get(key);
            return value != null;
        }
        return Boolean.FALSE;
    }

    private boolean checkWitchOpen() {
         return googleRecaptchaConfig.getCacheGooleRecaptchaSwitch() != null && googleRecaptchaConfig.getCacheGooleRecaptchaSwitch() == 1;
    }


}
