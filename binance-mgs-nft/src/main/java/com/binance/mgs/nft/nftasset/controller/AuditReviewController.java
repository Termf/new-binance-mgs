package com.binance.mgs.nft.nftasset.controller;

import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.assetservice.api.audit.IContentAuditApi;
import com.binance.nft.assetservice.api.data.vo.audit.TextFileAuditResp;
import com.binance.nft.bnbgtwservice.api.iface.IAuditReviewApi;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
public class AuditReviewController {

    private final IAuditReviewApi auditReviewApi;

    private final BaseHelper baseHelper;

    private final IContentAuditApi contentAuditApi;

    private final String SENSITIVE_FIELDS="sensitiveFields";

    @PostMapping("/audit/review")
    @UserOperation(eventName = "NFT_Audit_Review", name = "NFT_Audit_Review",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    public CommonRet auditReview(@RequestBody @Valid @NotNull Map<String,String> request){

        APIResponse<List<String>> textCheckResult = auditReviewApi.textCheck(
                APIRequest.instance(request.values().stream().collect(Collectors.toList())));
        baseHelper.checkResponse(textCheckResult);
        List<String> sensibleFields = new ArrayList<>();
        for(Map.Entry<String,String> item : request.entrySet()){
            if(!textCheckResult.getData().contains(item.getValue())){
                sensibleFields.add(item.getKey());
            }
        }
        if(CollectionUtils.isNotEmpty(sensibleFields)){
            CommonRet result = new CommonRet();
            result.setCode(GeneralCode.SYS_VALID.getCode());
            result.setMessage(GeneralCode.SYS_VALID.getMessage());
            result.setData(ImmutableMap.of(SENSITIVE_FIELDS,sensibleFields));
            return result;
        }
        return new CommonRet();
    }

    @UserOperation(eventName = "NFT_Audit_Review", name = "NFT_Audit_Review",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/audit/review/text")
    public CommonRet<TextFileAuditResp> auditReviewText(@RequestBody @Valid @NotNull Map<String,String> request){

        APIResponse<TextFileAuditResp> response = contentAuditApi.
                textAudit(APIRequest.instance(request.get("text")));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
