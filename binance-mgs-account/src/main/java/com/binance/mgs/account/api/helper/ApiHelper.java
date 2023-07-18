package com.binance.mgs.account.api.helper;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserDeviceApi;
import com.binance.account.vo.apimanage.response.ApiModelResponse;
import com.binance.account.vo.device.request.UserDeviceRequest;
import com.binance.account.vo.device.response.AddUserDeviceResponse;
import com.binance.compliance.api.UserComplianceApi;
import com.binance.compliance.vo.request.UserComplianceCheckRequest;
import com.binance.compliance.vo.response.UserComplianceCheckResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.constant.LocalLogKeys;
import com.binance.userbigdata.api.KycCertificateApi;
import com.binance.userbigdata.api.SubUserApiManageApi;
import com.binance.userbigdata.vo.kyc.response.KycBriefInfoResp;
import com.binance.userbigdata.vo.user.request.UserIdReq;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ApiHelper extends BaseHelper {

    @Value("#{'${black.kyc.country:US}'.split(',')}")
    private Set<String> blackKycCountryList;

    @Resource
    private CommonUserDeviceHelper commonUserDeviceHelper;
    @Resource
    private UserDeviceApi userDeviceApi;
    @Autowired
    private KycCertificateApi kycCertificateApi;
    @Autowired
    private SubUserApiManageApi subUserApiManageApi;
    @Autowired
    private UserComplianceApi userComplianceApi;

    @Value("${skip.kyc.check.switch:true}")
    private boolean skipKycCheckSwitch;

    @Value("${black.kyc.check.switch:false}")
    private boolean blackKycCheckSwitch;

    @Value("${check.kyc.start.time:2021-08-23 08:00:00}")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkUpdateApiStartTime;

    @Value("${apiKeyCheck.switchTo.compliance:false}")
    private boolean apiKeyCheckSwitchToCompliance;

    public Map<String, String> logDeviceInfo() {
        String userEmail = this.getUserEmail();
        Map<String, String> deviceInfo = commonUserDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(),
                this.getUserIdStr(), userEmail);
        if (deviceInfo == null) {
            deviceInfo = Collections.emptyMap();
        }
        UserOperationHelper.log(ImmutableMap.of("device", deviceInfo));
        return deviceInfo;
    }

    public AddUserDeviceResponse associateSensitiveDevice(String source, Map<String, String> content) {
        UserDeviceRequest userDeviceRequest = new UserDeviceRequest();
        userDeviceRequest.setUserId(this.getUserId());
        userDeviceRequest.setAgentType(this.getClientType());
        userDeviceRequest.setSource(source);
        userDeviceRequest.setContent(content);
        APIResponse<AddUserDeviceResponse> response = userDeviceApi.associateSensitiveDevice(this.getInstance(userDeviceRequest));
        if (response != null && response.getStatus() == APIResponse.Status.OK) {
            AddUserDeviceResponse addUserDeviceResponse = response.getData();
            if (addUserDeviceResponse != null && addUserDeviceResponse.getId() != null) {
                UserOperationHelper.log(ImmutableMap.of(LocalLogKeys.DEVICE_PK, addUserDeviceResponse.getId()));
            }
            return response.getData();
        }
        log.error("{},ApiHelper.associateSensitiveDevice is error:{}", source, JSON.toJSONString(response));
        return null;
    }

    public List<ApiModelResponse> filterUnconfirmed(List<ApiModelResponse> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return dataList;
        }
        Iterator<ApiModelResponse> iterator = dataList.iterator();
        while (iterator.hasNext()) {
            ApiModelResponse apiModelResponse = iterator.next();
            //status=1 表示新建
            int apiNewStatus = 1;
            if (apiModelResponse.getStatus() == apiNewStatus
                    && !apiModelResponse.isApiEmailVerify()) {
                iterator.remove();
            }
        }
        return dataList;
    }

    public KycBriefInfoResp checkPassAboveIntermediateKyc(Long userId) throws Exception {
        return this.checkPassAboveIntermediateKyc(userId, false);
    }

    /**
     * 对存量更改（即update操作）的检查要在指定时间开启
     */
    public KycBriefInfoResp checkPassAboveIntermediateKyc(Long userId, boolean isUpdate) throws Exception {
        // 跳过kyc检查
        if (skipKycCheckSwitch) {
            KycBriefInfoResp resp = new KycBriefInfoResp();
            resp.setPass(true);
            return resp;
        }
        if (isUpdate && new Date().before(checkUpdateApiStartTime)) {
            KycBriefInfoResp resp = new KycBriefInfoResp();
            resp.setPass(true);
            return resp;
        }

        if(apiKeyCheckSwitchToCompliance) {
            UserComplianceCheckRequest request = new UserComplianceCheckRequest();
            request.setUserId(getUserId());
            request.setProductLine("MAINSITE");
            request.setOperation("API_KEY_CHECK");
            request.setFront(true);
            APIResponse<UserComplianceCheckResponse> complianceApiResponse = userComplianceApi.userComplianceCheck(getInstance(request));
            checkResponse(complianceApiResponse);
            UserComplianceCheckResponse userComplianceCheckResponse = complianceApiResponse.getData();
            if(userComplianceCheckResponse != null && !userComplianceCheckResponse.isPass()) {
                throw new BusinessException(userComplianceCheckResponse.getErrorCode(), userComplianceCheckResponse.getErrorMessage());
            }
            KycBriefInfoResp resp = new KycBriefInfoResp();
            resp.setPass(true);
            return resp;
        }

        UserIdReq request = new UserIdReq();
        request.setUserId(userId);
        APIResponse<KycBriefInfoResp> apiResponse = kycCertificateApi.checkPassAboveIntermediateKyc(getInstance(request));
        checkResponse(apiResponse);

        KycBriefInfoResp resp = apiResponse.getData();
        if (resp == null) {
            resp = new KycBriefInfoResp();
        }

        //美国kyc单独开控制
        if (blackKycCheckSwitch && StringUtils.isNotBlank(resp.getFillCountry()) && blackKycCountryList.contains(resp.getFillCountry())) {
            throw new BusinessException(GeneralCode.USER_IN_BLACK_KYC);
        }

        return resp;
    }
}
