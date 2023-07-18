package com.binance.mgs.nft.fantoken.controller.activity;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fantoken.helper.activity.AlpineGpNftI18nHelper;
import com.binance.mgs.nft.fanverse.helper.FanActivityRequestHelper;
import com.binance.nft.fantoken.activity.ifae.alpine.IAlpineGpNftManageApi;
import com.binance.nft.fantoken.activity.request.FanTokenActivityBaseRequest;
import com.binance.nft.fantoken.activity.request.FilterRequest;
import com.binance.nft.fantoken.activity.request.alpine.gpnft.AlpineGpNftRequest;
import com.binance.nft.fantoken.activity.response.StatusResponse;
import com.binance.nft.fantoken.activity.response.alpine.gpnft.AlpineGpNftHomepageResponse;
import com.binance.nft.fantoken.activity.response.alpine.gpnft.QueryOpenAlpineGpNftResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
public class AlpineGpNftController {

    /** fantoken tool */
    private final BaseHelper baseHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final FanActivityRequestHelper activityRequestHelper;
    private final AlpineGpNftI18nHelper alpineGpNftI18nHelper;

    private final IAlpineGpNftManageApi alpineGpNftManageApi;

    /**
     * <h2>homepage</h2>
     * 可以不登录
     * */
    @PostMapping("/gp-nft-homepage")
    public CommonRet<AlpineGpNftHomepageResponse> homepage(@RequestBody FanTokenActivityBaseRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<AlpineGpNftHomepageResponse> response = alpineGpNftManageApi.homepage(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            alpineGpNftI18nHelper.doAlpineGpNftMatchInfo(response.getData().getMatchInfo());
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>match list</h2>
     * 可以不登录
     * */
    @PostMapping("/gp-nft-match-list")
    public CommonRet<AlpineGpNftHomepageResponse> matchList(@RequestBody FilterRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<AlpineGpNftHomepageResponse> response = alpineGpNftManageApi.matchList(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())
                && CollectionUtils.isNotEmpty(response.getData().getMatchList())) {
            response.getData().getMatchList().forEach(alpineGpNftI18nHelper::doAlpineGpNftMatchInfo);
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>领取 Gp Nft</h2>
     * 需要登录
     * */
    @PostMapping("/claim-gp-nft")
    public CommonRet<StatusResponse> claimGpNft(@RequestBody AlpineGpNftRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<StatusResponse> response = alpineGpNftManageApi.claimGpNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>领取 Gp Nft</h2>
     * 需要登录
     * */
    @PostMapping("/open-gp-nft")
    public CommonRet<StatusResponse> openGpNft(@RequestBody AlpineGpNftRequest request) {

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
        APIResponse<StatusResponse> response = alpineGpNftManageApi.openGpNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>领取 Gp Nft</h2>
     * 需要登录
     * */
    @PostMapping("/query-open-gp-nft")
    public CommonRet<QueryOpenAlpineGpNftResponse> queryOpenGpNft(@RequestBody AlpineGpNftRequest request) {

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
        APIResponse<QueryOpenAlpineGpNftResponse> response = alpineGpNftManageApi.queryOpenGpNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            alpineGpNftI18nHelper.doFtNftInfo(response.getData().getNftInfo());
        }

        return new CommonRet<>(response.getData());
    }
}
