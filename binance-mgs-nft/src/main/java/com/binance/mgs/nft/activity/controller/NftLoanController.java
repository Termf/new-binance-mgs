package com.binance.mgs.nft.activity.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.activityservice.api.NftLoanApi;
import com.binance.nft.activityservice.request.LoanPreApplyRequest;
import com.binance.nft.activityservice.request.LoanRepayRequest;
import com.binance.nft.activityservice.response.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class NftLoanController {

    @Resource
    private NftLoanApi nftLoanApi;

    @Resource
    private BaseHelper baseHelper;


    @PostMapping("/friendly/nft/nft-activity/loan/config")
    public CommonRet<List<LoanConfigResponse>> config() {
        APIResponse<List<LoanConfigResponse>> response = nftLoanApi.config();
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @GetMapping("/private/nft/nft-activity/loan/user-asset")
    public CommonRet<List<LoanUserAssetResponse>> userAsset() {
        APIResponse<List<LoanUserAssetResponse>>  response = nftLoanApi.userAsset(baseHelper.getUserId());
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @GetMapping("/private/nft/nft-activity/loan/repay-check")
    public CommonRet<LoanRepayCheckResponse> repayCheck(Long orderNo) {
        APIResponse<LoanRepayCheckResponse>  response = nftLoanApi.repayCheck(orderNo);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    @PostMapping("/private/nft/nft-activity/loan/repay")
    public CommonRet<Void> repay(@RequestBody LoanRepayRequest request) {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void>  response = nftLoanApi.repay(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }


    @GetMapping("/private/nft/nft-activity/loan/apply-detail")
    public CommonRet<LoanDetailResponse> applyDetail(Long orderNo, Integer page, Integer rows) {
        APIResponse<LoanDetailResponse>  response = nftLoanApi.loanDetail(orderNo, page, rows);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


}
