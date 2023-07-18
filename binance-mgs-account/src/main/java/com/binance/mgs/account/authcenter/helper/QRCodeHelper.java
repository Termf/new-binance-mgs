package com.binance.mgs.account.authcenter.helper;

import com.binance.authcenter.vo.CreateQrTokenResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.RiskHelper;
import com.binance.mgs.account.authcenter.dto.QRCodeDto;
import com.binance.mgs.account.authcenter.dto.QRCodeStatus;
import com.binance.mgs.account.authcenter.vo.QrCodeArg;
import com.binance.mgs.account.authcenter.vo.QrCodeCreateArg;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryArg;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.platform.mgs.enums.MgsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
public class QRCodeHelper extends BaseHelper {
    private static final int QRCODE_LEN = 16;
    public static final int QRCODE_SHOW_LEN = 8;
    @Value("${qrcode.timeout:60}")
    private long qrCodeTimeout;
    @Value("${qrcode.switch:true}")
    private boolean qrCodeSwitch;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Resource
    private AuthHelper authHelper;
    @Resource
    private RiskHelper riskHelper;

    /**
     * 生成二维码
     *
     * @param qrCodeCreateArg
     * @return
     */
    public String create(QrCodeCreateArg qrCodeCreateArg) {
        if (!qrCodeSwitch) {
            throw new BusinessException(MgsErrorCode.QRODE_LOGIN_FORBIDDEN);
        }
        String deviceInfo = getAndCheckDeviceInfo();
        int i = 0;
        while (i < 3) {
            String qrcode = RandomStringUtils.randomAlphanumeric(QRCODE_LEN);
            if (RedisCacheUtils.get(CacheKey.getQRCode(qrcode)) == null) {
                // 若random不重复，结束循环
                QRCodeDto dto = new QRCodeDto();
                dto.setDeviceInfo(deviceInfo);
                dto.setCreateIp(WebUtils.getRequestIp());
                dto.setCreateClientType(getClientType());
                dto.setQrCodeStatus(QRCodeStatus.NEW);
                dto.setRandom(qrCodeCreateArg.getRandom());
                RedisCacheUtils.set(CacheKey.getQRCode(qrcode), dto, qrCodeTimeout);
                return qrcode;
            }
            qrcode = RandomStringUtils.randomAlphanumeric(QRCODE_LEN);
            i++;
        }
        // 三次依然重复直接提示系统繁忙
        throw new BusinessException(GeneralCode.SYS_ZUUL_ERROR);
    }

