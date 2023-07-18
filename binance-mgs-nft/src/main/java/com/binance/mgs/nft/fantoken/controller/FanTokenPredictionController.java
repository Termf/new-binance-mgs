package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.prediction.IPredictionUserManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.prediction.CreatePredictionInviteQRCodeRequest;
import com.binance.nft.fantoken.request.prediction.CreatePredictionUserAddressRequest;
import com.binance.nft.fantoken.request.prediction.PredictionRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.prediction.CreatePredictionInviteQRCodeResponse;
import com.binance.nft.fantoken.vo.SimpleTeamInfo;
import com.binance.nft.fantoken.vo.prediction.InviterPosterVO;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignEventInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignLeaderboardInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignRewardsInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignUserInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionProfileInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionUserAddressVO;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h1>FanToken Prediction</h1>
 * 新增备注:
 *  1. Prediction 增加 socialBanner
 * */
@SuppressWarnings("all")
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/prediction/xyz")
@RestController
@RequiredArgsConstructor
public class FanTokenPredictionController {

    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IPredictionUserManageApi predictionUserManageApi;

    @GetMapping("/prediction-team")
    public CommonRet<List<SimpleTeamInfo>> queryPredictionFanTokenTeamInfo() {

        boolean isGray = fanTokenCheckHelper.isGray();
        Long userId = baseHelper.getUserId();

        PredictionRequest request = PredictionRequest.builder()
                .isGray(isGray)
                .userId(userId)
                .clientType(baseHelper.getClientType())
                .build();

        APIResponse<List<SimpleTeamInfo>> response = predictionUserManageApi.queryPredictionFanTokenTeamInfo(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);
        List<SimpleTeamInfo> result = fanTokenCheckHelper.getGccComplianceTeamInfo(isGray, userId, response.getData());

        // i18n
        fanTokenI18nHelper.doSimpleTeamInfoList(result);
        return new CommonRet<>(result);
    }

    @PostMapping("/prediction-campaign-event")
    public CommonRet<PredictionCampaignEventInfo> predictionCampaignEvent(@RequestBody CommonPageRequest<
            PredictionRequest> request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request.getParams()) {
            request.getParams().setIsGray(fanTokenCheckHelper.isGray());
            request.getParams().setUserId(baseHelper.getUserId());
            request.getParams().setClientType(baseHelper.getClientType());
        }

        APIResponse<PredictionCampaignEventInfo> response = predictionUserManageApi.predictionCampaignEvent(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData() && null != response.getData().getPredictionCampaigns()) {
            fanTokenI18nHelper.doPredictionCampaignSimpleInfo(response.getData().getPredictionCampaigns().getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/prediction-campaign-profile")
    public CommonRet<CommonPageResponse<PredictionCampaignInfo>> predictionCampaignProfile(@RequestBody CommonPageRequest<
            PredictionRequest> request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request.getParams()) {
            request.getParams().setIsGray(fanTokenCheckHelper.isGray());
            request.getParams().setUserId(baseHelper.getUserId());
            request.getParams().setClientType(baseHelper.getClientType());
        }

        APIResponse<CommonPageResponse<PredictionCampaignInfo>> response = predictionUserManageApi.predictionCampaignProfile(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData() && CollectionUtils.isNotEmpty(response.getData().getData())) {
            response.getData().getData().forEach(fanTokenI18nHelper::doPredictionCampaignInfo);
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/prediction-campaign-leaderboard")
    public CommonRet<PredictionCampaignLeaderboardInfo> predictionCampaignLeaderboard(@RequestBody CommonPageRequest<
            PredictionRequest> request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request.getParams()) {
            request.getParams().setIsGray(fanTokenCheckHelper.isGray());
            request.getParams().setUserId(baseHelper.getUserId());
            request.getParams().setClientType(baseHelper.getClientType());
        }

        APIResponse<PredictionCampaignLeaderboardInfo> response = predictionUserManageApi.predictionCampaignLeaderboard(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doPredictionCampaignLeaderboardInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/prediction-campaign-rewards")
    public CommonRet<PredictionCampaignRewardsInfo> predictionCampaignRewards(@RequestBody CommonPageRequest<
            PredictionRequest> request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request.getParams()) {
            request.getParams().setIsGray(fanTokenCheckHelper.isGray());
            request.getParams().setUserId(baseHelper.getUserId());
            request.getParams().setClientType(baseHelper.getClientType());
        }

        APIResponse<PredictionCampaignRewardsInfo> response = predictionUserManageApi.predictionCampaignRewards(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doPredictionCampaignRewardsInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/user-prediction-score")
    public CommonRet<Void> userPredictionScore(@RequestBody PredictionRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        APIResponse<Void> response = predictionUserManageApi.userPredictionScore(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("/prediction-profile")
    public CommonRet<PredictionProfileInfo> predictionProfile(@RequestBody PredictionRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());

        APIResponse<PredictionProfileInfo> response = predictionUserManageApi.predictionProfile(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doPredictionProfileInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/user-claimed-rewards")
    public CommonRet<Void> predictionUserClaimedRewards(@RequestBody PredictionRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());

        APIResponse<Void> response = predictionUserManageApi.predictionUserClaimedRewards(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("/user-submit-address")
    public CommonRet<Void> predictionUserSubmitAddress(@RequestBody CreatePredictionUserAddressRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);

        APIResponse<Void> response = predictionUserManageApi.predictionUserSubmitAddress(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("/user-view-address")
    public CommonRet<PredictionUserAddressVO> predictionUserViewAddress(@RequestBody PredictionRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());

        APIResponse<PredictionUserAddressVO> response = predictionUserManageApi.predictionUserViewAddress(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite-create-poster")
    public CommonRet<InviterPosterVO> inviteCreatePoster(@RequestBody PredictionRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        APIResponse<InviterPosterVO> response = predictionUserManageApi.inviteCreatePoster(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doInviterPosterVO(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite-poster-info")
    public CommonRet<InviterPosterVO> invitePosterInfo(@RequestBody PredictionRequest request) {

        // 不需要用户登录
        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());

        APIResponse<InviterPosterVO> response = predictionUserManageApi.invitePosterInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doInviterPosterVO(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite-accept-invitation")
    public CommonRet<Void> inviteAcceptInvitation(@RequestBody PredictionRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        APIResponse<Void> response = predictionUserManageApi.inviteAcceptInvitation(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/invite-campaign-leaderboard")
    public CommonRet<CommonPageResponse<PredictionCampaignUserInfo>> inviteCampaignLeaderboard(
            @RequestBody CommonPageRequest<PredictionRequest> request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request.getParams()) {
            request.getParams().setIsGray(fanTokenCheckHelper.isGray());
            request.getParams().setUserId(baseHelper.getUserId());
            request.getParams().setClientType(baseHelper.getClientType());
        }

        APIResponse<CommonPageResponse<PredictionCampaignUserInfo>> response =
                predictionUserManageApi.inviteCampaignLeaderboard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite-create-qrcode")
    public CommonRet<CreatePredictionInviteQRCodeResponse> createQRCode(@RequestBody CreatePredictionInviteQRCodeRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        // 填充 userId 和 local
        request.setUserId(userId);
        request.setLocale(baseHelper.getLanguage());

        APIResponse<CreatePredictionInviteQRCodeResponse> response = predictionUserManageApi.createQRCode(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
