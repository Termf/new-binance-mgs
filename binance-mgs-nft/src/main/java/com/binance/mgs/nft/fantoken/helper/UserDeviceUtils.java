package com.binance.mgs.nft.fantoken.helper;

import com.alibaba.fastjson.JSON;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.security.EncryptionUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.utils.StringUtil;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RegExUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * user-device 工具类
 **/
@Slf4j
public class UserDeviceUtils extends BaseHelper {
    public static final String HEADER_DEVICE_INFO = "device-info";

    public static final String PARAM_DEVICE_INFO = "device_info";

    public static final String BNC_UUID = "bnc-uuid";

    /**
     * 安全获取设备信息
     * @param request
     * @return 异常情况返回空map
     */
    public static Map<String, String> safeBuildDeviceInfo(HttpServletRequest request) {
        String deviceInfoStr = request.getHeader(HEADER_DEVICE_INFO);
        if (StringUtils.isBlank(deviceInfoStr)) {
            deviceInfoStr = request.getParameter(PARAM_DEVICE_INFO);
        }

        if (StringUtils.isBlank(deviceInfoStr)) {
            log.warn("found no device info");
        }
        HashMap<String, String> map = buildDeviceInfo(request, deviceInfoStr);
        return map == null ? new HashMap<>(0) : map;
    }

    private static HashMap<String, String> buildDeviceInfo(HttpServletRequest request, String deviceInfoStr) {
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
}