package com.binance.mgs.nft.nftasset.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.nftasset.controller.helper.KycHelper;
import com.binance.nft.assetservice.api.data.vo.report.CreateReportVo;
import com.binance.nft.assetservice.api.data.vo.report.ReportReasonVo;
import com.binance.nft.assetservice.api.report.INftReportApi;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class NftReportController {

    private final INftReportApi nftReportApi;

    private final BaseHelper baseHelper;

    private final CrowdinHelper crowdinHelper;

    private final KycHelper kycHelper;

    @GetMapping("/public/nft/report/report-reason/list")
    public CommonRet<List<ReportReasonVo>> reportReasonList() {

        APIResponse<List<ReportReasonVo>> response = nftReportApi.reasonList();
        baseHelper.checkResponse(response);
        List<ReportReasonVo> data = response.getData();
        if (CollectionUtils.isNotEmpty(data)) {
            for (ReportReasonVo reportReasonVo : data) {
                String message = crowdinHelper.getMessageByKey(reportReasonVo.getReasonCode(), baseHelper.getLanguage());
                message = StringUtils.equals(message, reportReasonVo.getReasonCode()) ?
                        reportReasonVo.getReasonName()
                        : message;
                reportReasonVo.setReasonName(message);
            }
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/public/nft/audit/callback")
    public CommonRet<Boolean> callBack(@RequestBody String message) {

        log.error("audit file callback, the file is: [msg={}]", message);
        APIResponse<Boolean> response = nftReportApi.fileAuditCallback(APIRequest.instance(message));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Report_Create", name = "NFT_Report_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/report/create-report")
    public CommonRet<Boolean> createReport(@RequestBody @Valid CreateReportVo createReportVo) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        createReportVo.setUserId(userId);
        kycHelper.userComplianceValidate(userId);
        APIResponse<Boolean> response = nftReportApi.createReport(
                APIRequest.instance(createReportVo)
        );
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
