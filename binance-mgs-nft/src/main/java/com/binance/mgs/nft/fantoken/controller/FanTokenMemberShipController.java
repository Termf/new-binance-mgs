package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenMemberShipI18nHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenRequestHelper;
import com.binance.nft.fantoken.ifae.membership.IMemberShipInviteManageApi;
import com.binance.nft.fantoken.ifae.membership.IMemberShipRewardManageApi;
import com.binance.nft.fantoken.ifae.membership.IMemberShipTaskManageApi;
import com.binance.nft.fantoken.ifae.membership.IMemberShipTierManageApi;
import com.binance.nft.fantoken.ifae.worldcup.IWorldCupManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.FanTokenBaseRequest;
import com.binance.nft.fantoken.request.fanshop.OrderIdRequest;
import com.binance.nft.fantoken.request.membership.MemberShipInviteClaimRequest;
import com.binance.nft.fantoken.request.membership.MemberShipRewardRequest;
import com.binance.nft.fantoken.request.membership.MemberShipTaskRequest;
import com.binance.nft.fantoken.request.membership.QueryInviteEventConfigRequest;
import com.binance.nft.fantoken.request.membership.QueryUserFriendsInfosByPageRequest;
import com.binance.nft.fantoken.request.membership.QueryUserInviteStatisticInfoRequest;
import com.binance.nft.fantoken.request.worldcup.WorldCupUserInviteRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.membership.MemberShipInviteClaimRewardResponse;
import com.binance.nft.fantoken.response.membership.MemberShipInviteEventConfigResponse;
import com.binance.nft.fantoken.response.membership.MemberShipInviteEventStatisticInfoResponse;
import com.binance.nft.fantoken.response.membership.MemberShipInviteLeaderBoardResponse;
import com.binance.nft.fantoken.response.membership.MemberShipRewardUserPaymentResponse;
import com.binance.nft.fantoken.response.membership.MemberShipTaskResponse;
import com.binance.nft.fantoken.response.membership.MemberShipTierInfoResponse;
import com.binance.nft.fantoken.response.membership.MemberShipUserTierInfoResponse;
import com.binance.nft.fantoken.response.membership.OpenMysteryNftOrderResponse;
import com.binance.nft.fantoken.response.membership.QueryUserClaimBadgeResponse;
import com.binance.nft.fantoken.response.membership.QueryUserInviteInfoSimpleResponse;
import com.binance.nft.fantoken.response.membership.RoundMysteryNftOrderResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupUserInviteResponse;
import com.binance.nft.fantoken.vo.FanTokenNftItemInfo;
import com.binance.nft.fantoken.vo.membership.InviteLeaderBoardInfo;
import com.binance.nft.fantoken.vo.membership.MemberShipRewardInfo;
import com.binance.nft.fantoken.vo.membership.MemberShipTaskInfo;
import com.binance.nft.fantoken.vo.worldcup.WorldCupVoucherInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * <h1>MemberShip</h1>
 * */
@SuppressWarnings("all")
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/membership")
@RestController
@RequiredArgsConstructor
public class FanTokenMemberShipController {

    /** fantoken tool */
    private final BaseHelper baseHelper;
    private final FanTokenMemberShipI18nHelper memberShipI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final FanTokenRequestHelper fanTokenRequestHelper;

    /** membership api */
    private final IMemberShipTierManageApi memberShipTierManageApi;
    private final IMemberShipTaskManageApi memberShipTaskManageApi;
    private final IMemberShipRewardManageApi memberShipRewardManageApi;
    private final IMemberShipInviteManageApi memberShipInviteManageApi;
    private final IWorldCupManageApi worldCupManageApi;

