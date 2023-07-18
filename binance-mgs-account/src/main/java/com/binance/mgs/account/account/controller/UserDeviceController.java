package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserDeviceApi;
import com.binance.account.api.UserSecurityLogApi;
import com.binance.account.common.query.SearchResult;
import com.binance.account.vo.device.request.UserDeviceListRequest;
import com.binance.account.vo.device.response.UserDeviceVo;
import com.binance.account.vo.security.request.UserSecurityRequest;
import com.binance.account.vo.security.response.GetUserSecurityLogResponse;
import com.binance.accountdevicequery.api.UserDeviceQueryApi;
import com.binance.accountdevicequery.vo.constants.UserDeviceQueryVo;
import com.binance.accountdevicequery.vo.request.UserDeviceListQueryRequest;
import com.binance.accountlog.api.AccountLogUserSecurityLogApi;
import com.binance.authcenter.api.AuthApi;
import com.binance.authcenter.vo.LoginDeviceResponse;
import com.binance.authcenter.vo.UserIdRequest;
import com.binance.master.enums.OrderByEnum;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.account.vo.DeleteDeviceArg;
import com.binance.mgs.account.account.vo.ListUserDeviceLogArg;
import com.binance.mgs.account.account.vo.LoginDeviceRet;
import com.binance.mgs.account.account.vo.PageUserDeviceLogArg;
import com.binance.mgs.account.account.vo.UserDeviceRet;
import com.binance.mgs.account.account.vo.UserSecurityLogRet;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.util.Ip2LocationSwitchUtils;
import com.binance.mgs.business.account.vo.LocationInfo;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonPageArg;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.ListTransformUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/private/account/user-device")
@Slf4j
public class UserDeviceController extends BaseAction {

    @Resource
    private UserSecurityLogApi userSecurityLogApi;
    @Resource
    private AccountLogUserSecurityLogApi accountLogUserSecurityLogApi;
    @Resource
    private UserDeviceApi userDeviceApi;
    @Resource
    private UserDeviceQueryApi userDeviceQueryApi;
    @Resource
    private UserDeviceHelper userDeviceHelper;
    @Resource
    private AuthApi authApi;
    @Resource
    private AuthHelper authHelper;


    @Value("${user-security-log.accountlog.switch:false}")
    private Boolean accountlogSwitch;

    @Value("${user.device.migration.to.device.query.switch:false}")
    private Boolean userDeviceMigration2QuerySwitch;

