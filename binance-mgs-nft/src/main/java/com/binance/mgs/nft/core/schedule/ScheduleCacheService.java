package com.binance.mgs.nft.core.schedule;

import com.binance.master.utils.JsonUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.mgs.nft.core.redis.RedisCommonConfig;
import com.binance.mgs.nft.google.GoogleRecaptchaConfig;
import com.binance.mgs.nft.google.GoogleRecaptchaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScheduleCacheService {

    @Autowired
    private GoogleRecaptchaConfig googleRecaptchaConfig;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private BaseHelper baseHelper;



    @PostConstruct
    public void loadGoogleRecaptureCode() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(
                        this::refreshGoogleAuthToken, 0, 30, TimeUnit.SECONDS
                );
    }




    public String refreshGoogleAuthToken()  {
        String googleAuthToken = null;
        try {
            String key = String.format(RedisCommonConfig.GOOGLE_LIMIT_REFRESH_TOKEN_KEY,baseHelper.getUserId());
             googleAuthToken = GoogleRecaptchaUtils.getGooleAuthorizationToken(initRecaptchaPluginAuth());
            redisTemplate.opsForValue().set(key,googleAuthToken,30,TimeUnit.SECONDS);
            log.info("refreshGoogleAuthToken " + googleAuthToken);
        } catch (IOException e) {
            log.error("exceptioncode " + googleAuthToken,e);
        }
        return googleAuthToken;
    }

    private String initRecaptchaPluginAuth() {
        Map<String, String> params = new HashMap<>();
        params.put("type", googleRecaptchaConfig.getType());
        params.put("client_id", googleRecaptchaConfig.getClient_id());
        params.put("client_email", googleRecaptchaConfig.getClient_email());
        params.put("private_key", googleRecaptchaConfig.getPrivate_key());
        params.put("private_key_id", googleRecaptchaConfig.getPrivate_key_id());
        params.put("project_id", googleRecaptchaConfig.getProjectId());
        params.put("token_uri", googleRecaptchaConfig.getToken_uri());
        params.put("quota_project_id", googleRecaptchaConfig.getProjectId());
        return JsonUtils.toJsonNotNullKey(params);
    }

    public Object getGoogleAuthToken() {
        String key = String.format(RedisCommonConfig.GOOGLE_LIMIT_REFRESH_TOKEN_KEY,baseHelper.getUserId());
        return redisTemplate.opsForValue().get(key);
    }
}
