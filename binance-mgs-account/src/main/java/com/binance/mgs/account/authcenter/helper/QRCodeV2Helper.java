package com.binance.mgs.account.authcenter.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.binance.marketing.api.AppQrCodeApi;
import com.binance.marketing.vo.appqrcode.response.AppQrCodeConfigVo;
import com.binance.marketing.vo.qrcode.QrCodeResponse;
import com.binance.marketing.vo.qrcode.QueryQrCodeRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.Geoip2Utils;
import com.binance.master.utils.WebUtils;
import com.binance.master.utils.security.Base64Util;
import com.binance.mgs.account.account.dto.RiskLoginInfoDto;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.account.helper.RiskKafkaHelper;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.account.vo.UserDeviceRet;
import com.binance.mgs.account.authcenter.dto.QRCodeDto;
import com.binance.mgs.account.authcenter.dto.QRCodeStatus;
import com.binance.mgs.account.authcenter.helper.qrcode.QrCodeApi;
import com.binance.mgs.account.authcenter.helper.qrcode.QrCodeApiFactory;
import com.binance.mgs.account.authcenter.vo.QrCodeArg;
import com.binance.mgs.account.authcenter.vo.QrCodeContentRet;
import com.binance.mgs.account.authcenter.vo.QrCodeCreateArg;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryArg;
import com.binance.mgs.account.constant.QRCodeActionType;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.binance.platform.mgs.utils.PKGenarator;
import com.binance.qrcode.api.PaymentQrCodeApi;
import com.binance.qrcode.enums.QrCodePrefix;
import com.binance.risk.api.RiskIpQualityScoreApi;
import com.binance.risk.vo.fraud.IpQualityScoreRequest;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class QRCodeV2Helper extends BaseHelper {
    private static final int QRCODE_LEN = 16;
    public static final int QRCODE_SHOW_LEN = 8;
    @Value("${qrcode.timeout:60}")
    private long qrCodeTimeout;
    @Value("${qrcode.switch:true}")
    private boolean qrCodeSwitch;
    // 为了减少对下游的访问，默认缓存1分钟
    @Value("${qrcode.default.cache.timeout:60}")
    private int defaultCacheTimeout;
    @Resource
    private UserDeviceHelper userDeviceHelper;
    @Resource
    private RiskKafkaHelper riskKafkaHelper;

    @Resource
    private RiskHelper riskHelper;
    @Autowired
    private AppQrCodeApi appQrCodeApi;
    @Autowired
    private com.binance.qrcode.api.QrCodeApi qrCodeApi;
    @Autowired
    private PaymentQrCodeApi paymentQrCodeApi;
    @Autowired
    private RiskIpQualityScoreApi riskIpQualityScoreApi;
    @Autowired
    private QrCodeApiFactory qrCodeApiFactory;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private CrowdinHelper crowdinHelper;

    @Value("${qrcode.ip.geo.enable:true}")
    private Boolean geoEnable;

    private LoadingCache<String, AppQrCodeConfigVo> qrCodeConfig =
            CacheBuilder.newBuilder().maximumSize(20).expireAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<String, AppQrCodeConfigVo>() {
                @Override
                public AppQrCodeConfigVo load(String key) throws Exception {
                    APIResponse<AppQrCodeConfigVo> apiResponse = appQrCodeApi.queryAppQrCodeConfig(key);
                    checkResponse(apiResponse);
                    return apiResponse.getData();
                }
            });

    /**
     * 生成二维码
     *
     * @return
     * @param qrCodeCreateArg
     */
    public String create(QrCodeCreateArg qrCodeCreateArg, QRCodeDto.Type type) throws ExecutionException {
        if (!qrCodeSwitch) {
            throw new BusinessException(MgsErrorCode.QRODE_LOGIN_FORBIDDEN);
        }

        String deviceInfo = getAndCheckDeviceInfo();
        // 设备是否关联老的qrCode
        String oldQrCode = redisTemplate.opsForValue().get(CacheKey.getQrcodeDevice(deviceInfo));
        if(oldQrCode!=null){
            // 老qrCode作废
            deleteQrCode(oldQrCode);
        }
        String qrcode = PKGenarator.getId();

        final HttpServletRequest request = WebUtils.getHttpServletRequest();
        QRCodeDto dto = new QRCodeDto();
        dto.setDeviceInfo(deviceInfo);
        dto.setDeviceInfoMap(userDeviceHelper.buildDeviceInfo(request, getUserIdStr(), getUserEmail(), deviceInfo));
        dto.setCreateIp(WebUtils.getRequestIp());
        dto.setCreateClientType(getClientType());
        dto.setQrCodeStatus(QRCodeStatus.NEW);
        dto.setRandom(qrCodeCreateArg.getRandom());
        dto.setType(type.name());
        dto.setFVideoId(CommonUserDeviceHelper.getFVideoId(request));
        AppQrCodeConfigVo appQrCodeConfigVo = qrCodeConfig.get(dto.getType());
        dto.setExpireDate(DateUtils.addSeconds(new Date(), appQrCodeConfigVo.getExpireTimes()));
        setQrCode(dto, qrcode);
        // 设备关联qrCode
        redisTemplate.opsForValue().set(CacheKey.getQrcodeDevice(deviceInfo),qrcode,appQrCodeConfigVo.getExpireTimes(),TimeUnit.SECONDS);
        return qrcode;
    }

    private void setQrCode(QRCodeDto dto, String qrcode) {
        String key = CacheKey.getQRCode(qrcode);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(dto));
        if (dto.getExpireDate() != null) {
            redisTemplate.expireAt(key, dto.getExpireDate());
        }
    }

    /**
     * 扫码风控校验
     *
     * @param qrCodeArg
     * @param dto
     */
    private void checkRisk(QrCodeArg qrCodeArg, QRCodeDto dto) {
        // 1. 判断被扫设备是否为黑名单
        if (riskHelper.isBlackDevice(dto.getDeviceInfo(), dto.getCreateClientType(), dto.getCreateIp())) {
            log.warn("qrcode = {} scan refused cause device is in black list,device={}", qrCodeArg.getQrCode(), dto.getDeviceInfo());
            throw new BusinessException(MgsErrorCode.BLACK_DEVICE);
        }
        // 2. 判断被扫设备ip是否为黑名单
        if (riskHelper.isBlackIp(dto.getCreateIp())) {
            log.warn("qrcode = {} scan refused cause create ip is in black list,ip={}", qrCodeArg.getQrCode(), dto.getCreateIp());
            throw new BusinessException(MgsErrorCode.BLACK_IP);
        }
        // 3. 判断扫码设备ip是否为黑名单
        if (riskHelper.isBlackIp(WebUtils.getRequestIp())) {
            log.warn("qrcode = {} scan refused cause create ip is in black list,ip={}", qrCodeArg.getQrCode(), dto.getCreateIp());
            throw new BusinessException(MgsErrorCode.BLACK_IP);
        }
    }

    private QRCodeDto getQrCode(String qrCode) {
        String qrCodeStr = redisTemplate.opsForValue().get(CacheKey.getQRCode(qrCode));
        if (qrCodeStr != null) {
            return JSON.parseObject(qrCodeStr, QRCodeDto.class);
        }
        return null;
    }

    /**
     * 授权确认
     *
     * @param qrCodeArg
     */
    public String confirm(QrCodeArg qrCodeArg) {
        // 10s之内同一个用户只允许调用一次
        String key = CacheKey.getQrcodeConfirm(getUserIdStr());

        //发送登录信息给risk
        riskKafkaHelper.sendLoginInfoToRiskByUserId(getUserId(), WebUtils.getRequestIp(), RiskLoginInfoDto.QR_CODE_LOGIN);

        Boolean isNotExists = redisTemplate.opsForValue().setIfAbsent(key, "Y", 10, TimeUnit.SECONDS);
        if (!isNotExists) {
            throw new BusinessException(GeneralCode.GW_TOO_MANY_REQUESTS);
        }
        try {
            QRCodeDto dto = getQrCode(qrCodeArg.getQrCode());
            if (dto == null || dto.getQrCodeStatus() != QRCodeStatus.SCAN) {
                log.info("qrcode={},authed,but is expired", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
                // throw new BusinessException(MgsErrorCode.QRCODE_IS_EXPIRE);
                return QRCodeStatus.EXPIRED.name();
            }
            // log user device info & fvideoid (both scanning device and scanned device)
            logScanDeviceInfo(dto.getFVideoId());

            // 风控规则校验
            checkRisk(qrCodeArg, dto);
            qrCodeApiFactory.get(dto.getType()).doConfirm(dto);
            dto.setQrCodeStatus(QRCodeStatus.CONFIRM);
            dto.setScanIp(WebUtils.getRequestIp());
            setQrCode(dto, qrCodeArg.getQrCode());
            log.info("qrcode={},authed", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));

            // log qrcode device info
            UserOperationHelper.log("qrcode-device-info", dto.getDeviceInfoMap());
            return dto.getQrCodeStatus().name();
        } finally {
            redisTemplate.delete(key);
        }
    }

    /**
     * 二维码状态查询
     *
     * @param qrCodeQueryArg
     * @return
     */
    public QRCodeDto query(QrCodeQueryArg qrCodeQueryArg) {
        String deviceInfo = getAndCheckDeviceInfo();
        QRCodeDto qrCodeDto = getQrCode(qrCodeQueryArg.getQrCode());
        if (qrCodeDto != null) {
            // 设备是否匹配
            if (!StringUtils.equals(deviceInfo, qrCodeDto.getDeviceInfo())) {
                throw new BusinessException(MgsErrorCode.DEVICE_NOT_MATCH);
            }
            // 查询qrcode状态所带的random必须跟二维码生成时所带的random一直
            if (!StringUtils.equals(qrCodeQueryArg.getRandom(), qrCodeDto.getRandom())) {
                throw new BusinessException(MgsErrorCode.QRODE_NOT_MATCH);
            }
            // 确认成功后，删除qrcode，避免反复使用
            if (qrCodeDto.getQrCodeStatus() == QRCodeStatus.CONFIRM) {
                deleteQrCode(qrCodeQueryArg.getQrCode());
            }
        }
        return qrCodeDto;
    }

    private void deleteQrCode(String qrCode) {
        redisTemplate.delete(CacheKey.getQRCode(qrCode));
    }

    public void logScanDeviceInfo(String qrCodeDeviceFVideoId) {
        final HttpServletRequest request = WebUtils.getHttpServletRequest();
        final Map<String, String> scanningDeviceInfo = userDeviceHelper.buildDeviceInfo(request, getUserIdStr(), getUserEmail());
        UserOperationHelper.log("deviceInfo", scanningDeviceInfo);
        UserOperationHelper.log("scan-device-" + LocalLogKeys.FVIDEO_ID, CommonUserDeviceHelper.getFVideoId(request));
        UserOperationHelper.log("qrcode-device-" + LocalLogKeys.FVIDEO_ID, qrCodeDeviceFVideoId);
    }

    public QrCodeContentRet getQrCodeContent(QrCodeArg qrCodeArg) throws Exception {
        log.info("qrcode={},scaned", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
        String qrCode = qrCodeArg.getQrCode();
        QrCodeContentRet ret = new QrCodeContentRet();
        ret.setQrCode(qrCodeArg.getQrCode());
        ret.setStatus(QRCodeStatus.EXPIRED.name());
        QRCodeDto qrCodeDto = getQrCode(qrCode);
        if (qrCodeDto != null) {
            if(qrCodeDto.getQrCodeStatus() == QRCodeStatus.CONFIRM){
                // 扫码已确认的返回已过期
                return ret;
            }
            // 风控规则校验
            checkRisk(qrCodeArg, qrCodeDto);
            AppQrCodeConfigVo appQrCodeConfigVo = qrCodeConfig.get(qrCodeDto.getType());
            if (appQrCodeConfigVo == null) {
                log.warn("qrcode type {} is not exists", qrCodeDto.getType());
                // 不存在当作过期处理
                return ret;
            }
            if (QRCodeActionType.isConfirm(appQrCodeConfigVo.getActionType()) && StringUtils.isBlank(getUserIdStr())) {
                log.info("qrcode={},user is not logined", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
                throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
            }
            // 更新服务端状态
            qrCodeDto.setQrCodeStatus(QRCodeStatus.SCAN);
            qrCodeDto.setScanIp(WebUtils.getRequestIp());
            setQrCode(qrCodeDto, qrCodeArg.getQrCode());

            // 设置状态
            ret.setStatus(QRCodeStatus.SCAN.name());
            ret.setActionType(appQrCodeConfigVo.getActionType());
            // 设置content信息
            setContentInfo(ret, appQrCodeConfigVo);
            if(QRCodeActionType.isConfirm(appQrCodeConfigVo.getActionType())){
                // 设置扩展信息
                setExtendInfo(ret, qrCodeDto);
                QrCodeApi qrCodeApi = qrCodeApiFactory.get(qrCodeDto.getType());
                if(qrCodeApi!=null){
                    qrCodeApi.setAdditionInfo(ret,qrCodeDto);
                }
            }
        }
        return ret;
    }

    // 迁移完成之后，可以删除这个方法
    @Deprecated
    public QrCodeContentRet getDeepLinkQrCodeContent(QrCodeArg qrCodeArg) throws Exception {
        log.info("qrcode={},scaned", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
        QrCodeContentRet ret = new QrCodeContentRet();
        ret.setQrCode(qrCodeArg.getQrCode());
        ret.setStatus(QRCodeStatus.EXPIRED.name());
        QrCodeResponse qrCodeResponse = getDeepLinkQrCodeInfo(qrCodeArg);
        if (qrCodeResponse != null) {
            // 设置状态
            ret.setStatus(QRCodeStatus.SCAN.name());
            ret.setActionType(qrCodeResponse.getActionType());
            if(StringUtils.isNotBlank(qrCodeResponse.getDeepLinkPath())){
                QrCodeContentRet.DeepLinkContent content = new QrCodeContentRet.DeepLinkContent();
                content.setPath(qrCodeResponse.getDeepLinkPath());
                ret.setDeepLinkContent(content);
            }
            ret.setEnableUrlRedirect(qrCodeResponse.isEnableUrlRedirect());
            if(qrCodeResponse.isEnableUrlRedirect()){
                QrCodeContentRet.UrlContent urlContent = new QrCodeContentRet.UrlContent();
                urlContent.setPath(qrCodeResponse.getUrlPath());
                urlContent.setDomainPrefix(qrCodeResponse.getDomainPrefix());
                ret.setUrlContent(urlContent);
            }
        }
        return ret;
    }

    public QrCodeContentRet getDeepLinkQrCodeContentV2(QrCodeArg qrCodeArg) throws Exception {
        log.info("qrcode={},scaned", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
        QrCodeContentRet ret = new QrCodeContentRet();
        ret.setQrCode(qrCodeArg.getQrCode());
        ret.setStatus(QRCodeStatus.EXPIRED.name());
        com.binance.qrcode.vo.QrCodeResponse qrCodeResponse = getDeepLinkQrCodeInfoV2(qrCodeArg);
        if (qrCodeResponse != null) {
            // 设置状态
            ret.setStatus(QRCodeStatus.SCAN.name());
            ret.setActionType(qrCodeResponse.getActionType());
            if(StringUtils.isNotBlank(qrCodeResponse.getDeepLinkPath())){
                QrCodeContentRet.DeepLinkContent content = new QrCodeContentRet.DeepLinkContent();
                content.setPath(qrCodeResponse.getDeepLinkPath());
                ret.setDeepLinkContent(content);
            }
            ret.setEnableUrlRedirect(qrCodeResponse.isEnableUrlRedirect());
            if(qrCodeResponse.isEnableUrlRedirect()){
                QrCodeContentRet.UrlContent urlContent = new QrCodeContentRet.UrlContent();
                urlContent.setPath(qrCodeResponse.getUrlPath());
                urlContent.setDomainPrefix(qrCodeResponse.getDomainPrefix());
                ret.setUrlContent(urlContent);
            }
        }
        return ret;
    }

    @Deprecated
    // 迁移完成后可以删除该方法
    private QrCodeResponse getDeepLinkQrCodeInfo(QrCodeArg qrCodeArg) {
        String key = CacheKey.getQRCode(qrCodeArg.getQrCode());
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(value)) {
            return JSON.parseObject(value, QrCodeResponse.class);
        }
        QueryQrCodeRequest queryQrCodeRequest = new QueryQrCodeRequest();
        queryQrCodeRequest.setQrCode(qrCodeArg.getQrCode());
        APIResponse<QrCodeResponse> apiResponse = appQrCodeApi.queryByQrCode(getInstance(queryQrCodeRequest));
        QrCodeResponse data = apiResponse.getData();
        if (data != null) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(data));
            if (data.getCreateTime() != null && data.getExpireTimes() > 0) {
                Date expireTime = new Date(data.getCreateTime().getTime() + data.getExpireTimes() * 1000);
                redisTemplate.expireAt(key, expireTime);
            }
        }
        return data;
    }
    private com.binance.qrcode.vo.QrCodeResponse getDeepLinkQrCodeInfoV2(QrCodeArg qrCodeArg) {
        String key = CacheKey.getQRCode(qrCodeArg.getQrCode());
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(value)) {
            return JSON.parseObject(value, com.binance.qrcode.vo.QrCodeResponse.class);
        }

        com.binance.qrcode.vo.QueryQrCodeRequest queryQrCodeRequest = new com.binance.qrcode.vo.QueryQrCodeRequest();
        queryQrCodeRequest.setQrCode(qrCodeArg.getQrCode());
        APIResponse<com.binance.qrcode.vo.QrCodeResponse> apiResponse;
        if (QrCodePrefix.isPayment(qrCodeArg.getQrCode())) {
            apiResponse = paymentQrCodeApi.queryByQrCode(getInstance(queryQrCodeRequest));
        } else {
            apiResponse = qrCodeApi.queryByQrCode(getInstance(queryQrCodeRequest));
        }
        com.binance.qrcode.vo.QrCodeResponse data = apiResponse.getData();
        if (data != null) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(data));
            if (data.getCreateTime() != null) {
                int expireTimes = data.getExpireTimes();
                // mgs最多缓存1分钟，若<1分钟的，则用原来的，超过1分钟，则缓存1分钟，expireTimes<0表示永久的，也只缓存1分钟
                if (expireTimes < 0 || data.getExpireTimes() > defaultCacheTimeout) {
                    expireTimes = defaultCacheTimeout;
                }
                Date expireTime = new Date(data.getCreateTime().getTime() + expireTimes * 1000);
                redisTemplate.expireAt(key, expireTime);
            }
        }
        return apiResponse.getData();
    }

    private void setContentInfo(QrCodeContentRet ret, AppQrCodeConfigVo appQrCodeConfigVo) {
        JSONObject contentJson = JSON.parseObject(appQrCodeConfigVo.getConfirmContent());
        if (QRCodeActionType.isConfirm(appQrCodeConfigVo.getActionType())) {
            QrCodeContentRet.ConfirmContent content = new QrCodeContentRet.ConfirmContent();
            content.setTitle(getFrontMessage(contentJson.getString("title")));
            content.setConfirmText(getFrontMessage(contentJson.getString("confirmText")));
            content.setCancelText(getFrontMessage(contentJson.getString("cancelText")));
            content.setMessage(getFrontMessage("qr-default-message"));
            ret.setConfirmContent(content);
        } else if (QRCodeActionType.isDeepLink(appQrCodeConfigVo.getActionType())) {
            QrCodeContentRet.DeepLinkContent content = new QrCodeContentRet.DeepLinkContent();
            content.setPath(contentJson.getString("path"));
            ret.setDeepLinkContent(content);
        }
    }

    private void setExtendInfo(QrCodeContentRet ret, QRCodeDto qrCodeDto) {
        String ip = qrCodeDto.getCreateIp();
        // 封装额外设备信息
        Map<String, String> extendInfo = new LinkedHashMap<>();
        extendInfo.put(getFrontMessage("qr-ip-address"), ip);
        extendInfo.put(getFrontMessage("qr-country"), getLocation(ip));
        extendInfo.put(getFrontMessage("qr-device-name"), getDeviceName(qrCodeDto));
        ret.setExtendInfo(Lists.newArrayList(extendInfo.entrySet()));
    }

    private String getFrontMessage(String key) {
        return crowdinHelper.getMessageByKey(key,getLanguage());
    }

    private String getDeviceName(QRCodeDto qrCodeDto) {
        try {
            JSONObject content = JSON.parseObject(Base64Util.decode(qrCodeDto.getDeviceInfo()));
            return content.getString(UserDeviceRet.DEVICE_NAME);
        } catch (Exception e) {
            log.warn("getDeviceName error", e);
        }
        return "UNKNOWN";
    }

    private String getLocation(String ip) {
        String region = redisTemplate.opsForValue().get(CacheKey.getUserIp(ip));
        if(StringUtils.isNotBlank(region)){
           return region;
        }
        region = "UNKNOWN";

        if (geoEnable) {
            Geoip2Utils.Geoip2Detail detail = Geoip2Utils.getDetail(ip);
            try {
                region = Optional.ofNullable(detail)
                        .map(Geoip2Utils.Geoip2Detail::getCity)
                        .map(City::getName)
                        .orElse(
                                Optional.ofNullable(detail)
                                        .map(Geoip2Utils.Geoip2Detail::getCountry)
                                        .map(Country::getIsoCode)
                                        .orElse("UNKNOWN")
                        );
            } catch (Throwable e) {
                region = "UNKNOWN";
                log.error(e.getMessage(), e);
            }
            log.info("use Geoip2Utils.getDetail,ip:{},region:{}", ip, region);
        } else {
            IpQualityScoreRequest ipQualityScoreRequest = new IpQualityScoreRequest();
            ipQualityScoreRequest.setIpAddress(ip);
            APIResponse<Map<String, Object>> ipInfo = riskIpQualityScoreApi.getIpFraudScore(getInstance(ipQualityScoreRequest));
            Map<String, Object> ipInfoData = ipInfo.getData();
            if (ipInfoData != null && ipInfoData.get("region") != null) {
                region = ipInfoData.get("region").toString();
                // 缓存一小时
            }
            log.info("use getIpFraudScore,ip:{}", ip);
        }

        redisTemplate.opsForValue().set(CacheKey.getUserIp(ip),region,1,TimeUnit.HOURS);
        return region;
    }

}
