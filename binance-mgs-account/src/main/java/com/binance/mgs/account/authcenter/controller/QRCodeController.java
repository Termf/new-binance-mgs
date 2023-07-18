package com.binance.mgs.account.authcenter.controller;

import com.binance.master.constant.Constant;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.authcenter.AuthCenterBaseAction;
import com.binance.mgs.account.authcenter.dto.QRCodeDto;
import com.binance.mgs.account.authcenter.dto.QRCodeStatus;
import com.binance.mgs.account.authcenter.helper.AuthHelper;
import com.binance.mgs.account.authcenter.helper.QRCodeHelper;
import com.binance.mgs.account.authcenter.vo.QrCodeArg;
import com.binance.mgs.account.authcenter.vo.QrCodeCreateArg;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryArg;
import com.binance.mgs.account.authcenter.vo.QrCodeQueryRet;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.platform.mgs.annotations.CacheControl;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.ctrip.framework.apollo.ConfigService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class QRCodeController extends AuthCenterBaseAction {
    @Resource
    private QRCodeHelper qrCodeHelper;
    @Resource
    private AuthHelper authHelper;

    @GetMapping(value = "/public/qrcode/config/{id}")
    @ApiOperation(value = "获取qrcode相关配置信息",
            notes = "id有5种类型 1：二维码登录是否开启，2：web端扫码提示内容-英文 3：web端扫码提示内容-中文 ，4：app端扫码提示内容英文 5：app端扫码提示内容中文")
    @CacheControl(maxAge = 30)
    public CommonRet<String> qrCodeConfig(@PathVariable("id") int id) {
        CommonRet<String> ret = new CommonRet<>();
        switch (id) {
            case 1:
                ret.setData(ConfigService.getAppConfig().getProperty("qrcode.switch", ""));
                break;
            case 2:
                ret.setData(ConfigService.getAppConfig().getProperty("qrcode.webtip.en", ""));
                break;
            case 3:
                ret.setData(ConfigService.getAppConfig().getProperty("qrcode.webtip.cn", ""));
                break;
            case 4:
                ret.setData(ConfigService.getAppConfig().getProperty("qrcode.apptip.en", ""));
                break;
            case 5:
                ret.setData(ConfigService.getAppConfig().getProperty("qrcode.apptip.cn", ""));
                break;
            default:
                break;
        }
        return ret;
    }

    @PostMapping(value = "/public/qrcode/get")
    @CacheControl(noStore = true)
    @DDoSPreMonitor(action = "qrCode.getQRCode")
    @AccountDefenseResource(name="qrCode.getQRCode")
    public CommonRet<String> getQRCode(@RequestBody @Valid QrCodeCreateArg qrCodeCreateArg) {
        CommonRet<String> ret = new CommonRet<>();
        String qrCode = qrCodeHelper.create(qrCodeCreateArg);
        ret.setData(qrCode);
        return ret;
    }

    @PostMapping(value = "/private/qrcode/scan")
    @UserOperation(eventName = "qrcodeScan", name = "扫码登陆-扫描二维码", responseKeys = {"$.success"},
            responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "qrCode.scan")
    public CommonRet<String> scan(@RequestBody @Valid QrCodeArg qrCodeArg, HttpServletRequest request) {
        qrCodeHelper.logDeviceInfo(request);
        CommonRet<String> ret = new CommonRet<>();
        qrCodeHelper.scan(qrCodeArg);
        return ret;
    }


    @PostMapping(value = "/private/qrcode/auth")
    @UserOperation(eventName = "qrcodeAuth", name = "扫码登陆-确认登录", responseKeys = {"$.success"},
            responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "qrCode.auth")
    public CommonRet<String> auth(@RequestBody @Valid QrCodeArg qrCodeArg, HttpServletRequest request) {
        qrCodeHelper.logDeviceInfo(request);
        CommonRet<String> ret = new CommonRet<>();
        qrCodeHelper.auth(qrCodeArg);
        return ret;
    }

    @PostMapping(value = "/public/qrcode/query")
    @CacheControl(noStore = true)
    @DDoSPreMonitor(action = "qrCode.query")
    @AccountDefenseResource(name="qrCode.query")
    public CommonRet<QrCodeQueryRet> query(@RequestBody @Valid QrCodeQueryArg qrCodeQueryArg,
                                           HttpServletRequest request, HttpServletResponse response) {
        CommonRet<QrCodeQueryRet> ret = new CommonRet<>();
        QrCodeQueryRet data = new QrCodeQueryRet();
        ret.setData(data);
        QRCodeDto qrCodeDto = qrCodeHelper.query(qrCodeQueryArg);
        if (qrCodeDto == null) {
            data.setStatus(QRCodeStatus.EXPIRED.name());
        } else {
            // 设置二维码状态
            data.setStatus(qrCodeDto.getQrCodeStatus().name());
            // 若为已确认状态，则同步登录态
            if (qrCodeDto.getQrCodeStatus() == QRCodeStatus.CONFIRM) {
                log.info("qrcode={},get token",
                        StringUtils.left(qrCodeQueryArg.getQrCode(), QRCodeHelper.QRCODE_SHOW_LEN));

                if (baseHelper.isFromWeb()) {
                    // web端种cookie
                    boolean isTopDomain = true;
                    BaseHelper.setCookie(request, response, isTopDomain, Constant.COOKIE_TOKEN, qrCodeDto.getToken());
                    BaseHelper.setCookie(request, response, isTopDomain, Constant.COOKIE_NEW_CSRFTOKEN,
                            qrCodeDto.getCsrfToken(), false);
                    // set dummy cookie
                    BaseHelper.setCookie(request, response, isTopDomain, Constant.COOKIE_MIX_TOKEN1,
                            authHelper.getDummyCookieValue(qrCodeDto.getToken()));
                    BaseHelper.setCookie(request, response, isTopDomain, Constant.COOKIE_MIX_TOKEN2,
                            authHelper.getDummyCookieValue(qrCodeDto.getToken()));
                    BaseHelper.setCookie(request, response, isTopDomain, Constant.COOKIE_MIX_TOKEN3,
                            authHelper.getDummyCookieValue(qrCodeDto.getToken()));
                } else {
                    // 客户端例如mac，pc，返回token
                    data.setToken(qrCodeDto.getToken());
                }
            }
        }
        return ret;
    }
}
