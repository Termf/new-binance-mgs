package com.binance.mgs.account.authcenter.controller;

import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.advice.QrCodeLoginQueryMonitor;
import com.binance.mgs.account.authcenter.dto.QRCodeDto;
import com.binance.mgs.account.authcenter.dto.QRCodeStatus;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.helper.QRCodeHelper;
import com.binance.mgs.account.authcenter.helper.QRCodeV2Helper;
import com.binance.mgs.account.authcenter.vo.QrCodeCreateArg;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryArg;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryRet;
import com.binance.mgs.account.service.RiskService;
import com.binance.mgs.account.service.UserComplianceService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class QRCodeLoginController extends BaseAction {
    public static final String QRCODE_GET = "qrcode:get:";
    public static final String QRCODE_QUERY = "qrcode:query:";
    @Value("${ddos.qrcode.query.check.switch:false}")
    private boolean ddosQrcodeQueryCheckSwitch;
    @Value("${ddos.qrcode.query.ip.limit.count:1000}")
    private int ddosQrcodeQueryIpLimitCount;
    @Value("${ddos.qrcode.get.check.switch:false}")
    private boolean ddosQrcodeGetCheckSwitch;
    @Value("${ddos.qrcode.get.ip.limit.count:100}")
    private int ddosQrcodeGetIpLimitCount;

    @Value("${qrcode.clearSerialNo.switch:false}")
    private boolean qrcodeClearSerialNoSwitch;
    
    @Resource
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;

    @Resource
    private QRCodeV2Helper qrCodeV2Helper;
    @Resource
    private AuthHelper authHelper;
    @Autowired
    private UserComplianceService userComplianceService;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private RiskService riskService;

    @PostMapping(value = "/public/qrcode/login/get")
    @DDoSPreMonitor(action = "QRCodeLogin.getQRCode")
    @AccountDefenseResource(name="QRCodeLogin.getQRCode")
    public CommonRet<String> getQRCode(@RequestBody @Valid QrCodeCreateArg qrCodeCreateArg) throws ExecutionException {
        String ip = WebUtils.getRequestIp();
        if (ddosQrcodeGetCheckSwitch && ddosCacheSeviceHelper.ipVisitCountWithUpperLimit(QRCODE_GET + ip, (long) (ddosQrcodeGetIpLimitCount * 1.2)) > ddosQrcodeGetIpLimitCount) {
            log.info("ddos qrcode-login-get banIp,error random={} ip={}",qrCodeCreateArg.getRandom(), ip);
            ddosCacheSeviceHelper.banIp(ip);
        }
        CommonRet<String> ret = new CommonRet<>();
        String qrCode = qrCodeV2Helper.create(qrCodeCreateArg, QRCodeDto.Type.LOGIN);
        ret.setData(qrCode);
        return ret;
    }

    @PostMapping(value = "/public/qrcode/login/query")
    @AccountDefenseResource(name="QRCodeLogin.query")
    @QrCodeLoginQueryMonitor
    @UserOperation(eventName = "loginByQrcode", name = "用户扫码登陆", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"},
            sendToSensorData = false, sendToBigData = false)
    public CommonRet<QrCodeQueryRet> query(@RequestBody @Valid QrCodeQueryArg qrCodeQueryArg, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        String ip = WebUtils.getRequestIp();
        if (ddosQrcodeQueryCheckSwitch && ddosCacheSeviceHelper.ipVisitCountWithUpperLimit(QRCODE_QUERY + ip, (long) (ddosQrcodeQueryIpLimitCount * 1.2)) > ddosQrcodeQueryIpLimitCount) {
            log.info("ddos qrcode-login-query banIp,error qrCode={} ip={}",qrCodeQueryArg.getQrCode(), ip);
            ddosCacheSeviceHelper.banIp(ip);
        }
        CommonRet<QrCodeQueryRet> ret = new CommonRet<>();
        QrCodeQueryRet data = new QrCodeQueryRet();
        ret.setData(data);
        QRCodeDto qrCodeDto = qrCodeV2Helper.query(qrCodeQueryArg);
        if (qrCodeDto == null) {
            data.setStatus(QRCodeStatus.EXPIRED.name());
        } else {
            // 设置二维码状态
            data.setStatus(qrCodeDto.getQrCodeStatus().name());
            // 若为已确认状态，则同步登录态
            if (qrCodeDto.getQrCodeStatus() == QRCodeStatus.CONFIRM) {
                log.info("qrcode={},get token", StringUtils.left(qrCodeQueryArg.getQrCode(), QRCodeHelper.QRCODE_SHOW_LEN));

                Long userId = null;
                try {
                    // 设置bncLocation
                    userId = authHelper.getUserIdByToken(qrCodeDto.getToken());
                } catch (Exception e) {
                    log.error("QRCodeLoginController.query get userId error, userId: {}", userId);
                }
                if(userId != null) {
                    UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.USER_ID, userId));
                    Map<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, String.valueOf(userId), "");
                    String fvideoId = userDeviceHelper.getFVideoId(request);
                    String requestIp = WebUtils.getRequestIp();
                    String clientType = WebUtils.getClientType();
                    boolean riskLoginRuleResult= riskService.loginRiskRuleTimeOut(userId, deviceInfo, fvideoId, requestIp, clientType);
                    if(riskLoginRuleResult){
                        log.error("QRCodeLoginController.query risk error, userId: {}", userId);
                        throw new BusinessException(GeneralCode.USER_DISABLED_LOGIN);
                    }
                    userComplianceService.complianceBlockLoginWithTimeout(userId);
                    data.setBncLocation(userComplianceService.getBncLocationWithTimeout(userId));
                }

                if (baseHelper.isFromWeb()) {
                    // web端种cookie
                    final String code = authHelper.setAuthCookie(request, response, qrCodeDto.getToken(), qrCodeDto.getCsrfToken());
                    
                    // 清除web端原本的半登录态
                    if (qrcodeClearSerialNoSwitch) {
                        String serialNo = BaseHelper.getCookieValue(Constant.COOKIE_SERIAL_NO);
                        if (StringUtils.isNotEmpty(serialNo)) {
                            try {
                                authHelper.delSerialNoCookie(request, response);
                                authHelper.delSerialNo(serialNo);
                                log.info("qrcode={} clear serialNo", StringUtils.left(qrCodeQueryArg.getQrCode(), QRCodeHelper.QRCODE_SHOW_LEN));
                            } catch (Exception e) {
                                log.error("QRCodeLoginController.query clear serialNo error", e);
                            }
                        }
                    }
                    data.setCode(code);
                } else {
                    // 客户端例如mac，pc，返回token
                    data.setToken(qrCodeDto.getToken());
                }
            }
        }
        return ret;
    }


}