    @PostMapping("/tier/tier-info")
    public CommonRet<MemberShipTierInfoResponse> tierInfo(@RequestBody FanTokenBaseRequest request) {

        fanTokenRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<MemberShipTierInfoResponse> response = memberShipTierManageApi.tierInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        memberShipI18nHelper.doTierInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>MemberShip 用户信息</h2>
     * */
    @PostMapping("/tier/user-info")
    public CommonRet<MemberShipUserTierInfoResponse> userInfo(@RequestBody FanTokenBaseRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, request);
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipParticipateComplianceCheck(userId);

        APIResponse<MemberShipUserTierInfoResponse> response = memberShipTierManageApi.userInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // 如果是不合规用户, 多一步填充工作
        if (isNonComplianceUser) {
            // 当返回的 badge info 存在时
            if (Objects.nonNull(response) && Objects.nonNull(response.getData()) && Objects.nonNull(response.getData().getBadgeInfo())) {
                FanTokenNftItemInfo badgeInfo = response.getData().getBadgeInfo();
                // 如果还没有领取
                if (!badgeInfo.getIsClaimed()) {
                    badgeInfo.setCanNotClaimReason(NumberUtils.INTEGER_ONE.toString());
                    badgeInfo.setCanClaim(Boolean.FALSE);
                }
            }
            log.error("fantoken user is non compliance user (in tierUserInfo): [userId={}], [hasKyc={}], [isNonComplianceUser={}]",
                    userId, request.getHasKyc(), isNonComplianceUser);
        }

        memberShipI18nHelper.doUserInfo(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/tier/user-claim-badge")
    public CommonRet<Void> userClaimBadge(@RequestBody FanTokenBaseRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, request);
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipParticipateComplianceCheck(userId);

        if (isNonComplianceUser) {
            log.error("fantoken user is non compliance user (in userClaimBadge): [userId={}], [hasKyc={}], [isNonComplianceUser={}]",
                    userId, request.getHasKyc(), isNonComplianceUser);
            return new CommonRet<>();
        }

        APIResponse<Void> response = memberShipTierManageApi.userClaimBadge(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("/tier/query-user-claim-badge")
    public CommonRet<QueryUserClaimBadgeResponse> queryUserClaimBadge(@RequestBody FanTokenBaseRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipParticipateComplianceCheck(userId);
        // 如果是不合规用户, 直接 fake 返回即可
        if (isNonComplianceUser) {
            QueryUserClaimBadgeResponse fakeResp = QueryUserClaimBadgeResponse.builder()
                    .nftInfo(FanTokenNftItemInfo.builder().canNotClaimReason(NumberUtils.INTEGER_ONE.toString()).build())
                    .distributeError(Boolean.TRUE)
                    .build();
            return new CommonRet<>(fakeResp);
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<QueryUserClaimBadgeResponse> response = memberShipTierManageApi.queryUserClaimBadge(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/task/task-center")
    public CommonRet<CommonPageResponse<MemberShipTaskInfo>> taskCenter(@RequestBody MemberShipTaskRequest request) {

        fanTokenRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<CommonPageResponse<MemberShipTaskInfo>> response = memberShipTaskManageApi.taskCenter(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        memberShipI18nHelper.doTaskCenter(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/task/landing-page")
    public CommonRet<MemberShipTaskResponse> landingPageTask(@RequestBody MemberShipTaskRequest request) {

        fanTokenRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<MemberShipTaskResponse> response = memberShipTaskManageApi.landingPageTask(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        memberShipI18nHelper.doLandingPageTask(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/task/user-claim-task")
    public CommonRet<Void> userClaimTask(@RequestBody MemberShipTaskRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = memberShipTaskManageApi.userClaimTask(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/reward/reward-pool")
    public CommonRet<CommonPageResponse<MemberShipRewardInfo>> rewardPool(@RequestBody MemberShipRewardRequest request) {

        fanTokenRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<CommonPageResponse<MemberShipRewardInfo>> response = memberShipRewardManageApi.rewardPool(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        memberShipI18nHelper.doRewardPool(response.getData());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/reward/user-claim-token-reward")
    public CommonRet<WorldCupVoucherInfo> claimTokenReward(@RequestBody MemberShipRewardRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        boolean hasKyc = fanTokenCheckHelper.hasKyc(userId);
        // 一定要通过 kyc, 才能领取 reward
        if (!hasKyc) {
            return new CommonRet<>();
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, hasKyc, request);

        // 合规校验
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipComplianceCheck(userId);
        if (isNonComplianceUser) {
            log.error("fantoken user is non compliance user (in claimTokenReward): [userId={}], [hasKyc={}], [isNonComplianceUser={}]",
                    userId, hasKyc, isNonComplianceUser);
            return new CommonRet<>();
        }

        APIResponse<WorldCupVoucherInfo> response = memberShipRewardManageApi.claimTokenReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/reward/open-mystery-nft-order")
    public CommonRet<OpenMysteryNftOrderResponse> openMysteryNftOrder(@RequestBody OrderIdRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);  // 需要登录
        APIResponse<OpenMysteryNftOrderResponse> response = memberShipRewardManageApi.openMysteryNftOrder(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/reward/round-open-mystery-nft-order")
    public CommonRet<RoundMysteryNftOrderResponse> roundOpenMysteryNftOrder(@RequestBody OrderIdRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);  // 需要登录
        APIResponse<RoundMysteryNftOrderResponse> response = memberShipRewardManageApi.roundOpenMysteryNftOrder(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/query-invite-info")
    public CommonRet<MemberShipInviteEventConfigResponse> queryInviteEventConfig(@RequestBody QueryInviteEventConfigRequest request) {

        // 不需要登陆
        APIResponse<MemberShipInviteEventConfigResponse> response = memberShipInviteManageApi.queryInviteEventConfig(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/query-user-statistic-info")
    public CommonRet<MemberShipInviteEventStatisticInfoResponse> queryUserInviteStatisticInfo(@RequestBody QueryUserInviteStatisticInfoRequest request) {

        Long userId = baseHelper.getUserId();
        request.setUserId(userId);  // 可以不需要登录
        APIResponse<MemberShipInviteEventStatisticInfoResponse> response = memberShipInviteManageApi.queryUserInviteStatisticInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }


    @PostMapping("/invite/query-user-friends-infos")
    public CommonRet<List<InviteLeaderBoardInfo>> queryInvitedFriends(@RequestBody QueryUserFriendsInfosByPageRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);  // 需要登录
        APIResponse<List<InviteLeaderBoardInfo>> response = memberShipInviteManageApi.queryInvitedFriends(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/query-invite-event-leaderboard")
    public CommonRet<MemberShipInviteLeaderBoardResponse> queryInviteEventLeaderBoard(@RequestBody CommonPageRequest<QueryUserFriendsInfosByPageRequest> request) {

        Long userId = baseHelper.getUserId();
        if (request.getParams() == null){
            request.setParams(QueryUserFriendsInfosByPageRequest.builder().userId(userId).build());
        } else {
            request.getParams().setUserId(userId);
        }

        APIResponse<MemberShipInviteLeaderBoardResponse> response = memberShipInviteManageApi.queryInviteEventLeaderBoard(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/simple-query-user-invite-info")
    public CommonRet<QueryUserInviteInfoSimpleResponse> simpleQueryUserInviteInfo(@RequestBody QueryUserInviteStatisticInfoRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        request.setUserId(userId);
        APIResponse<QueryUserInviteInfoSimpleResponse> response = memberShipInviteManageApi.simpleQueryUserInviteInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/claim-user-personal-reward")
    public CommonRet<MemberShipInviteClaimRewardResponse> claimUserPersonalPoolReward(@RequestBody MemberShipInviteClaimRequest request) {
        // 需要登录
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 需要KYC
        boolean hasKyc = fanTokenCheckHelper.hasKyc(userId);
        if (!hasKyc) {
            return new CommonRet<>();
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, hasKyc, request);

        // 合规校验
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipComplianceCheck(userId);
        if (isNonComplianceUser) {
            log.error("fantoken user is non compliance user (in claimTokenReward): [userId={}], [hasKyc={}], [isNonComplianceUser={}]",
                    userId, hasKyc, isNonComplianceUser);
            return new CommonRet<>();
        }
        APIResponse<MemberShipInviteClaimRewardResponse> response = memberShipInviteManageApi.claimUserPersonalPoolReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/claim-user-global-reward")
    public CommonRet<MemberShipInviteClaimRewardResponse> claimUserGlobalPoolReward(@RequestBody MemberShipInviteClaimRequest request) {
        // 需要登录
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 需要KYC
        boolean hasKyc = fanTokenCheckHelper.hasKyc(userId);
        if (!hasKyc) {
            return new CommonRet<>();
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, hasKyc, request);
        // 合规校验
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipComplianceCheck(userId);
        if (isNonComplianceUser) {
            log.error("fantoken user is non compliance user (in claimTokenReward): [userId={}], [hasKyc={}], [isNonComplianceUser={}]",
                    userId, hasKyc, isNonComplianceUser);
            return new CommonRet<>();
        }
        APIResponse<MemberShipInviteClaimRewardResponse> response = memberShipInviteManageApi.claimUserGlobalPoolReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/invite/user-ref")
    public CommonRet<WorldCupUserInviteResponse> userRef(@RequestBody WorldCupUserInviteRequest request) {

        Long userId = baseHelper.getUserId();

        // 一定需要是登录用户
        if (null == userId) {
            return new CommonRet<>();
        }
        // membership 风控信息
        fanTokenRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<WorldCupUserInviteResponse> response = worldCupManageApi.userRef(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/reward/user-payment-info")
    public CommonRet<MemberShipRewardUserPaymentResponse> userPaymentInfo(@RequestBody MemberShipRewardRequest request) {

        // 需要登录
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 需要KYC
        boolean hasKyc = fanTokenCheckHelper.hasKyc(userId);
        if (!hasKyc) {
            MemberShipRewardUserPaymentResponse fakeResp = MemberShipRewardUserPaymentResponse.builder()
                    .itemId(request.getItemId())
                    .canClaim(Boolean.FALSE)
                    .build();
            return new CommonRet<>(fakeResp);
        }

        fanTokenRequestHelper.initFanTokenBaseRequest(userId, hasKyc, request);

        // 合规校验
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipComplianceCheck(userId);
        if (isNonComplianceUser) {
            log.error("fantoken user is non compliance user (in userPaymentInfo): [userId={}], [hasKyc={}], [isNonComplianceUser={}]",
                    userId, hasKyc, isNonComplianceUser);
            MemberShipRewardUserPaymentResponse fakeResp = MemberShipRewardUserPaymentResponse.builder()
                    .itemId(request.getItemId())
                    .canClaim(Boolean.FALSE)
                    .build();
            return new CommonRet<>(fakeResp);
        }

        APIResponse<MemberShipRewardUserPaymentResponse> response = memberShipRewardManageApi.userPaymentInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
