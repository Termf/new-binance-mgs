package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserDeviceApi;
import com.binance.account.vo.device.request.UserDeviceDeleteRequest;
import com.binance.account.vo.security.request.IdLongRequest;
import com.binance.accountdevicequery.api.UserDeviceQueryApi;
import com.binance.accountdevicequery.vo.request.IdLongQueryRequest;
import com.binance.authcenter.api.AuthApi;
import com.binance.authcenter.vo.LogoutRequest;
import com.binance.authcenter.vo.LogoutResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.security.EncryptionUtils;
import com.binance.mgs.account.account.vo.UserDeviceRet;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.StringUtil;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * user-device 工具类
 **/
@Component
@Slf4j
public class UserDeviceHelper extends BaseHelper {
    public static final String HEADER_DEVICE_INFO = "device-info";

    public static final String PARAM_DEVICE_INFO = "device_info";

    public static final String BNC_UUID = "bnc-uuid";
    @Resource
    private UserDeviceApi userDeviceApi;
    @Resource
    private UserDeviceQueryApi userDeviceQueryApi;
    @Resource
    private AuthApi authApi;
    @Resource
    private AuthHelper authHelper;

    @Value("${user.device.migration.to.device.query.switch:false}")
    private Boolean userDeviceMigration2QuerySwitch;

    public void deleteUserDevice(HttpServletRequest request, Long devicePk, String deviceId) throws Exception {
        UserDeviceRet device = getUserDevice(devicePk);
        if (device == null) {
            throw new BusinessException(MgsErrorCode.DEVICE_NOT_EXISTS);
        }
        if (!device.getUserId().equals(getUserId())) {
            log.warn("Someone try to delete other's device, userId:{}, devicePk:{}, deviceId:{}", getUserId(), devicePk,
                    deviceId);
            throw new BusinessException(MgsErrorCode.WRONE_DEVICE);
        }

        UserDeviceDeleteRequest userDeviceDeleteRequest = new UserDeviceDeleteRequest();
        userDeviceDeleteRequest.setDevicePk(devicePk);
        userDeviceDeleteRequest.setSource("user");
        userDeviceDeleteRequest.setMemo("用户删除设备");
        userDeviceDeleteRequest.setUserId(getUserId());
        APIResponse deleteDeviceResponse = userDeviceApi.deleteDevice(getInstance(userDeviceDeleteRequest));
        checkResponse(deleteDeviceResponse);
        log.info("device={}", JsonUtils.toJsonNotNullKey(device));
        // 跨设备类型删除的时候，强制踢出该设备类型对应的token
        if (!StringUtils.equalsIgnoreCase(getClientType(), device.getAgentType())) {
            log.info("forceLogout");
            forceLogout(device.getAgentType());
        }
        // 判断删除的设备是否为当前登录的设备
        JSONObject content = JSON.parseObject(device.getContent());
        if (StringUtils.equals(deviceId, content.getString(UserDeviceRet.DEVICE_ID))) {
            log.info("delete and logout");
            authHelper.logout(request);
        }
    }

    private void forceLogout(String clientType) throws Exception {
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setUserId(String.valueOf(getUserId()));
        logoutRequest.setClientType(clientType);
        APIResponse<LogoutResponse> apiResponse = authApi.logout(getInstance(logoutRequest));
        log.info("delete device logout:{}", apiResponse);
    }

    private UserDeviceRet getUserDevice(Long devicePk) {
        if (userDeviceMigration2QuerySwitch) {
            return getUserDeviceInUserDeviceQuery(devicePk);
        } else {
            return getUserDeviceInUserDevice(devicePk);
        }
    }

    /**
     * 获取用户设备信息，在account获取
     *
     * @param devicePk 设备pk
     * @return {@link UserDeviceRet}
     */
    private UserDeviceRet getUserDeviceInUserDevice(Long devicePk) {
        IdLongRequest idLongRequest = new IdLongRequest();
        idLongRequest.setId(devicePk);
        idLongRequest.setUserId(getUserId());
        APIResponse response = userDeviceApi.getDevice(getInstance(idLongRequest));
        checkResponse(response);
        return JSON.parseObject(JSON.toJSONString(response.getData()), UserDeviceRet.class);
    }

