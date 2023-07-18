package com.binance.mgs.account.account.helper;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.antibot.CaptchaExceptionCounterMetrics;
import com.binance.accountmonitorcenter.event.metrics.antibot.CaptchaHealthCounterMetrics;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.BitUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.enums.CaptchaType;
import com.binance.mgs.account.config.GoogleRecaptchaConfig;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.business.captcha.config.GeetestConfig;
import com.binance.security.antibot.api.AntiBotApi;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Component
public class CaptchaHealthHelper extends AccountBaseAction {

    @Value("${gt.verify.timeout:500}")
    private Integer gtVerifyTimeOut;
    @Value("${bCaptcha.verify.timeout:500}")
    private Integer bCaptchaVerifyTimeOut;
    @Value("${google.recaptcha.score.timeout:200}")
    private Integer googleRecaptchaTimeOut;

    @Value("${anti.bot.new.captcha.captchaType:bCAPTCHA,reCAPTCHA,gt,bCAPTCHA2}")
    private String[] captchaTypeConfigs;
    @Value("${captcha.health.check.switch:true}")
    private boolean healthCheckSwitch;
    @Value("${captcha.health.time.window:300}")
    private int timeOutWindow;
    @Value("${captcha.health.window.period:3600}")
    private int slideWindowPeriod;
    @Value("${captcha.health.timeout.alert.count:50}")
    private long alertCount;
    @Value("${captcha.health.check.interval:300}")
    private int intervalSecond;

    private long status; // 记录不同captcha的状态
    private static final Map<String, Boolean> CAPTCHA_STATUS_MAP = Maps.newConcurrentMap(); // 与status等价，只不过map用的时候更方便直观

    private volatile boolean scheduled; // 定时检查的任务应该只被调度一次

    @Autowired
    private GeetestConfig geetestConfig;
    @Autowired
    private GoogleRecaptchaConfig googleRecaptchaConfig;
    @Autowired
    private AntiBotApi antiBotApi;

    @Autowired
    @Qualifier("gtExecutor")
    private ExecutorService gtExecutor;

    @Autowired
    @Qualifier("bCaptchaExecutor")
    private ExecutorService bCaptchaExecutor;

    @Autowired
    @Qualifier("reCaptchaExecutor")
    private ExecutorService reCaptchaExecutor;

