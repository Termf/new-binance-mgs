package com.binance.mgs.nft.user.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.tradeservice.api.IUserBillingApi;
import com.binance.nft.tradeservice.request.UserBillingRequest;
import com.binance.nft.tradeservice.response.UserBillingItemVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api
@Slf4j
@RestController
@RequestMapping("/v1/private/nft/")
public class UserBillingController {

    @Resource
    private BaseHelper baseHelper;
    @Resource
    private CrowdinHelper crowdinHelper;
    @Resource
    private IUserBillingApi userBillingApi;

    @GetMapping("nft-account/funds-detail/list")
    public CommonRet<SearchResult<UserBillingItemVo>> fundsDetailList(UserBillingRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SearchResult<UserBillingItemVo>> response = userBillingApi.fundsDetailList(request);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("nft-account/funds-detail/export-submit")
    public CommonRet<String> exportSubmit(@RequestBody UserBillingRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<String> response = userBillingApi.fundsDetailExportSubmit(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("nft-account/funds-detail/export-result")
    public CommonRet<String> exportResult(String token) {
        APIResponse<String> response = userBillingApi.fundsDetailExportResult(token);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


}
