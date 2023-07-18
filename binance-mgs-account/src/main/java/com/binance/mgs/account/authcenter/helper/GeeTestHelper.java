package com.binance.mgs.account.authcenter.helper;

import com.binance.mgs.account.util.RedisSlidingWindow;
import com.binance.platform.mgs.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

@Slf4j
@Component
public class GeeTestHelper {

    public static final String REGISTER = "register";
    public static final String LOGIN = "login";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Value("${geeTest.register.limit:2}")
    private Long registerLimit;
    @Value("${geeTest.register.windowInSecond:3600}")
    private Long registerWindowSize;
    @Value("${geeTest.login.limit:2}")
    private Long loginLimit;
    @Value("${geeTest.login.windowInSecond:3600}")
    private Long loginWindowSize;

    private RedisSlidingWindow register;
    private RedisSlidingWindow login;

    @PostConstruct
    private void init() {
        register = new RedisSlidingWindow(redisTemplate, registerLimit, registerWindowSize, TimeUnit.SECONDS);
        login = new RedisSlidingWindow(redisTemplate, loginLimit, loginWindowSize, TimeUnit.SECONDS);
    }
    @Async("securityAsync")
    public void increaseRegister(String ip) {
        increase(register, ip, REGISTER);
    }
    @Async("securityAsync")
    public void increaseLogin(String ip) {
        increase(login, ip, LOGIN);
    }

    public boolean checkRegister(String ip) {
        return check(register, ip, REGISTER);
    }

    public boolean checkLogin(String ip) {
        return check(login, ip, LOGIN);
    }

    private void increase(RedisSlidingWindow client, String ip, String action) {
        try {
            client.increase(buildKey(action).apply(ip));
        } catch (Exception e) {
            log.error(StringUtil.format("GeeTestHelper increase err! ip: {}", ip), e);
        }
    }

    private boolean check(RedisSlidingWindow client, String ip, String action) {
        boolean overLimit = false;
        try {
            overLimit = client.checkOverLimit(buildKey(action).apply(ip));
        } catch (Exception e) {
            log.error(StringUtil.format("GeeTestHelper check err! ip: {}", ip), e);
        }
        return overLimit;
    }

    private UnaryOperator<String> buildKey(String action) {
        return ip -> "GeeTestHelper:{action}:{ip}".replace("{action}", action).replace("{ip}", ip);
    }

}
