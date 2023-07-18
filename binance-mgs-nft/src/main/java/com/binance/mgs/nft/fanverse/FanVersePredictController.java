package com.binance.mgs.nft.fanverse;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fanverse.helper.FanActivityRequestHelper;
import com.binance.mgs.nft.fanverse.helper.FanVerseI18nHelper;
import com.binance.nft.fantoken.activity.ifae.bws.IBwsPredictManageApi;
import com.binance.nft.fantoken.activity.request.CommonPageRequest;
import com.binance.nft.fantoken.activity.request.FanTokenActivityBaseRequest;
import com.binance.nft.fantoken.activity.request.bws.utility.predict.BwsPredictCampaignRequest;
import com.binance.nft.fantoken.activity.request.bws.utility.predict.BwsPredictCampaignRewardRequest;
import com.binance.nft.fantoken.activity.request.bws.utility.predict.BwsPredictMatchRequest;
import com.binance.nft.fantoken.activity.response.CommonPageResponse;
import com.binance.nft.fantoken.activity.response.bws.utility.predict.BwsPredictCampaignLeaderBoardResponse;
import com.binance.nft.fantoken.activity.response.bws.utility.predict.BwsPredictCampaignResponse;
import com.binance.nft.fantoken.activity.response.bws.utility.predict.BwsPredictCampaignRewardResponse;
import com.binance.nft.fantoken.activity.response.bws.utility.predict.BwsPredictMatchResponse;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsCampaignRewardInfo;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/friendly/nft/bws/fanverse")
public class FanVersePredictController {
    private final BaseHelper baseHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final FanVerseI18nHelper fanVerseI18nHelper;
    private final FanActivityRequestHelper activityRequestHelper;
    private final IBwsPredictManageApi bwsPredictManageApi;


    @PostMapping("/predict/merchant-match-list")
    public CommonRet<CommonPageResponse<BwsPredictMatchResponse>> queryRecentPredictMatch(@RequestBody CommonPageRequest<BwsPredictMatchRequest> request) {
        BwsPredictMatchRequest params = request.getParams();
        if (params == null) {
            params = new BwsPredictMatchRequest();
        }
        activityRequestHelper.initFanTokenBaseRequest(params);
        request.setParams(params);
        APIResponse<CommonPageResponse<BwsPredictMatchResponse>> response = bwsPredictManageApi.queryMerchantPredictMatchPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())
                && CollectionUtils.isNotEmpty(response.getData().getData())) {
            response.getData().getData().forEach(data -> fanVerseI18nHelper.doBwsPredictMatchInfo(data.getMatchInfo()));
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/campaign-info")
    public CommonRet<BwsPredictCampaignResponse> queryPredictCampaignInfo(@RequestBody BwsPredictCampaignRequest request) {
        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BwsPredictCampaignResponse> response = bwsPredictManageApi.queryPredictCampaignInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            fanVerseI18nHelper.doBwsPredictCampaignInfo(response.getData().getCampaignInfo());
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/match-info")
    public CommonRet<BwsPredictMatchResponse> queryPredictMatchInfo(@RequestBody BwsPredictMatchRequest request) {
        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BwsPredictMatchResponse> response = bwsPredictManageApi.queryPredictMatchInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            fanVerseI18nHelper.doBwsPredictMatchInfo(response.getData().getMatchInfo());
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/user-predict")
    public CommonRet<Void> userPredictMatch(@RequestBody BwsPredictMatchRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // SG合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = bwsPredictManageApi.userPredictMatch(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("/predict/campaign-reward-info")
    public CommonRet<BwsPredictCampaignRewardResponse> queryCampaignRewardInfo(@RequestBody BwsPredictCampaignRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<BwsPredictCampaignRewardResponse> response = bwsPredictManageApi.queryCampaignRewardInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/campaign-leaderboard-info")
    public CommonRet<BwsPredictCampaignLeaderBoardResponse> queryCampaignLeaderBoardInfo(@RequestBody CommonPageRequest<BwsPredictCampaignRequest> request) {
        BwsPredictCampaignRequest params = request.getParams();
        if (params == null) {
            params = new BwsPredictCampaignRequest();
        }
        activityRequestHelper.initFanTokenBaseRequest(params);
        request.setParams(params);
        APIResponse<BwsPredictCampaignLeaderBoardResponse> response = bwsPredictManageApi.queryMerchantLeaderBoardInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/campaign-friends-leaderboard-info")
    public CommonRet<BwsPredictCampaignLeaderBoardResponse> queryFriendsCampaignLeaderBoardInfo(@RequestBody CommonPageRequest<BwsPredictCampaignRequest> request) {
        BwsPredictCampaignRequest params = request.getParams();
        if (params == null) {
            params = new BwsPredictCampaignRequest();
        }
        activityRequestHelper.initFanTokenBaseRequest(params);
        request.setParams(params);
        APIResponse<BwsPredictCampaignLeaderBoardResponse> response = bwsPredictManageApi.queryCampaignFriendsLeaderBoardInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/merchant-leaderboard-info")
    public CommonRet<BwsPredictCampaignLeaderBoardResponse> queryMerchantLeaderBoardInfo(@RequestBody CommonPageRequest<BwsPredictCampaignRequest> request) {
        BwsPredictCampaignRequest params = request.getParams();
        if (params == null) {
            params = new BwsPredictCampaignRequest();
        }
        activityRequestHelper.initFanTokenBaseRequest(params);
        request.setParams(params);
        APIResponse<BwsPredictCampaignLeaderBoardResponse> response = bwsPredictManageApi.queryMerchantLeaderBoardInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/claim-match-reward")
    public CommonRet<Void> claimPredictMatchReward(@RequestBody BwsPredictMatchRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 强制KYC校验
        fanTokenCheckHelper.userComplianceValidate(userId);
        // SG合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = bwsPredictManageApi.claimPredictMatchReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/page-user-merchant-reward")
    public CommonRet<CommonPageResponse<BwsCampaignRewardInfo>> queryUserMerchantPredictRewardPage(@RequestBody CommonPageRequest<FanTokenActivityBaseRequest> request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        FanTokenActivityBaseRequest params = request.getParams();
        if (params == null) {
            params = new FanTokenActivityBaseRequest();
        }
        activityRequestHelper.initFanTokenBaseRequest(userId, params);
        request.setParams(params);
        APIResponse<CommonPageResponse<BwsCampaignRewardInfo>> response = bwsPredictManageApi.queryUserMerchantPredictRewardPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/claim-campaign-reward")
    public CommonRet<Void> claimPredictCampaignReward(@RequestBody BwsPredictCampaignRewardRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 强制KYC校验
        fanTokenCheckHelper.userComplianceValidate(userId);
        // SG合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = bwsPredictManageApi.claimPredictCampaignReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/predict/update-campaign-reward-form")
    public CommonRet<Void> updateUserCampaignRewardForm(@RequestBody BwsPredictCampaignRewardRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 强制KYC校验
        fanTokenCheckHelper.userComplianceValidate(userId);
        // SG合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = bwsPredictManageApi.updateUserCampaignRewardForm(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
