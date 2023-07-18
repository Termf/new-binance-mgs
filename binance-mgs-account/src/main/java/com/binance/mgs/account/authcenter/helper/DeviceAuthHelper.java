package com.binance.mgs.account.authcenter.helper;

import com.binance.authcenter.vo.CreateDeviceTokenResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.authcenter.dto.DeviceAuthCodeDto;
import com.binance.mgs.account.authcenter.dto.DeviceAuthCodeStatus;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.enums.MgsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DeviceAuthHelper extends BaseHelper {
    public static final int DEVICE_AUTH_CODE_SHOW_LEN = 8;
    @Value("${device.auth.timeout:300}")
    private long deviceAuthCodeTimeout;
    @Value("${device.auth.switch:false}")
    private boolean deviceAuthSwitch;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Resource
    private AuthHelper authHelper;
    @Resource
    private RiskHelper riskHelper;

    /**
     * 保存设备授权码
     *
     * @return
     */
    public void create(String userId, String uuid) {
        if (!deviceAuthSwitch) {
            // 紧急情况可以临时关闭设备授权登录功能
            return;
        }
        String deviceInfo = getAndCheckDeviceInfo();
        DeviceAuthCodeDto dto = new DeviceAuthCodeDto();
        dto.setDeviceInfo(deviceInfo);
        dto.setCreateIp(WebUtils.getRequestIp());
        dto.setCreateClientType(getClientType());
        dto.setDeviceAuthCodeStatus(DeviceAuthCodeStatus.NEW);
        // 绑定临时token
        RedisCacheUtils.set(CacheKey.getDeviceAuthCode(uuid), dto, deviceAuthCodeTimeout);
        // 只有最后一次登录生成的临时token是有效的
        RedisCacheUtils.set(CacheKey.getUserBindDeviceAuthCode(userId), uuid, deviceAuthCodeTimeout);
    }

    /**
     * 设备授权风控校验
     *
     * @param uuid
     * @param dto
     */
    private void checkRisk(String uuid, DeviceAuthCodeDto dto) {
        // 1. 判断登陆设备是否为黑名单
        if (riskHelper.isBlackDevice(dto.getDeviceInfo(), dto.getCreateClientType(), dto.getCreateIp())) {
            log.warn("uuid = {} auth refused cause device is in black list,device={}", uuid, dto.getDeviceInfo());
            throw new BusinessException(MgsErrorCode.BLACK_DEVICE);
        }
        // 2. 判断登陆设备ip是否为黑名单
        if (riskHelper.isBlackIp(dto.getCreateIp())) {
            log.warn("uuid = {} auth refused cause create ip is in black list,ip={}", uuid, dto.getCreateIp());
            throw new BusinessException(MgsErrorCode.BLACK_IP);
        }
        // 3. 判断授权设备ip是否为黑名单
        if (riskHelper.isBlackIp(WebUtils.getRequestIp())) {
            log.warn("uuid = {} auth refused cause create ip is in black list,ip={}", uuid, dto.getCreateIp());
            throw new BusinessException(MgsErrorCode.BLACK_IP);
        }
    }

    private DeviceAuthCodeDto getAndCheckCode(String uuid) {
        DeviceAuthCodeDto dto = RedisCacheUtils.get(CacheKey.getDeviceAuthCode(uuid), DeviceAuthCodeDto.class);
        if (dto == null) {
            throw new BusinessException(AccountMgsErrorCode.DEVICE_AUTH_CODE_IS_EXPIRE);
        }
        return dto;
    }

    /**
     * 授权确认
     *
     * @param userId
     */
    public void auth(String userId) {
        String uuid = RedisCacheUtils.get(CacheKey.getUserBindDeviceAuthCode(userId));
        if (StringUtils.isBlank(uuid)) {
            log.info("userId={} auth device fail,uuid is null", userId);
            return;
        }
        DeviceAuthCodeDto dto = getAndCheckCode(uuid);
        // 风控规则校验
        checkRisk(uuid, dto);
        // 访问authcenter，根据app端token，生成web端的token
        CreateDeviceTokenResponse tokenResponse = authHelper.createByDeviceToken();
        if (tokenResponse != null) {
            dto.setToken(tokenResponse.getToken());
            dto.setCsrfToken(tokenResponse.getCsrfToken());
        } else {
            log.error("设备授权生成token失败");
        }
        dto.setDeviceAuthCodeStatus(DeviceAuthCodeStatus.CONFIRM);
        dto.setAuthIp(WebUtils.getRequestIp());
        RedisCacheUtils.set(CacheKey.getDeviceAuthCode(uuid), dto, deviceAuthCodeTimeout);
        log.info("uuid={},authed", StringUtils.left(uuid, DEVICE_AUTH_CODE_SHOW_LEN));
    }

    /**
     * 设备授权码状态查询
     *
     * @param uuid
     * @return
     */
    public DeviceAuthCodeDto query(String uuid) {
        String deviceInfo = getAndCheckDeviceInfo();
        DeviceAuthCodeDto deviceAuthCodeDto =
                RedisCacheUtils.get(CacheKey.getDeviceAuthCode(uuid), DeviceAuthCodeDto.class);
        if (deviceAuthCodeDto != null) {
            // ip是否匹配
            // if (!StringUtils.equals(WebUtils.getRequestIp(), DeviceAuthCodeDto.getCreateIp())) {
            // throw new BusinessException(MgsErrorCode.IP_NOT_MATCH);
            // }
            // 设备是否匹配
            if (!StringUtils.equals(deviceInfo, deviceAuthCodeDto.getDeviceInfo())) {
                throw new BusinessException(MgsErrorCode.DEVICE_NOT_MATCH);
            }
            // 确认成功后，删除uuid，避免反复使用
            if (deviceAuthCodeDto.getDeviceAuthCodeStatus() == DeviceAuthCodeStatus.CONFIRM) {
                RedisCacheUtils.del(CacheKey.getDeviceAuthCode(uuid));
            }
        }
        return deviceAuthCodeDto;
    }

    public void logDeviceInfo(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceInfo",
                userDeviceHelper.buildDeviceInfo(request, getUserIdStr(), getUserEmail(), getAndCheckDeviceInfo()));
        UserOperationHelper.log(map);
    }
}
