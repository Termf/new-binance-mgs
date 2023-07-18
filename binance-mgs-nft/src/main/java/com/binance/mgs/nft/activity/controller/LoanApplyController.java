package com.binance.mgs.nft.activity.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.activity.response.PageResponse;
import com.binance.nft.activityservice.api.NftLoanApi;
import com.binance.nft.activityservice.dto.UserOrderHistory;
import com.binance.nft.activityservice.request.LoanApplyRequest;
import com.binance.nft.activityservice.request.LoanPreApplyRequest;
import com.binance.nft.activityservice.response.LoanPreApplyResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author: felix
 * @date: 18.4.23
 * @description:
 */
@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class LoanApplyController {

    @Resource
    private NftLoanApi nftLoanApi;

    @Resource
    private BaseHelper baseHelper;

    @PostMapping(value = "/private/nft/activity/loan/pre-apply")
    public CommonRet<LoanPreApplyResponse> preApply(@RequestBody @Valid LoanPreApplyRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<LoanPreApplyResponse> response = nftLoanApi.preApply(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping(value = "/private/nft/activity/loan/apply")
    public CommonRet<String> apply(@RequestBody @Valid LoanApplyRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<String> response = nftLoanApi.apply(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/nft-activity/loan/apply-history")
    public CommonRet<SearchResult<UserOrderHistory>> userApplyOrders(@RequestParam("type") @NotNull Integer type, @RequestParam("page") @NotNull Integer page, @RequestParam("rows") @NotNull Integer rows) {
        APIResponse<SearchResult<UserOrderHistory>> listAPIResponse = nftLoanApi.userApplyOrders(baseHelper.getUserId(), type, page, rows);
        baseHelper.checkResponse(listAPIResponse);
        return new CommonRet<>(listAPIResponse.getData());
    }


}
