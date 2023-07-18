package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenWorldCupI18nHelper;
import com.binance.mgs.nft.fantoken.helper.UserDeviceUtils;
import com.binance.nft.fantoken.ifae.worldcup.IWorldCupManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.CommonQueryByPageRequest;
import com.binance.nft.fantoken.request.FanTokenBaseRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupAppInviteDomainRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupDailyChallengeRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupLeaderboardRewardUserAddressRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupMatchDayLeaderboardRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupOnboardingRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupUserClaimRewardRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupUserInviteRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupUserPredictRequest;
import com.binance.nft.fantoken.response.worldcup.WorldCupAppInviteDomainResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupCountryNftWallResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupDailyChallengeResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupLeaderboardRewardResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupMatchDayLeaderboardResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupMatchDayRewardClaimResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupMatchDayRewardTierResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupOnboardingResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupRiskAppealResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupUserInviteResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupUserPredictResponse;
import com.binance.nft.fantoken.vo.prediction.PredictionUserAddressVO;
import com.binance.nft.fantoken.vo.worldcup.WorldCupMatchDayInfo;
import com.binance.nft.fantoken.vo.worldcup.WorldCupUserMatchDayDashboardInfo;
import com.binance.nft.fantoken.vo.worldcup.WorldCupUserMatchListDashboardInfo;
import com.binance.nft.fantoken.vo.worldcup.WorldCupVoucherInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/friendly/nft/fantoken/xyz0203/worldcup/xyz")
public class FanTokenWorldCupController {

    private final BaseHelper baseHelper;
    private final FanTokenWorldCupI18nHelper worldCupI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IWorldCupManageApi worldCupManageApi;

    private void fillingWorldCupOnboardingRequestBaseInfo(Long userId, WorldCupOnboardingRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(null != userId && fanTokenCheckHelper.hasKyc(userId));
    }

    private void fillingWorldCupUserInviteRequestBaseInfo(Long userId, WorldCupUserInviteRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(fanTokenCheckHelper.hasKyc(userId));
    }

    private void fillingWorldCupRiskInfo(FanTokenBaseRequest request) {

        request.setFvideoId(WebUtils.getHeader("fvideo-id"));
        request.setIp(IPUtils.getIp());

        Map<String, String> deviceInfo = UserDeviceUtils.safeBuildDeviceInfo(WebUtils.getHttpServletRequest());
        request.setDeviceName(deviceInfo.get("device_name"));
        request.setScreenResolution(deviceInfo.get("screen_resolution"));
        request.setSystemLang(deviceInfo.get("system_lang"));
        request.setSystemVersion(deviceInfo.get("system_version"));
        request.setTimezone(deviceInfo.get("timezone"));
        request.setPlatform(deviceInfo.get("platform"));
    }

    private void setResponseHeaders(Map<String, List<String>> headers) {

        if (MapUtils.isEmpty(headers)) {
            return;
        }

        headers.forEach((key,value)->{
            if (!value.isEmpty()) {
                value.forEach(v -> {
                    //((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse().setHeader(key, v);
                    WebUtils.getHttpServletResponse().addHeader(key, v);
                });

            }
        });
    }

    /**
     * <h2>触发巴林合规的响应</h2>
     * */
    private CommonRet buildBahreynComplianceRet(Long userId) {

        log.error("current request trigger bahreyn compliance: [userId={}]", null != userId ? userId : "anonmous");
        CommonRet<Object> ret = new CommonRet<>();
        ret.setCode(GeneralCode.SYS_ACCESS_LIMITED.getCode());
        ret.setMessage(GeneralCode.SYS_ACCESS_LIMITED.getMessage());
        return ret;
    }

    /**
     * <h2>巴林合规校验</h2>
     * */
    @PostMapping("/onboarding/check-bahreyn-compliance")
    public CommonRet<WorldCupOnboardingResponse> checkBahreynCompliance(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // sg 合规校验
        if (null != userId) {
            if (fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
                log.error("current request trigger sg compliance: [userId={}]", userId);
                return buildBahreynComplianceRet(userId);
            }
        }

        return new CommonRet<>();
    }

    //////////////////////////////////////////////////// Onboarding ////////////////////////////////////////////////////

