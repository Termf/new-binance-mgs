package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSON;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.security.EncryptionUtils;
import com.binance.mgs.account.account.dto.AbnormalActionDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.enums.EnumErrorLogType;
import com.binance.risk.api.RiskDeviceBlackListApi;
import com.binance.risk.api.RiskIpBlackListApi;
import com.binance.risk.vo.blacklist.IpAddressRequestVo;
import com.binance.risk.vo.device.blacklist.request.RiskDeviceBlackListRequest;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class RiskHelper extends BaseHelper {

    @Resource
    private ErrorLogToMqHelper errorLogToMqHelper;
    @Resource
    private RiskDeviceBlackListApi riskDeviceBlackListApi;
    @Resource
    private RiskIpBlackListApi riskIpBlackListApi;

//    @Value("${pnk.redis.addresses}")
//    private String redisAddresses;
    // @Value("${site.secret}")
    // private String SITE_SECRET;
    // @Value("${site.verify.url}")
    // private String SITE_VERIFY_URL;
    // private static final String SECRET_PARAM = "secret";
    // private static final String RESPONSE_PARAM = "response";

    private static final int FORGET_PSW_EXPIRED_TIME = 60 * 60 * 24;
    private static final String FORGET_PWS_FORBIDDEN_CACHE_PREFIX = "request:user:forgetpsw:";

    private Set<String> ipWhiteListSet = new HashSet<>();

//    private static LoadingCache<String, Integer> forgetPswForbiddenCache;

//    @PostConstruct
//    private void init() {
//        // 保持原样，不然新旧接口不一致导致数据会翻倍，旧接口停用后再改造
//        forgetPswForbiddenCache = CacheBuilder.newBuilder().redisAddresses(redisAddresses).redisPassword(null)
//                .expireAfterAccess(1, TimeUnit.MINUTES).prefix(FORGET_PWS_FORBIDDEN_CACHE_PREFIX).build();
//    }

    public void checkForgetPswLimits(String email) {
        String ipAddress = WebUtils.getRequestIp();
        int forgetPswLimit = NumberUtils.toInt(sysConfigHelper.getCodeByDisplayName("forget_psw_limits"), 5);
        String forgetPswIpWhitelist = sysConfigHelper.getCodeByDisplayName("forget_psw_ip_whitelist");
        if (StringUtils.isNotBlank(forgetPswIpWhitelist)) {
            String[] ips = forgetPswIpWhitelist.split(",");
            Collections.addAll(ipWhiteListSet, ips);
        }

        if (!CollectionUtils.isEmpty(ipWhiteListSet) && ipWhiteListSet.contains(ipAddress)) {
            return;
        }

//        String times = forgetPswForbiddenCache.getRaw(ipAddress);
//        Long count = forgetPswForbiddenCache.incr(ipAddress);
//        if (count > 1) {
//            AbnormalActionDto actionDto = new AbnormalActionDto();
//            actionDto.setIpAddress(ipAddress);
//            actionDto.setEmail(email);
//            actionDto.setType(EnumErrorLogType.FFP);
//            errorLogToMqHelper.sendAbnormalActionToRisk(actionDto, count);
//        }
//        if (times == null) {
//            forgetPswForbiddenCache.expireRaw(ipAddress, FORGET_PSW_EXPIRED_TIME);
//        } else if (count > forgetPswLimit) {
//            log.info("exceededForgetPswLimits IP:{}", ipAddress);
//            throw new BusinessException(MgsErrorCode.FORGET_PWD_OVER_LIMIT);
//        }

    }

    /**
     * 当该ip当日被不同用户频繁注册时，发mq到风控
     *
     * @param userId 用户id
     * @param email 用户邮箱
     */
    public void frequentRegisterSendMq(Long userId, String email) {
        String ipAddress = WebUtils.getRequestIp();
        try {
            AbnormalActionDto abnormalActionDto = new AbnormalActionDto();
            abnormalActionDto.setUserId(userId);
            abnormalActionDto.setEmail(email);
            abnormalActionDto.setIpAddress(ipAddress);
            abnormalActionDto.setType(EnumErrorLogType.FR);
            errorLogToMqHelper.sendToRisk(abnormalActionDto);
        } catch (Exception e) {
            log.error("frequentRegisterLog方法出错，userId:{},ip:{},error:{}", userId, ipAddress, e);
        }
    }


    /**
     * 当该ip当日被不同用户频繁解绑谷歌验证或手机时，发mq到风控
     *
     * @param userId
     */
    public void frequentUnfaSendMq(Long userId, String email, EnumErrorLogType type) {
        String ipAddress = WebUtils.getRequestIp();
        try {
            AbnormalActionDto abnormalActionDto = new AbnormalActionDto();
            abnormalActionDto.setUserId(userId);
            abnormalActionDto.setEmail(email);
            abnormalActionDto.setIpAddress(ipAddress);
            abnormalActionDto.setType(type);
            abnormalActionDto.setClientType(getClientType());
            errorLogToMqHelper.sendToRisk(abnormalActionDto);
        } catch (Exception e) {
            log.error("frequentUnfaSendMq方法出错，userId:{},ip:{},error:{}", userId, ipAddress, e);
        }
    }

    /**
     * 当该ip当日被不同用户频繁授权设备时，发mq到风控
     * 
     * @param userId
     * @param email
     * @return
     */
    public void frequentGrantDeviceSendMq(Long userId, String email) {
        String ipAddress = WebUtils.getRequestIp();
        try {
            AbnormalActionDto abnormalActionDto = new AbnormalActionDto();
            abnormalActionDto.setUserId(userId);
            abnormalActionDto.setEmail(email);
            abnormalActionDto.setIpAddress(ipAddress);
            abnormalActionDto.setType(EnumErrorLogType.FGD);
            abnormalActionDto.setClientType(getClientType());
            errorLogToMqHelper.sendToRisk(abnormalActionDto);
        } catch (Exception e) {
            log.error(String.format("frequentGrantDeviceSendMq方法出错，email:%s,ip:%s", email, ipAddress), e);
        }
    }


    public boolean isBlackDevice(String deviceInfo, String clientType, String ip) {
        if (!StringUtils.equalsIgnoreCase(clientType, "web")) {
            // 非web端直接忽略不校验
            return false;
        }
        // 默认非黑名单
        boolean ret = false;
        // 处理数据前后多余的空格、引号
        deviceInfo = RegExUtils.replaceAll(deviceInfo, "\\r|\\n|\\\\s", "");
        deviceInfo = EncryptionUtils.base64DecodeToString(deviceInfo, Charsets.UTF_8);
        HashMap<String, Object> deviceMap = JSON.parseObject(deviceInfo, HashMap.class);
        RiskDeviceBlackListRequest request = new RiskDeviceBlackListRequest();
        request.setLoginIp(ip);
        request.setClientType(clientType);
        String fingerprint = (String) deviceMap.get("fingerprint");
        if (StringUtils.isBlank(fingerprint)) {
            throw new BusinessException(GeneralCode.USER_DEVICE_ERROR);
        }
        request.setFingerprint(fingerprint);
        try {
            APIResponse<Boolean> apiResponse = riskDeviceBlackListApi.isExistBlacklist(getInstance(request));
            ret = apiResponse.getData();
        } catch (Exception e) {
            log.warn("isBlackDevice方法出错，request = {}", JSON.toJSONString(request), e);
        }
        return ret;
    }

    public boolean isBlackIp(String ip) {
        // 默认非黑名单
        boolean ret = false;
        IpAddressRequestVo request = new IpAddressRequestVo();
        request.setIpAddress(ip);
        try {
            APIResponse<Boolean> apiResponse = riskIpBlackListApi.isExistBlacklist(getInstance(request));
            ret = apiResponse.getData();
        } catch (Exception e) {
            log.warn("isBlackIp方法出错，request = {}", JSON.toJSONString(request), e);
        }
        return ret;
    }
}
