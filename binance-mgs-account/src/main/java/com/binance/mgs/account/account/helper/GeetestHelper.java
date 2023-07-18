package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSONObject;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.error.BusinessException;
import com.binance.master.lib.GeetestLib2;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.version.VersionHelper;
import com.binance.mgs.account.account.enums.CaptchaType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.account.vo.GtCodeRet;
import com.binance.platform.mgs.business.captcha.config.GeetestConfig;
import com.binance.platform.mgs.business.captcha.dto.GeeTestSessionDto;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GeetestHelper {

    private static final int SERIAL_NO_TIMEOUT = 30 * 60;

    @Value("${gt.code.threshold:100}")
    private int gtCodeThreshold;

    @Value("${captcha.timeout.default.pass:true}")
    private boolean captchaTimeOutPass;
    @Value("${gt.verify.timeout:500}")
    private Integer gtVerifyTimeOut;
    @Value("${geetest.fallback:false}")
    private boolean geeTestFallback;
    @Value("${captcha.exception.block.switch:true}")
    private boolean blockIfException;
    @Value("${ddos.sharding.redis.migrate.write.switch:false}")
    private boolean ddosShardingRedisMigrateSwitch;
    @Value("${ddos.sharding.redis.migrate.read.switch:false}")
    private boolean ddosShardingRedisMigrateReadSwitch;

    @Autowired
    private BaseHelper baseHelper;
    @Autowired
    private SysConfigHelper sysConfigHelper;
    @Autowired
    private CaptchaHealthHelper captchaHealthHelper;
    @Autowired
    private GeetestConfig geetestConfig;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Autowired
    @Qualifier("gtExecutor")
    private ExecutorService gtExecutor;
    @Autowired
    private com.binance.mgs.account.account.helper.VersionHelper versionHelper;

    public boolean validateCodeByGeetest(ValidateCodeArg validateCodeArg) {
        String gtId = null;
        try {
            String switchStr = sysConfigHelper.getCodeByDisplayName("gee_test_switch");
            if ("off".equals(switchStr)) {
                log.info("validateCodeByGeetest geeTest closed");
                throw new BusinessException("geeTest closed.");
            }

            GeetestLib2 gtSdk;
            HttpServletRequest request = WebUtils.getHttpServletRequest();
            String clientType = BaseHelper.getClientType(request);
            String versionCode = VersionHelper.getVersion(request);
            if (StringUtils.equalsIgnoreCase("web", clientType) || (StringUtils.equalsIgnoreCase("h5", clientType))
                    || (StringUtils.equalsIgnoreCase("mac", clientType)) || (StringUtils.startsWithIgnoreCase(clientType, "pc"))) {

                log.info(String.format("validate geeTest channel2, clientType:%s, versionCode:%s", clientType, versionCode));

                gtSdk = GeetestLib2.getInstance(geetestConfig.getGeetestId2(), geetestConfig.getGeetestKey2(), GeetestConfig.isnewfailback());
            } else {

                log.info(String.format("validate geeTest channel1, clientType:%s, versionCode:%s", clientType, versionCode));

                gtSdk = GeetestLib2.getInstance(geetestConfig.getGeetestId(), geetestConfig.getGeetestKey(), GeetestConfig.isnewfailback());
            }

            String challenge = validateCodeArg.getGeetestChallenge();
            String validate = validateCodeArg.getGeetestValidate();
            String secCode = validateCodeArg.getGeetestSecCode();

            gtId = StringUtils.defaultIfBlank(BaseHelper.getCookieValue(request, Constant.COOKIE_GT_ID), validateCodeArg.getGtId());
            if (StringUtils.isBlank(gtId)) {
                log.info("cookie gtid is null,userId={}", baseHelper.getUserIdStr());
                return false;
            }
            GeeTestSessionDto geeTestSessionDto;
            if (ddosShardingRedisMigrateReadSwitch) {
                geeTestSessionDto = ShardingRedisCacheUtils.get(CacheConstant.getGeeTestSerialNo(gtId), GeeTestSessionDto.class);
                if (geeTestSessionDto == null) {
                    geeTestSessionDto = RedisCacheUtils.get(CacheKey.getGeeTestSerialNo(gtId), GeeTestSessionDto.class);
                }
            } else {
                geeTestSessionDto = RedisCacheUtils.get(CacheKey.getGeeTestSerialNo(gtId), GeeTestSessionDto.class);
            }
            if (geeTestSessionDto == null) {
                log.info("geeTestSessionDto is expire ,gtId={},userId={}", gtId, baseHelper.getUserIdStr());
                return false;
            }
            // 从session中获取gt-server状态
            Integer gtServerStatusCode = geeTestSessionDto.getServerStatus();
            // 从session中获取userIp
            String userIp = geeTestSessionDto.getIp();
            int gtResult = 0;
            if (gtServerStatusCode != null && gtServerStatusCode == 1) {
                // gt-server正常，向gt-server进行二次验证
                Future<Integer> future = gtExecutor.submit(() -> gtSdk.enhancedValidateRequest(challenge, validate, secCode, userIp));
                gtResult = future.get(gtVerifyTimeOut, TimeUnit.MILLISECONDS);
            } else {
                // gt-server非正常情况下，进行failback模式验证
                if (geeTestFallback) {
                    Future<Integer> future = gtExecutor.submit(() -> gtSdk.failbackValidateRequest(challenge, validate, secCode));
                    gtResult = future.get(gtVerifyTimeOut, TimeUnit.MILLISECONDS);
                    log.error("极验处于宕机模式，后端忽略验证,gtServerStatusCode={}", gtServerStatusCode);
                } else {
                    log.error("极验处于宕机状态,后端验证不通过,gtServerStatusCode={}", gtServerStatusCode);
                }
            }

            // 验证失败
            if (gtResult != 1) {
                log.info("validateCodeByGeetest failed, userIpFromSession={}, gtVersion={}", userIp, gtSdk.getVersionInfo());
                return false;
            }
            ddosCacheSeviceHelper.setVerifyResult(gtId, true);
            return true;
        } catch (TimeoutException e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.gt, e);
            return captchaTimeOutPass;
        } catch (Exception e) {
            if (StringUtils.isNotBlank(gtId)) {
                ddosCacheSeviceHelper.setVerifyResult(gtId, false);
            }
            captchaHealthHelper.recordCaptchaException(CaptchaType.gt, e);
            return blockIfException;
        }
    }

    public void mockValidate(GeetestConfig geetestConfig, Integer gtVerifyTimeOut, ExecutorService gtExecutor, CaptchaHealthHelper captchaHealthHelper) {
        try {
            log.info("mockValidate gt");
            String envFlag = EnvUtil.getEnvFlag();
            boolean isQa = EnvUtil.isQa();
            boolean isProd = EnvUtil.isProd();

            GeetestLib2 gtSdk = GeetestLib2.getInstance(geetestConfig.getGeetestId2(), geetestConfig.getGeetestKey2(), GeetestConfig.isnewfailback());
            Future<Integer> future = gtExecutor.submit(() -> {
                int result = 0;
                if (isProd) {
                    result = gtSdk.enhancedValidateRequest("9f80d05605d4ce1d20ddfa39113308f0", "523486550a91788aba0a51522cd2fb74", "523486550a91788aba0a51522cd2fb74|jordan", "");
                }

                if (isQa) {
                    result = gtSdk.enhancedValidateRequest("4f6219a786ce9ebf669abf66fa54ebb1", "9199cd753db136a506a89d2af326c1e0", "9199cd753db136a506a89d2af326c1e0|jordan", "");
                }
                log.info("mockValidate gt resp env={} result={}", envFlag, result);
                return result;
            });
            future.get(gtVerifyTimeOut, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.gt, e);
        } catch (Exception e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.gt, e);
            log.warn("mockValidate gt exception", e);
        }
    }

    @Async("gtCodeExecutorAsync")
    public Future<CommonRet<GtCodeRet>> getGtCodeAsync(String ip, String serialNo, HttpServletRequest request) {
        WebUtils.setCurrentHttpRequest(request);
        StopWatch watch = new StopWatch();
        watch.start("init");
        CommonRet<GtCodeRet> ret = new CommonRet<>();
        GeetestLib2 gtSdk = null;

        try {
            String clientType = BaseHelper.getClientType(request);

            String versionCode = versionHelper.getVersion();
            if (StringUtils.equalsIgnoreCase("web", clientType) || (StringUtils.equalsIgnoreCase("h5", clientType))
                    || (StringUtils.equalsIgnoreCase("mac", clientType))
                    || (StringUtils.startsWithIgnoreCase(clientType, "pc"))) {
                log.info(String.format("getGtCode geeTest channel2, client:%s, versionCode:%s", clientType, versionCode));
                gtSdk = GeetestLib2.getInstance(geetestConfig.getGeetestId2(), geetestConfig.getGeetestKey2(),
                        GeetestConfig.isnewfailback());
            } else {

                log.info(String.format("getGtCode geeTest channel1, client:%s, versionCode:%s", clientType, versionCode));
                gtSdk = GeetestLib2.getInstance(geetestConfig.getGeetestId(), geetestConfig.getGeetestKey(),
                        GeetestConfig.isnewfailback());
            }
            watch.stop();
            watch.start("preProcess");
            // 使用userIp作为极验的userId进行验证预处理
            int serverStatus = gtSdk.preProcess(ip);
            if (serverStatus != 1) {
                log.info("geetest serverStatus error:", serverStatus);
//            throw new BusinessException(MgsErrorCode.GEETEST_ERROR);
            }
            watch.stop();
            watch.start("end");
            GeeTestSessionDto dto = new GeeTestSessionDto();
            dto.setIp(ip);
            dto.setServerStatus(serverStatus);
            if (ddosShardingRedisMigrateSwitch) {
                ShardingRedisCacheUtils.set(CacheConstant.getGeeTestSerialNo(serialNo), dto, SERIAL_NO_TIMEOUT);
            } else {
                ShardingRedisCacheUtils.set(CacheConstant.getGeeTestSerialNo(serialNo), dto, SERIAL_NO_TIMEOUT);
                RedisCacheUtils.set(CacheKey.getGeeTestSerialNo(serialNo), dto, SERIAL_NO_TIMEOUT);
            }
            String gtResp = gtSdk.getResponseStr();
            JSONObject json = JSONObject.parseObject(gtResp);
            GtCodeRet data = new GtCodeRet();
            data.setChallenge(json.getString("challenge"));
            data.setGt(json.getString("gt"));
            data.setGtId(serialNo);
            ret.setData(data);
            return new AsyncResult(ret);
        } finally {
            if (gtSdk != null) {
                gtSdk.reset();
            }
            watch.stop();
            if (watch.getTotalTimeMillis() > gtCodeThreshold) {
                log.warn("gt-code too slow  {}", watch.prettyPrint());
            }
        }
    }
}
