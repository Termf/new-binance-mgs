package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserDeviceApi;
import com.binance.account.api.UserOperationLogApi;
import com.binance.account.vo.device.request.UserDeviceListRequest;
import com.binance.account.vo.device.response.UserDeviceVo;
import com.binance.account.vo.operationlog.UserOperationLogVo;
import com.binance.account.vo.security.request.CountTodaysUserOperationLogsRequest;
import com.binance.account.vo.security.request.FindTodaysUserOperationLogsRequest;
import com.binance.account.vo.security.response.UserOperationLogResultResponse;
import com.binance.accountdevicequery.api.UserDeviceQueryApi;
import com.binance.accountdevicequery.vo.constants.UserDeviceQueryVo;
import com.binance.accountdevicequery.vo.request.UserDeviceListQueryRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.dto.AbnormalActionDto;
import com.binance.mgs.account.account.dto.AbnormalActionMessageDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.MQHelper;
import com.binance.platform.mgs.enums.EnumErrorLogType;
import com.binance.userbigdata.api.UserOperationLogESApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class ErrorLogToMqHelper extends BaseHelper {
//    private static final String FORGET_PWS_FORBIDDEN_CACHE_PREFIX = "request:user:forgetpsw:";
//    private static final int FORGET_PSW_EXPIRED_TIME = 60 * 60 * 24;
//    private static LoadingCache<String, Integer> forgetPswForbiddenCache;

    @Resource
    private MQHelper mqHelper;
    @Resource
    private UserOperationLogApi userOperationLogApi;
    @Resource
    private UserDeviceApi userDeviceApi;
    @Resource
    private UserDeviceQueryApi userDeviceQueryApi;
//    @Value("${pnk.redis.addresses}")
//    private String redisAddresses;
    @Resource
    private UserOperationLogESApi userOperationLogESApi;
    @Value("${user.operation.log.esQuery.switch:false}")
    private Boolean userOperationLogEsQuerySwitch;

    @Value("${user.device.migration.to.device.query.switch:false}")
    private Boolean userDeviceMigration2QuerySwitch;

//    @PostConstruct
//    private void init() {
//        forgetPswForbiddenCache = CacheBuilder.newBuilder().redisAddresses(redisAddresses).redisPassword(null)
//                .expireAfterAccess(1, TimeUnit.MINUTES).prefix(FORGET_PWS_FORBIDDEN_CACHE_PREFIX).build();
//    }



//    /**
//     * ip统计--代码注入
//     *
//     * @param request
//     * @param ipAddress
//     * @param type
//     * @return
//     */
//    public Long countRecordIp(HttpServletRequest request, String ipAddress, String type) {
//        String key = new StringBuilder().append(ipAddress).append("_").append(type).toString();
//        String times = forgetPswForbiddenCache.getRaw(key);
//        Long count = forgetPswForbiddenCache.incr(key);
//        if (times == null) {
//            forgetPswForbiddenCache.expireRaw(key, FORGET_PSW_EXPIRED_TIME);
//        }
//        return count;
//    }


    /**
     * 统计发mq到风控的个数
     *
     * @param actionDto
     */
    public void sendToRisk(AbnormalActionDto actionDto) {
        // 1.获取当日该ip被不同用户进行 某类型的行为日志操作的集合
        // 跟风控确认过，已经不看这个消息了，先注释，若长时间没人反馈再删除 2020-11-26
//        UserOperationLogResultResponse userOperationLogResultResponse = getLogListByIpAndOperation(actionDto);
//
//        // 2. 若该ip已被其他用户操作过该类型的行为，这需要发送MQ到风控
//        if (userOperationLogResultResponse != null
//                && !CollectionUtils.isEmpty(userOperationLogResultResponse.getRows())) {
//            long size = userOperationLogResultResponse.getTotal();
//
//            // 3.判断当日该用户是否是第一次操作该ip
//            Long count = getUserCountByIpAndOperation(actionDto);
//            if (count == 0) {
//                // 该用户今日第一次操作该ip，判断size
//                if (size == 1) {
//                    // 该ip今日被别的用户操作过一次，则把历史用户的记录和当前用户的记录发mq到风控
//                    UserOperationLogVo user = userOperationLogResultResponse.getRows().get(0);
//                    sendAbnormalActionToRisk(actionDto, user, size);
//                }
//                // size大于1，该ip已经被别的用户操作过多次，则只将当前用户的记录发mq到风控
//                sendAbnormalActionToRisk(actionDto, null, size + 1);
//            }
//        }


    }

    private void sendAbnormalActionToRisk(AbnormalActionDto actionDto, UserOperationLogVo user, long size) {
        AbnormalActionMessageDto messageDto = new AbnormalActionMessageDto();
        if (user != null && user.getUserId() != null) {
            messageDto.setUserId(String.valueOf(user.getUserId()));
            messageDto.setIpAddress(user.getRealIp());
            messageDto.setEmail(user.getEmail());
            messageDto.setType(actionDto.getType().getValue());
            messageDto.setDevice(user.getClientType());
        } else {
            messageDto.setUserId(String.valueOf(actionDto.getUserId()));
            messageDto.setIpAddress(actionDto.getIpAddress());
            messageDto.setEmail(actionDto.getEmail());
            messageDto.setType(actionDto.getType().getValue());
            messageDto.setDevice(actionDto.getClientType());
        }
        if (actionDto.getType() == EnumErrorLogType.FGD && StringUtils.isNotBlank(messageDto.getUserId())) {
            // 频繁授权设备时发送设备指纹到风控
            String content = getDeviceContentByUserId(Long.valueOf(messageDto.getUserId()));
            messageDto.setDetail(content);
        }
        log.info("{}该ip频繁{}:{}次,本次使用该ip的用户为{}", messageDto.getIpAddress(), actionDto.getType().getOperation(), size,
                messageDto.getUserId());
        mqHelper.sendAbnormalActionToRisk(JSON.toJSONString(messageDto));
    }

    public void sendAbnormalActionToRisk(AbnormalActionDto actionDto, long count) {
        AbnormalActionMessageDto messageDto = new AbnormalActionMessageDto();
        messageDto.setUserId(String.valueOf(actionDto.getUserId()));
        messageDto.setIpAddress(actionDto.getIpAddress());
        messageDto.setEmail(actionDto.getEmail());
        messageDto.setType(actionDto.getType().getValue());
        messageDto.setDevice(actionDto.getClientType());
        log.info("{}该ip频繁{}:{}次,本次使用该ip的用户为{}", messageDto.getIpAddress(), actionDto.getType().getOperation(), count,
                messageDto.getUserId());
        mqHelper.sendAbnormalActionToRisk(JSON.toJSONString(messageDto));
    }

    /**
     * 获取当日该ip被不同用户进行 某类型的行为日志操作的集合
     *
     * @param actionDto
     * @return
     */
    private UserOperationLogResultResponse getLogListByIpAndOperation(AbnormalActionDto actionDto) {
        try {
            if (userOperationLogEsQuerySwitch) {
                // 开启后，走user-big-data接口
                com.binance.userbigdata.vo.security.request.FindTodaysUserOperationLogsRequest request = new com.binance.userbigdata.vo.security.request.FindTodaysUserOperationLogsRequest();
                request.setExcludeUserId(actionDto.getUserId());
                request.setIp(actionDto.getIpAddress());
                request.setOperation(actionDto.getType().getOperation());
                APIResponse<com.binance.userbigdata.vo.security.response.UserOperationLogResultResponse> apiResponse =
                        userOperationLogESApi.findTodaysUserOperationLogs(getInstance(request));
                log.info("userOperationLogESApi.findTodaysUserOperationLogs.apiResonse = {}", apiResponse);
                checkResponse(apiResponse);
                UserOperationLogResultResponse resultResponse = new UserOperationLogResultResponse();
                BeanUtils.copyProperties(apiResponse.getData(), resultResponse);
                return resultResponse;
            } else {
                FindTodaysUserOperationLogsRequest request = new FindTodaysUserOperationLogsRequest();
                request.setExcludeUserId(actionDto.getUserId());
                request.setIp(actionDto.getIpAddress());
                request.setOperation(actionDto.getType().getOperation());
                APIResponse<UserOperationLogResultResponse> apiResponse =
                        userOperationLogApi.findTodaysUserOperationLogs(getInstance(request));
                log.info("userOperationLogApi.findTodaysUserOperationLogs.apiResonse = {}", apiResponse);
                return apiResponse.getData();
            }
        } catch (Exception e) {
            log.warn("getLogListByIpAndOperation error", e);
        }
        return null;
    }

    private long getUserCountByIpAndOperation(AbnormalActionDto actionDto) {
        try {
            CountTodaysUserOperationLogsRequest request = new CountTodaysUserOperationLogsRequest();
            request.setUserId(actionDto.getUserId());
            request.setIp(actionDto.getIpAddress());
            request.setOperation(actionDto.getType().getOperation());
            APIResponse<Long> apiResponse = userOperationLogApi.countTodaysUserOperationLogs(getInstance(request));
            log.info("countTodaysUserOperationLogs.apiResonse = {}", apiResponse);
            return apiResponse.getData();
        } catch (Exception e) {
            log.warn("getUserCountByIpAndOperation error", e);
        }
        return 0;
    }

    /**
     * 获取设备指纹
     *
     * @param userId
     * @return
     */
    private String getDeviceContentByUserId(Long userId) {
        if (userDeviceMigration2QuerySwitch) {
            return getDeviceContentByUserIdInUserDeviceQuery(userId);
        } else {
            return getDeviceContentByUserIdInUserDevice(userId);
        }
    }


    /**
     * 获取设备指纹，从account获取
     *
     * @param userId 用户id
     * @return {@link String}
     */
    private String getDeviceContentByUserIdInUserDevice(Long userId) {
        UserDeviceListRequest userDeviceListRequest = new UserDeviceListRequest();
        userDeviceListRequest.setUserId(userId);
        APIResponse<List<UserDeviceVo>> apiResponse = userDeviceApi.listDevice(getInstance(userDeviceListRequest));
        checkResponse(apiResponse);
        List<UserDeviceVo> userDevices = apiResponse.getData();
        if (!CollectionUtils.isEmpty(userDevices)) {
            if (userDevices.get(0) != null) {
                return userDevices.get(0).getContent();// 返回设备指纹
            }
        }
        return null;
    }

    /**
     * 获取设备指纹，从device-query获取
     *
     * @param userId 用户id
     * @return {@link String}
     */
    private String getDeviceContentByUserIdInUserDeviceQuery(Long userId) {
        UserDeviceListQueryRequest userDeviceListQueryRequest = new UserDeviceListQueryRequest();
        userDeviceListQueryRequest.setUserId(userId);
        APIResponse<List<UserDeviceQueryVo>> apiResponse = userDeviceQueryApi.listDevice(getInstance(userDeviceListQueryRequest));
        checkResponse(apiResponse);
        List<UserDeviceQueryVo> userDevices = apiResponse.getData();
        if (!CollectionUtils.isEmpty(userDevices)) {
            if (userDevices.get(0) != null) {
                return userDevices.get(0).getContent();// 返回设备指纹
            }
        }
        return null;
    }

}
