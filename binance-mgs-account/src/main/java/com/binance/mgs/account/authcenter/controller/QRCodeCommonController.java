package com.binance.mgs.account.authcenter.controller;

import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.authcenter.helper.QRCodeV2Helper;
import com.binance.mgs.account.authcenter.helper.QrCodeDomainHelper;
import com.binance.mgs.account.authcenter.vo.*;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.constant.CacheKey;
import com.binance.qrcode.api.QrCodeApi;
import com.binance.qrcode.enums.QrCodePrefix;
import com.binance.qrcode.vo.CreateQrCodeRequest;
import com.binance.qrcode.vo.CreateQrCodeResponse;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class QRCodeCommonController extends BaseAction {
    @Value("${qrcode.common.create.timeout.second:30}")
    private long createQrCodeTimeoutSecond;
    @Value("${qrcode.common.url.pattern:https://{host}/{lang}/qr/{hash}}")
    private String qrCodeUrlPattern;

    @Value("${qrcode.common.querystring.pattern:^[0-9A-Za-z&=_+/]{0,500}$}")
    private String queryStringPattern;

    @Value("#{'${qrcode.common.biz.whitelist:SPOT_GRID_WEBVIEW,SPOT_GRID,APP_SHARE_DOWNLOAD}'.split(',')}")
    private List<String> bizTypes;

    @Resource
    private QRCodeV2Helper qrCodeV2Helper;
    @Resource
    private QrCodeApi qrCodeApi;
    @Resource
    private QrCodeDomainHelper qrCodeDomainHelper;

    @PostMapping(value = "/private/qrcode/confirm")
    @UserOperation(eventName = "qrcodeLoginConfirm", name = "扫码登陆-确认登录", responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<QrCodeConfirmRet> confirm(@RequestBody @Valid QrCodeArg qrCodeArg, HttpServletRequest request) {
        String status = qrCodeV2Helper.confirm(qrCodeArg);
        QrCodeConfirmRet data = new QrCodeConfirmRet();
        data.setStatus(status);
        return ok(data);
    }

    @PostMapping(value = "/friendly/qrcode/get-content")
    public CommonRet<QrCodeContentRet> getQrCodeContent(@RequestBody @Valid QrCodeArg qrCodeArg) throws Exception {
        CommonRet<QrCodeContentRet> ret = new CommonRet<>();
        QrCodeContentRet data;
        if (StringUtils.startsWith(qrCodeArg.getQrCode(), "DEEPLINK")) {
            data = qrCodeV2Helper.getDeepLinkQrCodeContent(qrCodeArg);
        } else if (StringUtils.startsWithAny(qrCodeArg.getQrCode(), QrCodePrefix.DEEP_LINK.getKey(), QrCodePrefix.PAYMENT.getKey(),QrCodePrefix.DEEP_LINK_TIME_LIMIT.getKey())) {
            data = qrCodeV2Helper.getDeepLinkQrCodeContentV2(qrCodeArg);
        } else {
            data = qrCodeV2Helper.getQrCodeContent(qrCodeArg);
        }
        ret.setData(data);
        return ret;
    }

    @PostMapping(value = "/private/qrcode/create")
    public CommonRet<CreateQrCodeUrlRet> createQrCodeUrl(@RequestBody @Valid CreateQrCodeUrlArg arg) throws Exception {
        Preconditions.checkArgument(bizTypes.contains(arg.getBizType()), "bizType is invalid");
        String key = CacheConstant.getCreateQrcodeLimit(getUserId());
        String value = ShardingRedisCacheUtils.get(key);
        if (value == null) {
            // 没有触发限流
            Pattern p = Pattern.compile(queryStringPattern);
            Preconditions.checkArgument(StringUtils.isBlank(arg.getDeepLinkQueryString()) || p.matcher(arg.getDeepLinkQueryString()).find(), "Query String is invalid");
            Preconditions.checkArgument(StringUtils.isBlank(arg.getUrlPathQueryString()) || p.matcher(arg.getUrlPathQueryString()).find(), "Query String is invalid");

            String domain = qrCodeDomainHelper.getDomainByType(arg);

            CreateQrCodeRequest request = new CreateQrCodeRequest();
            request.setBizType(arg.getBizType());
            request.setDeepLinkQueryString(arg.getDeepLinkQueryString());
            request.setUrlPathQueryString(arg.getUrlPathQueryString());
            request.setUserId(getUserId());
            APIResponse<CreateQrCodeResponse> response = qrCodeApi.createQrCode(APIRequest.instance(request));
            checkResponse(response);
            String url = qrCodeUrlPattern.replace("{host}", domain).replace("{lang}", baseHelper.getLanguage()).replace("{hash}", response.getData().getQrCode());
            // 成功之后才放入redis，默认30s不允许重复生成qrcode，若是失败的，走不到这里，也不会限制用户30s不能重复请求
            ShardingRedisCacheUtils.set(key, "Y", createQrCodeTimeoutSecond);
            return ok(new CreateQrCodeUrlRet(url));
        } else {
            log.info("user {} createQrCodeUrl too frequently arg ={}", getUserId(), arg);
            throw new BusinessException(GeneralCode.GW_TOO_MANY_REQUESTS);
        }
    }
}