    @Autowired
    protected MetricsEventPublisher monitorMetricsEventPublisher;

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("captcha-record-timeout-task-%d").build());
    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("captcha-health-check-task-%d"));

    @ApolloConfigChangeListener
    public void onChange(ConfigChangeEvent changeEvent) {
        for (String changedKey : changeEvent.changedKeys()) {
            if (changedKey.startsWith("anti.bot.new.captcha.captchaType")) {
                ConfigChange change = changeEvent.getChange(changedKey);
                String newValue = StringUtils.defaultString(change.getNewValue());
                String[] newCaptchaTypeConfigs = newValue.split(",");

                log.info("captcha status before refresh status={} CAPTCHA_STATUS_MAP={}", status, StringUtils.join(CAPTCHA_STATUS_MAP));
                CAPTCHA_STATUS_MAP.clear();
                for (String captchaType : newCaptchaTypeConfigs) {
                    status = BitUtils.enable(status, CaptchaStatusUtils.captchaStatus(captchaType));
                    CAPTCHA_STATUS_MAP.put(captchaType, true);
                    checkAndSyncCaptchaHealth(captchaType);
                }
                log.info("captcha status refresh status={} CAPTCHA_STATUS_MAP={}", status, StringUtils.join(CAPTCHA_STATUS_MAP));
            }
        }
    }

    @PostConstruct
    private void init() {
        for (String captchaType : captchaTypeConfigs) {
            status = BitUtils.enable(status, CaptchaStatusUtils.captchaStatus(captchaType));
            CAPTCHA_STATUS_MAP.put(captchaType, true);
            log.info("captcha status init status={} CAPTCHA_STATUS_MAP={}", status, StringUtils.join(CAPTCHA_STATUS_MAP));
        }
    }

    /**
     * 出现到超时，唤醒定时检测
     */
    private void tryWakeUpSchedule(HttpServletRequest servletRequest) {
        if (scheduled) {
            return;
        }

        if (healthCheckSwitch) {
            synchronized (this) {
                if (!scheduled) {
                    scheduled = true;
                    for (String captchaType : captchaTypeConfigs) {
                        status = BitUtils.enable(status, CaptchaStatusUtils.captchaStatus(captchaType));
                        CAPTCHA_STATUS_MAP.put(captchaType, true);
                    }

                    log.info("should only scheduled once !!!");
                    scheduledExecutor.scheduleAtFixedRate(() -> {
                        if (healthCheckSwitch) {
                            WebUtils.setCurrentHttpRequest(servletRequest);
                            log.info("pingCaptcha and check health");
                            for (String captchaType : captchaTypeConfigs) {
                                pingCaptcha(captchaType);
                                removeOutOfWindow(captchaType);
                                checkAndSyncCaptchaHealth(captchaType);
                            }
                        }
                    }, 30, intervalSecond, TimeUnit.SECONDS);
                }
            }
        }
    }

    /**
     * 记录超时人机超时，并触发health-check检查
     *
     * @param captchaType
     */
    public void recordCaptchaException(CaptchaType captchaType, Throwable throwable) {
        tryWakeUpSchedule(WebUtils.getHttpServletRequest());
        singleThreadExecutor.execute(() -> {
            try {
                if (throwable instanceof BusinessException) {
                    monitorMetricsEventPublisher.publish(CaptchaExceptionCounterMetrics.builder().exception("BusinessException").captchaType(captchaType.name()).build());
                } else {
                    if (throwable instanceof TimeoutException) {
                        monitorMetricsEventPublisher.publish(CaptchaExceptionCounterMetrics.builder().exception("TimeoutException").captchaType(captchaType.name()).build());
                    } else {
                        monitorMetricsEventPublisher.publish(CaptchaExceptionCounterMetrics.builder().exception("UnExpectedException").captchaType(captchaType.name()).build());
                    }
                    String key = CacheConstant.ACCOUNT_DDOS_CAPTCHA_HEALTH_PREFIX + ":" + captchaType.name();

                    removeOutOfWindow(captchaType.name());
                    ShardingRedisCacheUtils.zadd(key, "ts:" + System.currentTimeMillis(), System.currentTimeMillis()); // zset计数
                    ShardingRedisCacheUtils.expire(key, slideWindowPeriod, TimeUnit.SECONDS);

                    checkAndSyncCaptchaHealth(captchaType.name());
                }
            } catch (Exception e) {
                log.warn("recordCaptchaException occur exception", e);
            }
        });
    }

    private void removeOutOfWindow(String captchaType) {
        String key = CacheConstant.ACCOUNT_DDOS_CAPTCHA_HEALTH_PREFIX + ":" + captchaType;
        long curTs = System.currentTimeMillis();
        long maxScore = curTs - TimeUnit.SECONDS.toMillis(timeOutWindow);
        ShardingRedisCacheUtils.zremRangeByScore(key, 0, maxScore);//按score清除过期成员
    }

    private void checkAndSyncCaptchaHealth(String captchaType) {
        try {
            String key = CacheConstant.ACCOUNT_DDOS_CAPTCHA_HEALTH_PREFIX + ":" + captchaType;
            Long size = ShardingRedisCacheUtils.zCard(key);
            boolean isDown = Optional.ofNullable(size).orElse(0L).compareTo(alertCount) >= 0;

            log.info("captcha health check window size={},alterCount={},status={},captchaType={},captchaStatus={}", size, alertCount, status, captchaType, BitUtils.isEnable(status, CaptchaStatusUtils.captchaStatus(captchaType)));
            if (isDown) {
                if (BitUtils.isEnable(status, CaptchaStatusUtils.captchaStatus(captchaType))) {
                    status = BitUtils.disable(status, CaptchaStatusUtils.captchaStatus(captchaType));
                    refreshCaptchaStatusMap();
                }
            } else {
                if (!BitUtils.isEnable(status, CaptchaStatusUtils.captchaStatus(captchaType))) {
                    status = BitUtils.enable(status, CaptchaStatusUtils.captchaStatus(captchaType));
                    refreshCaptchaStatusMap();
                }
            }
            monitorMetricsEventPublisher.publish(CaptchaHealthCounterMetrics.builder().captchaName(captchaType).captchaHealth(isDown ? "down" : "up").build());
        } catch (Exception e) {
            log.info("checkAndSyncCaptchaHealth {} exception", captchaType, e);
        }
    }

    private void refreshCaptchaStatusMap() {
        log.info("refreshCaptchaStatusMap before {}", StringUtils.join(CAPTCHA_STATUS_MAP));
        for (String captchaType : captchaTypeConfigs) {
            if (BitUtils.isEnable(status, CaptchaStatusUtils.captchaStatus(captchaType))) {
                CAPTCHA_STATUS_MAP.put(captchaType, true);
            } else {
                CAPTCHA_STATUS_MAP.put(captchaType, false);
            }
        }
        log.info("refreshCaptchaStatusMap after {}", StringUtils.join(CAPTCHA_STATUS_MAP));
    }

    public String getCaptcha(String captchaType) {
        if (!healthCheckSwitch) {
            return captchaType;
        }

        if (BooleanUtils.isTrue(CAPTCHA_STATUS_MAP.get(captchaType))) {
            return captchaType;
        }
        log.info("getCaptcha candidate captchaType {} is down", captchaType);
        return backUpCaptcha();
    }

    public String backUpCaptcha() {
        if (!healthCheckSwitch) {
            return captchaTypeConfigs[1];
        }

        // 优先级按照配置的顺序，哪个可用就用哪个
        for (String captchaType : captchaTypeConfigs) {
            if (BooleanUtils.isTrue(CAPTCHA_STATUS_MAP.get(captchaType))) {
                // reCAPTCHA兼容性问题，只有web端可用
                if (captchaType.equals(CaptchaType.reCAPTCHA.name()) && !isFromWeb()) {
                    continue;
                }
                return captchaType;
            }
        }
        log.info("backUpCaptcha return random");
        return "random";
    }

    private boolean isFromWeb() {
        TerminalEnum terminalEnum = WebUtils.getTerminal();
        if (terminalEnum != null) {
            return terminalEnum == TerminalEnum.WEB;
        }
        return false;
    }

    public String reCaptchaFirst() {
        if (!healthCheckSwitch) {
            return CaptchaType.reCAPTCHA.name();
        }

        if (CaptchaStatusUtils.reCaptchaUp(status)) {
            return CaptchaType.reCAPTCHA.name();
        }
        return backUpCaptcha();
    }

    public static class CaptchaStatusUtils {
        public static final long gt = 1L;
        public static final long bCAPTCHA = 2L;
        public static final long reCAPTCHA = 4L;
        public static final long bCAPTCHA2 = 8L;

        public static boolean gtUp(Long status) {
            return BitUtils.isEnable(status, CaptchaStatusUtils.gt);
        }

        public static boolean bCaptchaUp(Long status) {
            return BitUtils.isEnable(status, CaptchaStatusUtils.bCAPTCHA);
        }

        public static boolean reCaptchaUp(Long status) {
            return BitUtils.isEnable(status, CaptchaStatusUtils.reCAPTCHA);
        }

        public static boolean bCaptcha2Up(Long status) {
            return BitUtils.isEnable(status, CaptchaStatusUtils.bCAPTCHA2);
        }

        private static long captchaStatus(String captchaType) {
            switch (captchaType) {
                case "gt":
                    return gt;
                case "bCAPTCHA":
                    return bCAPTCHA;
                case "reCAPTCHA":
                    return reCAPTCHA;
                case "bCAPTCHA2":
                    return bCAPTCHA2;
                default:
                    return 0L;
            }
        }
    }

    /**
     * 在人机down的时候开始ping，用来确认什么时候恢复
     */
    public void pingCaptcha(String captchaType) {
        switch (captchaType) {
            case "gt":
                if (!CaptchaStatusUtils.gtUp(status)) {
                    new GeetestHelper().mockValidate(geetestConfig, gtVerifyTimeOut, gtExecutor, this);
                }
                return;
            case "bCAPTCHA":
                if (!CaptchaStatusUtils.bCaptchaUp(status)) {
                    new BCaptchaHelper().mockValidate(bCaptchaVerifyTimeOut, antiBotApi, bCaptchaExecutor, this);
                }
                return;
            case "reCAPTCHA":
                if (!CaptchaStatusUtils.reCaptchaUp(status)) {
                    new GoogleRecaptchaHelper().mockValidate(googleRecaptchaTimeOut, googleRecaptchaConfig, reCaptchaExecutor, this);
                }
                return;
            case "bCAPTCHA2":
                if (!CaptchaStatusUtils.bCaptcha2Up(status)) {
                    new BCaptchaHelper().mockValidateV2(bCaptchaVerifyTimeOut, antiBotApi, bCaptchaExecutor, this);
                }
                return;
            default:
        }
    }
}