    /**
     * 扫码
     *
     * @param qrCodeArg
     */
    public void scan(QrCodeArg qrCodeArg) {
        QRCodeDto dto = getAndCheckQrCode(qrCodeArg.getQrCode());
        if (dto.getQrCodeStatus() != QRCodeStatus.NEW) {
            log.info("qrcode={},scaned but is expired", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
            throw new BusinessException(MgsErrorCode.QRCODE_IS_EXPIRE);
        }
        dto.setQrCodeStatus(QRCodeStatus.SCAN);
        dto.setScanIp(WebUtils.getRequestIp());
        RedisCacheUtils.set(CacheKey.getQRCode(qrCodeArg.getQrCode()), dto, qrCodeTimeout);
        // 记录用户扫描的二维码，当授权时，二维码如果跟最近扫描的不一致，则授权失败
        RedisCacheUtils.set(CacheKey.getScanedQRCode(getUserIdStr()), qrCodeArg.getQrCode(), qrCodeTimeout);
        log.info("qrcode={},scaned", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
        // 风控规则校验
        checkRisk(qrCodeArg, dto);

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
            log.warn("qrcode = {} scan refused cause device is in black list,device={}", qrCodeArg.getQrCode(),
                    dto.getDeviceInfo());
            throw new BusinessException(MgsErrorCode.BLACK_DEVICE);
        }
        // 2. 判断被扫设备ip是否为黑名单
        if (riskHelper.isBlackIp(dto.getCreateIp())) {
            log.warn("qrcode = {} scan refused cause create ip is in black list,ip={}", qrCodeArg.getQrCode(),
                    dto.getCreateIp());
            throw new BusinessException(MgsErrorCode.BLACK_IP);
        }
        // 3. 判断扫码设备ip是否为黑名单
        if (riskHelper.isBlackIp(WebUtils.getRequestIp())) {
            log.warn("qrcode = {} scan refused cause create ip is in black list,ip={}", qrCodeArg.getQrCode(),
                    dto.getCreateIp());
            throw new BusinessException(MgsErrorCode.BLACK_IP);
        }
    }

    private QRCodeDto getAndCheckQrCode(String qrCode) {
        QRCodeDto dto = RedisCacheUtils.get(CacheKey.getQRCode(qrCode), QRCodeDto.class);
        if (dto == null) {
            throw new BusinessException(MgsErrorCode.QRCODE_IS_EXPIRE);
        }
        return dto;
    }

    /**
     * 授权确认
     *
     * @param qrCodeArg
     */
    public void auth(QrCodeArg qrCodeArg) {
        QRCodeDto dto = getAndCheckQrCode(qrCodeArg.getQrCode());
        if (dto.getQrCodeStatus() != QRCodeStatus.SCAN) {
            log.info("qrcode={},authed,but is expired", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));
            throw new BusinessException(MgsErrorCode.QRCODE_IS_EXPIRE);
        }
        // 当授权时，二维码如果跟最近扫描的不一致，则授权失败
        String scanedQrcode = RedisCacheUtils.get(CacheKey.getScanedQRCode(getUserIdStr()));
        if (!StringUtils.equals(scanedQrcode, qrCodeArg.getQrCode())) {
            log.info("qrcode={},auth failed, qrcode is not latest,latest code is={}",
                    StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN),
                    StringUtils.left(scanedQrcode, QRCODE_SHOW_LEN));
            throw new BusinessException(MgsErrorCode.QRCODE_IS_EXPIRE);
        }

        // 访问authcenter，根据app端token，生成web端的token
        CreateQrTokenResponse tokenResponse = authHelper.createQrToken(dto.getCreateClientType());
        if (tokenResponse != null) {
            dto.setToken(tokenResponse.getToken());
            dto.setCsrfToken(tokenResponse.getCsrfToken());
        } else {
            log.error("扫码确认生成token失败");
        }
        dto.setQrCodeStatus(QRCodeStatus.CONFIRM);
        dto.setScanIp(WebUtils.getRequestIp());
        RedisCacheUtils.set(CacheKey.getQRCode(qrCodeArg.getQrCode()), dto, qrCodeTimeout);
        log.info("qrcode={},authed", StringUtils.left(qrCodeArg.getQrCode(), QRCODE_SHOW_LEN));

        // log qrcode device info
        UserOperationHelper.log("qrcode-device-info", dto.getDeviceInfoMap());
    }

    /**
     * 二维码状态查询
     *
     * @param qrCodeQueryArg
     * @return
     */
    public QRCodeDto query(QrCodeQueryArg qrCodeQueryArg) {
        String deviceInfo = getAndCheckDeviceInfo();
        QRCodeDto qrCodeDto = RedisCacheUtils.get(CacheKey.getQRCode(qrCodeQueryArg.getQrCode()), QRCodeDto.class);
        if (qrCodeDto != null) {
            // ip是否匹配
            // if (!StringUtils.equals(WebUtils.getRequestIp(), qrCodeDto.getCreateIp())) {
            // throw new BusinessException(MgsErrorCode.IP_NOT_MATCH);
            // }
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
                RedisCacheUtils.del(CacheKey.getQRCode(qrCodeQueryArg.getQrCode()));
            }
        }
        return qrCodeDto;
    }

    public void logDeviceInfo(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceInfo",
                userDeviceHelper.buildDeviceInfo(request, getUserIdStr(), getUserEmail(), getAndCheckDeviceInfo()));
        UserOperationHelper.log(map);
    }
}
