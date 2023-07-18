package com.binance.mgs.account.account.helper;

import com.binance.accountdefensecenter.api.LimitApi;
import com.binance.accountdefensecenter.core.enums.CallAppEnum;
import com.binance.accountdefensecenter.vo.limit.DdosLimitRequest;
import com.binance.accountdefensecenter.vo.limit.DdosLimitResponse;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.antibot.BanIpPerformanceCounterMetrics;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.enums.UserPerformanceEnum;
import com.binance.mgs.account.ddos.DdosOperationEnum;
import com.binance.mgs.account.ddos.IpLimitDdosCommonRequest;
import com.binance.mgs.account.security.helper.AntiBotHelper;
import com.binance.mgs.account.security.vo.CaptchaValidateInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.utils.DomainUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 为了ddos攻击专门的redis集群
 *
 */
@Slf4j
@Getter
@Component
public class DdosCacheSeviceHelper {

    @Getter
    private static final String GT_FORBIDDEN_CACHE_PREFIX = "request:gt:forbidden:";
    public static final String ACCOUNT_REGISTER_EMAIL_KEY = "account:register:email";

    @Value("${ddos.batch.send.size:10}")
    private int ddosBatchSendSize;
    @Value("${anti.bot.session.banIp.timeout:1800}")
    private long sessionTimeOutSecondsAfterBanIp;
    @Value("${sessionId.bind.banIp.status.switch:false}")
    private boolean bindBanIpStatus;

    @Autowired
    private DdosShardingRedisCacheHelper ddosShardingRedisCacheHelper;
    @Autowired
    private LimitApi limitApi;
    @Autowired
    private BaseHelper baseHelper;

    private RedisTemplate<String, Object> accountDdosRedisTemplate=AccountDdosRedisHelper.getInstance();

    @Autowired
    private MetricsEventPublisher metricsEventPublisher;

    /**
     * 设置email缓存时间
     */
    public void setRegisterEmailCache(String email) {
        try{
            String key = ACCOUNT_REGISTER_EMAIL_KEY+email;
            log.info("setRegisterEmailCache key={}",key);
            accountDdosRedisTemplate.opsForValue().set(key, "1",1, TimeUnit.HOURS);
        }catch (Exception e){
            log.error("setRegisterEmailCache error",e);
        }

    }

    /**
     * 获取email cache
     *
     */
    public String getEmailCacheWithOperationEnum(String email, DdosOperationEnum ddosOperationEnum) {
        try{
            String key = ACCOUNT_REGISTER_EMAIL_KEY+ddosOperationEnum.getKey()+email;
            String value = (String)accountDdosRedisTemplate.opsForValue().get(key);
            log.info("getEmailCacheWithOperationEnum key={}，value={}",key,value);
            return value;
        }catch (Exception e){
            log.error("getEmailCacheWithOperationEnum error",e);
            return null;
        }

    }

    /**
     * 设置email缓存时间
     */
    public void setEmailCacheWithOperationEnum(String email, DdosOperationEnum ddosOperationEnum) {
        try{
            String key = ACCOUNT_REGISTER_EMAIL_KEY+ddosOperationEnum.getKey()+email;
            log.info("setEmailCacheWithOperationEnum key={}",key);
            accountDdosRedisTemplate.opsForValue().set(key, "1",1, TimeUnit.HOURS);
        }catch (Exception e){
            log.error("setEmailCacheWithOperationEnum error",e);
        }

    }

    /**
     * 设置email缓存时间
     */
    public void setEmailOrMobileCacheWithOperationEnum(String email,String mobile, DdosOperationEnum ddosOperationEnum) {
        try{
            if(StringUtils.isNotBlank(email)){
                email=email.trim().toLowerCase();
            }else{
                email="";
            }
            if(StringUtils.isNotBlank(mobile)){
                mobile=mobile.trim().toLowerCase();
            }else{
                mobile="";
            }
            String key = ACCOUNT_REGISTER_EMAIL_KEY+ddosOperationEnum.getKey()+email+mobile;
            log.info("setEmailCacheWithOperationEnum key={}",key);
            accountDdosRedisTemplate.opsForValue().set(key, "1",1, TimeUnit.HOURS);
        }catch (Exception e){
            log.error("setEmailCacheWithOperationEnum error",e);
        }

    }


    /**
     * 获取email cache
     *
     */
    public String getEmailOrMobileCacheWithOperationEnum(String email,String mobile,DdosOperationEnum ddosOperationEnum) {
        try{
            if(StringUtils.isNotBlank(email)){
                email=email.trim().toLowerCase();
            }else{
                email="";
            }
            if(StringUtils.isNotBlank(mobile)){
                mobile=mobile.trim().toLowerCase();
            }else{
                mobile="";
            }
            String key = ACCOUNT_REGISTER_EMAIL_KEY+ddosOperationEnum.getKey()+email+mobile;
            String value = (String)accountDdosRedisTemplate.opsForValue().get(key);
            log.info("getEmailCacheWithOperationEnum key={}，value={}",key,value);
            return value;
        }catch (Exception e){
            log.error("getEmailCacheWithOperationEnum error",e);
            return null;
        }

    }

    /**
     * 统计人机token使用次数
     * 一般最多5条命
     */
    public Long incrGtForbiddenCache(String token) {
        return ddosShardingRedisCacheHelper.incrCaptchaTokenCount(token);
    }

