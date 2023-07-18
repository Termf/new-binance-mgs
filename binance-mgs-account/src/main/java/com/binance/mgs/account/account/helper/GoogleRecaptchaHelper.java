package com.binance.mgs.account.account.helper;

import com.amazonaws.util.StringInputStream;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.antibot.CaptchaHistogramMetrics;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.utils.HttpClientUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.enums.CaptchaType;
import com.binance.mgs.account.account.vo.google.GoogleRecaptchaResponse;
import com.binance.mgs.account.config.GoogleRecaptchaConfig;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.common.TrackingUtils;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * google人机验证企业版
 */
@Slf4j
@Component
public class GoogleRecaptchaHelper {

    public static final String ACCOUNT_GOOGLE_LIMIT_REFRESH_TOKEN_KEY = "account:google:recaptcha:limit:refresh:token:";
    public static final String GOOGLE_VERIFY_URL = "https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments";

    private RedisTemplate<String, Object> accountMgsRedisTemplate = AccountMgsRedisHelper.getInstance();

    @Autowired
    @Qualifier("reCaptchaExecutor")
    private ExecutorService reCaptchaExecutor;

    @Autowired
    private GoogleRecaptchaConfig googleRecaptchaConfig;
    @Autowired
    private CaptchaHealthHelper captchaHealthHelper;
    @Autowired
    private MetricsEventPublisher metricsEventPublisher;

    @Value("${captcha.timeout.default.pass:true}")
    private boolean captchaTimeOutPass;

    @Value("${google.recaptcha.score.min:0.6}")
    private Double googleRecaptchaScoreMin;

    @Value("${google.recaptcha.score.timeout:200}")
    private Long googleRecaptchaTimeOut;

    @Value("${google.recaptcha.token.cache.seconds:300}")
    private long tokenCacheSeconds;

