package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import com.binance.nft.market.ifae.MetaDataRefreshApi;
import com.binance.nft.market.request.MetaDataRefreshRequest;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.nft.common.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author joy
 * @date 2022/12/6 15:14
 */
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class UserMarketController {

    private final BaseHelper baseHelper;

    private final MetaDataRefreshApi metaDataRefreshApi;

    private final IUserComplianceApi iUserComplianceApi;

    @PostMapping("/private/nft/meta-data/refresh")
    public CommonRet<Boolean> refresh(@RequestBody MetaDataRefreshRequest request) {
        Long userId = baseHelper.getUserId();
        if (ObjectUtils.isEmpty(userId)){
            return new CommonRet<>(Boolean.FALSE);
        }
        if (!checkKyc(userId)) {
            return new CommonRet<>(Boolean.FALSE);
        }
        request.setUserId(userId);
        APIResponse<Boolean> response = metaDataRefreshApi.addRefresh(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    private boolean checkKyc(Long userId) {
        APIResponse<UserComplianceCheckRet> response = iUserComplianceApi
                .complianceCheck(APIRequest.instance(generateReq(userId)));
        baseHelper.checkResponse(response);
        return response.getData().getPass();
    }

    private UserComplianceCheckReq generateReq(Long userId) {
        return UserComplianceCheckReq.builder()
                .type(ComplianceTypeEnum.genType(ComplianceTypeEnum.KYC_CHECK, ComplianceTypeEnum.CLEAR_CHECK))
                .userId(userId)
                .ip(IPUtils.getIpAddress(WebUtils.getHttpServletRequest()))
                .front(false)
                .build();
    }
}
