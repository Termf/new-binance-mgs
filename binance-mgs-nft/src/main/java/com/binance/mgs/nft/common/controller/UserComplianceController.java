package com.binance.mgs.nft.common.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/private/nft/compliance")
public class UserComplianceController {

    private final BaseHelper baseHelper;

    private final IUserComplianceApi userComplianceApi;

    @UserOperation(eventName = "NFT_Compliance_Check", name = "NFT_Compliance_Check",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/check")
    public CommonRet<Void> complianceCheck(@RequestBody UserComplianceCheckReq request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        request.setIp(IPUtils.getIpAddress(WebUtils.getHttpServletRequest()));
        request.setFront(false);
        APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        if(!response.getData().getPass()) {
            throw new BusinessException(response.getData().getErrorCode(), response.getData().getErrorMessage());
        }
        return new CommonRet<>();
    }
}
