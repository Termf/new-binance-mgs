package com.binance.mgs.nft.activity.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.activityservice.api.CR7InfoApi;
import com.binance.nft.activityservice.api.CR7UserApi;
import com.binance.nft.activityservice.request.CR7BoxOpenRequest;
import com.binance.nft.activityservice.request.CR7RedeemRequest;
import com.binance.nft.activityservice.request.Cr7NeedKycRequest;
import com.binance.nft.activityservice.response.CR7BoxOpenResponse;
import com.binance.nft.activityservice.response.CR7BoxOpenResultResponse;
import com.binance.nft.activityservice.response.CR7ConfigResponse;
import com.binance.nft.activityservice.response.Cr7NeedKycResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author joy
 * @date 2022/11/1 19:10
 */
@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class NftActivityCR7Controller {
    @Resource
    private CR7InfoApi cr7InfoApi;

    @Resource
    private CR7UserApi cr7UserApi;
    @Resource
    private BaseHelper baseHelper;

    @GetMapping("/public/nft/nft-activity/cr7/cr7-config")
    public CommonRet<CR7ConfigResponse> getCR7Config() {
        APIResponse<CR7ConfigResponse> response = cr7InfoApi.getCR7Config();
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-activity/cr7/redeem")
    public CommonRet<Void> redeem(@RequestBody CR7RedeemRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        APIResponse<Void> response = cr7InfoApi.redeem(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @GetMapping("/private/nft/nft-activity/cr7/need-kyc")
    public CommonRet<Cr7NeedKycResponse> needKyc() {
        Long userId = baseHelper.getUserId();
        Cr7NeedKycRequest request = new Cr7NeedKycRequest();
        request.setUserId(userId);
        APIResponse<Cr7NeedKycResponse> response = cr7UserApi.needKyc(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }



    @PostMapping("/private/nft/nft-activity/cr7/box-open")
    public CommonRet<CR7BoxOpenResponse> boxOpen(@RequestBody CR7BoxOpenRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        APIResponse<CR7BoxOpenResponse> response = cr7InfoApi.open(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/nft-activity/cr7/box-open-result")
    public CommonRet<CR7BoxOpenResultResponse> boxOpenResult(Long openId) {
        APIResponse<CR7BoxOpenResultResponse> response = cr7InfoApi.openResult(openId);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
