package com.binance.mgs.account.account.controller;

import com.binance.margin.api.bookkeeper.MarginAccountBridgeApi;
import com.binance.margin.api.bookkeeper.response.UserAssetSummaryResponse;
import com.binance.margin.api.profit.ProfitApi;
import com.binance.margin.api.profit.response.ProfitsResponse;
import com.binance.margin.isolated.api.profit.response.ProfitSummaryResponse;
import com.binance.margin.isolated.api.user.UserBridgeApi;
import com.binance.margin.isolated.api.user.response.AccountDetailsResponse;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.vo.margin.CrossMarginAccountDetailQueryArg;
import com.binance.mgs.account.account.vo.margin.CrossMarginProfitQueryArg;
import com.binance.mgs.account.account.vo.margin.IsolatedMarginDetailQueryArg;
import com.binance.mgs.account.account.vo.margin.IsolatedMarginProfileQueryArg;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/v1/private/account/subUser/margin")
public class SubUserMarginController extends BaseAction {


    @Autowired
    private ProfitApi profitApi;

    @Autowired
    private com.binance.margin.isolated.api.profit.ProfitApi isolatedProfitApi;

    @Autowired
    private VerifyRelationService verifyRelationService;

    @Autowired
    private MarginAccountBridgeApi marginAccountBridgeApi;

    @Autowired
    private UserBridgeApi userBridgeApi;

    @PostMapping("/cross-margin/profile")
    @ApiOperation(value = "查询全仓子账户不同时间段的资产收益率")
    public CommonRet<ProfitsResponse> queryCrossMarginProfile(@RequestBody @Validated CrossMarginProfitQueryArg arg) {
        Long subUserId = verifyRelationService.checkManageSubUserBindingAndGetSubUserId(arg.getEmail(),true);
        APIResponse<ProfitsResponse> apiResponse = profitApi.accountProfit(subUserId, arg.getType());
        checkResponse(apiResponse);
        return ok(apiResponse.getData());
    }

    @PostMapping("/cross-margin/details")
    @ApiOperation(value = "查询全仓子账户资产明细")
    public CommonRet<UserAssetSummaryResponse> queryCrossMarginAccountDetails(@RequestBody @Validated CrossMarginAccountDetailQueryArg arg) {
        Long subUserId = verifyRelationService.checkManageSubUserBindingAndGetSubUserId(arg.getEmail(),true);
        APIResponse<UserAssetSummaryResponse> apiResponse = marginAccountBridgeApi.userAssetSummary(subUserId);
        checkResponse(apiResponse);
        return ok(apiResponse.getData());
    }


    @PostMapping("/isolated-margin/profile")
    @ApiOperation(value = "查询逐仓子账户不同时间段的资产收益率")
    public CommonRet<ProfitSummaryResponse> queryIsolatedMarginProfile(@RequestBody @Validated IsolatedMarginProfileQueryArg arg) {
        Long subUserId = verifyRelationService.checkManageSubUserBindingAndGetSubUserId(arg.getEmail(),false);
        APIResponse<ProfitSummaryResponse> apiResponse = isolatedProfitApi.profit(subUserId, null, arg.getType());
        checkResponse(apiResponse);
        return ok(apiResponse.getData());
    }

    @PostMapping("/isolated-margin/details")
    @ApiOperation(value = "查询逐仓子账户资产明细")
    public CommonRet<AccountDetailsResponse> queryIsolatedMarginSummary(@RequestBody @Validated IsolatedMarginDetailQueryArg arg){
        Long subUserId = verifyRelationService.checkManageSubUserBindingAndGetSubUserId(arg.getEmail(),false);
        APIResponse<AccountDetailsResponse> apiResponse = userBridgeApi.accountDetails(subUserId, null, false);
        checkResponse(apiResponse);
        return ok(apiResponse.getData());
    }
}