    /**
     *
     * 获取用户设备的登录log
     *
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping("/log")
    public CommonRet<List<UserSecurityLogRet>> listUserDeviceLog(@RequestBody @Validated ListUserDeviceLogArg arg) throws Exception {
        CommonRet<List<UserSecurityLogRet>> ret = new CommonRet<>();
        GetUserSecurityLogResponse getUserSecurityLogResponse;
        if (accountlogSwitch){
            com.binance.accountlog.vo.security.request.UserSecurityRequest userSecurityRequest = new com.binance.accountlog.vo.security.request.UserSecurityRequest();
            userSecurityRequest.setUserId(getUserId());
            userSecurityRequest.setDevicePk(arg.getDevicePk());
            userSecurityRequest.setOperateType("login");
            userSecurityRequest.setOrder(OrderByEnum.DESC);
            userSecurityRequest.setSort("id");
            APIResponse<com.binance.accountlog.vo.security.response.GetUserSecurityLogResponse> apiResponse = accountLogUserSecurityLogApi.getLogPage(getInstance(userSecurityRequest));
            checkResponse(apiResponse);
            getUserSecurityLogResponse = CopyBeanUtils.copy(apiResponse.getData(), GetUserSecurityLogResponse.class);
        } else {
            UserSecurityRequest userSecurityRequest = new UserSecurityRequest();
            userSecurityRequest.setUserId(getUserId());
            userSecurityRequest.setDevicePk(arg.getDevicePk());
            userSecurityRequest.setOperateType("login");
            userSecurityRequest.setOrder(OrderByEnum.DESC);
            userSecurityRequest.setSort("id");
            APIResponse<GetUserSecurityLogResponse> apiResponse = userSecurityLogApi.getLogPage(getInstance(userSecurityRequest));
            checkResponse(apiResponse);
            getUserSecurityLogResponse = apiResponse.getData();
        }
        // 返回正常，则对api返回的数据进行裁剪
        if (getUserSecurityLogResponse != null && !CollectionUtils.isEmpty(getUserSecurityLogResponse.getResult())) {
            ret.setData(ListTransformUtil.transform(getUserSecurityLogResponse.getResult(), UserSecurityLogRet.class));
        }
        return ret;
    }

    /**
     *
     * 分页获取用户设备的登录log
     *
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping("/log/page")
    public CommonPageRet<UserSecurityLogRet> pageUserDeviceLog(@RequestBody @Validated PageUserDeviceLogArg arg) throws Exception {
        CommonPageRet<UserSecurityLogRet> ret = new CommonPageRet<>();

        GetUserSecurityLogResponse getUserSecurityLogResponse;
        if (accountlogSwitch){
            com.binance.accountlog.vo.security.request.UserSecurityRequest userSecurityRequest = new com.binance.accountlog.vo.security.request.UserSecurityRequest();
            userSecurityRequest.setUserId(getUserId());
            userSecurityRequest.setDevicePk(arg.getDevicePk());
            userSecurityRequest.setOperateType("login");
            userSecurityRequest.setOrder(OrderByEnum.DESC);
            userSecurityRequest.setSort("id");
            userSecurityRequest.setLimit(arg.getRows());
            userSecurityRequest.setOffset((arg.getPage() - 1) * arg.getRows());
            APIResponse<com.binance.accountlog.vo.security.response.GetUserSecurityLogResponse> apiResponse = accountLogUserSecurityLogApi.getLogPage(getInstance(userSecurityRequest));
            checkResponse(apiResponse);
            getUserSecurityLogResponse = CopyBeanUtils.copy(apiResponse.getData(), GetUserSecurityLogResponse.class);
        } else {
            UserSecurityRequest userSecurityRequest = new UserSecurityRequest();
            userSecurityRequest.setUserId(getUserId());
            userSecurityRequest.setDevicePk(arg.getDevicePk());
            userSecurityRequest.setOperateType("login");
            userSecurityRequest.setOrder(OrderByEnum.DESC);
            userSecurityRequest.setSort("id");
            userSecurityRequest.setLimit(arg.getRows());
            userSecurityRequest.setOffset((arg.getPage() - 1) * arg.getRows());
            APIResponse<GetUserSecurityLogResponse> apiResponse = userSecurityLogApi.getLogPage(getInstance(userSecurityRequest));
            checkResponse(apiResponse);
            getUserSecurityLogResponse = apiResponse.getData();
        }
        // 返回正常，则对api返回的数据进行裁剪
        if (getUserSecurityLogResponse != null && !CollectionUtils.isEmpty(getUserSecurityLogResponse.getResult())) {
            ret.setData(ListTransformUtil.transform(getUserSecurityLogResponse.getResult(), UserSecurityLogRet.class));
            ret.setTotal(getUserSecurityLogResponse.getCount());
            // LOCATION 格式要求需要重新获取。
            Map<String, LocationInfo> ipLocCache = Maps.newHashMap();
            ret.getData().stream().forEach(log -> {
                LocationInfo locResult = ipLocCache.get(log.getIp());
                if (locResult == null) {
                    locResult = Ip2LocationSwitchUtils.getDetail(log.getIp());
                    ipLocCache.put(log.getIp(), locResult);
                }
                if (locResult != null) {
                    log.setIpLocation(locResult.getCity() + ", " + locResult.getCountryShort());
                }
            });

        }
        return ret;
    }

    /**
     *
     * 删除设备
     *
     * @param deleteDeviceArg
     * @return
     */
    @PostMapping("/delete")
    @UserOperation(name = "删除设备", eventName = "deleteDevice", requestKeys = {"devicePk", "deviceId"},
            requestKeyDisplayNames = {"devicePk", "deviceId"}, responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<Void> deleteDevice(HttpServletRequest request, HttpServletResponse response,
            @RequestBody @Validated DeleteDeviceArg deleteDeviceArg) throws Exception {
        userDeviceHelper.deleteUserDevice(request, deleteDeviceArg.getDevicePk(), deleteDeviceArg.getDeviceId());
        return new CommonRet<>();
    }

    /**
     *
     * 获取设备列表
     *
     * @return
     */
    @PostMapping("/list")
    public CommonRet<List<UserDeviceRet>> listUserDevices() {
        if (userDeviceMigration2QuerySwitch) {
            return listUserDevicesInUserDeviceQuery();
        } else {
            return listUserDevicesInUserDevice();
        }
    }

    /**
     *  获取设备列表，从account获取
     *
     * @return {@link CommonRet}<{@link List}<{@link UserDeviceRet}>>
     */
    private CommonRet<List<UserDeviceRet>> listUserDevicesInUserDevice(){
        UserDeviceListRequest userDeviceListRequest = new UserDeviceListRequest();
        userDeviceListRequest.setUserId(getUserId());
        userDeviceListRequest.setExcludeSource("withdraw");
        APIResponse<List<UserDeviceVo>> apiResponse = userDeviceApi.listDevice(getInstance(userDeviceListRequest));
        checkResponse(apiResponse);

        CommonRet<List<UserDeviceRet>> ret = new CommonRet<>();
        if (!CollectionUtils.isEmpty(apiResponse.getData())) {
            List<UserDeviceRet> data =
                    apiResponse.getData().parallelStream().map(convertDevice).filter(Objects::nonNull).collect(Collectors.toList());
            // 判断哪些设备当前处于登录状态
            final Map<String, LoginDeviceResponse> validLoginDevice = authHelper.getValidLoginDevice();
            data.forEach(e -> {
                LoginDeviceResponse loginDeviceResponse = validLoginDevice.get(e.getDeviceName());
                if (loginDeviceResponse != null) {
                    e.setClientType(loginDeviceResponse.getClientType());
                }
            });
            ret.setData(data);
        }
        return ret;
    }

    /**
     *  获取设备列表，从device-query获取
     *
     * @return {@link CommonRet}<{@link List}<{@link UserDeviceRet}>>
     */
    private CommonRet<List<UserDeviceRet>> listUserDevicesInUserDeviceQuery(){
        UserDeviceListQueryRequest userDeviceListQueryRequest = new UserDeviceListQueryRequest();
        userDeviceListQueryRequest.setUserId(getUserId());
        userDeviceListQueryRequest.setExcludeSource("withdraw");
        APIResponse<List<UserDeviceQueryVo>> apiResponse = userDeviceQueryApi.listDevice(getInstance(userDeviceListQueryRequest));
        checkResponse(apiResponse);
        CommonRet<List<UserDeviceRet>> ret = new CommonRet<>();
        if (!CollectionUtils.isEmpty(apiResponse.getData())) {
            List<UserDeviceRet> data =
                    apiResponse.getData().parallelStream().map(convertDeviceQuery).filter(Objects::nonNull).collect(Collectors.toList());
            // 判断哪些设备当前处于登录状态
            final Map<String, LoginDeviceResponse> validLoginDevice = authHelper.getValidLoginDevice();
            data.forEach(e -> {
                LoginDeviceResponse loginDeviceResponse = validLoginDevice.get(e.getDeviceName());
                if (loginDeviceResponse != null) {
                    e.setClientType(loginDeviceResponse.getClientType());
                }
            });
            ret.setData(data);
        }
        return ret;
    }

    /**
     *
     * 获取设备列表
     *
     * @return
     */
    @PostMapping("/page")
    public CommonPageRet<UserDeviceRet> pageUserDevices(@RequestBody @Validated CommonPageArg arg) {
        if (userDeviceMigration2QuerySwitch) {
            return pageUserDevicesInUserDeviceQuery(arg);
        } else {
            return pageUserDevicesInUserDevice(arg);
        }
    }

    /**
     * 分页获取设备列表，从account获取
     *
     * @param arg 参数
     * @return {@link CommonPageRet}<{@link UserDeviceRet}>
     */
    private CommonPageRet<UserDeviceRet> pageUserDevicesInUserDevice(CommonPageArg arg){
        UserDeviceListRequest userDeviceListRequest = new UserDeviceListRequest();
        userDeviceListRequest.setUserId(getUserId());
        userDeviceListRequest.setLimit(arg.getRows());
        userDeviceListRequest.setExcludeSource("withdraw");
        userDeviceListRequest.setOffset((arg.getPage() - 1) * arg.getRows());

        APIResponse<SearchResult<UserDeviceVo>> apiResponse = userDeviceApi.pageDevice(getInstance(userDeviceListRequest));
        checkResponse(apiResponse);
        CommonPageRet<UserDeviceRet> ret = new CommonPageRet<>();
        if (apiResponse.getData() != null && !CollectionUtils.isEmpty(apiResponse.getData().getRows())) {
            List<UserDeviceRet> data =
                    apiResponse.getData().getRows().parallelStream().map(convertDevice).filter(Objects::nonNull).collect(Collectors.toList());
            // 判断哪些设备当前处于登录状态
            final Map<String, LoginDeviceResponse> validLoginDevice = authHelper.getValidLoginDevice();
            data.forEach(e -> {
                LoginDeviceResponse loginDeviceResponse = validLoginDevice.get(e.getDeviceName());
                if (loginDeviceResponse != null) {
                    e.setClientType(loginDeviceResponse.getClientType());
                }
            });
            ret.setData(data);
            ret.setTotal(apiResponse.getData().getTotal());
        }
        return ret;
    }

    /**
     * 分页获取设备列表，从device-query获取
     *
     * @param arg 参数
     * @return {@link CommonPageRet}<{@link UserDeviceRet}>
     */
    private CommonPageRet<UserDeviceRet> pageUserDevicesInUserDeviceQuery(CommonPageArg arg){
        UserDeviceListQueryRequest userDeviceListQueryRequest = new UserDeviceListQueryRequest();
        userDeviceListQueryRequest.setUserId(getUserId());
        userDeviceListQueryRequest.setLimit(arg.getRows());
        userDeviceListQueryRequest.setExcludeSource("withdraw");
        userDeviceListQueryRequest.setOffset((arg.getPage() - 1) * arg.getRows());

        APIResponse<com.binance.master.commons.SearchResult<UserDeviceQueryVo>> apiResponse = userDeviceQueryApi.pageDevice(getInstance(userDeviceListQueryRequest));
        checkResponse(apiResponse);
        CommonPageRet<UserDeviceRet> ret = new CommonPageRet<>();
        if (apiResponse.getData() != null && !CollectionUtils.isEmpty(apiResponse.getData().getRows())) {
            List<UserDeviceRet> data =
                    apiResponse.getData().getRows().parallelStream().map(convertDeviceQuery).filter(Objects::nonNull).collect(Collectors.toList());
            // 判断哪些设备当前处于登录状态
            final Map<String, LoginDeviceResponse> validLoginDevice = authHelper.getValidLoginDevice();
            data.forEach(e -> {
                LoginDeviceResponse loginDeviceResponse = validLoginDevice.get(e.getDeviceName());
                if (loginDeviceResponse != null) {
                    e.setClientType(loginDeviceResponse.getClientType());
                }
            });
            ret.setData(data);
            ret.setTotal(apiResponse.getData().getTotal());
        }
        return ret;
    }

    private Function<UserDeviceVo, UserDeviceRet> convertDevice = device -> {
        try {
            // 提现设备，不展示给用户
            if ("withdraw".equalsIgnoreCase(device.getSource())) {
                return null;
            }
            UserDeviceRet vo = new UserDeviceRet();
            vo.setId(device.getId().toString());
            vo.setUserId(device.getUserId());
            vo.setAgentType(device.getAgentType());
            vo.setSource(device.getSource());
            if (device.getActiveTime() != null) {
                vo.setLoginTime(device.getActiveTime().getTime());
            }

            JSONObject content = JSON.parseObject(device.getContent());
            String loginIp = content.getString(UserDeviceRet.LOGIN_IP);
            vo.setLoginIp(loginIp);
            vo.setDeviceName(content.getString(UserDeviceRet.DEVICE_NAME));
            String locationCity = "";
            if (StringUtils.isNotBlank(loginIp)) {
                locationCity = Ip2LocationSwitchUtils.getCountryCity(loginIp);
                locationCity = StringUtils.replace(locationCity, ", Province of China", "");
            }
            if (StringUtils.isBlank(locationCity)) {
                locationCity = content.getString(UserDeviceRet.LOCATION_CITY);
            }
            vo.setLocationCity(locationCity);
            return vo;
        } catch (Exception e) {
            log.error("transform UserDevice to UserDeviceVO error, data:{}", JSON.toJSONString(device), e);
            return null;
        }
    };

    private Function<UserDeviceQueryVo, UserDeviceRet> convertDeviceQuery = device -> {
        try {
            // 提现设备，不展示给用户
            if ("withdraw".equalsIgnoreCase(device.getSource())) {
                return null;
            }
            UserDeviceRet vo = new UserDeviceRet();
            vo.setId(device.getId().toString());
            vo.setUserId(device.getUserId());
            vo.setAgentType(device.getAgentType());
            vo.setSource(device.getSource());
            if (device.getActiveTime() != null) {
                vo.setLoginTime(device.getActiveTime().getTime());
            }

            JSONObject content = JSON.parseObject(device.getContent());
            String loginIp = content.getString(UserDeviceRet.LOGIN_IP);
            vo.setLoginIp(loginIp);
            vo.setDeviceName(content.getString(UserDeviceRet.DEVICE_NAME));
            String locationCity = "";
            if (StringUtils.isNotBlank(loginIp)) {
                locationCity = Ip2LocationSwitchUtils.getCountryCity(loginIp);
                locationCity = StringUtils.replace(locationCity, ", Province of China", "");
            }
            if (StringUtils.isBlank(locationCity)) {
                locationCity = content.getString(UserDeviceRet.LOCATION_CITY);
            }
            vo.setLocationCity(locationCity);
            return vo;
        } catch (Exception e) {
            log.error("transform UserDeviceQueryVo to UserDeviceRet error, data:{}", JSON.toJSONString(device), e);
            return null;
        }
    };

    /**
     *
     * 获取处于登录状态的设备列表
     *
     * @return
     */
    @PostMapping("/valid-login-device")
    public CommonRet<List<LoginDeviceRet>> getValidLoginDevice() {
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(getUserId());
        APIResponse<List<LoginDeviceResponse>> apiResponse = authApi.listAllLoginDevice(getInstance(userIdRequest));
        checkResponse(apiResponse);

        CommonRet<List<LoginDeviceRet>> ret = new CommonRet<>();
        if (!CollectionUtils.isEmpty(apiResponse.getData())) {
            List<LoginDeviceRet> data = apiResponse.getData().parallelStream().map(device -> {
                LoginDeviceRet vo = CopyBeanUtils.copy(device, LoginDeviceRet.class);
                try {
                    String locationCity = "";
                    if (StringUtils.isNotBlank(device.getLoginIp())) {
                        locationCity = Ip2LocationSwitchUtils.getCountryCity(device.getLoginIp());
                        locationCity = StringUtils.replace(locationCity, ", Province of China", "");
                    }
                    vo.setLocationCity(locationCity);
                } catch (Exception e) {
                    log.error("transform UserDevice to UserDeviceVO error, data:{}", JSON.toJSONString(device), e);
                }
                return vo;
            }).collect(Collectors.toList());
            // 按照登录时间排序
            data.sort(Comparator.comparing(LoginDeviceRet::getLoginTime).reversed());
            ret.setData(data);
        }
        return ret;
    }

}