    /**
     * 设置人机验证是否通过
     */
    public void setVerifyResult(String token, Boolean isSuccess) {
        ddosShardingRedisCacheHelper.setVerifyResult(token, isSuccess);
    }

    /**
     * 获取人机验证是否通过
     */
    public boolean getVerifyResult(String gtId) {
        return ddosShardingRedisCacheHelper.getVerifyResult(gtId);
    }

    public Long ipVisitCount(String ip, String type) {
        return ddosShardingRedisCacheHelper.ipVisitCount(ip, type);
    }

    /**
     * 超过上限则不在计数
     * @param ip
     * @param upperLimit
     * @return
     */
    public Long ipVisitCountWithUpperLimit(String ip, long upperLimit) {
        return ddosShardingRedisCacheHelper.ipVisitCountWithUpperLimit(ip, upperLimit);
    }

    public void banIp(String ip) {
        log.info("ban DDos attack Ip : {}", ip);
        sendIpLimitDdos(ip);
    }

    public Boolean isDdosAttach(String email, String mobile, String ip, DdosOperationEnum ddosOperationEnum) {
        String cacheValue = getEmailOrMobileCacheWithOperationEnum(email, mobile, ddosOperationEnum);
        if (StringUtils.isBlank(cacheValue)) {
            sendIpLimitDdos(ip);
            return true;
        }
        return false;
    }

    public Long subAccountActionCount(Long parentUserId, String action, int expireTime) {
        return ddosShardingRedisCacheHelper.subAccountActionCount(parentUserId, action, expireTime);
    }

    public boolean banIpIfNecessary(UserPerformanceEnum performance, String ip, int limit, String logMsg) {
        switch (performance) {
            case NORMAL_REGISTER:
            case NORMAL_FORGET_PASS:
            case NORMAL_GET_2FA_LIST:
            case NOT_EXIST:
            case CAPTCHA_ILLEGAL:
            case CAPTCHA_VERIFY_ERROR:
            case SESSION_ID_ILLEGAL:
            case ILLEGAL_VERIFY_INFO:
            case ILLEGAL_USER_INFO:
                long visitCount = ipVisitCount(ip, performance.name());
                if (visitCount > limit) {
                    banIp(ip);
                    String sessionId = AntiBotHelper.getThreadLocalSessionId();
                    markSessionIdBanIp(sessionId);
                    log.warn("banIp performance={}, ip={} count={} limit={},sessionId={}, msg=[{}]", performance, ip, visitCount, limit, sessionId, logMsg);
                    metricsEventPublisher.publish(BanIpPerformanceCounterMetrics.builder().performance(performance.name()).build());
                    return true;
                }
                break;
            default:
                log.warn("default performance={}", performance);
        }
        return false;
    }

    public void setValidateInfo(String key, CaptchaValidateInfo value, long time) {
        ddosShardingRedisCacheHelper.setValidateInfo(key, value, time);
    }

    public CaptchaValidateInfo getValidateInfo(String key) {
        return ddosShardingRedisCacheHelper.getValidateInfo(key);
    }

    public String getValidateId(String key) {
        return Objects.isNull(ddosShardingRedisCacheHelper.getValidateInfo(key)) ? null : ddosShardingRedisCacheHelper.getValidateInfo(key).getValidateId();
    }

    private void markSessionIdBanIp(String sessionId) {
        if (bindBanIpStatus){
            if (StringUtils.isNotBlank(sessionId)) {
                CaptchaValidateInfo info = getValidateInfo(sessionId);
                if (info != null) {
                    info.setStatus(AntiBotHelper.getVALIDATE_STATUS_BAN_IP());
                    setValidateInfo(sessionId, info, sessionTimeOutSecondsAfterBanIp);
                    log.info("sessionId {} bind banIp status", sessionId);
                }
            }
        }
    }

    private void sendIpLimitDdos(String ip) {
        sendIpLimitDdosDefenseCenter(createIpLimitDdosCommonRequest(ip));
    }

    private void sendIpLimitDdosDefenseCenter(IpLimitDdosCommonRequest ipLimitDdosCommonRequest) {
        try {
            APIResponse<DdosLimitResponse> response = limitApi.limit(APIRequest.instance(DdosLimitRequest.builder()
                    .ip(ipLimitDdosCommonRequest.getIp())
                    .app(ipLimitDdosCommonRequest.getApp())
                    .domain(ipLimitDdosCommonRequest.getDomain())
                    .reason(ipLimitDdosCommonRequest.getReason())
                    .build()));
            baseHelper.checkResponse(response);
        } catch (Exception e) {
            log.error(String.format("Exception while sending DdosLimitRequest [%s]", ipLimitDdosCommonRequest), e);
        }
    }

    private IpLimitDdosCommonRequest createIpLimitDdosCommonRequest(String ip) {
        IpLimitDdosCommonRequest ipLimitDdosCommonRequest = new IpLimitDdosCommonRequest();
        ipLimitDdosCommonRequest.setIp(ip);
        ipLimitDdosCommonRequest.setBatchSendSize(ddosBatchSendSize);
        String domain = DomainUtils.getDomain();
        ipLimitDdosCommonRequest.setDomain(domain);
        ipLimitDdosCommonRequest.setApp(CallAppEnum.BINANCE_MGS_ACCOUNT.name());
        ipLimitDdosCommonRequest.setReason("Too many request");
        return ipLimitDdosCommonRequest;
    }
}
