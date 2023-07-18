package com.binance.mgs.nft.activity.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.access.DecodeCheck;
import com.binance.mgs.nft.activity.request.NftConsumeRequest;
import com.binance.mgs.nft.activity.response.NftDistributeHisVo;
import com.binance.mgs.nft.activity.response.PageResponse;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.mgs.nft.trade.config.TradeConfig;
import com.binance.nft.activityservice.api.ActivityInfoApi;
import com.binance.nft.activityservice.request.*;
import com.binance.nft.activityservice.response.*;
import com.binance.nft.activityservice.vo.NftSubActivityVo;
import com.binance.nft.activityservice.vo.RewardsPendingVo;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.api.client.util.Lists;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * @author: felix
 * @date: 15.3.22
 * @description:
 */
@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class NftActivityController {

    @Resource
    private ActivityInfoApi activityInfoApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private TradeConfig tradeConfig;
    @Resource
    private NftInboxHelper nftInboxHelper;

    @GetMapping("/public/nft/activity/info")
    public CommonRet<NftSubActivityVo> activityInfo(@RequestParam(value = "subActivityCode") String subActivityCode, HttpServletRequest httpServletRequest) throws Exception {
        ActivityInfoRequest request = new ActivityInfoRequest();
        request.setSubActivityCode(Long.parseLong(subActivityCode));
        request.setIsGray(isGray(httpServletRequest) ? 1 : 0);
        APIResponse<NftSubActivityVo> response = activityInfoApi.activityinfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/activity/confirmResult")
    public CommonRet<DistributeConfirmResult> confirmResult(@RequestParam(value = "historyCode") String historyCode) throws Exception {
        APIResponse<DistributeConfirmResult> response = activityInfoApi.confirmResult(APIRequest.instance(historyCode));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/isParticipate")
    public CommonRet<NftParticipateHistoryResponse> isParticipate(@Valid @RequestBody ParticipateHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<NftParticipateHistoryResponse> response = activityInfoApi.isParticipate(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @UserOperation(eventName = "NFT_Activity_distribute", name = "NFT_Activity_distribute",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GoogleRecaptha("/private/nft/activity/activityNftDistribute")
    @PostMapping("/private/nft/activity/activityNftDistribute")
    public CommonRet<NftActicityDistributeResponse> activityNftDistribute(@Valid @RequestBody SubActivityCodeRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<NftActicityDistributeResponse> response = activityInfoApi.activityNftDistribute(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-activity/distribute-history")
    public CommonRet<PageResponse<NftDistributeHisVo>> distributeHistroy(@Valid @RequestBody DistributeHistroyPageRequest request)throws Exception{
        request.setUserId(baseHelper.getUserId());
        APIResponse<DistributeHistroryPageResponse> response = activityInfoApi.distributeHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        PageResponse<NftDistributeHisVo> retResult = new PageResponse<>();
        if (response.getData() != null && CollectionUtils.isNotEmpty(response.getData().getRecords())) {
            List<NftDistributeHisVo> records = Lists.newArrayList();
            List<Long> bizIdList = Lists.newArrayList();
            response.getData().getRecords().forEach(vo->{
                records.add(CopyBeanUtils.fastCopy(vo, NftDistributeHisVo.class));
                bizIdList.add(vo.getId());
            });
            retResult.setPage(response.getData().getPage());
            retResult.setTotal(response.getData().getTotal());
            retResult.setRecords(records);
            nftInboxHelper.appendHistoryFlag(baseHelper.getUserId(), records, Arrays.asList(new BizIdModel(HistoryType.DISTRIBUTION.name(),bizIdList)), NftDistributeHisVo::getId, NftDistributeHisVo::setUnreadFlag);
        }

        return new CommonRet<>(retResult);
    }

    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);
    }

    @GetMapping("/private/nft/activity/rewardsPendingList")
    public CommonRet<List<RewardsPendingVo>> rewardsPendingList()throws Exception{
        APIResponse<List<RewardsPendingVo>> response = activityInfoApi.rewardsPendingList(APIRequest.instance(baseHelper.getUserId()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @DecodeCheck(headerSignature = "activity-signature",privateKey = "activityPrivateKey",checkParameter = {"userId","code","timestamp","subActivityCode"})
    @PostMapping("/public/nft/activity/consume")
    public CommonRet<ActivityCodeConsumeResponse> consume( @RequestBody NftConsumeRequest request) throws Exception {

        ActivityCodeConsumeRequest build = ActivityCodeConsumeRequest.builder().subActivityCode(request.getSubActivityCode()).userId(request.getUserId())
                .code(request.getCode()).build();
        log.info("consume request = " + JsonUtils.toJsonHasNullKey(build));
        APIResponse<ActivityCodeConsumeResponse> response = activityInfoApi.consume(APIRequest.instance(build));
        log.info("consume response = " + JsonUtils.toJsonHasNullKey(response));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