    @PostMapping("/onboarding/user-info")
    public CommonRet<WorldCupOnboardingResponse> onboardingUserInfo(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupOnboardingResponse> response = worldCupManageApi.userInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/onboarding/landing-page-match")
    public CommonRet<WorldCupMatchDayInfo> landingPageMatch(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupMatchDayInfo> response = worldCupManageApi.landingPageMatch(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupOnboardingResponse(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/onboarding/user-claim-passport")
    public CommonRet<Void> userClaimPassport(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupRiskInfo(request);
        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<Void> response = worldCupManageApi.userClaimPassport(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/onboarding/query-user-claim-passport")
    public CommonRet<WorldCupOnboardingResponse> queryUserClaimPassport(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupOnboardingResponse> response = worldCupManageApi.queryUserClaimPassport(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户 passport 信息</h2>
     * 新增 token id
     * */
    @PostMapping("/onboarding/user-passport-page")
    public CommonRet<WorldCupOnboardingResponse> userPassportPage(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupOnboardingResponse> response = worldCupManageApi.userPassportPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/onboarding/user-country-nft-wall")
    public CommonRet<WorldCupCountryNftWallResponse> countryNftWall(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupCountryNftWallResponse> response = worldCupManageApi.countryNftWall(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupCountryNftWallResponse(response.getData());

        return new CommonRet<>(response.getData());
    }

    ////////////////////////////////////////////////// Daily Challenge //////////////////////////////////////////////////

    private void fillingWorldCupDailyChallengeRequestBaseInfo(Long userId, WorldCupDailyChallengeRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(null != userId && fanTokenCheckHelper.hasKyc(userId));
    }

    @PostMapping("/daily-challenge/match-list")
    public CommonRet<WorldCupDailyChallengeResponse> matchDayList(@RequestBody WorldCupDailyChallengeRequest request) {

        Long userId = baseHelper.getUserId();       // 可以不需要登录

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupDailyChallengeRequestBaseInfo(userId, request);

        APIResponse<WorldCupDailyChallengeResponse> response = worldCupManageApi.matchDayList(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupDailyChallengeResponse(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/match-list-dashboard")
    public CommonRet<WorldCupUserMatchListDashboardInfo> matchDayListDashboard(@RequestBody WorldCupDailyChallengeRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupDailyChallengeRequestBaseInfo(userId, request);
        APIResponse<WorldCupUserMatchListDashboardInfo> response = worldCupManageApi.matchDayListDashboard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/match-day")
    public CommonRet<WorldCupDailyChallengeResponse> matchDayDetail(@RequestBody WorldCupDailyChallengeRequest request) {

        Long userId = baseHelper.getUserId();       // 可以不需要登录

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupDailyChallengeRequestBaseInfo(userId, request);
        APIResponse<WorldCupDailyChallengeResponse> response = worldCupManageApi.matchDayDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupDailyChallengeResponse(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/match-day-dashboard")
    public CommonRet<WorldCupUserMatchDayDashboardInfo> matchDayDashboard(@RequestBody WorldCupDailyChallengeRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupDailyChallengeRequestBaseInfo(userId, request);
        APIResponse<WorldCupUserMatchDayDashboardInfo> response = worldCupManageApi.matchDayDashboard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/user-predict-match-day")
    public CommonRet<WorldCupUserPredictResponse> userPredictMatchDay(@RequestBody WorldCupUserPredictRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(fanTokenCheckHelper.hasKyc(userId));

        APIResponse<WorldCupUserPredictResponse> response = worldCupManageApi.userPredictMatchDay(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/polling-user-predict-match-day")
    public CommonRet<WorldCupUserPredictResponse> pollingUserPredictMatchDay(@RequestBody WorldCupUserPredictRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(fanTokenCheckHelper.hasKyc(userId));

        APIResponse<WorldCupUserPredictResponse> response = worldCupManageApi.pollingUserPredictMatchDay(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/user-claim-matchday-reward")
    public CommonRet<WorldCupMatchDayRewardClaimResponse> userClaimMatchDayReward(@RequestBody WorldCupUserClaimRewardRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId || StringUtils.isBlank(request.getMatchDayId())) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());

        fillingWorldCupRiskInfo(request);

        APIResponse<WorldCupMatchDayRewardClaimResponse> response = worldCupManageApi.userClaimMatchDayReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupMatchDayRewardClaimResponse(response.getData());

        // set risk header
        if (response.getData()!=null && response.getData().getRiskDetail()!=null) {
            setResponseHeaders(response.getData().getRiskDetail().getHeaders());
            // 不透传到前端
            response.getData().getRiskDetail().setHeaders(null);
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/user-claim-matchlist-reward")
    public CommonRet<WorldCupMatchDayRewardClaimResponse> userClaimMatchListReward(@RequestBody WorldCupUserClaimRewardRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());

        fillingWorldCupRiskInfo(request);

        APIResponse<WorldCupMatchDayRewardClaimResponse> response = worldCupManageApi.userClaimMatchListReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupMatchDayRewardClaimResponse(response.getData());

        // set risk header
        if (response.getData()!=null && response.getData().getRiskDetail()!=null) {
            setResponseHeaders(response.getData().getRiskDetail().getHeaders());
            // 不透传到前端
            response.getData().getRiskDetail().setHeaders(null);
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/query-new-user-voucher-distribute-info")
    public CommonRet<WorldCupVoucherInfo> queryNewUserVoucherDistributeInfo(@RequestBody WorldCupUserClaimRewardRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());

        APIResponse<WorldCupVoucherInfo> response = worldCupManageApi.queryNewUserVoucherDistributeInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    //////////////////////////////////////////////////// Leaderboard ////////////////////////////////////////////////////

    @PostMapping("/daily-challenge/match-day-leaderboard")
    public CommonRet<WorldCupMatchDayLeaderboardResponse> matchdayLeaderboard(@RequestBody WorldCupMatchDayLeaderboardRequest request) {

        Long userId = baseHelper.getUserId();       // 可以不需要登录

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(null != userId && fanTokenCheckHelper.hasKyc(userId));

        APIResponse<WorldCupMatchDayLeaderboardResponse> response = worldCupManageApi.matchdayLeaderboard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/worldcup-challenge/worldcup-leaderboard")
    public CommonRet<WorldCupMatchDayLeaderboardResponse> worldcupLeaderboard(@RequestBody CommonPageRequest<CommonQueryByPageRequest> request) {

        Long userId = baseHelper.getUserId();       // 可以不需要登录

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        CommonQueryByPageRequest pr = CommonQueryByPageRequest.builder()
                .query(CommonQueryByPageRequest.Query.builder().userId(userId).build())
                .build();
        request.setParams(pr);

        APIResponse<WorldCupMatchDayLeaderboardResponse> response = worldCupManageApi.worldcupLeaderboard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>好友排行榜</h2>
     * 2022.11.15 对于没有排行榜数据的情况, 仍然展示好友信息
     * */
    @PostMapping("/worldcup-challenge/friend-leaderboard")
    public CommonRet<WorldCupMatchDayLeaderboardResponse> friendLeaderboard(@RequestBody CommonPageRequest<CommonQueryByPageRequest> request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        CommonQueryByPageRequest pr = CommonQueryByPageRequest.builder()
                .query(CommonQueryByPageRequest.Query.builder().userId(userId).build())
                .build();
        request.setParams(pr);

        APIResponse<WorldCupMatchDayLeaderboardResponse> response = worldCupManageApi.friendLeaderboard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    ///////////////////////////////////////////////// Leaderboard Reward ////////////////////////////////////////////////

    @PostMapping("/worldcup-challenge/has-generate-leaderboard-reward")
    public CommonRet<WorldCupLeaderboardRewardResponse> hasGenerateLeaderboardReward(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupLeaderboardRewardResponse> response = worldCupManageApi.hasGenerateLeaderboardReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/worldcup-challenge/worldcup-leaderboard-reward-info")
    public CommonRet<WorldCupLeaderboardRewardResponse> worldcupLeaderboardRewardInfo(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupLeaderboardRewardResponse> response = worldCupManageApi.worldcupLeaderboardRewardInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupLeaderboardRewardResponse(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/worldcup-challenge/user-submit-information")
    public CommonRet<WorldCupRiskAppealResponse> userSubmitInformation(@RequestBody WorldCupLeaderboardRewardUserAddressRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        // 填充用户信息
        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());

        fillingWorldCupRiskInfo(request);

        APIResponse<WorldCupRiskAppealResponse> response = worldCupManageApi.userSubmitInformation(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // set risk header
        if (response.getData()!=null) {
            setResponseHeaders(response.getData().getHeaders());
            // 不透传到前端
            response.getData().setHeaders(null);
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/worldcup-challenge/view-user-information")
    public CommonRet<PredictionUserAddressVO> viewUserInformation(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<PredictionUserAddressVO> response = worldCupManageApi.viewUserInformation(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/worldcup-challenge/claim-leaderboard-nft")
    public CommonRet<WorldCupRiskAppealResponse> userClaimWorldCupLeaderboardNft(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId) || fanTokenCheckHelper.isSGUserForWorldCup(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        fillingWorldCupRiskInfo(request);
        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupRiskAppealResponse> response = worldCupManageApi.userClaimWorldCupLeaderboardNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // set risk header
        if (response.getData()!=null) {
            setResponseHeaders(response.getData().getHeaders());
            // 不透传到前端
            response.getData().setHeaders(null);
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/worldcup-challenge/query-claim-leaderboard-nft")
    public CommonRet<WorldCupOnboardingResponse> queryUserClaimWorldCupLeaderboardNft(@RequestBody WorldCupOnboardingRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        fillingWorldCupOnboardingRequestBaseInfo(userId, request);
        APIResponse<WorldCupOnboardingResponse> response = worldCupManageApi.queryUserClaimWorldCupLeaderboardNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupOnboardingResponseForLeaderboardNftReward(response.getData());

        return new CommonRet<>(response.getData());
    }

    ////////////////////////////////////////////////////// Invite //////////////////////////////////////////////////////

    @PostMapping("/invite/user-ref")
    public CommonRet<WorldCupUserInviteResponse> userRef(@RequestBody WorldCupUserInviteRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }
        request.setUserId(userId);
        // membership 风控信息
        fillingWorldCupRiskInfo(request);
        APIResponse<WorldCupUserInviteResponse> response = worldCupManageApi.userRef(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/app-domain")
    public CommonRet<WorldCupAppInviteDomainResponse> queryAppInviteDomain(@RequestBody WorldCupAppInviteDomainRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setUserId(userId);
        APIResponse<WorldCupAppInviteDomainResponse> response = worldCupManageApi.queryAppInviteDomain(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/query-friend-binding-info")
    public CommonRet<WorldCupUserInviteResponse> queryFriendBindingInfo(@RequestBody WorldCupUserInviteRequest request) {

        // 如果没有传递 ref 信息, 是异常请求, 直接返回
        if (StringUtils.isBlank(request.getRefId())) {
            return new CommonRet<>();
        }

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setUserId(userId);
        APIResponse<WorldCupUserInviteResponse> response = worldCupManageApi.queryFriendBindingInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/matchday-prediction")
    public CommonRet<WorldCupMatchDayInfo> matchDayPrediction(@RequestBody WorldCupUserInviteRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setUserId(userId);
        APIResponse<WorldCupMatchDayInfo> response = worldCupManageApi.matchDayPrediction(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        worldCupI18nHelper.doWorldCupOnboardingResponse(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/matchlist-reward")
    public CommonRet<WorldCupUserMatchListDashboardInfo> matchListReward(@RequestBody WorldCupUserInviteRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        request.setUserId(userId);
        APIResponse<WorldCupUserMatchListDashboardInfo> response = worldCupManageApi.matchListReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/daily-challenge/matchday-reward-tier-info")
    public CommonRet<WorldCupMatchDayRewardTierResponse> matchDayRewardTierInfo(@RequestBody WorldCupDailyChallengeRequest request) {

        Long userId = baseHelper.getUserId();

        // 巴林合规校验
        if (fanTokenCheckHelper.isTriggerBahreynCompliance(userId)) {
            return buildBahreynComplianceRet(userId);
        }

        APIResponse<WorldCupMatchDayRewardTierResponse> response = worldCupManageApi.matchDayRewardTierInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
