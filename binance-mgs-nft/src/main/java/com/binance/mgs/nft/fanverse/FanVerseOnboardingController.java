package com.binance.mgs.nft.fanverse;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fanverse.helper.FanActivityRequestHelper;
import com.binance.mgs.nft.fanverse.helper.FanVerseI18nHelper;
import com.binance.nft.fantoken.activity.ifae.bws.IBwsManageApi;
import com.binance.nft.fantoken.activity.ifae.bws.IBwsPredictManageApi;
import com.binance.nft.fantoken.activity.request.CommonPageRequest;
import com.binance.nft.fantoken.activity.request.CommonQueryByPageRequest;
import com.binance.nft.fantoken.activity.request.bws.onboarding.BwsOnboardingUserRequest;
import com.binance.nft.fantoken.activity.request.bws.utility.BwsUtilityVoteUserRequest;
import com.binance.nft.fantoken.activity.request.bws.utility.predict.BwsPredictMatchRequest;
import com.binance.nft.fantoken.activity.response.CommonPageResponse;
import com.binance.nft.fantoken.activity.response.bws.onboarding.BwsOnboardingUserResponse;
import com.binance.nft.fantoken.activity.vo.bws.onboarding.BwsMerchantInfo;
import com.binance.nft.fantoken.activity.vo.bws.utility.BinanceUserInfo;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsVoteInfo;
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
public class FanVerseOnboardingController {

    private final BaseHelper baseHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final FanVerseI18nHelper fanVerseI18nHelper;
    private final FanActivityRequestHelper activityRequestHelper;
    private final IBwsManageApi bwsManageApi;
    private final IBwsPredictManageApi bwsPredictManageApi;

    /**
     * <h2>merchant info</h2>
     * 可以不登录
     * */
    @PostMapping("/onboarding/page-merchant-info")
    public CommonRet<CommonPageResponse<BwsMerchantInfo>> pageMerchantInfo(@RequestBody CommonPageRequest<
            CommonQueryByPageRequest> request) {

        APIResponse<CommonPageResponse<BwsMerchantInfo>> response = bwsManageApi.pageMerchantInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())
                && CollectionUtils.isNotEmpty(response.getData().getData())) {
            response.getData().getData().forEach(fanVerseI18nHelper::doBwsMerchantInfo);
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>homepage 信息</h2>
     * 可以不登录
     * */
    @PostMapping("/onboarding/user-info")
    public CommonRet<BwsOnboardingUserResponse> userInfo(@RequestBody BwsOnboardingUserRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BwsOnboardingUserResponse> response = bwsManageApi.userInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanVerseI18nHelper.doOnboardingUserInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户领取 passport</h2>
     * 需要登录, 但是可以不需要 kyc
     * */
    @PostMapping("/onboarding/user-claim-passport")
    public CommonRet<BwsOnboardingUserResponse> userClaimPassport(@RequestBody BwsOnboardingUserRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // SG 合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<BwsOnboardingUserResponse> response = bwsManageApi.userClaimPassport(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>vote 列表页</h2>
     * 可以不登录
     * */
    @PostMapping("/vote/page-vote-info")
    public CommonRet<CommonPageResponse<BwsVoteInfo>> queryPageVoteInfo(@RequestBody BwsUtilityVoteUserRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<CommonPageResponse<BwsVoteInfo>> response = bwsManageApi.queryPageVoteInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData()) && CollectionUtils.isNotEmpty(
                response.getData().getData())) {
            response.getData().getData().forEach(fanVerseI18nHelper::doBwsVoteInfo);
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>vote 详情页</h2>
     * 可以不登录
     * */
    @PostMapping("/vote/vote-info")
    public CommonRet<BwsVoteInfo> queryVoteInfo(@RequestBody BwsUtilityVoteUserRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BwsVoteInfo> response = bwsManageApi.queryVoteInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanVerseI18nHelper.doBwsVoteInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>vote update 详情</h2>
     * 可以不登录
     * */
    @PostMapping("/vote/vote-update-info")
    public CommonRet<BwsVoteInfo> queryVoteUpdateInfo(@RequestBody BwsUtilityVoteUserRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BwsVoteInfo> response = bwsManageApi.queryVoteUpdateInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanVerseI18nHelper.doBwsVoteInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户 vote</h2>
     * 需要登录, 但是可以不需要 kyc
     * */
    @PostMapping("/vote/user-vote")
    public CommonRet<Void> userVote(@RequestBody BwsUtilityVoteUserRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // SG 合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = bwsManageApi.userVote(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    /**
     * <h2>vote 统计信息</h2>
     * 可以不登录
     * */
    @PostMapping("/vote/vote-statistic-info")
    public CommonRet<BwsVoteInfo> queryVoteStatisticInfo(@RequestBody BwsUtilityVoteUserRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BwsVoteInfo> response = bwsManageApi.queryVoteStatisticInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>通过 RefId 查询所属用户信息</h2>
     * 可以不登录
     * */
    @PostMapping("/invite/query-userinfo-by-ref")
    public CommonRet<BinanceUserInfo> queryUserInfoByRef(@RequestBody BwsPredictMatchRequest request) {

        activityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<BinanceUserInfo> response = bwsManageApi.queryUserInfoByRef(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>通过 RefId 查询所属用户信息</h2>
     * 需要登录
     * */
    @PostMapping("/invite/binding-user-invite-relation")
    public CommonRet<Void> bindingUserInviteRelation(@RequestBody BwsPredictMatchRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // SG 合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        activityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = bwsManageApi.bindingUserInviteRelation(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

}
