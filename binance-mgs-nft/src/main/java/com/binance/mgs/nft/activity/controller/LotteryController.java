package com.binance.mgs.nft.activity.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.activityservice.api.SpinLotteryApi;
import com.binance.nft.activityservice.request.LotteryHistoryRequest;
import com.binance.nft.activityservice.request.SpinLotteryRequest;
import com.binance.nft.activityservice.response.LotteryConfigResponse;
import com.binance.nft.activityservice.response.LotteryHistoryResponse;
import com.binance.nft.activityservice.response.SpinLotteryResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class LotteryController {

    @Resource
    private SpinLotteryApi lotteryApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private CrowdinHelper crowdinHelper;


    @GetMapping("/private/nft/activity/lottery-config")
    public CommonRet<LotteryConfigResponse> lotteryConfig() throws Exception {
        APIResponse<LotteryConfigResponse> response = lotteryApi.lotteryConfig();
        baseHelper.checkResponse(response);
        LotteryConfigResponse data = response.getData();
        if(StringUtils.isNotBlank(data.getTitle())) {
            data.setTitle(crowdinHelper.getMessageByKey(data.getTitle(), baseHelper.getLanguage()));
        }
        if(StringUtils.isNotBlank(data.getDescription())) {
            data.setDescription(crowdinHelper.getMessageByKey(data.getDescription(), baseHelper.getLanguage()));
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/spin-lottery")
    public CommonRet<SpinLotteryResponse> lottery(@Valid @RequestBody SpinLotteryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SpinLotteryResponse> response = lotteryApi.spinLottery(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/lottery-history")
    public CommonRet<LotteryHistoryResponse> lotteryHistory(@Valid @RequestBody LotteryHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<LotteryHistoryResponse> response = lotteryApi.lotteryHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
