package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KycHelper {

    private final IUserComplianceApi userComplianceApi;

    private final BaseHelper baseHelper;

    public void userComplianceValidate(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .type(ComplianceTypeEnum.genType(ComplianceTypeEnum.KYC_CHECK, ComplianceTypeEnum.CLEAR_CHECK))
                .userId(userId)
                .front(false)
                .build();
        APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        if (!response.getData().getPass()) {
            log.warn("kycCheck check fail {} {}", userId, response.getData());
            throw new BusinessException(response.getData().getErrorCode(), response.getData().getErrorMessage());
        }
    }
}