    /**
     * 获取用户设备信息，在device-query获取
     *
     * @param devicePk 设备pk
     * @return {@link UserDeviceRet}
     */
    private UserDeviceRet getUserDeviceInUserDeviceQuery(Long devicePk) {
        IdLongQueryRequest idLongQueryRequest = new IdLongQueryRequest();
        idLongQueryRequest.setId(devicePk);
        idLongQueryRequest.setUserId(getUserId());
        APIResponse response = userDeviceQueryApi.getDevice(getInstance(idLongQueryRequest));
        checkResponse(response);
        return JSON.parseObject(JSON.toJSONString(response.getData()), UserDeviceRet.class);
    }

    /**
     * 获取设备信息
     *
     * @param request
     * @param userId
     * @param email
     * @return
     */
    public static HashMap<String, String> buildDeviceInfo(HttpServletRequest request, String userId, String email, String deviceInfoStr) {
        String clientType = getClientType(request);
        String ipAddress = WebUtils.getRequestIp();
        try {
            // 处理数据前后多余的空格、引号
            deviceInfoStr = RegExUtils.replaceAll(deviceInfoStr, "\\r|\\n|\\\\s", "");
            if (EncryptionUtils.isBase64(deviceInfoStr)) {
                deviceInfoStr = EncryptionUtils.base64DecodeToString(deviceInfoStr, Charsets.UTF_8);
            } else {
                log.warn("deviceInfoStr has not been base64 encoded, clientType:{}, deviceInfoStr:{}", clientType, deviceInfoStr);
            }
            deviceInfoStr = StringUtil.filterEmoji(deviceInfoStr);
            if (!StringUtil.isJson(deviceInfoStr)) {
                log.warn("device info is empty or non-json, clientType:{}, deviceInfoStr:{}", clientType, deviceInfoStr);
                return null;
            }

            HashMap<String, String> deviceInfo = JSON.parseObject(deviceInfoStr, HashMap.class);
            // 强制转换所有非String为String
            for (Map.Entry entry : deviceInfo.entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
            deviceInfo.put("login_ip", ipAddress);
            // 取web专属字段
            if ("web".equalsIgnoreCase(clientType) || "h5".equalsIgnoreCase(clientType)) {
                deviceInfo.put("accept", request.getHeader("Accept"));
                deviceInfo.put("content_encoding", request.getHeader("Accept-Encoding"));
                deviceInfo.put("content_lang", request.getHeader("Accept-Language"));
                log.info("Accept-Language: {}", request.getHeader("Accept-Language"));
            }
            if (deviceInfo != null && deviceInfo.get("timezoneOffset") != null) {
                // timezoneOffset 存的居然是int，不转成String，调用会报异常
                deviceInfo.put("timezoneOffset", String.valueOf(deviceInfo.get("timezoneOffset")));
            }

            String bncUuid = request.getHeader(BNC_UUID);
            if (StringUtils.isNotBlank(bncUuid)) {
                log.info("got bnc-uuid from header={}", bncUuid);
                deviceInfo.put(BNC_UUID, bncUuid);
            }

            return deviceInfo;
        } catch (Exception e) {
            log.error("buildDeviceInfo error, deviceInfoStr:{}", deviceInfoStr, e);
            return null;
        }
    }


    public static HashMap<String, String> buildDeviceInfo(HttpServletRequest request, String userId, String email) {
        String deviceInfoStr = request.getHeader(HEADER_DEVICE_INFO);
        if (StringUtils.isBlank(deviceInfoStr)) {
            deviceInfoStr = request.getParameter(PARAM_DEVICE_INFO);
        }

        if (StringUtils.isBlank(deviceInfoStr)) {
            log.warn("found no device info");
        }
        return buildDeviceInfo(request, userId, email, deviceInfoStr);
    }
    
    public String getBncUuid(HttpServletRequest request) {
        String bncUuid = request.getHeader(BNC_UUID);
        return bncUuid;
    }

}