    public boolean timeOutRecaptchaAssessment(String token, String siteKey, String expectedAction) {
        String traceId = TrackingUtils.getTrace();
        String ip = WebUtils.getRequestIp();
        try {
            Future<Boolean> future = reCaptchaExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    TrackingUtils.saveTrace(traceId);
                    return GoogleRecaptchaHelper.this.recaptchaAssessment(token, siteKey, expectedAction, ip);
                }
            });
            return future.get(googleRecaptchaTimeOut, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.reCAPTCHA, e);
            return captchaTimeOutPass;
        } catch (Exception e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.reCAPTCHA, e);
            log.error("recaptchaAssessment error", e);
            return true;
        }
    }

    /**
     * google人机验证评估 https://cloud.google.com/recaptcha-enterprise/docs/create-assessment
     * 一个token只能调一次，所以在一个流程中需要多次验证时，本地缓存这个成功状态
     *
     * @param token          令牌（前端传递）
     * @param siteKey        与网站/应用关联的reCAPTCHA密钥（前端传递）
     * @param expectedAction 用户发起的操作
     * @return
     */
    public boolean recaptchaAssessment(String token, String siteKey, String expectedAction, String requestIp) {
        log.info("create recaptchaAssessment,ip={}", requestIp);
        String cacheKey = StringUtils.join(":", CacheConstant.ACCOUNT_MGS_RECAPTCHA_ASSESSMENT_PREFIX, token, requestIp, expectedAction);
        Object cacheContent = ShardingRedisCacheUtils.get(cacheKey);
        if (cacheContent != null) {
            log.info("pass checkGoogleRecaptcha from cache");
            return true;
        }

        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("siteKey", siteKey);
        params.put("expectedAction", expectedAction);
        Map<String, Object> eventParam = new HashMap<>();
        eventParam.put("event", params);
        try {
            HashMap<String, String> headerMap = new HashMap<>();
            headerMap.put("Authorization", "Bearer " + Strings.nullToEmpty(getGoogleAuthToken()));
            String content = HttpClientUtils.postJson(String.format(GOOGLE_VERIFY_URL, googleRecaptchaConfig.getProjectId()), eventParam, headerMap);
            GoogleRecaptchaResponse response = JsonUtils.toObj(content, GoogleRecaptchaResponse.class);
            if (response == null) {
                return false;
            }
            log.info("checkGoogleRecaptcha riskAnalysis={},tokenProperties={}", response.getRiskAnalysis(), response.getTokenProperties());
            boolean passAssessment = false;
            Double score = 0.0;
            if (response.getRiskAnalysis() != null && response.getRiskAnalysis().getScore() != null) {
                score = response.getRiskAnalysis().getScore();
                passAssessment = score >= googleRecaptchaScoreMin;
            }
            metricsEventPublisher.publish(CaptchaHistogramMetrics.builder().bizType(expectedAction).captchaType("reCAPTCHA").captchaStatus(passAssessment ? "success" : "fail").number(score).build());
            if (passAssessment) {
                accountMgsRedisTemplate.opsForValue().set(cacheKey, content, tokenCacheSeconds, TimeUnit.SECONDS);
                return true;
            }
        } catch (Exception e) {
            log.error("checkGoogleRecaptcha error params = {},e" + JsonUtils.toJsonNotNullKey(params), e);
        }
        return false;
    }

    private String getGoogleAuthToken() {
        Object googleAuthToken = accountMgsRedisTemplate.opsForValue().get(ACCOUNT_GOOGLE_LIMIT_REFRESH_TOKEN_KEY);
        if (googleAuthToken == null) {
            googleAuthToken = refreshGoogleAuthToken();
        }
        return String.valueOf(googleAuthToken);
    }

    private String refreshGoogleAuthToken() {
        String googleAuthToken = null;
        try {
            String recaptchaPluginAuth = buildRecaptchaPluginAuth();
            googleAuthToken = requestGoogleAuthToken(recaptchaPluginAuth);
            accountMgsRedisTemplate.opsForValue().set(ACCOUNT_GOOGLE_LIMIT_REFRESH_TOKEN_KEY, googleAuthToken, 60, TimeUnit.SECONDS);
            log.info("refreshGoogleAuthToken success");
        } catch (IOException e) {
            log.error("refreshGoogleAuthToken exception ", e);
        }
        return googleAuthToken;
    }

    private String requestGoogleAuthToken(String recaptchaPluginAuth) throws IOException {
        GoogleCredentials scoped = ServiceAccountCredentials.fromStream(new StringInputStream(recaptchaPluginAuth)).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
        AccessToken accessToken = scoped.refreshAccessToken();
        return accessToken.getTokenValue();
    }

    private String buildRecaptchaPluginAuth() {
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

    public void mockValidate(Integer googleRecaptchaTimeOut, GoogleRecaptchaConfig googleRecaptchaConfig, ExecutorService reCaptchaExecutor, CaptchaHealthHelper captchaHealthHelper) {
        log.info("mockValidate reCAPTCHA");
        try {
            Map<String, String> params = new HashMap<>();
            params.put("token", "03AGdBq251-k_YvZpDuHbXnUwG94RaF2PSlDZoQ9xgLjGM4xzJjCBt3cWEvYakZhsLs1EfpPC_g-voc5xe3zDIYq3k9RCjSg17NuJL7FnGAkBoh9yWsJfnplXXoVynxNOJBsHn8z1734MjHBO1Jj3pcBAW710NzmFcGShF4Q4m_mXPMxZ-3gzxALSK8idPuZgy1jGHojFgbj60bUl1RuZa7EU0WlE1hJEPMqVbbxcCeCx3AaRCRknqLdFZ8hYN3jgeCHrMS6zenVkJepNQ_L4IUlICfIlqmF-owv212QRkucabG6r4NEO_EEYv1BL928VRt4qvJwWbgKvEKwtNiiOdb-Q-uBox7NYac1-j1ERbCwZ0b1QHpYQ2GzLy_K9OTSiUCKdFJKlRczjFw528h329-QmOox-mvoTCB_1aGX0jnzV7pjkPff74szOWi9hTEX3Yv-Tsz6QVtszVrFDe-xbgno2RVhWbTzJcsPo4CYPVoqDXpK7y30k3KDvCx4bAB7tgqvT5WvpIX4HEtLI9yTCK22R_vHRZruVZnrv3U9i8XUyPfYPOaNLcbERKCHs3zHCB04ADnIWdoa75qINO35TR78tBtuI_gRF6tz0A7onORNJTN42RWPJN6hkhcRGl2avQzZ-uOOPOSu_u0KThqmhYA8SugTxT0-IW41TtCIn6yPCFx4hibDQAtgqD1vv3i5Sc0aaaJMju20v7X3o2TcAu6bGvgKncaqQx4UoVbbVWyPb9ljhtdcXG1L8nVKawqeiIOgnuz-h8BZFOtixaekX9KM3p6viNX9Ql-MGTfiiaFPkWCIcWxKpWsioCogoxliM1iScRo8Qf9UmOHB-GRENHqEyMqMijeL0LDIv6QIusZYWhrOwXZAFEbr89tZ_X6V8GuaMAXo_B6Zv27NH6xfxXpiPpcN3SgbXklMVvBXJHXuLdu8mHDFeIqc_4f3G2UA3bohpangedfEdURk3ESP7bCL1htD_5sH2DVm7v31zWzvQAzWJLhPuGgMztdKMO3LVacSjOYrTGoH8_gY3gWp950ZCpLeeBFe4EtVnueKax7qEBoEFIhYAkDSTFa1NDux4qPre7EkrLTSeuZecOWv0oDjMdXk1Butej18R4SG_TwX3PydKwZgQAmg_LVee9zhC2aCHpTzg0ZGj5ycHZan61-KoE4n552fOrTNrh84FP3R342hbu3_cQr_ia0y90oCZ8VxNn7ljH3uE_");
            params.put("siteKey", "6LckbvYaAAAAANZTAYZ8rH2KnWqN8VDe8HFbjxJI");
            params.put("expectedAction", "mockTest");
            Map<String, Object> eventParam = new HashMap<>();
            eventParam.put("event", params);

            HashMap<String, String> headerMap = new HashMap<>();
            Map<String, String> pluginOauth = new HashMap<>();
            pluginOauth.put("type", googleRecaptchaConfig.getType());
            pluginOauth.put("client_id", googleRecaptchaConfig.getClient_id());
            pluginOauth.put("client_email", googleRecaptchaConfig.getClient_email());
            pluginOauth.put("private_key", googleRecaptchaConfig.getPrivate_key());
            pluginOauth.put("private_key_id", googleRecaptchaConfig.getPrivate_key_id());
            pluginOauth.put("project_id", googleRecaptchaConfig.getProjectId());
            pluginOauth.put("token_uri", googleRecaptchaConfig.getToken_uri());
            pluginOauth.put("quota_project_id", googleRecaptchaConfig.getProjectId());
            String recaptchaPluginAuth = JsonUtils.toJsonNotNullKey(pluginOauth);

            headerMap.put("Authorization", "Bearer " + Strings.nullToEmpty(requestGoogleAuthToken(recaptchaPluginAuth)));
            Future<Boolean> future = reCaptchaExecutor.submit(() -> {
                String json = HttpClientUtils.postJson(String.format(GOOGLE_VERIFY_URL, googleRecaptchaConfig.getProjectId()), eventParam, headerMap);
                log.info("mockValidate reCAPTCHA resp {}", json);
                return true;
            });
            future.get(googleRecaptchaTimeOut, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.reCAPTCHA, e);
        } catch (Exception e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.reCAPTCHA, e);
            log.error("mockValidate reCAPTCHA exception", e);
        }
    }
}
