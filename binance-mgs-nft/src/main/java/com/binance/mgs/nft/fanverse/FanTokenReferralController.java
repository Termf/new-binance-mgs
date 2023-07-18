package com.binance.mgs.nft.fanverse;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fanverse.helper.FanActivityRequestHelper;
import com.binance.mgs.nft.fanverse.helper.FanVerseI18nHelper;
import com.binance.nft.fantoken.activity.ifae.referral.IFanTokenActivityReferralEventManageApi;
import com.binance.nft.fantoken.activity.request.CommonPageRequest;
import com.binance.nft.fantoken.activity.request.referral.ReferralEventRequest;
import com.binance.nft.fantoken.activity.request.referral.ReferralFriendsRequest;
import com.binance.nft.fantoken.activity.response.referral.ReferralEventResponse;
import com.binance.nft.fantoken.activity.response.referral.ReferralFriendsResponse;
import com.binance.nft.fantoken.activity.response.referral.ReferralRewardClaimResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/friendly/nft/fantoken/referral")
public class FanTokenReferralController {
    private final BaseHelper baseHelper;
    private final FanActivityRequestHelper fanActivityRequestHelper;
    private final FanVerseI18nHelper fanVerseI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IFanTokenActivityReferralEventManageApi fanTokenActivityReferralEventManageApi;

    @PostMapping("/friends-info-list")
    CommonRet<ReferralFriendsResponse> queryUserReferralFriendsByPage(@RequestBody CommonPageRequest<ReferralFriendsRequest> request) {
        // 必须登录
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        ReferralFriendsRequest param = request.getParams();
        if (Objects.isNull(param)) {
            param = new ReferralFriendsRequest();
            request.setParams(param);
        }
        // 不填充kyc参数，这里hasKyc是指分页查询参数，不是用户状态
        param.setUserId(userId);

        APIResponse<ReferralFriendsResponse> response = fanTokenActivityReferralEventManageApi.queryUserReferralFriendsByPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/event-info")
    CommonRet<ReferralEventResponse> queryReferralEventInfo(@RequestBody ReferralEventRequest request) {

        fanActivityRequestHelper.initFanTokenBaseRequest(request);

        APIResponse<ReferralEventResponse> response = fanTokenActivityReferralEventManageApi.queryReferralEventInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            fanVerseI18nHelper.doReferralEventInfo(response.getData().getEventInfo());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/claim-new-user-reward")
    CommonRet<ReferralRewardClaimResponse> claimNewUserReferralEventReward(@RequestBody ReferralEventRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 强制KYC校验
        fanTokenCheckHelper.userComplianceValidate(userId);
        // SG合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        fanActivityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<ReferralRewardClaimResponse> response = fanTokenActivityReferralEventManageApi.claimNewUserReferralEventReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/claim-referral-reward")
    CommonRet<ReferralRewardClaimResponse> claimInvitorReferralEventReward(@RequestBody ReferralEventRequest request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        // 强制KYC校验
        fanTokenCheckHelper.userComplianceValidate(userId);
        // SG合规校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        fanActivityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<ReferralRewardClaimResponse> response = fanTokenActivityReferralEventManageApi.claimInvitorReferralEventReward(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
