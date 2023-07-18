package com.binance.mgs.nft.fantoken.helper;

import com.binance.master.utils.WebUtils;
import com.binance.nft.fantoken.request.FanTokenBaseRequest;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * <h1>FanToken 请求处理工具类</h1>
 * */
@SuppressWarnings("all")
@Component
@RequiredArgsConstructor
public class FanTokenRequestHelper {

    private final BaseHelper baseHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    /**
     * <h2>初始化请求对象</h2>
     * */
    public void initFanTokenBaseRequest(FanTokenBaseRequest request) {

        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        request.setHasKyc(Objects.nonNull(userId) && fanTokenCheckHelper.hasKyc(userId));
        initFanTokenBaseRequestGeneralField(request);
    }

    /**
     * <h2>初始化请求对象</h2>
     * */
    public void initFanTokenBaseRequest(Long userId, FanTokenBaseRequest request) {

        request.setUserId(userId);
        request.setHasKyc(Objects.nonNull(userId) && fanTokenCheckHelper.hasKyc(userId));
        initFanTokenBaseRequestGeneralField(request);
    }

    /**
     * <h2>初始化请求对象</h2>
     * */
    public void initFanTokenBaseRequest(Long userId, Boolean hasKyc, FanTokenBaseRequest request) {

        request.setUserId(userId);
        request.setHasKyc(hasKyc);
        initFanTokenBaseRequestGeneralField(request);
    }

    private void initFanTokenBaseRequestGeneralField(FanTokenBaseRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());

        // 一些合规会使用到的用户设备信息
        request.setFvideoId(WebUtils.getHeader("fvideo-id"));
        request.setIp(WebUtils.getRequestIp());

        Map<String, String> deviceInfo = UserDeviceUtils.safeBuildDeviceInfo(WebUtils.getHttpServletRequest());
        request.setDeviceName(deviceInfo.get("device_name"));
        request.setScreenResolution(deviceInfo.get("screen_resolution"));
        request.setSystemLang(deviceInfo.get("system_lang"));
        request.setSystemVersion(deviceInfo.get("system_version"));
        request.setTimezone(deviceInfo.get("timezone"));
        request.setPlatform(deviceInfo.get("platform"));
    }
}
