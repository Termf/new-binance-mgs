package com.binance.mgs.account.service;

import com.alibaba.fastjson.JSONObject;
import com.binance.certification.api.KycCertificateApi;
import com.binance.certification.common.enums.KycCertificateStatus;
import com.binance.certification.request.KycDetailInfoRequest;
import com.binance.certification.response.KycDetailInfoResponse;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;

/**
 * @author sean w
 * @date 2022/8/3
 **/
@Slf4j
@Service
public class UserKycService extends BaseHelper {

    @Value("#{'${broker.need.sign.issue.contry:IN}'.split(',')}")
    private Set<String> needSignSpecialIssueCountry;

    @Resource
    private KycCertificateApi kycCertificateApi;

    public Boolean isNeedSignSpecialIssue(Long userId) {
        String kycCountryCode = getKycCountryCode(userId);
        return checkNeedSignSpecialIssue(needSignSpecialIssueCountry, kycCountryCode);
    }

    public String getKycCountryCode(Long userId) {
        KycDetailInfoRequest kycRequest = new KycDetailInfoRequest();
        kycRequest.setUserId(userId);
        kycRequest.setIdNumberMask(true);
        kycRequest.setIncludeChannelRisk(false);
        kycRequest.setIgnoreImage(true);

        String kycCountryCode = null;
        try {
            APIResponse<KycDetailInfoResponse> kycResponse = kycCertificateApi.getKycDetailInfo(getInstance(kycRequest));
            log.info("broker getKycDetailInfo result:{}", kycResponse==null?null: JSONObject.toJSONString(kycResponse));
            checkResponse(kycResponse);
            KycDetailInfoResponse kycDetail = (kycResponse==null?null:kycResponse.getData());
            if (kycDetail != null && KycCertificateStatus.PASS.equals(kycDetail.getKycStatus())) {
                kycCountryCode = kycDetail.getCertificateCountry();
            }
        } catch (Exception e) {
            log.error("getKycCountryCode error", e);
        }
        return kycCountryCode;
    }

    private boolean checkNeedSignSpecialIssue(Set<String> needSignSpecialIssueCountry, String kycCountryCode){
        boolean isSatisfiedPrefix = false;
        if (StringUtils.isNotBlank(kycCountryCode)) {
            kycCountryCode = kycCountryCode.toUpperCase();
        }
        if (!needSignSpecialIssueCountry.isEmpty()) {
            for (String prefix : needSignSpecialIssueCountry) {
                if (StringUtils.isNotBlank(prefix) && prefix.equals(kycCountryCode)) {
                    isSatisfiedPrefix = true;
                    break;
                }
            }
        }
        return isSatisfiedPrefix;
    }
}
