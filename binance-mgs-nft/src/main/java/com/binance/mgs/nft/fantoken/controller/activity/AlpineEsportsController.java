package com.binance.mgs.nft.fantoken.controller.activity;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fantoken.helper.activity.AlpineEsportsI18nHelper;
import com.binance.mgs.nft.fanverse.helper.FanActivityRequestHelper;
import com.binance.nft.fantoken.activity.ifae.alpine.esports.IAlpineEsportsManageApi;
import com.binance.nft.fantoken.activity.request.alpine.esports.AlpineEsportsRequest;
import com.binance.nft.fantoken.activity.response.StatusResponse;
import com.binance.nft.fantoken.activity.response.alpine.esports.AlpineEsportsHomepageResponse;
import com.binance.nft.fantoken.activity.vo.alpine.esports.AlpineEsportsNftOpenStatusInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@SuppressWarnings("all")
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/activity/alpine")
@RestController
@RequiredArgsConstructor
public class AlpineEsportsController {

    /** fantoken tool */
    private final BaseHelper baseHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final FanActivityRequestHelper activityRequestHelper;
    private final AlpineEsportsI18nHelper alpineEsportsI18nHelper;

    private final IAlpineEsportsManageApi alpineEsportsManageApi;

    /**
     * <h2>homepage</h2>
     * 可以不登录
     * */
    @PostMapping("/esports-journey-homepage")
    public CommonRet<AlpineEsportsHomepageResponse> homepage(@RequestBody AlpineEsportsRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<AlpineEsportsHomepageResponse> response = alpineEsportsManageApi.homepage(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            alpineEsportsI18nHelper.doAlpineEsportsHomepageResponse(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户参与</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/claim-esports-journey-nft")
    public CommonRet<StatusResponse> participate(@RequestBody AlpineEsportsRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // SG 合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<StatusResponse> response = alpineEsportsManageApi.participate(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户打开</h2>
     * 需要登录, 需要 kyc
     * */
    @PostMapping("/open-esports-journey-nft")
    public CommonRet<StatusResponse> open(@RequestBody AlpineEsportsRequest request) {

        // 需要登录
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 需要 KYC
        boolean hasKyc = fanTokenCheckHelper.hasKyc(userId);
        if (!hasKyc) {
            return new CommonRet<>();
        }

        activityRequestHelper.initFanTokenBaseRequest(userId, hasKyc, request);
        APIResponse<StatusResponse> response = alpineEsportsManageApi.open(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户轮询打开</h2>
     * 需要登录, 需要 kyc
     * */
    @PostMapping("/round-esports-journey-nft")
    public CommonRet<AlpineEsportsNftOpenStatusInfo> round(@RequestBody AlpineEsportsRequest request) {

        // 需要登录
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 需要 KYC
        boolean hasKyc = fanTokenCheckHelper.hasKyc(userId);
        if (!hasKyc) {
            return new CommonRet<>();
        }

        activityRequestHelper.initFanTokenBaseRequest(userId, hasKyc, request);
        APIResponse<AlpineEsportsNftOpenStatusInfo> response = alpineEsportsManageApi.round(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response.getData())) {
            alpineEsportsI18nHelper.doAlpineEsportsNftOpenStatusInfo(response.getData());
        }

        return new CommonRet<>(response.getData());
    }
}
