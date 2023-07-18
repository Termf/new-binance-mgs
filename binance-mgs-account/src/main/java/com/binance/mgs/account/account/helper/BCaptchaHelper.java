package com.binance.mgs.account.account.helper;

import com.binance.master.constant.Constant;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.models.APIResponse.Status;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.enums.CaptchaType;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.security.antibot.api.AntiBotApi;
import com.binance.security.jantibot.common.vo.CaptchaValidateRequest;
import com.binance.security.jantibot.common.vo.CaptchaValidateResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class BCaptchaHelper {
    private static final Logger log = LoggerFactory.getLogger(BCaptchaHelper.class);
    @Resource
    private AntiBotApi antiBotApi;
    @Autowired
    private CaptchaHealthHelper captchaHealthHelper;

    @Autowired
    @Qualifier("bCaptchaExecutor")
    private ExecutorService bCaptchaExecutor;

    @Value("${captcha.exception.block.switch:true}")
    private boolean blockIfException;
    @Value("${captcha.timeout.default.pass:true}")
    private boolean captchaTimeOutPass;

    public boolean validate(String bCaptchaToken, String bizId, int timeOut, TimeUnit timeUnit) {
        try {
            APIRequest<CaptchaValidateRequest> request = APIRequest.instance(CaptchaValidateRequest.class);
            CaptchaValidateRequest body = new CaptchaValidateRequest();
            body.setToken(bCaptchaToken);
            body.setUserIp(WebUtils.getRequestIp());
            body.setUserAgent(WebUtils.getBrowser());
            String deviceInfoStr = WebUtils.getHeader("device-info");
            if (StringUtils.isBlank(deviceInfoStr)) {
                deviceInfoStr = WebUtils.getParameter("device_info");
            }

            body.setDeviceInfo(deviceInfoStr);
            body.setBizId(bizId);
            TerminalEnum terminalEnum = WebUtils.getTerminal();
            if (terminalEnum != null) {
                body.setClientType(terminalEnum.getCode());
            }
            body.setDeviceId(WebUtils.getParameter("bnc-uuid"));
            request.setBody(body);
            String traceId = TrackingUtils.getTrace();
            String envFlag = EnvUtil.getEnvFlag();
            Future<Boolean> future = this.bCaptchaExecutor.submit(() -> {
                TrackingUtils.saveTrace(traceId);
                RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                APIResponse<CaptchaValidateResponse> response = this.antiBotApi.validateCaptcha(request);
                log.info("antibot validateCaptcha response: {}", JsonUtils.toJsonNotNullKey(response));
                if (this.isOk(response) && response.getData() != null) {
                    CaptchaValidateResponse data = response.getData();
                    UserOperationHelper.log("validateId", data.getValidateId());
                    return response.getData().isSuccess();
                } else {
                    return false;
                }
            });
            return future.get(timeOut, timeUnit);
        } catch (BusinessException e) {
            log.error("bCaptcha validate business error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
            return false;
        } catch (TimeoutException e) {
            log.error("bCaptcha validate timeout error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
            return captchaTimeOutPass;
        } catch (Exception e) {
            log.error("bCaptcha validate other error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
            return !blockIfException;
        }
    }

    public boolean isOk(APIResponse<?> resp) {
        return resp != null && resp.getStatus() == Status.OK;
    }

    public void mockValidate(Integer bCaptchaVerifyTimeOut, AntiBotApi antiBotApi, ExecutorService bCaptchaExecutor, CaptchaHealthHelper captchaHealthHelper) {
        try {
            log.info("mockValidate bCAPTCHA");
            CaptchaValidateRequest request = new CaptchaValidateRequest();
            request.setBizId("mockTest");
            request.setToken("captcha#830a5794e6a341079daf545daaa19039-zOus9EJR4yd6Oiq1nDZtoB4ZifQgPYeVb7H2Typbv3hnjvX5");
            request.setUserIp("192.168.146.107");
            request.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");
            request.setDeviceInfo("{\"cpu_memory_size\":\"2837\",\"identification_type\":\"FaceID\",\"cpu_type\":\"CPU_TYPE_ARM64\",\"system_version\":\"15.0\",\"cpu_num\":\"6\",\"screen_resolution\":\"(375.0, 812.0)\",\"brand_model\":\"iPhone XR\",\"location_city\":\"hk\",\"login_ip\":\"192.168.3.37\",\"system_lang\":\"zh-Hans-CN\",\"device_name\":\"Serena\",\"operator\":\"CMHK\",\"app_install_date\":\"2022-03-31 10:56:52\",\"timezone\":\"GMT+8\",\"disk_size\":\"59.54G\",\"device_custom_name\":\"Serena\",\"device_uuid\":\"8B27E419-2DED-4A38-9D3B-ACEF992C71BB\"}");
            request.setClientType("web");
            request.setDeviceId("65266FAF-F4EC-44BA-AD17-4287EFAC8942");
            Future<Boolean> future = bCaptchaExecutor.submit(() -> {
                APIResponse<CaptchaValidateResponse> response = antiBotApi.validateCaptcha(APIRequest.instance(request));
                log.info("mockValidate bCAPTCHA resp {}", JsonUtils.toJsonNotNullKey(response));
                if (this.isOk(response) && response.getData() != null) {
                    return response.getData().isSuccess();
                } else {
                    return false;
                }
            });
            future.get(bCaptchaVerifyTimeOut, TimeUnit.MILLISECONDS);
        } catch (BusinessException e) {
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
            log.error("bCaptcha validate business error", e);
        } catch (TimeoutException e) {
            log.error("bCaptcha validate timeout error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
        } catch (Exception e) {
            log.error("bCaptcha validate other error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
        }
    }

    public void mockValidateV2(Integer bCaptchaVerifyTimeOut, AntiBotApi antiBotApi, ExecutorService bCaptchaExecutor, CaptchaHealthHelper captchaHealthHelper) {
        try {
            log.info("mockValidate bCAPTCHA2");
            CaptchaValidateRequest request = new CaptchaValidateRequest();
            request.setBizId("mockTest");
            request.setToken("captcha#830a5794e6a341079daf545daaa19039-zOus9EJR4yd6Oiq1nDZtoB4ZifQgPYeVb7H2Typbv3hnjvX5");
            request.setUserIp("192.168.146.107");
            request.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");
            request.setDeviceInfo("{\"cpu_memory_size\":\"2837\",\"identification_type\":\"FaceID\",\"cpu_type\":\"CPU_TYPE_ARM64\",\"system_version\":\"15.0\",\"cpu_num\":\"6\",\"screen_resolution\":\"(375.0, 812.0)\",\"brand_model\":\"iPhone XR\",\"location_city\":\"hk\",\"login_ip\":\"192.168.3.37\",\"system_lang\":\"zh-Hans-CN\",\"device_name\":\"Serena\",\"operator\":\"CMHK\",\"app_install_date\":\"2022-03-31 10:56:52\",\"timezone\":\"GMT+8\",\"disk_size\":\"59.54G\",\"device_custom_name\":\"Serena\",\"device_uuid\":\"8B27E419-2DED-4A38-9D3B-ACEF992C71BB\"}");
            request.setClientType("web");
            request.setDeviceId("65266FAF-F4EC-44BA-AD17-4287EFAC8942");
            Future<Boolean> future = bCaptchaExecutor.submit(() -> {
                APIResponse<CaptchaValidateResponse> response = antiBotApi.validateCaptcha(APIRequest.instance(request));
                log.info("mockValidate bCAPTCHA2 resp {}", JsonUtils.toJsonNotNullKey(response));
                if (this.isOk(response) && response.getData() != null) {
                    return response.getData().isSuccess();
                } else {
                    return false;
                }
            });
            future.get(bCaptchaVerifyTimeOut, TimeUnit.MILLISECONDS);
        } catch (BusinessException e) {
            log.error("bCaptcha2 validate business error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
        } catch (TimeoutException e) {
            log.error("bCaptcha2 validate timeout error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
        } catch (Exception e) {
            log.error("bCaptcha2 validate other error", e);
            captchaHealthHelper.recordCaptchaException(CaptchaType.bCAPTCHA, e);
        }
    }
}
